package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.decisionexecution.app.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries WHERE day >= :sinceDay ORDER BY day DESC")
    fun getEntriesSince(sinceDay: Long): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE day = :day")
    fun getEntry(day: Long): Flow<MoodEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MoodEntryEntity)
}
