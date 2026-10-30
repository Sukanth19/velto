package com.decisionexecution.app.domain.model

import java.time.LocalDate

/**
 * One check-in per day. Mood and energy are both 1 (lowest) to 5 (highest).
 */
data class MoodEntry(
    val day: LocalDate,
    val mood: Int,
    val energy: Int,
    val note: String?
) {
    init {
        require(mood in SCALE) { "Mood must be between 1 and 5" }
        require(energy in SCALE) { "Energy must be between 1 and 5" }
    }

    companion object {
        val SCALE = 1..5
    }
}
