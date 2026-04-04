package com.decisionexecution.app.domain.model

data class UserPreferences(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5
)
