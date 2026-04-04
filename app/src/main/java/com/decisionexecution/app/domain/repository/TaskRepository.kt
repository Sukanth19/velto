package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(id: String): Flow<Task?>
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun completeTask(taskId: String, completionTimestamp: Long)
    fun getTasksByCategory(category: TaskCategory): Flow<List<Task>>
}
