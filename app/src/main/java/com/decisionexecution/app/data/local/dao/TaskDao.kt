package com.decisionexecution.app.data.local.dao

import androidx.room.*
import com.decisionexecution.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskEntity?>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND category = :category")
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
    
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :timestamp WHERE id = :taskId")
    suspend fun completeTask(taskId: String, timestamp: Long)
}
