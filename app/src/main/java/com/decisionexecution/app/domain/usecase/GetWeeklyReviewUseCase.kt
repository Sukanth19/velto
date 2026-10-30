package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.FocusSessionRecord
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

/** Totals for one stretch of days */
data class PeriodTotals(
    val tasksDone: Int,
    val focusMinutes: Int,
    val habitCheckIns: Int,
    /** Check-ins out of every habit-day that was possible, 0 to 1; null without habits */
    val habitRate: Float?,
    val averageMood: Double?
)

/**
 * The last 7 days, compared with the 7 before, plus what stood out and one thing to try next.
 */
data class WeeklyReview(
    val start: LocalDate,
    val end: LocalDate,
    val thisWeek: PeriodTotals,
    val lastWeek: PeriodTotals,
    val bestDay: LocalDate?,
    val wins: List<String>,
    val focusWindow: FocusWindow?,
    /** Focus minutes by starting hour over the last month, 24 entries */
    val focusByHour: List<Int>,
    val suggestion: String
) {
    val isEmpty: Boolean get() = thisWeek.tasksDone == 0 && thisWeek.focusMinutes == 0 && thisWeek.habitCheckIns == 0

    /** Percent change in focus time; null when there's nothing last week to compare with */
    val focusChange: Int? get() = percentChange(lastWeek.focusMinutes, thisWeek.focusMinutes)
    val tasksChange: Int? get() = percentChange(lastWeek.tasksDone, thisWeek.tasksDone)
}

internal fun percentChange(before: Int, after: Int): Int? =
    if (before == 0) null else ((after - before) * 100.0 / before).roundToInt()

/**
 * When focus happens: minutes per hour of the day, and the two-hour window with the most.
 *
 * @property startHour First hour of the best window, 0-23
 * @property hours How many hours it spans: [WINDOW_HOURS], or 1 when only one of them saw any focus
 */
data class FocusWindow(val startHour: Int, val hours: Int, val minutes: Int) {
    val endHour: Int get() = (startHour + hours) % 24

    companion object {
        const val WINDOW_HOURS = 2
    }
}

/** Minutes of focus that started in each hour of the day, using each session's start time */
fun focusByHour(sessions: List<FocusSessionRecord>, zone: ZoneId): IntArray {
    val hours = IntArray(24)
    sessions.forEach { session ->
        val start = Instant.ofEpochMilli(session.completedAt - session.durationMinutes * 60_000L).atZone(zone)
        hours[start.hour] += session.durationMinutes
    }
    return hours
}

/** The best two-hour window, wrapping past midnight; null until there's an hour of focus to go on */
fun bestFocusWindow(byHour: IntArray): FocusWindow? {
    if (byHour.sum() < MIN_FOCUS_FOR_WINDOW) return null
    val best = (0 until 24).maxBy { start -> (0 until FocusWindow.WINDOW_HOURS).sumOf { byHour[(start + it) % 24] } }
    val minutes = (0 until FocusWindow.WINDOW_HOURS).sumOf { byHour[(best + it) % 24] }
    // Two windows can tie on one busy hour; trim the empty hour so it reads "9am–10am", not "8am–10am"
    return when {
        byHour[best] == 0 -> FocusWindow((best + 1) % 24, 1, minutes)
        byHour[(best + 1) % 24] == 0 -> FocusWindow(best, 1, minutes)
        else -> FocusWindow(best, FocusWindow.WINDOW_HOURS, minutes)
    }
}

private const val MIN_FOCUS_FOR_WINDOW = 60

class GetWeeklyReviewUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository,
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository
) {
    operator fun invoke(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Flow<WeeklyReview> {
        // Two weeks to compare, and a month of sessions for a steadier read on focus hours
        val historyStart = today.minusDays(FOCUS_HISTORY_DAYS - 1)
        val start = historyStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return combine(
            performanceRepository.getCompletionsInPeriod(start, end),
            performanceRepository.getFocusSessionsInPeriod(start, end),
            habitRepository.getHabits(today.minusDays(13)),
            moodRepository.getEntries(today.minusDays(13))
        ) { completions, sessions, habits, moods ->
            buildWeeklyReview(today, zone, completions, sessions, habits, moods)
        }
    }

    companion object {
        const val FOCUS_HISTORY_DAYS = 30L
    }
}

fun buildWeeklyReview(
    today: LocalDate,
    zone: ZoneId,
    completions: List<TaskCompletion>,
    sessions: List<FocusSessionRecord>,
    habits: List<HabitWithHistory>,
    moods: List<MoodEntry>
): WeeklyReview {
    fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val thisStart = today.minusDays(6)
    val lastStart = today.minusDays(13)
    val lastEnd = today.minusDays(7)

    fun totals(from: LocalDate, to: LocalDate): PeriodTotals {
        fun inRange(day: LocalDate) = !day.isBefore(from) && !day.isAfter(to)
        val checkIns = habits.sumOf { h -> h.doneDays.count(::inRange) }
        val possible = habits.sumOf { h ->
            // A habit created mid-week only counts from the day it was made
            val created = dayOf(h.habit.createdAt)
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.count { !it.isBefore(created) }
        }
        val dayMoods = moods.filter { inRange(it.day) }
        return PeriodTotals(
            tasksDone = completions.count { inRange(dayOf(it.completedAt)) },
            focusMinutes = sessions.filter { inRange(dayOf(it.completedAt)) }.sumOf { it.durationMinutes },
            habitCheckIns = checkIns,
            habitRate = if (possible > 0) checkIns.toFloat() / possible else null,
            averageMood = dayMoods.takeIf { it.isNotEmpty() }?.map { it.mood }?.average()
        )
    }

    val thisWeek = totals(thisStart, today)
    val lastWeek = totals(lastStart, lastEnd)
    val weekCompletions = completions.filter { !dayOf(it.completedAt).isBefore(thisStart) }
    val weekSessions = sessions.filter { !dayOf(it.completedAt).isBefore(thisStart) }
    val bestDay = (0..6).map { thisStart.plusDays(it.toLong()) }
        .maxByOrNull { day ->
            weekSessions.filter { dayOf(it.completedAt) == day }.sumOf { it.durationMinutes } +
                weekCompletions.count { dayOf(it.completedAt) == day } * 20
        }
        ?.takeIf { day -> weekSessions.any { dayOf(it.completedAt) == day } || weekCompletions.any { dayOf(it.completedAt) == day } }
    val byHour = focusByHour(sessions, zone)
    val window = bestFocusWindow(byHour)

    return WeeklyReview(
        start = thisStart,
        end = today,
        thisWeek = thisWeek,
        lastWeek = lastWeek,
        bestDay = bestDay,
        // Newest first, and a repeating task done three times shows once
        wins = weekCompletions.sortedByDescending { it.completedAt }.map { it.taskTitle }.distinct().take(MAX_WINS),
        focusWindow = window,
        focusByHour = byHour.toList(),
        suggestion = suggestionFor(thisWeek, lastWeek, window)
    )
}

/** One concrete thing to try, picked from whatever looks weakest */
internal fun suggestionFor(thisWeek: PeriodTotals, lastWeek: PeriodTotals, window: FocusWindow?): String {
    val habitRate = thisWeek.habitRate
    return when {
        thisWeek.tasksDone == 0 && thisWeek.focusMinutes == 0 ->
            "Fresh start: try one 15-minute focus session tomorrow. Pick for me will choose the task."
        habitRate != null && habitRate < 0.5f ->
            "Habits slipped this week. Pick the one that matters most and protect just that one."
        lastWeek.focusMinutes > 0 && thisWeek.focusMinutes < lastWeek.focusMinutes * 0.75 ->
            "Focus dipped from last week. One extra 25-minute session a day would get you back."
        window != null ->
            "You focus best around ${formatHour(window.startHour)}–${formatHour(window.endHour)}. Save your hardest task for then."
        thisWeek.averageMood != null && thisWeek.averageMood < 2.5 ->
            "It's been a heavy week. Go easy: lean on low-effort tasks and keep the streak alive."
        else -> "Solid week. Try raising your daily focus goal by 15 minutes."
    }
}

/** "9am", "12pm", "6pm" */
fun formatHour(hour: Int): String = when (val h = hour % 24) {
    0 -> "12am"
    12 -> "12pm"
    in 1..11 -> "${h}am"
    else -> "${h - 12}pm"
}

private const val MAX_WINS = 5
