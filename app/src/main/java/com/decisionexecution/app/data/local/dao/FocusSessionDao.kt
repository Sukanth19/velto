package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.decisionexecution.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp")
    fun getFocusSessionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM focus_sessions WHERE taskId = :taskId AND completedAt > :since")
    suspend fun minutesForTaskSince(taskId: String, since: Long): Int

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp")
    fun getTotalFocusMinutesInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int?>
}
