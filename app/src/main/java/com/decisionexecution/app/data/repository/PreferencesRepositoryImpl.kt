package com.decisionexecution.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesRepositoryImpl @Inject constructor(
    private val context: Context
) : PreferencesRepository {
    
    private object PreferencesKeys {
        val FOCUS_DURATION = intPreferencesKey("focus_duration_minutes")
        val BREAK_DURATION = intPreferencesKey("break_duration_minutes")
    }
    
    override fun getPreferences(): Flow<UserPreferences> {
        return context.dataStore.data.map { preferences ->
            UserPreferences(
                focusDurationMinutes = preferences[PreferencesKeys.FOCUS_DURATION] ?: 25,
                breakDurationMinutes = preferences[PreferencesKeys.BREAK_DURATION] ?: 5
            )
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
}
