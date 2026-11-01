package com.decisionexecution.app.domain.model

data class UserPreferences(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** How many focus minutes a day count as hitting the goal on Home and the Today widget */
    val dailyFocusGoalMinutes: Int = 120,
    /** Turn on Do Not Disturb while a focus phase runs (needs the user to grant DND access) */
    val dndDuringFocus: Boolean = false,
    /** Evening nudge when habits are still unchecked */
    val habitReminder: Reminder = Reminder(enabled = true, minuteOfDay = 20 * 60),
    /** Morning nudge to log mood, skipped once it's logged */
    val moodReminder: Reminder = Reminder(enabled = true, minuteOfDay = 9 * 60),
    /** Sunday evening nudge to look back at the week */
    val weeklyReviewReminder: Reminder = Reminder(enabled = true, minuteOfDay = 18 * 60),
    /** Colour palette for the whole app; [ThemePalette.WALLPAPER] uses Material You colours where available */
    val themePalette: ThemePalette = ThemePalette.WALLPAPER,
    /** True black backgrounds in dark mode, easier on OLED batteries */
    val pureBlack: Boolean = false
)

/** A daily reminder at [minuteOfDay] minutes after midnight, local time */
data class Reminder(val enabled: Boolean, val minuteOfDay: Int) {
    init {
        require(minuteOfDay in 0 until 24 * 60) { "Reminder time must be within a day" }
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Colour palettes the user can pick in settings. Order is the order shown. */
enum class ThemePalette { WALLPAPER, VIOLET, OCEAN, FOREST, SUNSET, ROSE, MONO }
