package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.Reminder
import com.decisionexecution.app.domain.model.FocusSound
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.ThemePalette
import com.decisionexecution.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun savePreferences(preferences: UserPreferences)
    suspend fun updateFocusDuration(minutes: Int)
    suspend fun updateBreakDuration(minutes: Int)
    suspend fun updateDailyFocusGoal(minutes: Int)
    suspend fun updateDndDuringFocus(enabled: Boolean)
    suspend fun updateHabitReminder(reminder: Reminder)
    suspend fun updateMoodReminder(reminder: Reminder)
    suspend fun updateWeeklyReviewReminder(reminder: Reminder)
    suspend fun updateThemeMode(mode: ThemeMode)
    suspend fun updateThemePalette(palette: ThemePalette)
    suspend fun updatePureBlack(enabled: Boolean)
    suspend fun updateFocusSound(sound: FocusSound)
    suspend fun updatePlantVariety(variety: com.decisionexecution.app.domain.service.PlantVariety)
    suspend fun saveFocusSession(sessionData: FocusSessionData)
    suspend fun getFocusSession(): FocusSessionData?
    suspend fun clearFocusSession()
}
