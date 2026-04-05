package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case for deleting a task.
 * 
 * Removes the task from the repository, making it no longer appear in any queries.
 * 
 * Requirements: 1.8
 */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Deletes a task by ID.
     * 
     * @param taskId The ID of the task to delete
     */
    suspend operator fun invoke(taskId: String) {
        taskRepository.deleteTask(taskId)
    }
}
