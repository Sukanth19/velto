package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Reverses [CompleteTaskUseCase]: the task becomes active again and its completion
 * record is removed so it no longer counts in metrics.
 */
class UndoCompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val performanceRepository: PerformanceRepository
) {
    /**
     * @param completedAt The timestamp [CompleteTaskUseCase] returned
     */
    suspend operator fun invoke(taskId: String, completedAt: Long) {
        taskRepository.uncompleteTask(taskId)
        performanceRepository.removeCompletion(taskId, completedAt)
    }
}
