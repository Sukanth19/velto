package com.decisionexecution.app.domain.model

data class UserPreferences(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** How many focus minutes a day count as hitting the goal on Home and the Today widget */
    val dailyFocusGoalMinutes: Int = 120
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
