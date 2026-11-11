package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Reverses [CompleteTaskUseCase]: a one-off task becomes active again, a repeating one goes back
 * to its old date and ticked steps, and the completion no longer counts in metrics.
 */
class UndoCompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val performanceRepository: PerformanceRepository,
    private val calendarSync: CalendarSyncUseCase
) {
    suspend operator fun invoke(receipt: CompletionReceipt) {
        val previous = receipt.rescheduled
        if (previous != null) {
            taskRepository.updateTask(previous)
            calendarSync.refresh(previous)
            taskRepository.setDoneSubtasks(receipt.taskId, receipt.doneSubtaskIds)
        } else {
            taskRepository.uncompleteTask(receipt.taskId)
        }
        performanceRepository.removeCompletion(receipt.taskId, receipt.completedAt)
    }
}
