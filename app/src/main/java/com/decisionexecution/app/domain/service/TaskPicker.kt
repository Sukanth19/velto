package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskEffort

/**
 * What the user has to work with right now: how long, and how much energy (1-5, null if unknown).
 */
data class PickContext(val minutes: Int, val energy: Int? = null)

/**
 * Picks for the moment rather than in general: the plain priority score, nudged towards quick
 * wins when time is short, deep work when there's plenty of it, and gentle tasks on low energy.
 */
class TaskPicker(private val scoringEngine: ScoringEngine = ScoringEngine()) {

    fun score(task: Task, context: PickContext): Double {
        var score = scoringEngine.calculateScore(task)
        when {
            context.minutes <= SHORT_MINUTES -> score += when (task.effort) {
                TaskEffort.LOW -> 3.0
                TaskEffort.MEDIUM -> 0.0
                TaskEffort.HIGH -> -4.0
            }
            context.minutes >= LONG_MINUTES -> {
                if (task.effort == TaskEffort.HIGH) score += 2.0
                if (task.energyTag == EnergyTag.DEEP_WORK) score += 1.5
            }
            task.effort == TaskEffort.HIGH -> score -= 1.0
        }
        val energy = context.energy ?: return score
        if (energy <= LOW_ENERGY) {
            if (task.effort == TaskEffort.LOW) score += 2.0
            if (task.effort == TaskEffort.HIGH) score -= 3.0
            if (task.energyTag == EnergyTag.LOW) score += 2.0
            if (task.energyTag == EnergyTag.DEEP_WORK) score -= 3.0
        } else if (energy >= HIGH_ENERGY) {
            if (task.energyTag == EnergyTag.DEEP_WORK) score += 2.0
            if (task.effort == TaskEffort.HIGH) score += 1.0
        }
        return score
    }

    /** Best fit first; ties go to the older task, like the main ranking */
    fun rank(tasks: List<Task>, context: PickContext): List<Task> =
        tasks.sortedWith(compareByDescending<Task> { score(it, context) }.thenBy { it.createdAt })

    /** A short line on why this one, shown under the pick */
    fun reason(task: Task, context: PickContext): String {
        val energy = context.energy
        return when {
            energy != null && energy <= LOW_ENERGY && task.effort != TaskEffort.HIGH -> "Gentle one for a low-energy day"
            energy != null && energy >= HIGH_ENERGY && task.energyTag == EnergyTag.DEEP_WORK -> "You're charged. Perfect for deep work"
            context.minutes <= SHORT_MINUTES && task.effort == TaskEffort.LOW -> "Quick win that fits in ${context.minutes} minutes"
            context.minutes >= LONG_MINUTES && task.effort == TaskEffort.HIGH -> "Enough time to make real progress on this"
            task.urgency >= 4 && task.importance >= 4 -> "Urgent and important. Get it off your plate"
            else -> "Best fit for right now"
        }
    }

    companion object {
        const val SHORT_MINUTES = 20
        const val LONG_MINUTES = 45
        const val LOW_ENERGY = 2
        const val HIGH_ENERGY = 4
    }
}
