package com.decisionexecution.app.domain.model

data class UserPreferences(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
