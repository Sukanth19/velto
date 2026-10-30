package com.decisionexecution.app.data.repository

import kotlin.io.path.createTempDirectory
import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.UserPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        
        checkAll(PROPERTY_ITERATIONS, focusDurationArb, breakDurationArb, themeModeArb) { focusDuration, breakDuration, themeMode ->
            // Create a test DataStore with a unique file for each test iteration
            val testDispatcher = UnconfinedTestDispatcher()
            val testScope = TestScope(testDispatcher)
            val tempDir = createTempDirectory().toFile()
            
            try {
                val testDataStore = PreferenceDataStoreFactory.create(
                    scope = testScope,
                    produceFile = { File(tempDir, "test_preferences_${System.nanoTime()}.preferences_pb") }
                )
                
                // Create repository with test DataStore
                val repository = PreferencesRepositoryImpl(testDataStore)
                
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
        
        checkAll(PROPERTY_ITERATIONS,
            taskIdArb,
            taskTitleArb,
            timestampArb,
            focusDurationSecondsArb,
            breakDurationSecondsArb,
            isPausedArb,
            pausedAtSecondsArb
        ) { taskId, taskTitle, timestamp, focusDuration, breakDuration, isPaused, pausedAt ->
            val testDispatcher = UnconfinedTestDispatcher()
            val testScope = TestScope(testDispatcher)
            val tempDir = createTempDirectory().toFile()
            
            try {
                val testDataStore = PreferenceDataStoreFactory.create(
                    scope = testScope,
                    produceFile = { File(tempDir, "test_session_${System.nanoTime()}.preferences_pb") }
                )
                
                val repository = PreferencesRepositoryImpl(testDataStore)
                
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
        val tempDir = createTempDirectory().toFile()
        
        try {
            val testDataStore = PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { File(tempDir, "test_empty_session.preferences_pb") }
            )
            
            val repository = PreferencesRepositoryImpl(testDataStore)
            
            // Retrieve session data without saving anything
            val retrievedSessionData = repository.getFocusSession()
            
            // Should return null when no session exists
            retrievedSessionData shouldBe null
        } finally {
            tempDir.deleteRecursively()
        }
    }
})
