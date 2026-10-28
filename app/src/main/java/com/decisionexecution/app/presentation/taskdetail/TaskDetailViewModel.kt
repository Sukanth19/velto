package com.decisionexecution.app.presentation.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.DeleteTaskUseCase
import com.decisionexecution.app.domain.usecase.GetTaskByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the task detail screen. `isFinished` tells the UI to navigate back.
 *
 * Requirements: 1.7, 1.8
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getTaskByIdUseCase: GetTaskByIdUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle[TASK_ID_ARG])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTaskByIdUseCase(taskId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load task") } }
                .collect { task -> _uiState.update { it.copy(task = task, isLoading = false) } }
        }
    }

    fun onComplete() = finishWith { completeTaskUseCase(taskId) }

    fun onDelete() = finishWith { deleteTaskUseCase(taskId) }

    private fun finishWith(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                _uiState.update { it.copy(isFinished = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Something went wrong") }
            }
        }
    }

    companion object {
        const val TASK_ID_ARG = "taskId"
    }
}
