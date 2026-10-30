package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.service.PlantState
import com.decisionexecution.app.domain.service.plantFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * The companion plant: its size comes from your level, its health from how recently you did
 * anything at all (a task, focus time, a habit or a mood check-in).
 */
class GetPlantUseCase @Inject constructor(
    private val getProgressUseCase: GetProgressUseCase,
    private val getInsightsUseCase: GetInsightsUseCase,
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository
) {
    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<PlantState> {
        val since = today.minusDays(LOOK_BACK_DAYS)
        return combine(
            getProgressUseCase(),
            getInsightsUseCase(today),
            habitRepository.getHabits(since),
            moodRepository.getEntries(since)
        ) { progress, insights, habits, moods ->
            val activeDays = insights.week.filter { it.isActive }.map { it.date } +
                habits.flatMap { it.doneDays } +
                moods.map { it.day }
            plantFor(progress.level.level, daysSince(activeDays.maxOrNull(), today))
        }
    }

    companion object {
        const val LOOK_BACK_DAYS = 30L
    }
}

/** Days from [lastActive] to [today]; anything unknown or older than the look-back counts as long ago */
fun daysSince(lastActive: LocalDate?, today: LocalDate): Int =
    lastActive?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) } ?: GetPlantUseCase.LOOK_BACK_DAYS.toInt()
