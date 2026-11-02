package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.DayPhase
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.service.rhythmBonus
import com.decisionexecution.app.domain.service.rhythmReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * The task Just Start would pick right now.
 *
 * @property reason Why it suits this time of day, or null when the plain priority decided it
 */
data class NextPick(val task: Task, val reason: String?)

/**
 * The best task to work on right now: the priority score, nudged by the time of day (hard tasks in
 * the user's peak hours, easy ones in a slump), leaving out anything snoozed with Not now.
 */
class GetBestTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val scoringEngine: ScoringEngine,
    private val dayPhaseSource: DayPhaseSource
) {
    operator fun invoke(): Flow<Task?> = pick().map { it?.task }

    fun pick(today: () -> LocalDate = LocalDate::now): Flow<NextPick?> =
        combine(taskRepository.getAllTasks(), dayPhaseSource.phase()) { tasks, phase ->
            val day = today()
            // If everything is snoozed, snoozing has nothing left to hide behind; show them anyway
            val candidates = tasks.filterNot { it.isSnoozed(day) }.ifEmpty { tasks }
            candidates
                .maxWithOrNull(compareBy<Task> { scoringEngine.calculateScore(it) + rhythmBonus(it, phase) }.thenBy { -it.createdAt })
                ?.let { NextPick(it, rhythmReason(it, phase)) }
        }
}

/** Where the user is in their day right now; updates as the hour turns and as new focus or energy is logged */
fun interface DayPhaseSource {
    fun phase(): Flow<DayPhase>

    companion object {
        /** For tests and previews: time of day never matters */
        val NEUTRAL = DayPhaseSource { kotlinx.coroutines.flow.flowOf(DayPhase.NEUTRAL) }
    }
}
