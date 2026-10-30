package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.decisionexecution.app.data.local.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position")
    fun getSubtasks(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position")
    suspend fun getSubtasksNow(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Query("UPDATE subtasks SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("UPDATE subtasks SET isDone = (id IN (:doneIds)) WHERE taskId = :taskId")
    suspend fun setDoneOnly(taskId: String, doneIds: List<String>)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteAllFor(taskId: String)

    /** Swaps a task's steps for [subtasks] in one go, as the edit screen saves them */
    @Transaction
    suspend fun replaceAll(taskId: String, subtasks: List<SubtaskEntity>) {
        deleteAllFor(taskId)
        insertAll(subtasks)
    }
}
