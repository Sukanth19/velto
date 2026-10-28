package com.decisionexecution.app.presentation.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.usecase.CreateTaskUseCase
import com.decisionexecution.app.domain.usecase.GetTaskByIdUseCase
import com.decisionexecution.app.domain.usecase.UpdateTaskUseCase
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
    val titleError: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false
)

/**
 * ViewModel for creating or editing a task. Editing keeps the original id and creation time.
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
 */
@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : ViewModel() {

    private val taskId: String? = savedStateHandle[TASK_ID_ARG]
    private var original: Task? = null

    private val _uiState = MutableStateFlow(AddEditTaskUiState(isEditing = taskId != null))
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
                        energyTag = task.energyTag
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
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                if (existing != null) updateTaskUseCase(task) else createTaskUseCase(task)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(error = e.message ?: "Invalid task") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save task") }
            }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    companion object {
        const val TASK_ID_ARG = "taskId"
    }
}
