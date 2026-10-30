package com.decisionexecution.app.domain.service

/**
 * Everything XP and achievements are computed from. Derived from stored history rather than kept
 * as a running score, so undoing a completion takes its XP back with it.
 */
data class LifetimeStats(
    val tasksCompleted: Int = 0,
    val focusMinutes: Int = 0,
    val focusSessions: Int = 0,
    val habitCheckIns: Int = 0,
    val habitsCreated: Int = 0,
    val moodEntries: Int = 0,
    val bestHabitStreak: Int = 0,
    val activityStreak: Int = 0,
    val inboxCaptured: Int = 0
)

/**
 * Where the user is on the level curve.
 *
 * @property xpIntoLevel XP earned since reaching [level]
 * @property xpForNextLevel XP the current level takes in total
 */
data class LevelProgress(
    val level: Int,
    val totalXp: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int
) {
    val fraction: Float get() = if (xpForNextLevel > 0) xpIntoLevel.toFloat() / xpForNextLevel else 0f
    val xpToGo: Int get() = xpForNextLevel - xpIntoLevel
    val title: String get() = levelTitle(level)
}

enum class Achievement(val title: String, val description: String, val unlocked: (LifetimeStats) -> Boolean) {
    FIRST_STEP("First step", "Complete your first task", { it.tasksCompleted >= 1 }),
    GETTING_THINGS_DONE("Getting things done", "Complete 10 tasks", { it.tasksCompleted >= 10 }),
    CENTURION("Centurion", "Complete 100 tasks", { it.tasksCompleted >= 100 }),
    IN_THE_ZONE("In the zone", "Finish a focus session", { it.focusSessions >= 1 }),
    DEEP_DIVER("Deep diver", "Focus for 10 hours in total", { it.focusMinutes >= 600 }),
    HABIT_FORMING("Habit forming", "Create your first habit", { it.habitsCreated >= 1 }),
    WEEK_STRONG("Week strong", "Keep a habit going for 7 days", { it.bestHabitStreak >= 7 }),
    UNSTOPPABLE("Unstoppable", "Keep a habit going for 30 days", { it.bestHabitStreak >= 30 }),
    ON_A_ROLL("On a roll", "Be active 5 days in a row", { it.activityStreak >= 5 }),
    DEAR_DIARY("Dear diary", "Log your mood 7 times", { it.moodEntries >= 7 }),
    BRAIN_DUMP("Brain dump", "Capture 10 thoughts in the inbox", { it.inboxCaptured >= 10 }),
    LEVEL_TEN("Double digits", "Reach level 10", { levelFor(xpFor(it)).level >= 10 })
}

object Xp {
    const val PER_TASK = 20
    const val PER_FOCUS_MINUTE = 1
    const val PER_HABIT_CHECK_IN = 10
    const val PER_MOOD_ENTRY = 5
    const val PER_CAPTURE = 2
}

fun xpFor(stats: LifetimeStats): Int =
    stats.tasksCompleted * Xp.PER_TASK +
        stats.focusMinutes * Xp.PER_FOCUS_MINUTE +
        stats.habitCheckIns * Xp.PER_HABIT_CHECK_IN +
        stats.moodEntries * Xp.PER_MOOD_ENTRY +
        stats.inboxCaptured * Xp.PER_CAPTURE

/** Level 1 takes 100 XP and each level after takes 50 more than the last, so early levels come quickly */
fun xpToCompleteLevel(level: Int): Int = 100 + 50 * (level - 1)

fun levelFor(totalXp: Int): LevelProgress {
    var level = 1
    var remaining = totalXp.coerceAtLeast(0)
    while (remaining >= xpToCompleteLevel(level)) {
        remaining -= xpToCompleteLevel(level)
        level++
    }
    return LevelProgress(level, totalXp.coerceAtLeast(0), remaining, xpToCompleteLevel(level))
}

fun levelTitle(level: Int): String = when {
    level < 3 -> "Spark"
    level < 5 -> "Starter"
    level < 8 -> "Mover"
    level < 12 -> "Builder"
    level < 17 -> "Achiever"
    level < 25 -> "Machine"
    else -> "Legend"
}

fun unlockedAchievements(stats: LifetimeStats): Set<Achievement> =
    Achievement.entries.filter { it.unlocked(stats) }.toSet()
