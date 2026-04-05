package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all active tasks.
 * 
 * Returns a Flow of all tasks that have not been completed.
 * 
 * Requirements: 1.1
 */
class GetAllTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Gets all active tasks.
     * 
     * @return Flow emitting the list of all active tasks
     */
    operator fun invoke(): Flow<List<Task>> {
        return taskRepository.getAllTasks()
    }
}
