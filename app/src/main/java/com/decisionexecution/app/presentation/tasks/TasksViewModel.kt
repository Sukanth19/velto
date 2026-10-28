package com.decisionexecution.app.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.DeleteTaskUseCase
import com.decisionexecution.app.domain.usecase.GetAllTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null
)

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
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.6, 9.7
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val scoringEngine: ScoringEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState(isLoading = true))
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val selectedCategory = MutableStateFlow<TaskCategory?>(null)

    init {
        viewModelScope.launch {
            combine(getAllTasksUseCase(), selectedCategory) { tasks, category ->
                _uiState.value.copy(
                    tasksByCategory = filterByCategory(groupTasksByCategory(tasks, scoringEngine), category),
                    selectedCategory = category,
                    activeTaskCount = tasks.size,
                    isLoading = false
                )
            }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load tasks") } }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onTaskComplete(taskId: String) {
        viewModelScope.launch {
            try {
                completeTaskUseCase(taskId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to complete task") }
            }
        }
    }

    fun onTaskDelete(taskId: String) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete task") }
            }
        }
    }

    fun onCategoryFilter(category: TaskCategory?) {
        selectedCategory.value = category
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }
}
