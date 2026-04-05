package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Use case for completing a task.
 * 
 * Marks the task as complete in the repository and records the completion
 * in the performance tracker for analytics.
 * 
 * Requirements: 1.9, 7.1
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val performanceRepository: PerformanceRepository
) {
    /**
     * Completes a task and records the completion.
     * 
     * @param taskId The ID of the task to complete
     * @param focusDuration Optional focus duration in minutes if completed via focus session
     */
    suspend operator fun invoke(taskId: String, focusDuration: Int? = null) {
        val timestamp = System.currentTimeMillis()
        
        // Mark task as complete
        taskRepository.completeTask(taskId, timestamp)
        
        // Record completion for performance tracking
        performanceRepository.recordCompletion(taskId, timestamp, focusDuration)
    }
}
