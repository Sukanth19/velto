package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.FocusSessionManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Not now: hides the task from Just Start until tomorrow, and counts as a dodge */
class SnoozeTaskUseCase @Inject constructor(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(task: Task, today: LocalDate = LocalDate.now()) {
        taskRepository.snooze(task.id, today.plusDays(1))
    }
}

/**
 * For a task the user keeps dodging: puts a tiny first step at the top of its steps and starts a
 * two-minute starter session on it. Starting is the hard part; once the two minutes are up the
 * user picks whether to keep going.
 */
class ShrinkTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val focusSessionManager: FocusSessionManager
) {
    suspend operator fun invoke(task: Task, firstStep: String?) {
        val step = firstStep?.trim().orEmpty()
        if (step.isNotEmpty()) {
            val existing = taskRepository.getSubtasks(task.id).first()
            taskRepository.replaceSubtasks(task.id, listOf(Subtask(UUID.randomUUID().toString(), task.id, step, false, 0)) + existing)
        }
        taskRepository.clearDodges(task.id)
        focusSessionManager.startStarter(task)
    }

    companion object {
        /** Ideas for a first step small enough to not be scary */
        val SUGGESTIONS = listOf(
            "Open it up",
            "Write one messy sentence",
            "Get out what you need",
            "List the next three steps",
            "Do the worst version"
        )
    }
}
