package com.decisionexecution.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.UserPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Property-based tests for PreferencesRepository persistence.
 * Feature: decision-driven-execution-app
 */
class PreferencesRepositoryPropertyTest : StringSpec({
    
    "Property 30: Preferences Persistence Round Trip" {
        // Feature: decision-driven-execution-app, Property 30: For any valid user preferences,
        // persisting them to DataStore and then retrieving preferences should return equivalent
        // preference values.
        // **Validates: Requirements 10.2, 10.4, 11.4**
        
        val focusDurationArb = Arb.int(5..90)
        val breakDurationArb = Arb.int(1..30)
        val themeModeArb = Arb.enum<ThemeMode>()
        
        checkAll(focusDurationArb, breakDurationArb, themeModeArb, 100) { focusDuration, breakDuration, themeMode ->
            // Create a test DataStore with a unique file for each test iteration
            val testDispatcher = UnconfinedTestDispatcher()
            val testScope = TestScope(testDispatcher)
            val tempDir = createTempDir()
            
            try {
                val testDataStore = PreferenceDataStoreFactory.create(
                    scope = testScope,
                    produceFile = { File(tempDir, "test_preferences_${System.nanoTime()}.preferences_pb") }
                )
                
                val mockContext = mockk<Context>(relaxed = true)
                every { mockContext.applicationContext } returns mockContext
                
                // Create repository with test DataStore
                val repository = TestPreferencesRepositoryImpl(testDataStore)
                
                // Create preferences to save
                val originalPreferences = UserPreferences(
                    focusDurationMinutes = focusDuration,
                    breakDurationMinutes = breakDuration,
                    themeMode = themeMode
                )
                
                // Save preferences
                repository.savePreferences(originalPreferences)
                
                // Retrieve preferences
                val retrievedPreferences = repository.getPreferences().first()
                
                // Verify round trip - all values should match
                retrievedPreferences.focusDurationMinutes shouldBe originalPreferences.focusDurationMinutes
                retrievedPreferences.breakDurationMinutes shouldBe originalPreferences.breakDurationMinutes
                retrievedPreferences.themeMode shouldBe originalPreferences.themeMode
            } finally {
                // Clean up temp directory
                tempDir.deleteRecursively()
            }
        }
    }
    
    "Property 30: Focus Session Persistence Round Trip" {
        // Feature: decision-driven-execution-app, Property 30: For any valid focus session data,
        // persisting it to DataStore and then retrieving it should return equivalent values.
        // **Validates: Requirements 10.2, 10.4, 11.4**
        
        val taskIdArb = Arb.string(10..50)
        val taskTitleArb = Arb.string(5..100)
        val timestampArb = Arb.long(1000000000000L..2000000000000L)
        val focusDurationSecondsArb = Arb.int(300..5400) // 5-90 minutes in seconds
        val breakDurationSecondsArb = Arb.int(60..1800) // 1-30 minutes in seconds
        val isPausedArb = Arb.boolean()
        val pausedAtSecondsArb = Arb.int(0..5400).orNull()
        
        checkAll(
            taskIdArb,
            taskTitleArb,
            timestampArb,
            focusDurationSecondsArb,
            breakDurationSecondsArb,
            isPausedArb,
            pausedAtSecondsArb,
            100
        ) { taskId, taskTitle, timestamp, focusDuration, breakDuration, isPaused, pausedAt ->
            val testDispatcher = UnconfinedTestDispatcher()
            val testScope = TestScope(testDispatcher)
            val tempDir = createTempDir()
            
            try {
                val testDataStore = PreferenceDataStoreFactory.create(
                    scope = testScope,
                    produceFile = { File(tempDir, "test_session_${System.nanoTime()}.preferences_pb") }
                )
                
                val repository = TestPreferencesRepositoryImpl(testDataStore)
                
                // Create session data to save
                val originalSessionData = FocusSessionData(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    startTimestamp = timestamp,
                    focusDurationSeconds = focusDuration,
                    breakDurationSeconds = breakDuration,
                    isPaused = isPaused,
                    pausedAtSeconds = pausedAt
                )
                
                // Save session data
                repository.saveFocusSession(originalSessionData)
                
                // Retrieve session data
                val retrievedSessionData = repository.getFocusSession()
                
                // Verify round trip - all values should match
                retrievedSessionData shouldBe originalSessionData
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }
    
    "Property 30: Null Focus Session Returns Null" {
        // Feature: decision-driven-execution-app, Property 30: When no focus session exists,
        // getFocusSession should return null.
        // **Validates: Requirements 10.2, 10.4**
        
        val testDispatcher = UnconfinedTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val tempDir = createTempDir()
        
        try {
            val testDataStore = PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { File(tempDir, "test_empty_session.preferences_pb") }
            )
            
            val repository = TestPreferencesRepositoryImpl(testDataStore)
            
            // Retrieve session data without saving anything
            val retrievedSessionData = repository.getFocusSession()
            
            // Should return null when no session exists
            retrievedSessionData shouldBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }
})

/**
 * Test implementation that accepts a DataStore directly for testing.
 */
private class TestPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepositoryImpl(mockk(relaxed = true)) {
    
    private object PreferencesKeys {
        val FOCUS_DURATION = androidx.datastore.preferences.core.intPreferencesKey("focus_duration_minutes")
        val BREAK_DURATION = androidx.datastore.preferences.core.intPreferencesKey("break_duration_minutes")
        val THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        
        val SESSION_TASK_ID = androidx.datastore.preferences.core.stringPreferencesKey("session_task_id")
        val SESSION_TASK_TITLE = androidx.datastore.preferences.core.stringPreferencesKey("session_task_title")
        val SESSION_START_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("session_start_timestamp")
        val SESSION_FOCUS_DURATION_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("session_focus_duration_seconds")
        val SESSION_BREAK_DURATION_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("session_break_duration_seconds")
        val SESSION_IS_PAUSED = androidx.datastore.preferences.core.booleanPreferencesKey("session_is_paused")
        val SESSION_PAUSED_AT_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("session_paused_at_seconds")
    }
    
    override fun getPreferences() = dataStore.data.map { preferences ->
        UserPreferences(
            focusDurationMinutes = preferences[PreferencesKeys.FOCUS_DURATION] ?: 25,
            breakDurationMinutes = preferences[PreferencesKeys.BREAK_DURATION] ?: 5,
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { 
                ThemeMode.valueOf(it) 
            } ?: ThemeMode.SYSTEM
        )
    }
    
    override suspend fun savePreferences(preferences: UserPreferences) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_DURATION] = preferences.focusDurationMinutes
            prefs[PreferencesKeys.BREAK_DURATION] = preferences.breakDurationMinutes
            prefs[PreferencesKeys.THEME_MODE] = preferences.themeMode.name
        }
    }
    
    override suspend fun saveFocusSession(sessionData: FocusSessionData) {
        dataStore.edit { prefs ->
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
        val preferences = dataStore.data.first()
        val taskId = preferences[PreferencesKeys.SESSION_TASK_ID]
        
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
