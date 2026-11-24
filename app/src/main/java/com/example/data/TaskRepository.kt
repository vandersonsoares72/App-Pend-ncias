package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class TaskRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) {
            "User must be signed in with Google before accessing Firestore."
        }
        return uid
    }

    private fun tasksCollection(userId: String) =
        db.collection("users").document(userId).collection("tasks")

    fun observeTasks(userId: String): Flow<List<KanbanTask>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user does not match requested collection owner." }
        val path = "users/$uid/tasks"
        emitAll(
            tasksCollection(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        KanbanTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(userId: String, draft: TaskDraft): Result<Unit> {
        var docPath = "users/$userId/tasks"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user task create." }

            val cleanTitle = draft.title.trim()
            val cleanDesc = draft.description.trim()
            val cleanCategory = draft.category.trim().ifBlank { "Trabalho" }.take(40)
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 120) {
                "Task title must be between 1 and 120 characters."
            }
            require(cleanDesc.length <= 500) {
                "Task description must be at most 500 characters."
            }

            val docRef = tasksCollection(uid).document()
            docPath = docRef.path

            val rawPayload: Map<String, Any?> = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "status" to draft.status.wireValue,
                "priority" to draft.priority.wireValue,
                "category" to cleanCategory,
                "scheduledAt" to draft.scheduledAtMillis?.let { Timestamp(Date(it)) },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val payload = rawPayload.filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTask(userId: String, taskId: String, draft: TaskDraft): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user task update." }
            require(taskId.isNotBlank()) { "Task ID must not be blank." }

            val cleanTitle = draft.title.trim()
            val cleanDesc = draft.description.trim()
            val cleanCategory = draft.category.trim().ifBlank { "Trabalho" }.take(40)
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 120) {
                "Task title must be between 1 and 120 characters."
            }
            require(cleanDesc.length <= 500) {
                "Task description must be at most 500 characters."
            }

            val updates: Map<String, Any?> = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "status" to draft.status.wireValue,
                "priority" to draft.priority.wireValue,
                "category" to cleanCategory,
                "scheduledAt" to draft.scheduledAtMillis?.let { Timestamp(Date(it)) },
                "updatedAt" to FieldValue.serverTimestamp()
            )

            tasksCollection(uid).document(taskId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun moveTaskStatus(userId: String, taskId: String, newStatus: TaskStatus): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user status update." }
            require(taskId.isNotBlank()) { "Task ID must not be blank." }

            tasksCollection(uid).document(taskId).update(
                mapOf(
                    "status" to newStatus.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user task deletion." }
            require(taskId.isNotBlank()) { "Task ID must not be blank." }

            tasksCollection(uid).document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
