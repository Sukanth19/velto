package com.decisionexecution.app.domain.model

data class UserPreferences(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** How many focus minutes a day count as hitting the goal on Home and the Today widget */
    val dailyFocusGoalMinutes: Int = 120,
    /** Turn on Do Not Disturb while a focus phase runs (needs the user to grant DND access) */
    val dndDuringFocus: Boolean = false
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
