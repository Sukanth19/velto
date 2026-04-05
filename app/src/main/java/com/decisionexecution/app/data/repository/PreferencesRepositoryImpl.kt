package com.decisionexecution.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.model.UserPreferences
import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesRepositoryImpl @Inject constructor(
    private val context: Context
) : PreferencesRepository {
    
    private object PreferencesKeys {
        val FOCUS_DURATION = intPreferencesKey("focus_duration_minutes")
        val BREAK_DURATION = intPreferencesKey("break_duration_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        
        // Focus session keys
        val SESSION_TASK_ID = stringPreferencesKey("session_task_id")
        val SESSION_TASK_TITLE = stringPreferencesKey("session_task_title")
        val SESSION_START_TIMESTAMP = longPreferencesKey("session_start_timestamp")
        val SESSION_FOCUS_DURATION_SECONDS = intPreferencesKey("session_focus_duration_seconds")
        val SESSION_BREAK_DURATION_SECONDS = intPreferencesKey("session_break_duration_seconds")
        val SESSION_IS_PAUSED = booleanPreferencesKey("session_is_paused")
        val SESSION_PAUSED_AT_SECONDS = intPreferencesKey("session_paused_at_seconds")
    }
    
    override fun getPreferences(): Flow<UserPreferences> {
        return context.dataStore.data.map { preferences ->
            UserPreferences(
                focusDurationMinutes = preferences[PreferencesKeys.FOCUS_DURATION] ?: 25,
                breakDurationMinutes = preferences[PreferencesKeys.BREAK_DURATION] ?: 5,
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { 
                    ThemeMode.valueOf(it) 
                } ?: ThemeMode.SYSTEM
            )
        }
    }
    
    override suspend fun savePreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_DURATION] = preferences.focusDurationMinutes
            prefs[PreferencesKeys.BREAK_DURATION] = preferences.breakDurationMinutes
            prefs[PreferencesKeys.THEME_MODE] = preferences.themeMode.name
        }
    }
    
    override suspend fun updateFocusDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOCUS_DURATION] = minutes
        }
    }
    
    override suspend fun updateBreakDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BREAK_DURATION] = minutes
        }
    }
    
    override suspend fun saveFocusSession(sessionData: FocusSessionData) {
        context.dataStore.edit { prefs ->
            sessionData.taskId?.let { prefs[PreferencesKeys.SESSION_TASK_ID] = it }
            sessionData.taskTitle?.let { prefs[PreferencesKeys.SESSION_TASK_TITLE] = it }
            sessionData.startTimestamp?.let { prefs[PreferencesKeys.SESSION_START_TIMESTAMP] = it }
            sessionData.focusDurationSeconds?.let { prefs[PreferencesKeys.SESSION_FOCUS_DURATION_SECONDS] = it }
            sessionData.breakDurationSeconds?.let { prefs[PreferencesKeys.SESSION_BREAK_DURATION_SECONDS] = it }
            prefs[PreferencesKeys.SESSION_IS_PAUSED] = sessionData.isPaused
            sessionData.pausedAtSeconds?.let { prefs[PreferencesKeys.SESSION_PAUSED_AT_SECONDS] = it }
        }
    }
    
    override suspend fun getFocusSession(): FocusSessionData? {
        val preferences = context.dataStore.data.first()
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
            pausedAtSeconds = preferences[PreferencesKeys.SESSION_PAUSED_AT_SECONDS]
        )
    }
}
