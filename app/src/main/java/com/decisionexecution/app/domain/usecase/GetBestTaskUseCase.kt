package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.ScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting the best task to work on right now.
 * 
 * Retrieves all active tasks and uses the scoring engine to determine
 * the highest-priority task based on urgency, importance, effort, and energy.
 * 
 * Requirements: 2.1, 2.2, 2.3
 */
class GetBestTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val scoringEngine: ScoringEngine
) {
    /**
     * Gets the best task to work on.
     * 
     * @return Flow emitting the best task, or null if no tasks are available
     */
    operator fun invoke(): Flow<Task?> {
        return taskRepository.getAllTasks().map { tasks ->
            scoringEngine.getBestTask(tasks)
        }
    }
}
