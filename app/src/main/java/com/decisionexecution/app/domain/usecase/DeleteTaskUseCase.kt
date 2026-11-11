package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case for deleting a task.
 * 
 * Removes the task from the repository, making it no longer appear in any queries,
 * and takes it off the linked calendar if it was added there.
 */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarSync: CalendarSyncUseCase
) {
    /**
     * Deletes a task by ID.
     * 
     * @param taskId The ID of the task to delete
     */
    suspend operator fun invoke(taskId: String) {
        calendarSync.remove(taskId)
        taskRepository.deleteTask(taskId)
    }
}
