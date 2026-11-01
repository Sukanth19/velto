package com.decisionexecution.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.model.UserPreferences
import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.ThemePalette
import com.decisionexecution.app.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private val defaults = UserPreferences()
    
    private object PreferencesKeys {
        val FOCUS_DURATION = intPreferencesKey("focus_duration_minutes")
        val BREAK_DURATION = intPreferencesKey("break_duration_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val DAILY_FOCUS_GOAL = intPreferencesKey("daily_focus_goal_minutes")
        val DND_DURING_FOCUS = booleanPreferencesKey("dnd_during_focus")
        val HABIT_REMINDER_ON = booleanPreferencesKey("habit_reminder_enabled")
        val HABIT_REMINDER_AT = intPreferencesKey("habit_reminder_minute")
        val MOOD_REMINDER_ON = booleanPreferencesKey("mood_reminder_enabled")
        val MOOD_REMINDER_AT = intPreferencesKey("mood_reminder_minute")
        val REVIEW_REMINDER_ON = booleanPreferencesKey("review_reminder_enabled")
        val REVIEW_REMINDER_AT = intPreferencesKey("review_reminder_minute")
        
        // Focus session keys
        val SESSION_TASK_ID = stringPreferencesKey("session_task_id")
        val SESSION_TASK_TITLE = stringPreferencesKey("session_task_title")
        val SESSION_START_TIMESTAMP = longPreferencesKey("session_start_timestamp")
        val SESSION_FOCUS_DURATION_SECONDS = intPreferencesKey("session_focus_duration_seconds")
        val SESSION_BREAK_DURATION_SECONDS = intPreferencesKey("session_break_duration_seconds")
        val SESSION_IS_PAUSED = booleanPreferencesKey("session_is_paused")
        val SESSION_PAUSED_AT_SECONDS = intPreferencesKey("session_paused_at_seconds")
        val SESSION_IS_ON_BREAK = booleanPreferencesKey("session_is_on_break")
    }
    
    override fun getPreferences(): Flow<UserPreferences> {
        return dataStore.data.map { preferences ->
            UserPreferences(
                focusDurationMinutes = preferences[PreferencesKeys.FOCUS_DURATION] ?: 25,
                breakDurationMinutes = preferences[PreferencesKeys.BREAK_DURATION] ?: 5,
                // An unknown stored name (from a newer or older version) falls back instead of crashing
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { name -> ThemeMode.entries.find { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                themePalette = preferences[PreferencesKeys.THEME_PALETTE]?.let { name -> ThemePalette.entries.find { it.name == name } }
                    ?: defaults.themePalette,
                pureBlack = preferences[PreferencesKeys.PURE_BLACK] ?: defaults.pureBlack,
                dailyFocusGoalMinutes = preferences[PreferencesKeys.DAILY_FOCUS_GOAL] ?: 120,
                dndDuringFocus = preferences[PreferencesKeys.DND_DURING_FOCUS] ?: false,
                habitReminder = reminder(preferences, PreferencesKeys.HABIT_REMINDER_ON, PreferencesKeys.HABIT_REMINDER_AT, defaults.habitReminder),
                moodReminder = reminder(preferences, PreferencesKeys.MOOD_REMINDER_ON, PreferencesKeys.MOOD_REMINDER_AT, defaults.moodReminder),
                weeklyReviewReminder = reminder(
                    preferences, PreferencesKeys.REVIEW_REMINDER_ON, PreferencesKeys.REVIEW_REMINDER_AT, defaults.weeklyReviewReminder
                )
            )
        }
    }
    
    override suspend fun savePreferences(preferences: UserPreferences) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_DURATION] = preferences.focusDurationMinutes
            prefs[PreferencesKeys.BREAK_DURATION] = preferences.breakDurationMinutes
            prefs[PreferencesKeys.THEME_MODE] = preferences.themeMode.name
            prefs[PreferencesKeys.THEME_PALETTE] = preferences.themePalette.name
            prefs[PreferencesKeys.PURE_BLACK] = preferences.pureBlack
            prefs[PreferencesKeys.DAILY_FOCUS_GOAL] = preferences.dailyFocusGoalMinutes
            prefs[PreferencesKeys.DND_DURING_FOCUS] = preferences.dndDuringFocus
            prefs.setReminder(PreferencesKeys.HABIT_REMINDER_ON, PreferencesKeys.HABIT_REMINDER_AT, preferences.habitReminder)
            prefs.setReminder(PreferencesKeys.MOOD_REMINDER_ON, PreferencesKeys.MOOD_REMINDER_AT, preferences.moodReminder)
            prefs.setReminder(PreferencesKeys.REVIEW_REMINDER_ON, PreferencesKeys.REVIEW_REMINDER_AT, preferences.weeklyReviewReminder)
        }
    }
    
    override suspend fun updateFocusDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOCUS_DURATION] = minutes
        }
    }
    
    override suspend fun updateBreakDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BREAK_DURATION] = minutes
        }
    }
    
    override suspend fun updateDailyFocusGoal(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_FOCUS_GOAL] = minutes
        }
    }
    
    override suspend fun updateDndDuringFocus(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_DURING_FOCUS] = enabled
        }
    }
    
    override suspend fun updateHabitReminder(reminder: Reminder) {
        dataStore.edit { it.setReminder(PreferencesKeys.HABIT_REMINDER_ON, PreferencesKeys.HABIT_REMINDER_AT, reminder) }
    }

    override suspend fun updateMoodReminder(reminder: Reminder) {
        dataStore.edit { it.setReminder(PreferencesKeys.MOOD_REMINDER_ON, PreferencesKeys.MOOD_REMINDER_AT, reminder) }
    }

    override suspend fun updateWeeklyReviewReminder(reminder: Reminder) {
        dataStore.edit { it.setReminder(PreferencesKeys.REVIEW_REMINDER_ON, PreferencesKeys.REVIEW_REMINDER_AT, reminder) }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    override suspend fun updateThemePalette(palette: ThemePalette) {
        dataStore.edit { it[PreferencesKeys.THEME_PALETTE] = palette.name }
    }

    override suspend fun updatePureBlack(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.PURE_BLACK] = enabled }
    }

    private fun MutablePreferences.setReminder(on: Preferences.Key<Boolean>, at: Preferences.Key<Int>, reminder: Reminder) {
        this[on] = reminder.enabled
        this[at] = reminder.minuteOfDay
    }

    // A stored time that's somehow out of range falls back rather than crashing every read
    private fun reminder(prefs: Preferences, on: Preferences.Key<Boolean>, at: Preferences.Key<Int>, default: Reminder) = Reminder(
        enabled = prefs[on] ?: default.enabled,
        minuteOfDay = prefs[at]?.takeIf { it in 0 until 24 * 60 } ?: default.minuteOfDay
    )

    override suspend fun saveFocusSession(sessionData: FocusSessionData) {
        dataStore.edit { prefs ->
            // Null fields are removed rather than skipped, so stale values never leak into the next read
            prefs.setOrRemove(PreferencesKeys.SESSION_TASK_ID, sessionData.taskId)
            prefs.setOrRemove(PreferencesKeys.SESSION_TASK_TITLE, sessionData.taskTitle)
            prefs.setOrRemove(PreferencesKeys.SESSION_START_TIMESTAMP, sessionData.startTimestamp)
            prefs.setOrRemove(PreferencesKeys.SESSION_FOCUS_DURATION_SECONDS, sessionData.focusDurationSeconds)
            prefs.setOrRemove(PreferencesKeys.SESSION_BREAK_DURATION_SECONDS, sessionData.breakDurationSeconds)
            prefs[PreferencesKeys.SESSION_IS_PAUSED] = sessionData.isPaused
            prefs.setOrRemove(PreferencesKeys.SESSION_PAUSED_AT_SECONDS, sessionData.pausedAtSeconds)
            prefs[PreferencesKeys.SESSION_IS_ON_BREAK] = sessionData.isOnBreak
        }
    }
    
    override suspend fun clearFocusSession() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.SESSION_TASK_ID)
            prefs.remove(PreferencesKeys.SESSION_TASK_TITLE)
            prefs.remove(PreferencesKeys.SESSION_START_TIMESTAMP)
            prefs.remove(PreferencesKeys.SESSION_FOCUS_DURATION_SECONDS)
            prefs.remove(PreferencesKeys.SESSION_BREAK_DURATION_SECONDS)
            prefs.remove(PreferencesKeys.SESSION_IS_PAUSED)
            prefs.remove(PreferencesKeys.SESSION_PAUSED_AT_SECONDS)
            prefs.remove(PreferencesKeys.SESSION_IS_ON_BREAK)
        }
    }
    
    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }
    
    override suspend fun getFocusSession(): FocusSessionData? {
        val preferences = dataStore.data.first()
        val taskId = preferences[PreferencesKeys.SESSION_TASK_ID]
        
        // If no task ID, no session exists
        if (taskId == null) return null
        
        return FocusSessionData(
            taskId = taskId,
            taskTitle = preferences[PreferencesKeys.SESSION_TASK_TITLE],
            startTimestamp = preferences[PreferencesKeys.SESSION_START_TIMESTAMP],
            focusDurationSeconds = preferences[PreferencesKeys.SESSION_FOCUS_DURATION_SECONDS],
            breakDurationSeconds = preferences[PreferencesKeys.SESSION_BREAK_DURATION_SECONDS],
            isPaused = preferences[PreferencesKeys.SESSION_IS_PAUSED] ?: false,
            pausedAtSeconds = preferences[PreferencesKeys.SESSION_PAUSED_AT_SECONDS],
            isOnBreak = preferences[PreferencesKeys.SESSION_IS_ON_BREAK] ?: false
        )
    }
}
