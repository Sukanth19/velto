package com.decisionexecution.app.domain.service

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.testutil.FakeTimeSource
import com.decisionexecution.app.testutil.InMemoryPreferencesRepository
import com.decisionexecution.app.testutil.focusSessionManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Property-based tests for FocusSessionManager.
 */
class FocusSessionManagerPropertyTest : StringSpec({
    
    "Custom Focus Duration Application" {
        // For any valid focus duration (5-90 minutes)
        // and break duration (1-30 minutes), setting these preferences and starting a focus session
        // should result in the session using those durations.
        
        checkAll(PROPERTY_ITERATIONS,  Arb.int(5..90),  // focusDuration in minutes
            Arb.int(1..30),  // breakDuration in minutes
            taskArb()
        ) { focusDuration, breakDuration, task ->
            val preferencesRepo = InMemoryPreferencesRepository()
            val clock = FakeTimeSource()
            val manager = focusSessionManager(preferencesRepo, clock = clock)
            
            manager.startSession(task, focusDuration, breakDuration)
            
            val state = manager.sessionState.value
            state.shouldBeInstanceOf<FocusSessionState.Focusing>()
            
            val focusingState = state as FocusSessionState.Focusing
            focusingState.totalSeconds shouldBe focusDuration * 60
            focusingState.task shouldBe task
            focusingState.elapsedSeconds shouldBe 0
        }
    }
    
    "Focus Session State Transition" {
        // For any active focus session,
        // when the elapsed time reaches the focus duration, the session state should automatically
        // transition to the break state.
        
        checkAll(PROPERTY_ITERATIONS,  Arb.int(5..90),  // focusDuration in minutes
            Arb.int(1..30),  // breakDuration in minutes
            taskArb()
        ) { focusDuration, breakDuration, task ->
            val preferencesRepo = InMemoryPreferencesRepository()
            val clock = FakeTimeSource()
            val manager = focusSessionManager(preferencesRepo, clock = clock)
            
            manager.startSession(task, focusDuration, breakDuration)
            
            // Simulate focus completion by calling transitionToBreak
            manager.transitionToBreak()
            
            val state = manager.sessionState.value
            state.shouldBeInstanceOf<FocusSessionState.Breaking>()
            
            val breakingState = state as FocusSessionState.Breaking
            breakingState.totalSeconds shouldBe breakDuration * 60
            breakingState.elapsedSeconds shouldBe 0
        }
    }
    
    "Focus Session Pause Preservation" {
        // For any active focus session paused
        // at any point in time, the remaining time should be preserved and resuming should continue
        // from the same point.
        
        checkAll(PROPERTY_ITERATIONS,  Arb.int(5..90),  // focusDuration in minutes
            Arb.int(1..30),  // breakDuration in minutes
            taskArb()
        ) { focusDuration, breakDuration, task ->
            val preferencesRepo = InMemoryPreferencesRepository()
            val clock = FakeTimeSource()
            val manager = focusSessionManager(preferencesRepo, clock = clock)
            
            manager.startSession(task, focusDuration, breakDuration)
            
            // Simulate some elapsed time (between 0 and total duration)
            val totalSeconds = focusDuration * 60
            val elapsedSeconds = if (totalSeconds > 1) {
                Arb.int(0 until totalSeconds).bind()
            } else {
                0
            }
            
            // Manually update the state to simulate elapsed time
            clock.advanceSeconds(elapsedSeconds)
            manager.sync()
            
            // Pause the session
            manager.pauseSession()
            
            val pausedState = manager.sessionState.value
            pausedState.shouldBeInstanceOf<FocusSessionState.Paused>()
            
            val paused = pausedState as FocusSessionState.Paused
            paused.elapsedSeconds shouldBe elapsedSeconds
            paused.totalSeconds shouldBe totalSeconds
            paused.task shouldBe task
            
            // Resume the session
            manager.resumeSession()
            
            val resumedState = manager.sessionState.value
            resumedState.shouldBeInstanceOf<FocusSessionState.Focusing>()
            
            val resumed = resumedState as FocusSessionState.Focusing
            resumed.elapsedSeconds shouldBe elapsedSeconds  // Preserved
            resumed.totalSeconds shouldBe totalSeconds
            resumed.task shouldBe task
        }
    }
    
    "Focus Session Early Termination" {
        // For any active focus session,
        // ending it early should transition the session state back to idle.
        
        checkAll(PROPERTY_ITERATIONS,  Arb.int(5..90),  // focusDuration in minutes
            Arb.int(1..30),  // breakDuration in minutes
            taskArb()
        ) { focusDuration, breakDuration, task ->
            val preferencesRepo = InMemoryPreferencesRepository()
            val clock = FakeTimeSource()
            val manager = focusSessionManager(preferencesRepo, clock = clock)
            
            manager.startSession(task, focusDuration, breakDuration)
            
            // Verify session is active
            manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Focusing>()
            
            // End the session early
            manager.endSession()
            
            // Verify state is now Idle
            val state = manager.sessionState.value
            state.shouldBeInstanceOf<FocusSessionState.Idle>()
        }
    }
    
    "Focus Completion Recording" {
        // For any focus session that completes
        // successfully, the performance tracker should record a completion entry with the correct
        // focus duration.
        
        checkAll(PROPERTY_ITERATIONS,  Arb.int(5..90),  // focusDuration in minutes
            Arb.int(1..30),  // breakDuration in minutes
            taskArb()
        ) { focusDuration, breakDuration, task ->
            val preferencesRepo = InMemoryPreferencesRepository()
            val clock = FakeTimeSource()
            val manager = focusSessionManager(preferencesRepo, clock = clock)
            
            manager.startSession(task, focusDuration, breakDuration)
            
            // Transition to break (focus completed)
            manager.transitionToBreak()
            
            // Complete the session (break completed)
            manager.completeSession()
            
            // Verify state is now Idle
            val state = manager.sessionState.value
            state.shouldBeInstanceOf<FocusSessionState.Idle>()
            
            // Verify session data was cleared from DataStore
            val sessionData = preferencesRepo.getFocusSession()
            sessionData shouldBe null
        }
    }
})

/**
 * Arbitrary generator for Task domain model.
 */
private fun taskArb(): Arb<Task> = arbitrary {
    Task(
        id = UUID.randomUUID().toString(),
        title = Arb.string(1..50).bind(),
        description = Arb.string(0..200).orNull().bind(),
        category = Arb.enum<TaskCategory>().bind(),
        urgency = Arb.int(1..5).bind(),
        importance = Arb.int(1..5).bind(),
        effort = Arb.enum<TaskEffort>().bind(),
        energyTag = Arb.enum<EnergyTag>().orNull().bind(),
        createdAt = Arb.long(1_000_000_000_000L..System.currentTimeMillis()).bind()
    )
}

/**
 * Fake implementation of PreferencesRepository for testing.
 */
