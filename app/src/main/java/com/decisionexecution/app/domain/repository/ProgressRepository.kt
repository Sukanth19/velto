package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.service.LifetimeStats
import kotlinx.coroutines.flow.Flow

/**
 * Lifetime totals that XP and achievements are derived from, plus which rewards were already celebrated.
 */
interface ProgressRepository {
    fun getLifetimeStats(): Flow<LifetimeStats>
    fun getCelebratedLevel(): Flow<Int>
    fun getCelebratedAchievements(): Flow<Set<String>>
    suspend fun markCelebrated(level: Int, achievementIds: Set<String>)
}
