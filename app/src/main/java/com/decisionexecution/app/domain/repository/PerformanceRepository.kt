package com.decisionexecution.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface PerformanceRepository {
    suspend fun recordCompletion(taskId: String, timestamp: Long, focusDuration: Int?)
    fun getCompletionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskCompletion>>
    fun getTotalFocusTimeInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int>
    fun getConsistencyScore(days: Int): Flow<Double>
}

data class TaskCompletion(
    val id: String,
    val taskId: String,
    val taskTitle: String,
    val completedAt: Long,
    val focusDuration: Int?
)
