package com.decisionexecution.app.data.local.dao

import androidx.room.*
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {
    @Insert
    suspend fun insertCompletion(completion: TaskCompletionEntity)
    
    @Query("SELECT * FROM task_completions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp ORDER BY completedAt DESC")
    fun getCompletionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskCompletionEntity>>
    
    @Query("SELECT SUM(focusDuration) FROM task_completions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp AND focusDuration IS NOT NULL")
    fun getTotalFocusTimeInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int?>
    
    @Query("SELECT COUNT(DISTINCT DATE(completedAt / 1000, 'unixepoch')) FROM task_completions WHERE completedAt >= :startTimestamp")
    fun getDaysWithCompletions(startTimestamp: Long): Flow<Int>
}
