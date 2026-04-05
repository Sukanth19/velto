package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a specific task by ID.
 * 
 * Returns a Flow that emits the task if found, or null if not found.
 * 
 * Requirements: 1.1
 */
class GetTaskByIdUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Gets a task by its ID.
     * 
     * @param id The ID of the task to retrieve
     * @return Flow emitting the task if found, or null if not found
     */
    operator fun invoke(id: String): Flow<Task?> {
        return taskRepository.getTaskById(id)
    }
}
