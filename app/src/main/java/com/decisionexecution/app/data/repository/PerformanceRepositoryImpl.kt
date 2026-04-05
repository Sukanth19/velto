package com.decisionexecution.app.data.repository

import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class PerformanceRepositoryImpl @Inject constructor(
    private val taskCompletionDao: TaskCompletionDao
) : PerformanceRepository {
    
    override suspend fun recordCompletion(taskId: String, timestamp: Long, focusDuration: Int?) {
        val completion = TaskCompletionEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            taskTitle = "", // Will be populated from task lookup in use case
            completedAt = timestamp,
            focusDuration = focusDuration
        )
        taskCompletionDao.insertCompletion(completion)
    }
    
    override fun getCompletionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskCompletion>> {
        return taskCompletionDao.getCompletionsInPeriod(startTimestamp, endTimestamp).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getTotalFocusTimeInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int> {
        return taskCompletionDao.getTotalFocusTimeInPeriod(startTimestamp, endTimestamp).map { total ->
            total ?: 0
        }
    }
    
    override fun getConsistencyScore(days: Int): Flow<Double> {
        val startTimestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return taskCompletionDao.getDaysWithCompletions(startTimestamp).map { daysWithCompletions ->
            if (days == 0) 0.0 else (daysWithCompletions.toDouble() / days.toDouble()) * 100.0
        }
    }
    
    private fun TaskCompletionEntity.toDomainModel(): TaskCompletion {
        return TaskCompletion(
            id = id,
            taskId = taskId,
            taskTitle = taskTitle,
            completedAt = completedAt,
            focusDuration = focusDuration
        )
    }
}
