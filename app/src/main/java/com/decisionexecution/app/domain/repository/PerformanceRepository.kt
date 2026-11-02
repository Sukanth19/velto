package com.decisionexecution.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface PerformanceRepository {
    suspend fun recordCompletion(
        taskId: String,
        timestamp: Long,
        focusDuration: Int?,
        estimateMinutes: Int? = null,
        actualMinutes: Int? = null
    )
    /** Focus minutes spent on a task since it was last completed (or ever, if it never was) */
    suspend fun focusMinutesSinceLastCompletion(taskId: String): Int
    /** Recent completions that had both an estimate and focus time, newest first */
    fun getEstimateSamples(limit: Int = 30): Flow<List<EstimateSample>>
    suspend fun recordFocusSession(taskId: String, timestamp: Long, durationMinutes: Int)
    suspend fun removeCompletion(taskId: String, timestamp: Long)
    fun getFocusSessionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<FocusSessionRecord>>
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

data class EstimateSample(val estimateMinutes: Int, val actualMinutes: Int)

data class FocusSessionRecord(
    val taskId: String,
    val completedAt: Long,
    val durationMinutes: Int
)
