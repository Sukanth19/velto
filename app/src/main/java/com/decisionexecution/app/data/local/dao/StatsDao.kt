package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Lifetime totals across tables, for XP and achievements. */
@Dao
interface StatsDao {
    @Query("SELECT COUNT(*) FROM task_completions")
    fun countCompletions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun countFocusSessions(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM focus_sessions")
    fun totalFocusMinutes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habits")
    fun countHabits(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mood_entries")
    fun countMoodEntries(): Flow<Int>

    @Query("SELECT COUNT(*) FROM inbox_items")
    fun countCaptures(): Flow<Int>
}
