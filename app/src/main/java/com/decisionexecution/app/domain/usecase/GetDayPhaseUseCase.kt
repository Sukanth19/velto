package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.service.DayPhase
import com.decisionexecution.app.domain.service.dayPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Peak, slump or neither, from the last month of focus sessions and today's logged energy.
 * Re-checks on the hour so a Home screen left open moves from peak to slump on its own.
 */
class GetDayPhaseUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository,
    private val moodRepository: MoodRepository
) : DayPhaseSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun phase(): Flow<DayPhase> {
        val zone = ZoneId.systemDefault()
        return hourlyTicks().flatMapLatest { now ->
            val today = now.toLocalDate()
            val start = today.minusDays(HISTORY_DAYS - 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            combine(
                performanceRepository.getFocusSessionsInPeriod(start, end),
                moodRepository.getEntry(today)
            ) { sessions, mood -> dayPhase(focusByHour(sessions, zone), now.hour, mood?.energy) }
        }.distinctUntilChanged()
    }

    private fun hourlyTicks(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            delay(Duration.between(now, nextHour).toMillis() + 1_000)
        }
    }

    private companion object {
        const val HISTORY_DAYS = 30L
    }
}
