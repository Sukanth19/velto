package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.usecase.bestFocusWindow

/** Where right now sits in the user's day, judged from when they usually focus and today's energy */
enum class DayPhase {
    /** Inside their best focus window: hand them the hard stuff */
    PEAK,
    /** Low energy logged today, or an hour they work in but rarely get much done: keep it light */
    SLUMP,
    NEUTRAL
}

/**
 * @param byHour Focus minutes that started in each hour of the day, recent history
 * @param energy Today's logged energy, 1-5, or null if not logged
 */
fun dayPhase(byHour: IntArray, hour: Int, energy: Int?): DayPhase {
    if (energy != null && energy <= TaskPicker.LOW_ENERGY) return DayPhase.SLUMP
    val window = bestFocusWindow(byHour) ?: return DayPhase.NEUTRAL
    if ((0 until window.hours).any { (window.startHour + it) % 24 == hour }) return DayPhase.PEAK
    // An hour they do focus in, just not much: a known slow stretch. Hours with no history say nothing.
    val activeHours = byHour.count { it > 0 }
    val busiest = byHour.max()
    if (activeHours >= MIN_ACTIVE_HOURS && byHour[hour] in 1..(busiest * SLUMP_SHARE).toInt()) return DayPhase.SLUMP
    return DayPhase.NEUTRAL
}

/** Nudges the score towards deep work at peak times and gentle tasks in a slump */
fun rhythmBonus(task: Task, phase: DayPhase): Double = when (phase) {
    DayPhase.PEAK -> {
        var bonus = 0.0
        if (task.energyTag == EnergyTag.DEEP_WORK) bonus += 2.0
        if (task.effort == TaskEffort.HIGH) bonus += 1.5
        if (task.effort == TaskEffort.LOW) bonus -= 0.5
        bonus
    }
    DayPhase.SLUMP -> {
        var bonus = 0.0
        if (task.effort == TaskEffort.LOW) bonus += 2.0
        if (task.energyTag == EnergyTag.LOW) bonus += 2.0
        if (task.energyTag == EnergyTag.DEEP_WORK) bonus -= 2.5
        if (task.effort == TaskEffort.HIGH) bonus -= 2.0
        bonus
    }
    DayPhase.NEUTRAL -> 0.0
}

/** Why this pick suits the time of day, or null when the time of day didn't matter */
fun rhythmReason(task: Task, phase: DayPhase): String? = when {
    phase == DayPhase.PEAK && (task.energyTag == EnergyTag.DEEP_WORK || task.effort == TaskEffort.HIGH) ->
        "Your peak focus hours. Hard stuff first"
    phase == DayPhase.SLUMP && task.effort != TaskEffort.HIGH && task.energyTag != EnergyTag.DEEP_WORK ->
        "An easy one for a slower stretch"
    else -> null
}

private const val MIN_ACTIVE_HOURS = 4
private const val SLUMP_SHARE = 0.25
