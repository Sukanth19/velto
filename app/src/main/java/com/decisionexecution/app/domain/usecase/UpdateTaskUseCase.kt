package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case for updating an existing task with validation.
 * 
 * Validates task attributes (urgency, importance) through the Task domain model's
 * init block, then persists the updated task to the repository.
 * 
 * Requirements: 1.7
 */
class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Updates an existing task.
     * 
     * @param task The task with updated attributes
     * @throws IllegalArgumentException if task attributes are invalid
     */
    suspend operator fun invoke(task: Task) {
        // Validation happens in Task's init block
        taskRepository.updateTask(task)
    }
}
