package com.decisionexecution.app.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.EisenhowerQuadrant
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.CreateTaskUseCase
import com.decisionexecution.app.domain.usecase.DeleteTaskUseCase
import com.decisionexecution.app.domain.usecase.GetAllTasksUseCase
import com.decisionexecution.app.domain.usecase.UndoCompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.CompletionReceipt
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasksByCategory: Map<TaskCategory, List<Task>> = emptyMap(),
    val selectedCategory: TaskCategory? = null,
    val activeTaskCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** The error came from loading, so the UI can offer Retry */
    val canRetry: Boolean = false,
    /** Tasks by priority quadrant, for the matrix view (respects the category filter) */
    val matrix: Map<EisenhowerQuadrant, List<Task>> = emptyMap(),
    val viewMode: TasksViewMode = TasksViewMode.LIST,
    /** The last complete or delete, offered for undo */
    val undo: UndoAction? = null
)

enum class TasksViewMode { LIST, MATRIX }

sealed class UndoAction {
    abstract val task: Task

    data class Completed(override val task: Task, val receipt: CompletionReceipt) : UndoAction()
    data class Deleted(override val task: Task) : UndoAction()
}

/**
 * Groups tasks by category, sorted by descending score within each group.
 * Every category is present so the UI can render empty sections.
 */
fun groupTasksByCategory(tasks: List<Task>, scoringEngine: ScoringEngine): Map<TaskCategory, List<Task>> {
    val grouped = tasks.groupBy { it.category }
    return TaskCategory.entries.associateWith { category ->
        grouped[category].orEmpty().sortedWith(
            compareByDescending<Task> { scoringEngine.calculateScore(it) }.thenBy { it.createdAt }
        )
    }
}

/**
 * Keeps only the selected category's group, or all groups when no category is selected.
 */
fun filterByCategory(
    tasksByCategory: Map<TaskCategory, List<Task>>,
    category: TaskCategory?
): Map<TaskCategory, List<Task>> =
    if (category == null) tasksByCategory else tasksByCategory.filterKeys { it == category }

/**
 * ViewModel for the Tasks screen: categorized lists, filtering, complete and delete actions.
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val scoringEngine: ScoringEngine,
    private val eisenhowerClassifier: EisenhowerClassifier,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val createTaskUseCase: CreateTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(isLoading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val selectedCategory = MutableStateFlow<TaskCategory?>(null)

    private var loadJob: Job? = null
    private var allTasks: List<Task> = emptyList()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, canRetry = false) }
            combine(getAllTasksUseCase(), selectedCategory) { tasks, category ->
                allTasks = tasks
                val grouped = filterByCategory(groupTasksByCategory(tasks, scoringEngine), category)
                _uiState.value.copy(
                    tasksByCategory = grouped,
                    matrix = eisenhowerClassifier.groupByQuadrant(grouped.values.flatten())
                        .mapValues { (_, inQuadrant) -> inQuadrant.sortedByDescending(scoringEngine::calculateScore) },
                    selectedCategory = category,
                    activeTaskCount = tasks.size,
                    isLoading = false
                )
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("load your tasks"), canRetry = true) }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onTaskComplete(taskId: String) {
        val task = allTasks.find { it.id == taskId }
        viewModelScope.launch {
            try {
                val receipt = completeTaskUseCase(taskId)
                task?.let { done -> _uiState.update { it.copy(undo = UndoAction.Completed(done, receipt)) } }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("complete the task")) }
            }
        }
    }

    fun onTaskDelete(taskId: String) {
        val task = allTasks.find { it.id == taskId }
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
                task?.let { gone -> _uiState.update { it.copy(undo = UndoAction.Deleted(gone)) } }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("delete the task")) }
            }
        }
    }

    fun onCategoryFilter(category: TaskCategory?) {
        selectedCategory.value = category
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null, canRetry = false) }
    }

    fun onRetry() = loadTasks()

    fun onViewModeChange(mode: TasksViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onUndo() {
        val action = _uiState.value.undo ?: return
        _uiState.update { it.copy(undo = null) }
        viewModelScope.launch {
            try {
                when (action) {
                    is UndoAction.Completed -> undoCompleteTaskUseCase(action.receipt)
                    // Re-inserting keeps the original id and creation time, so its score is unchanged
                    is UndoAction.Deleted -> createTaskUseCase(action.task)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("undo that")) }
            }
        }
    }

    fun onUndoDismissed() {
        _uiState.update { it.copy(undo = null) }
    }
}
