package com.decisionexecution.app.presentation.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.repository.TaskRepository
import java.time.LocalDate
import com.decisionexecution.app.domain.repository.InboxRepository
import com.decisionexecution.app.domain.usecase.CreateTaskUseCase
import com.decisionexecution.app.domain.usecase.GetTaskByIdUseCase
import com.decisionexecution.app.domain.usecase.UpdateTaskUseCase
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddEditTaskUiState(
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val category: TaskCategory = TaskCategory.PERSONAL,
    val urgency: Int = 3,
    val importance: Int = 3,
    val effort: TaskEffort = TaskEffort.MEDIUM,
    val energyTag: EnergyTag? = null,
    val dueDate: LocalDate? = null,
    val recurrence: Recurrence? = null,
    val steps: List<Subtask> = emptyList(),
    val stepDraft: String = "",
    val titleError: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false
)

/**
 * ViewModel for creating or editing a task. Editing keeps the original id and creation time.
 * A task made from an inbox item starts with its text and clears the item once saved.
 */
@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val inboxRepository: InboxRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskId: String? = savedStateHandle[TASK_ID_ARG]
    private val inboxItemId: String? = savedStateHandle[INBOX_ID_ARG]
    private var original: Task? = null

    private val _uiState = MutableStateFlow(
        AddEditTaskUiState(isEditing = taskId != null, title = savedStateHandle.get<String>(TITLE_ARG).orEmpty())
    )
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    init {
        if (taskId != null) {
            viewModelScope.launch {
                val task = getTaskByIdUseCase(taskId).first()
                if (task == null) {
                    _uiState.update { it.copy(error = "Task not found") }
                    return@launch
                }
                original = task
                _uiState.update {
                    it.copy(
                        title = task.title,
                        description = task.description.orEmpty(),
                        category = task.category,
                        urgency = task.urgency,
                        importance = task.importance,
                        effort = task.effort,
                        energyTag = task.energyTag,
                        dueDate = task.dueDate,
                        recurrence = task.recurrence,
                        steps = taskRepository.getSubtasks(task.id).first()
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onCategoryChange(value: TaskCategory) = _uiState.update { it.copy(category = value) }
    fun onUrgencyChange(value: Int) = _uiState.update { it.copy(urgency = value.coerceIn(1, 5)) }
    fun onImportanceChange(value: Int) = _uiState.update { it.copy(importance = value.coerceIn(1, 5)) }
    fun onEffortChange(value: TaskEffort) = _uiState.update { it.copy(effort = value) }
    fun onEnergyTagChange(value: EnergyTag?) = _uiState.update { it.copy(energyTag = value) }
    fun onDueDateChange(value: LocalDate?) = _uiState.update { it.copy(dueDate = value) }
    fun onRecurrenceChange(value: Recurrence?) = _uiState.update { it.copy(recurrence = value) }
    fun onStepDraftChange(value: String) = _uiState.update { it.copy(stepDraft = value) }

    fun onAddStep() {
        val title = _uiState.value.stepDraft.trim()
        if (title.isEmpty()) return
        _uiState.update {
            it.copy(steps = it.steps + Subtask(UUID.randomUUID().toString(), taskId.orEmpty(), title, false, it.steps.size), stepDraft = "")
        }
    }

    fun onRemoveStep(stepId: String) = _uiState.update { state -> state.copy(steps = state.steps.filterNot { it.id == stepId }) }

    fun onSave() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            return
        }
        viewModelScope.launch {
            try {
                val existing = original
                val task = Task(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    title = state.title.trim(),
                    description = state.description.trim().ifEmpty { null },
                    category = state.category,
                    urgency = state.urgency,
                    importance = state.importance,
                    effort = state.effort,
                    energyTag = state.energyTag,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    // A repeating task needs a date to repeat from; start today
                    dueDate = state.dueDate ?: LocalDate.now().takeIf { state.recurrence != null },
                    recurrence = state.recurrence
                )
                if (existing != null) updateTaskUseCase(task) else createTaskUseCase(task)
                // Also catches a step typed but never added with the + button
                val pending = state.stepDraft.trim().takeIf { it.isNotEmpty() }
                    ?.let { Subtask(UUID.randomUUID().toString(), task.id, it, false, state.steps.size) }
                val steps = state.steps + listOfNotNull(pending)
                if (steps.isNotEmpty() || existing != null) taskRepository.replaceSubtasks(task.id, steps)
                inboxItemId?.let { inboxRepository.delete(it) }
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("save the task")) }
            }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    companion object {
        const val TASK_ID_ARG = "taskId"
        const val TITLE_ARG = "title"
        const val INBOX_ID_ARG = "inboxId"
    }
}
