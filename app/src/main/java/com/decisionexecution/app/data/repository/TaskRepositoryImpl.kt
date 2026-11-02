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
import com.decisionexecution.app.data.local.dao.SubtaskDao
import com.decisionexecution.app.data.local.entity.SubtaskEntity
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Subtask
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskCompletionDao: TaskCompletionDao,
    private val subtaskDao: SubtaskDao
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
        // The foreign key cascades too, but only where SQLite enforces it
        subtaskDao.deleteAllFor(taskId)
        taskDao.deleteTask(taskId)
    }
    
    override suspend fun completeTask(taskId: String, completionTimestamp: Long) {
        taskDao.completeTask(taskId, completionTimestamp)
    }
    
    override suspend fun uncompleteTask(taskId: String) {
        taskDao.uncompleteTask(taskId)
    }
    
    override suspend fun addDodge(taskId: String) = taskDao.addDodge(taskId)

    override suspend fun clearDodges(taskId: String) = taskDao.clearDodges(taskId)

    override suspend fun snooze(taskId: String, until: LocalDate) = taskDao.snooze(taskId, until.toEpochDay())

    override fun getTasksByCategory(category: TaskCategory): Flow<List<Task>> {
        return taskDao.getTasksByCategory(category.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getSubtasks(taskId: String): Flow<List<Subtask>> =
        subtaskDao.getSubtasks(taskId).map { list -> list.map { Subtask(it.id, it.taskId, it.title, it.isDone, it.position) } }

    override suspend fun setSubtaskDone(subtaskId: String, done: Boolean) {
        subtaskDao.setDone(subtaskId, done)
    }

    override suspend fun replaceSubtasks(taskId: String, subtasks: List<Subtask>) {
        subtaskDao.replaceAll(
            taskId,
            subtasks.mapIndexed { index, s -> SubtaskEntity(s.id, taskId, s.title, s.isDone, index) }
        )
    }

    override suspend fun setDoneSubtasks(taskId: String, doneIds: Set<String>) {
        subtaskDao.setDoneOnly(taskId, doneIds.toList())
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
            createdAt = createdAt,
            dueDate = dueDate?.let(LocalDate::ofEpochDay),
            // A repeat from a newer app version is dropped rather than crashing
            recurrence = recurrence?.let { name -> Recurrence.entries.find { it.name == name } },
            estimateMinutes = estimateMinutes,
            dodges = dodges,
            snoozedUntil = snoozedUntil?.let(LocalDate::ofEpochDay)
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
            completedAt = null,
            dueDate = dueDate?.toEpochDay(),
            recurrence = recurrence?.name,
            estimateMinutes = estimateMinutes,
            dodges = dodges,
            snoozedUntil = snoozedUntil?.toEpochDay()
        )
    }
}
