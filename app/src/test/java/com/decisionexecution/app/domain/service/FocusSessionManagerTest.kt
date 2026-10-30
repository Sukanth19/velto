package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.testutil.FakeTimeSource
import com.decisionexecution.app.testutil.InMemoryPreferencesRepository
import com.decisionexecution.app.testutil.focusSessionManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Unit tests for FocusSessionManager edge cases.
 */
class FocusSessionManagerTest : StringSpec({

    "extending adds time to the focus phase, running or paused, but not to a break" {
        val clock = FakeTimeSource()
        val manager = focusSessionManager(clock = clock)
        manager.startSession(createTestTask(), focusDuration = 25, breakDuration = 5)
        clock.advanceSeconds(600)

        manager.extendSession(5)
        (manager.sessionState.value as FocusSessionState.Focusing).let {
            it.totalSeconds shouldBe 30 * 60
            it.elapsedSeconds shouldBe 600
        }

        manager.pauseSession()
        manager.extendSession(5)
        (manager.sessionState.value as FocusSessionState.Paused).totalSeconds shouldBe 35 * 60

        manager.resumeSession()
        manager.transitionToBreak()
        manager.extendSession(5)
        (manager.sessionState.value as FocusSessionState.Breaking).totalSeconds shouldBe 5 * 60
    }

    "an extended session only moves to the break once the extra time has run out" {
        val clock = FakeTimeSource()
        val manager = focusSessionManager(clock = clock)
        manager.startSession(createTestTask(), focusDuration = 25, breakDuration = 5)
        manager.extendSession(5)

        clock.advanceSeconds(26 * 60)
        manager.sync()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()

        clock.advanceSeconds(4 * 60)
        manager.sync()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
    }
    
    "should use default durations of 25 minutes focus and 5 minutes break" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        
        // Start session with default durations
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        
        val state = manager.sessionState.value
        state.shouldBeInstanceOf<FocusSessionState.Focusing>()
        
        val focusingState = state as FocusSessionState.Focusing
        focusingState.totalSeconds shouldBe 25 * 60  // 1500 seconds
        
        // Transition to break
        manager.transitionToBreak()
        
        val breakState = manager.sessionState.value
        breakState.shouldBeInstanceOf<FocusSessionState.Breaking>()
        
        val breakingState = breakState as FocusSessionState.Breaking
        breakingState.totalSeconds shouldBe 5 * 60  // 300 seconds
    }
    
    "should preserve exact remaining time when paused" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        
        // Simulate 10 minutes elapsed (600 seconds)
        clock.advanceSeconds(600)
        manager.sync()
        
        // Pause the session
        manager.pauseSession()
        
        val pausedState = manager.sessionState.value
        pausedState.shouldBeInstanceOf<FocusSessionState.Paused>()
        
        val paused = pausedState as FocusSessionState.Paused
        paused.elapsedSeconds shouldBe 600
        paused.totalSeconds shouldBe 1500
        
        // Remaining time should be 1500 - 600 = 900 seconds (15 minutes)
        val remainingSeconds = paused.totalSeconds - paused.elapsedSeconds
        remainingSeconds shouldBe 900
        
        // Resume and verify time is preserved
        manager.resumeSession()
        
        val resumedState = manager.sessionState.value
        resumedState.shouldBeInstanceOf<FocusSessionState.Focusing>()
        
        val resumed = resumedState as FocusSessionState.Focusing
        resumed.elapsedSeconds shouldBe 600  // Exact same elapsed time
        resumed.totalSeconds shouldBe 1500
    }
    
    "should transition through full cycle: Idle -> Focusing -> Breaking -> Idle" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        // Initial state should be Idle
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
        
        val task = createTestTask()
        
        // Start session -> Focusing
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
        
        // Transition to break -> Breaking
        manager.transitionToBreak()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
        
        // Complete session -> Idle
        manager.completeSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
    }
    
    "should handle pause and resume cycle correctly" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        
        // State: Focusing
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
        
        // Pause
        manager.pauseSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Paused>()
        
        // Resume
        manager.resumeSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
        
        // Pause again
        manager.pauseSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Paused>()
        
        // Resume again
        manager.resumeSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
    }
    
    "should not pause when not in Focusing state" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        // Try to pause when Idle
        manager.pauseSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        manager.transitionToBreak()
        
        // Try to pause when Breaking
        manager.pauseSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
    }
    
    "should not resume when not in Paused state" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        // Try to resume when Idle
        manager.resumeSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        
        // Try to resume when Focusing
        manager.resumeSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
    }
    
    "should end session from any state" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        
        // End from Focusing
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
        manager.endSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
        
        // End from Paused
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        manager.pauseSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Paused>()
        manager.endSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
        
        // End from Breaking
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        manager.transitionToBreak()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
        manager.endSession()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Idle>()
    }
    
    "should clear session data from DataStore when ending session" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        
        // Verify session data exists
        var sessionData = preferencesRepo.getFocusSession()
        sessionData shouldBe FocusSessionData(
            taskId = task.id,
            taskTitle = task.title,
            startTimestamp = sessionData?.startTimestamp,
            focusDurationSeconds = 1500,
            breakDurationSeconds = 300,
            isPaused = false,
            pausedAtSeconds = 0 // nothing counted yet
        )
        
        // End session
        manager.endSession()
        
        // Verify session data is cleared
        sessionData = preferencesRepo.getFocusSession()
        sessionData shouldBe null
    }
    
    "should save session data to DataStore when starting session" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 30, breakDuration = 10)
        
        val sessionData = preferencesRepo.getFocusSession()
        sessionData?.taskId shouldBe task.id
        sessionData?.taskTitle shouldBe task.title
        sessionData?.focusDurationSeconds shouldBe 1800  // 30 * 60
        sessionData?.breakDurationSeconds shouldBe 600   // 10 * 60
        sessionData?.isPaused shouldBe false
    }
    
    "should update DataStore when pausing session" {
        val preferencesRepo = InMemoryPreferencesRepository()
        val clock = FakeTimeSource()
        val manager = focusSessionManager(preferencesRepo, clock = clock)
        
        val task = createTestTask()
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        
        // Simulate elapsed time
        clock.advanceSeconds(300)
        manager.sync()
        
        manager.pauseSession()
        
        val sessionData = preferencesRepo.getFocusSession()
        sessionData?.isPaused shouldBe true
        sessionData?.pausedAtSeconds shouldBe 300
    }
})

/**
 * Helper function to create a test task.
 */
private fun createTestTask(): Task {
    return Task(
        id = UUID.randomUUID().toString(),
        title = "Test Task",
        description = "A task for testing",
        category = TaskCategory.WORK,
        urgency = 3,
        importance = 4,
        effort = TaskEffort.MEDIUM,
        energyTag = EnergyTag.LOW,
        createdAt = System.currentTimeMillis()
    )
}

/**
 * Fake implementation of PreferencesRepository for testing.
 */
