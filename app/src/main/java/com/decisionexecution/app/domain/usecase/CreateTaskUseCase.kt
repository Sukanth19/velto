package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case for creating a new task with validation.
 * 
 * Validates task attributes (urgency, importance) through the Task domain model's
 * init block, then persists the task to the repository.
 * 
 * Requirements: 1.1, 1.7
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Creates a new task.
     * 
     * @param task The task to create
     * @throws IllegalArgumentException if task attributes are invalid
     */
    suspend operator fun invoke(task: Task) {
        // Validation happens in Task's init block
        taskRepository.insertTask(task)
    }
}
