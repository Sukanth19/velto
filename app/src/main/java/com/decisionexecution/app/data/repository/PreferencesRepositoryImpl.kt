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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {
    
    private object PreferencesKeys {
        val FOCUS_DURATION = intPreferencesKey("focus_duration_minutes")
        val BREAK_DURATION = intPreferencesKey("break_duration_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAILY_FOCUS_GOAL = intPreferencesKey("daily_focus_goal_minutes")
        
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
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { 
                    ThemeMode.valueOf(it) 
                } ?: ThemeMode.SYSTEM,
                dailyFocusGoalMinutes = preferences[PreferencesKeys.DAILY_FOCUS_GOAL] ?: 120
            )
        }
    }
    
    override suspend fun savePreferences(preferences: UserPreferences) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_DURATION] = preferences.focusDurationMinutes
            prefs[PreferencesKeys.BREAK_DURATION] = preferences.breakDurationMinutes
            prefs[PreferencesKeys.THEME_MODE] = preferences.themeMode.name
            prefs[PreferencesKeys.DAILY_FOCUS_GOAL] = preferences.dailyFocusGoalMinutes
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
