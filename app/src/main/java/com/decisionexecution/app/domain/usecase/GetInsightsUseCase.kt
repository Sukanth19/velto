package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.FocusSessionRecord
import com.decisionexecution.app.domain.repository.PerformanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DayStats(
    val date: LocalDate,
    val completedTasks: Int,
    val focusMinutes: Int
) {
    val isActive: Boolean get() = completedTasks > 0 || focusMinutes > 0
}

data class Insights(
    /** The last 7 days, oldest first, ending today */
    val week: List<DayStats>,
    /** Consecutive active days up to today; today doesn't break it until it's over */
    val streakDays: Int
) {
    val weekFocusMinutes: Int get() = week.sumOf { it.focusMinutes }
    val weekCompletedTasks: Int get() = week.sumOf { it.completedTasks }
    val bestDay: DayStats? get() = week.filter { it.isActive }.maxByOrNull { it.focusMinutes * 10 + it.completedTasks }
}

/**
 * Weekly activity and streak for the Insights screen and Home.
 * Requirements: 7.2, 7.3, 7.4
 */
class GetInsightsUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository
) {
    operator fun invoke(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Flow<Insights> {
        val start = today.minusDays(HISTORY_DAYS - 1L).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return combine(
            performanceRepository.getCompletionsInPeriod(start, end),
            performanceRepository.getFocusSessionsInPeriod(start, end)
        ) { completions, sessions ->
            buildInsights(completions.map { it.completedAt }, sessions, today, zone)
        }
    }

    companion object {
        /** How far back the streak can see */
        const val HISTORY_DAYS = 120
    }
}

fun buildInsights(
    completionTimestamps: List<Long>,
    focusSessions: List<FocusSessionRecord>,
    today: LocalDate,
    zone: ZoneId
): Insights {
    fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val completedByDay = completionTimestamps.groupingBy(::dayOf).eachCount()
    val focusByDay = focusSessions.groupBy { dayOf(it.completedAt) }.mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
    fun statsFor(date: LocalDate) = DayStats(date, completedByDay[date] ?: 0, focusByDay[date] ?: 0)

    val week = (6 downTo 0).map { statsFor(today.minusDays(it.toLong())) }

    // Count back from today, or from yesterday if today hasn't had any activity yet
    var day = if (statsFor(today).isActive) today else today.minusDays(1)
    var streak = 0
    while (statsFor(day).isActive) {
        streak++
        day = day.minusDays(1)
    }
    return Insights(week, streak)
}
