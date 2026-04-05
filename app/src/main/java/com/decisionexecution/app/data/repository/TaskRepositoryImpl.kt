package com.decisionexecution.app.data.repository

import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.local.entity.TaskEntity
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.model.EnergyTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskCompletionDao: TaskCompletionDao
) : TaskRepository {
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllActiveTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getTaskById(id: String): Flow<Task?> {
        return taskDao.getTaskById(id).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }
    
    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }
    
    override suspend fun completeTask(taskId: String, completionTimestamp: Long) {
        taskDao.completeTask(taskId, completionTimestamp)
    }
    
    override fun getTasksByCategory(category: TaskCategory): Flow<List<Task>> {
        return taskDao.getTasksByCategory(category.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    // Entity to Domain Model mapping
    private fun TaskEntity.toDomainModel(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            category = TaskCategory.valueOf(category),
            urgency = urgency,
            importance = importance,
            effort = TaskEffort.valueOf(effort),
            energyTag = energyTag?.let { EnergyTag.valueOf(it) },
            createdAt = createdAt
        )
    }
    
    // Domain Model to Entity mapping
    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            category = category.name,
            urgency = urgency,
            importance = importance,
            effort = effort.name,
            energyTag = energyTag?.name,
            createdAt = createdAt,
            isCompleted = false,
            completedAt = null
        )
    }
}
