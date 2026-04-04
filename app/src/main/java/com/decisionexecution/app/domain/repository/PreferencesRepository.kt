package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun updateFocusDuration(minutes: Int)
    suspend fun updateBreakDuration(minutes: Int)
}
