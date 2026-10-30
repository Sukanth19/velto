package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.ProgressRepository
import com.decisionexecution.app.domain.service.Achievement
import com.decisionexecution.app.domain.service.LevelProgress
import com.decisionexecution.app.domain.service.LifetimeStats
import com.decisionexecution.app.domain.service.levelFor
import com.decisionexecution.app.domain.service.unlockedAchievements
import com.decisionexecution.app.domain.service.xpFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class Progress(
    val stats: LifetimeStats,
    val level: LevelProgress,
    val unlocked: Set<Achievement>
)

/**
 * Level, XP and achievements, derived from lifetime history plus the current activity streak.
 */
class GetProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val getInsightsUseCase: GetInsightsUseCase
) {
    operator fun invoke(): Flow<Progress> =
        combine(progressRepository.getLifetimeStats(), getInsightsUseCase()) { stats, insights ->
            progressOf(stats.copy(activityStreak = insights.streakDays))
        }
}

fun progressOf(stats: LifetimeStats): Progress =
    Progress(stats, levelFor(xpFor(stats)), unlockedAchievements(stats))
