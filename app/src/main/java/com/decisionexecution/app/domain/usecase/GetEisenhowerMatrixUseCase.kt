package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.EisenhowerQuadrant
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for getting tasks organized by Eisenhower Matrix quadrants.
 * 
 * Retrieves all active tasks and groups them by their Eisenhower quadrant
 * based on urgency and importance ratings.
 * 
 * Requirements: 6.1, 6.6
 */
class GetEisenhowerMatrixUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val eisenhowerClassifier: EisenhowerClassifier
) {
    /**
     * Gets tasks grouped by Eisenhower quadrant.
     * 
     * @return Flow emitting a map of EisenhowerQuadrant to list of tasks
     */
    operator fun invoke(): Flow<Map<EisenhowerQuadrant, List<Task>>> {
        return taskRepository.getAllTasks().map { tasks ->
            eisenhowerClassifier.groupByQuadrant(tasks)
        }
    }
}
