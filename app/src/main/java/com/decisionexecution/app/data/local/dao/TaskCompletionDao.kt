package com.decisionexecution.app.data.local.dao

import androidx.room.*
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {
    @Insert
    suspend fun insertCompletion(completion: TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND completedAt = :completedAt")
    suspend fun deleteCompletion(taskId: String, completedAt: Long)
    
    @Query("SELECT * FROM task_completions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp ORDER BY completedAt DESC")
    fun getCompletionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskCompletionEntity>>
    
    @Query("SELECT SUM(focusDuration) FROM task_completions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp AND focusDuration IS NOT NULL")
    fun getTotalFocusTimeInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int?>
    
    /** Completions where the user gave an estimate and then focused on the task, newest first */
    @Query("SELECT * FROM task_completions WHERE estimateMinutes > 0 AND actualMinutes > 0 ORDER BY completedAt DESC LIMIT :limit")
    fun getEstimatedCompletions(limit: Int): Flow<List<TaskCompletionEntity>>

    @Query("SELECT MAX(completedAt) FROM task_completions WHERE taskId = :taskId")
    suspend fun lastCompletionAt(taskId: String): Long?

    @Query("SELECT COUNT(DISTINCT DATE(completedAt / 1000, 'unixepoch')) FROM task_completions WHERE completedAt >= :startTimestamp")
    fun getDaysWithCompletions(startTimestamp: Long): Flow<Int>
}
