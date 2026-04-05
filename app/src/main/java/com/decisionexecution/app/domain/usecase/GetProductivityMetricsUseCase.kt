package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.PerformanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case for getting productivity metrics for a time period.
 * 
 * Retrieves completed tasks count, total focus time, and consistency score
 * for the specified time period.
 * 
 * Requirements: 7.2, 7.3, 7.4, 7.5
 */
class GetProductivityMetricsUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository
) {
    /**
     * Gets productivity metrics for a time period.
     * 
     * @param startTimestamp Start of the time period (milliseconds since epoch)
     * @param endTimestamp End of the time period (milliseconds since epoch)
     * @param days Number of days for consistency score calculation
     * @return Flow emitting ProductivityMetrics
     */
    operator fun invoke(
        startTimestamp: Long,
        endTimestamp: Long,
        days: Int
    ): Flow<ProductivityMetrics> {
        return combine(
            performanceRepository.getCompletionsInPeriod(startTimestamp, endTimestamp),
            performanceRepository.getTotalFocusTimeInPeriod(startTimestamp, endTimestamp),
            performanceRepository.getConsistencyScore(days)
        ) { completions, totalFocusTime, consistencyScore ->
            ProductivityMetrics(
                completedTasks = completions.size,
                totalFocusTime = totalFocusTime,
                consistencyScore = consistencyScore
            )
        }
    }
}

/**
 * Data class representing productivity metrics.
 * 
 * @property completedTasks Number of tasks completed in the period
 * @property totalFocusTime Total focus time in minutes
 * @property consistencyScore Consistency score (0-100) based on daily completion frequency
 */
data class ProductivityMetrics(
    val completedTasks: Int,
    val totalFocusTime: Int,
    val consistencyScore: Double
)
