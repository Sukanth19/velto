package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.FocusSessionRecord
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PerformanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * What the journal has learned: recent entries and how energy lines up with focus time.
 *
 * @property highEnergyFocus Average focus minutes on days logged at energy 4-5, null without such days
 * @property lowEnergyFocus Average focus minutes on days logged at energy 1-2, null without such days
 */
data class MoodInsights(
    val entries: List<MoodEntry>,
    val averageMood: Double?,
    val highEnergyFocus: Int?,
    val lowEnergyFocus: Int?
) {
    /** A one-line takeaway once there's enough data to compare, else null */
    val headline: String?
        get() {
            val high = highEnergyFocus ?: return null
            val low = lowEnergyFocus ?: return null
            return when {
                high > low && low > 0 && high >= low * 3 / 2 ->
                    "You focus ${"%.1f".format(high.toDouble() / low)}x longer on high-energy days"
                high > low -> "You focus ${high - low}m more on high-energy days"
                low > high -> "Low energy doesn't stop you: ${low - high}m more focus on those days"
                else -> "Your focus holds steady whatever your energy"
            }
        }
}

class GetMoodInsightsUseCase @Inject constructor(
    private val moodRepository: MoodRepository,
    private val performanceRepository: PerformanceRepository
) {
    operator fun invoke(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Flow<MoodInsights> {
        val since = today.minusDays(HISTORY_DAYS - 1L)
        val start = since.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return combine(
            moodRepository.getEntries(since),
            performanceRepository.getFocusSessionsInPeriod(start, end)
        ) { entries, sessions -> buildMoodInsights(entries, sessions, zone) }
    }

    companion object {
        const val HISTORY_DAYS = 60
    }
}

fun buildMoodInsights(entries: List<MoodEntry>, sessions: List<FocusSessionRecord>, zone: ZoneId): MoodInsights {
    val focusByDay = sessions
        .groupBy { Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }
        .mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
    fun averageFocus(days: List<MoodEntry>): Int? =
        days.takeIf { it.isNotEmpty() }?.map { focusByDay[it.day] ?: 0 }?.average()?.roundToInt()

    return MoodInsights(
        entries = entries.sortedByDescending { it.day },
        averageMood = entries.takeIf { it.isNotEmpty() }?.map { it.mood }?.average(),
        highEnergyFocus = averageFocus(entries.filter { it.energy >= 4 }),
        lowEnergyFocus = averageFocus(entries.filter { it.energy <= 2 })
    )
}
