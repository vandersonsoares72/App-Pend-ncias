package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskStatus(val wireValue: String) {
    PENDENTE("PENDENTE"),
    INICIADO("INICIADO"),
    CONCLUIDO("CONCLUIDO");

    companion object {
        fun fromWire(raw: String?): TaskStatus =
            entries.firstOrNull { it.wireValue == raw } ?: PENDENTE
    }
}

enum class TaskPriority(val wireValue: String, val sortWeight: Int) {
    ALTA("ALTA", 0),
    MEDIA("MEDIA", 1),
    BAIXA("BAIXA", 2);

    companion object {
        fun fromWire(raw: String?): TaskPriority =
            entries.firstOrNull { it.wireValue == raw } ?: MEDIA
    }
}

@Keep
@IgnoreExtraProperties
data class KanbanTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = TaskStatus.PENDENTE.wireValue,
    val priority: String = TaskPriority.MEDIA.wireValue,
    val category: String = "Trabalho",
    val scheduledAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): TaskStatus = TaskStatus.fromWire(status)
    fun priorityEnum(): TaskPriority = TaskPriority.fromWire(priority)
}

data class TaskDraft(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDENTE,
    val priority: TaskPriority = TaskPriority.MEDIA,
    val category: String = "Trabalho",
    val scheduledAtMillis: Long? = null
)
