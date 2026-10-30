package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_check_ins WHERE day >= :sinceDay")
    fun getCheckInsSince(sinceDay: Long): Flow<List<HabitCheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)

    // The foreign key cascades too, but only where SQLite enforces it
    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId")
    suspend fun deleteCheckIns(habitId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheckIn(checkIn: HabitCheckInEntity)

    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId AND day = :day")
    suspend fun deleteCheckIn(habitId: String, day: Long)
}
