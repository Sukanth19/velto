package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * What completing changed, so [UndoCompleteTaskUseCase] can put it back exactly.
 *
 * @property rescheduled The task as it was before a repeating task moved to its next date; null for one-off tasks
 * @property doneSubtaskIds Steps that were ticked before a repeating task reset them
 */
data class CompletionReceipt(
    val taskId: String,
    val completedAt: Long,
    val rescheduled: Task? = null,
    val doneSubtaskIds: Set<String> = emptySet()
)

/**
 * Completes a task and records it for metrics. A repeating task isn't closed: it moves to its
 * next date with its steps unticked, ready to go again.
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val performanceRepository: PerformanceRepository
) {
    /**
     * @param focusDuration Focus minutes if completed from a focus session
     */
    suspend operator fun invoke(taskId: String, focusDuration: Int? = null, today: LocalDate = LocalDate.now()): CompletionReceipt {
        val timestamp = System.currentTimeMillis()
        val task = taskRepository.getTaskById(taskId).first()
        val recurrence = task?.recurrence
        val receipt = if (task != null && recurrence != null) {
            val doneSteps = taskRepository.getSubtasks(taskId).first().filter { it.isDone }.map { it.id }.toSet()
            taskRepository.updateTask(task.copy(dueDate = recurrence.next(task.dueDate ?: today, today)))
            taskRepository.setDoneSubtasks(taskId, emptySet())
            CompletionReceipt(taskId, timestamp, rescheduled = task, doneSubtaskIds = doneSteps)
        } else {
            taskRepository.completeTask(taskId, timestamp)
            CompletionReceipt(taskId, timestamp)
        }
        performanceRepository.recordCompletion(taskId, timestamp, focusDuration)
        return receipt
    }
}
