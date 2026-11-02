package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.repository.EstimateSample
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How the user's guesses compare with reality: actual focus time divided by the estimate, as a
 * median so one marathon task doesn't skew it.
 */
data class EstimateCalibration(val ratio: Double, val samples: Int) {

    /** How long a task guessed at [minutes] will probably really take */
    fun likelyMinutes(minutes: Int): Int = (minutes * ratio).roundToInt().coerceAtLeast(1)

    /** Close enough that adjusting would just be noise */
    val isAccurate: Boolean get() = abs(ratio - 1.0) < ACCURATE_WITHIN

    val summary: String
        get() = when {
            isAccurate -> "Your estimates are spot on"
            ratio > 1 -> "You usually take ${formatRatio(ratio)}× your estimate"
            else -> "You usually finish in ${(ratio * 100).roundToInt()}% of your estimate"
        }

    companion object {
        const val MIN_SAMPLES = 3
        private const val ACCURATE_WITHIN = 0.15
    }
}

fun formatRatio(ratio: Double): String {
    val tenths = (ratio * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

/** Null until there are enough finished, estimated tasks to say anything */
fun calibrate(samples: List<EstimateSample>): EstimateCalibration? {
    val ratios = samples
        .filter { it.estimateMinutes > 0 && it.actualMinutes > 0 }
        .map { (it.actualMinutes.toDouble() / it.estimateMinutes).coerceIn(MIN_RATIO, MAX_RATIO) }
        .sorted()
    if (ratios.size < EstimateCalibration.MIN_SAMPLES) return null
    val mid = ratios.size / 2
    val median = if (ratios.size % 2 == 1) ratios[mid] else (ratios[mid - 1] + ratios[mid]) / 2
    return EstimateCalibration(median, ratios.size)
}

/** Estimate choices offered when adding a task */
val ESTIMATE_CHOICES = listOf(15, 30, 45, 60, 90, 120)

private const val MIN_RATIO = 0.25
private const val MAX_RATIO = 4.0
