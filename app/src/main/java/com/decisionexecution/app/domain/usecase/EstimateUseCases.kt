package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.service.EstimateCalibration
import com.decisionexecution.app.domain.service.calibrate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** How the user's time estimates usually turn out; null until there's enough history */
class GetEstimateCalibrationUseCase @Inject constructor(
    private val performanceRepository: PerformanceRepository
) {
    operator fun invoke(): Flow<EstimateCalibration?> =
        performanceRepository.getEstimateSamples(SAMPLES).map(::calibrate)

    private companion object {
        /** Recent enough to follow the user getting better at guessing */
        const val SAMPLES = 30
    }
}
