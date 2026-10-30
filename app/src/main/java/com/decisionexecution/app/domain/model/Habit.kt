package com.decisionexecution.app.domain.model

import java.time.LocalDate

/**
 * A daily habit. Check-ins are stored per calendar day, so a habit is either done today or not.
 */
data class Habit(
    val id: String,
    val name: String,
    val icon: HabitIcon,
    val createdAt: Long
)

enum class HabitIcon { SPARK, WATER, READ, MOVE, MIND, SLEEP, FOOD, CODE, MUSIC }

/**
 * A habit plus the days it was done, newest first, enough for streaks and the week strip.
 */
data class HabitWithHistory(
    val habit: Habit,
    val doneDays: Set<LocalDate>
) {
    fun isDoneOn(day: LocalDate): Boolean = day in doneDays

    /** Consecutive done days up to today; an unchecked today doesn't break it until the day is over */
    fun streak(today: LocalDate): Int {
        var day = if (isDoneOn(today)) today else today.minusDays(1)
        var streak = 0
        while (isDoneOn(day)) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
