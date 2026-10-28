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

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp")
    fun getTotalFocusMinutesInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int?>
}
