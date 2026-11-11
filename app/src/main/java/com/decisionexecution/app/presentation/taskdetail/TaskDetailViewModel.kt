package com.decisionexecution.app.presentation.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.CalendarRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.usecase.CalendarResult
import com.decisionexecution.app.domain.usecase.CalendarSyncUseCase
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.DeleteTaskUseCase
import com.decisionexecution.app.domain.usecase.GetTaskByIdUseCase
import com.decisionexecution.app.presentation.common.toUserMessage
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
    val steps: List<Subtask> = emptyList(),
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    /** A calendar is linked, so the task can go straight onto it */
    val calendarLinked: Boolean = false,
    /** The task already has an event on the linked calendar */
    val onCalendar: Boolean = false,
    /** Result of adding to or removing from the calendar, shown once */
    val calendarMessage: String? = null,
    val error: String? = null
)

/**
 * ViewModel for the task detail screen: the task, its steps to tick off, complete and delete.
 * `isFinished` tells the UI to navigate back.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getTaskByIdUseCase: GetTaskByIdUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val taskRepository: TaskRepository,
    calendarRepository: CalendarRepository,
    private val calendarSync: CalendarSyncUseCase
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle[TASK_ID_ARG])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTaskByIdUseCase(taskId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("load the task")) } }
                .collect { task -> _uiState.update { it.copy(task = task, isLoading = false) } }
        }
        viewModelScope.launch {
            taskRepository.getSubtasks(taskId).catch { }.collect { steps -> _uiState.update { it.copy(steps = steps) } }
        }
        viewModelScope.launch {
            calendarRepository.settings().catch { }.collect { settings -> _uiState.update { it.copy(calendarLinked = settings.isLinked) } }
        }
        viewModelScope.launch {
            calendarRepository.tasksOnCalendar().catch { }.collect { ids -> _uiState.update { it.copy(onCalendar = taskId in ids) } }
        }
    }

    fun onComplete() = finishWith("complete the task") { completeTaskUseCase(taskId) }

    fun onToggleStep(subtaskId: String, done: Boolean) {
        viewModelScope.launch { runCatching { taskRepository.setSubtaskDone(subtaskId, done) } }
    }

    fun onAddToCalendar() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            val message = when (calendarSync.add(task)) {
                CalendarResult.ADDED -> "Added to your calendar"
                CalendarResult.NOT_LINKED -> "Link a calendar in Settings first"
                CalendarResult.NO_DUE_DATE -> "Give it a due date first"
                CalendarResult.NO_PERMISSION -> "Velto can't reach your calendar. Link it again in Settings"
                CalendarResult.FAILED -> "Couldn't add it to your calendar. Please try again."
            }
            _uiState.update { it.copy(calendarMessage = message) }
        }
    }

    fun onRemoveFromCalendar() {
        viewModelScope.launch {
            calendarSync.remove(taskId)
            _uiState.update { it.copy(calendarMessage = "Removed from your calendar") }
        }
    }

    fun onCalendarMessageShown() = _uiState.update { it.copy(calendarMessage = null) }

    fun onDelete() = finishWith("delete the task") { deleteTaskUseCase(taskId) }

    private fun finishWith(description: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                _uiState.update { it.copy(isFinished = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage(description)) }
            }
        }
    }

    companion object {
        const val TASK_ID_ARG = "taskId"
    }
}
