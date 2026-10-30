package com.decisionexecution.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.decisionexecution.app.data.local.dao.HabitDao
import com.decisionexecution.app.data.local.dao.StatsDao
import com.decisionexecution.app.domain.repository.ProgressRepository
import com.decisionexecution.app.domain.service.LifetimeStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao,
    private val habitDao: HabitDao,
    private val dataStore: DataStore<Preferences>
) : ProgressRepository {

    override fun getLifetimeStats(): Flow<LifetimeStats> {
        val counts = combine(
            statsDao.countCompletions(),
            statsDao.countFocusSessions(),
            statsDao.totalFocusMinutes(),
            statsDao.countHabits(),
            statsDao.countMoodEntries()
        ) { completions, sessions, minutes, habits, moods ->
            LifetimeStats(
                tasksCompleted = completions,
                focusSessions = sessions,
                focusMinutes = minutes,
                habitsCreated = habits,
                moodEntries = moods
            )
        }
        return combine(counts, statsDao.countCaptures(), habitDao.getCheckInsSince(Long.MIN_VALUE)) { stats, captures, checkIns ->
            val longestRun = checkIns.groupBy({ it.habitId }, { it.day }).values.maxOfOrNull(::longestRun) ?: 0
            stats.copy(inboxCaptured = captures, habitCheckIns = checkIns.size, bestHabitStreak = longestRun)
        }
    }

    override fun getCelebratedLevel(): Flow<Int> = dataStore.data.map { it[CELEBRATED_LEVEL] ?: 1 }

    override fun getCelebratedAchievements(): Flow<Set<String>> = dataStore.data.map { it[CELEBRATED_ACHIEVEMENTS].orEmpty() }

    override suspend fun markCelebrated(level: Int, achievementIds: Set<String>) {
        dataStore.edit { prefs ->
            prefs[CELEBRATED_LEVEL] = maxOf(level, prefs[CELEBRATED_LEVEL] ?: 1)
            prefs[CELEBRATED_ACHIEVEMENTS] = prefs[CELEBRATED_ACHIEVEMENTS].orEmpty() + achievementIds
        }
    }

    private companion object {
        val CELEBRATED_LEVEL = intPreferencesKey("celebrated_level")
        val CELEBRATED_ACHIEVEMENTS = stringSetPreferencesKey("celebrated_achievements")
    }
}

/** Longest run of consecutive epoch days, ever */
internal fun longestRun(days: List<Long>): Int {
    var best = 0
    var current = 0
    var previous: Long? = null
    for (day in days.distinct().sorted()) {
        current = if (previous != null && day == previous + 1) current + 1 else 1
        best = maxOf(best, current)
        previous = day
    }
    return best
}
