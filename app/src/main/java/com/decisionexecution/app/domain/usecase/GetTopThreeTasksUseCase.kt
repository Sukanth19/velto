package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.ScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting the top three tasks to work on.
 * 
 * Retrieves all active tasks and uses the scoring engine to determine
 * the three highest-priority tasks in descending order by score.
 * 
 * Requirements: 2.1, 2.2, 2.3
 */
class GetTopThreeTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val scoringEngine: ScoringEngine
) {
    /**
     * Gets the top three tasks to work on.
     * 
     * @return Flow emitting a list of up to three tasks in descending priority order
     */
    operator fun invoke(): Flow<List<Task>> {
        return taskRepository.getAllTasks().map { tasks ->
            scoringEngine.getTopThreeTasks(tasks)
        }
    }
}
