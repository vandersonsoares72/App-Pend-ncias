package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KanbanTask
import com.example.data.TaskDraft
import com.example.data.TaskPriority
import com.example.data.TaskRepository
import com.example.data.TaskStatus
import com.example.ui.util.ScheduleBucket
import com.example.ui.util.classifyScheduleBucket
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TaskFilter {
    ALL,
    HIGH,
    MEDIUM,
    LOW,
    TODAY_OR_OVERDUE
}

enum class KanbanFeedback {
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    MOVED_PENDING,
    MOVED_STARTED,
    MOVED_DONE,
    SAVE_ERROR
}

data class BoardSnapshot(
    val allTasks: List<KanbanTask>,
    val filteredTasks: List<KanbanTask>,
    val pendingTasks: List<KanbanTask>,
    val startedTasks: List<KanbanTask>,
    val doneTasks: List<KanbanTask>,
    val overdueCount: Int,
    val totalCount: Int,
    val completedCount: Int
)

class KanbanViewModel(
    private val repository: TaskRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(TaskFilter.ALL)
    val activeFilter: StateFlow<TaskFilter> = _activeFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _feedback = MutableSharedFlow<KanbanFeedback>(extraBufferCapacity = 4)
    val feedback: SharedFlow<KanbanFeedback> = _feedback.asSharedFlow()

    private val rawTasksState: StateFlow<UiState<List<KanbanTask>>> =
        repository.observeTasks(currentUserId)
            .map<List<KanbanTask>, UiState<List<KanbanTask>>> { tasks ->
                UiState.Success(tasks)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing Kanban tasks", rootError)
                emit(UiState.Error(rootError.message ?: "Erro ao carregar tarefas"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val boardState: StateFlow<UiState<BoardSnapshot>> = combine(
        rawTasksState,
        _searchQuery,
        _activeFilter
    ) { state, query, filter ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(state.message)
            is UiState.Success -> {
                val now = System.currentTimeMillis()
                val all = state.data
                val trimmedQuery = query.trim().lowercase()

                val filtered = all.filter { task ->
                    val matchesSearch = trimmedQuery.isEmpty() ||
                        task.title.lowercase().contains(trimmedQuery) ||
                        task.description.lowercase().contains(trimmedQuery) ||
                        task.category.lowercase().contains(trimmedQuery)

                    val matchesFilter = when (filter) {
                        TaskFilter.ALL -> true
                        TaskFilter.HIGH -> task.priorityEnum() == TaskPriority.ALTA
                        TaskFilter.MEDIUM -> task.priorityEnum() == TaskPriority.MEDIA
                        TaskFilter.LOW -> task.priorityEnum() == TaskPriority.BAIXA
                        TaskFilter.TODAY_OR_OVERDUE -> {
                            val bucket = classifyScheduleBucket(task, now)
                            bucket == ScheduleBucket.OVERDUE || bucket == ScheduleBucket.TODAY
                        }
                    }

                    matchesSearch && matchesFilter
                }

                val comparator = compareBy<KanbanTask>(
                    { it.priorityEnum().sortWeight },
                    { it.scheduledAt?.toDate()?.time ?: Long.MAX_VALUE },
                    { -(it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
                )

                val pending = filtered
                    .filter { it.statusEnum() == TaskStatus.PENDENTE }
                    .sortedWith(comparator)
                val started = filtered
                    .filter { it.statusEnum() == TaskStatus.INICIADO }
                    .sortedWith(comparator)
                val done = filtered
                    .filter { it.statusEnum() == TaskStatus.CONCLUIDO }
                    .sortedByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }

                val overdueCount = all.count {
                    classifyScheduleBucket(it, now) == ScheduleBucket.OVERDUE
                }

                UiState.Success(
                    BoardSnapshot(
                        allTasks = all,
                        filteredTasks = filtered,
                        pendingTasks = pending,
                        startedTasks = started,
                        doneTasks = done,
                        overdueCount = overdueCount,
                        totalCount = all.size,
                        completedCount = all.count { it.statusEnum() == TaskStatus.CONCLUIDO }
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: TaskFilter) {
        _activeFilter.value = filter
    }

    fun saveTask(draft: TaskDraft, onSuccess: () -> Unit = {}) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (draft.id.isNullOrBlank()) {
                repository.createTask(currentUserId, draft)
            } else {
                repository.updateTask(currentUserId, draft.id, draft)
            }
            _isSaving.value = false
            if (result.isSuccess) {
                _feedback.tryEmit(
                    if (draft.id.isNullOrBlank()) KanbanFeedback.TASK_CREATED
                    else KanbanFeedback.TASK_UPDATED
                )
                onSuccess()
            } else {
                _feedback.tryEmit(KanbanFeedback.SAVE_ERROR)
            }
        }
    }

    fun moveTask(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            val result = repository.moveTaskStatus(currentUserId, taskId, newStatus)
            if (result.isSuccess) {
                val event = when (newStatus) {
                    TaskStatus.PENDENTE -> KanbanFeedback.MOVED_PENDING
                    TaskStatus.INICIADO -> KanbanFeedback.MOVED_STARTED
                    TaskStatus.CONCLUIDO -> KanbanFeedback.MOVED_DONE
                }
                _feedback.tryEmit(event)
            } else {
                _feedback.tryEmit(KanbanFeedback.SAVE_ERROR)
            }
        }
    }

    fun deleteTask(taskId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteTask(currentUserId, taskId)
            _isSaving.value = false
            if (result.isSuccess) {
                _feedback.tryEmit(KanbanFeedback.TASK_DELETED)
                onSuccess()
            } else {
                _feedback.tryEmit(KanbanFeedback.SAVE_ERROR)
            }
        }
    }

    private companion object {
        const val TAG = "KanbanVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
