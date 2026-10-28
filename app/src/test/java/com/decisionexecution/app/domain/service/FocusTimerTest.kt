package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.testutil.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import com.decisionexecution.app.domain.repository.PerformanceRepository
import io.mockk.coVerify
import io.mockk.mockk

/**
 * Clock-driven timer behaviour of FocusSessionManager: phase ends, background checks,
 * notifications, and surviving process death.
 * Feature: decision-driven-execution-app
 */
class FocusTimerTest : StringSpec({

    val task = Task("t-1", "Write essay", null, TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK, 1L)

    "elapsed time follows the clock" {
        // Requirements: 4.3
        val clock = FakeTimeSource()
        val manager = focusSessionManager(clock = clock)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)

        clock.advanceSeconds(90)
        manager.sync()

        val state = manager.sessionState.value as FocusSessionState.Focusing
        state.elapsedSeconds shouldBe 90
        state.totalSeconds shouldBe 1500
    }

    "starting schedules a background check for the end of focus" {
        // Requirements: 13.1
        val scheduler = RecordingScheduler()
        val manager = focusSessionManager(scheduler = scheduler)

        manager.startSession(task, focusDuration = 25, breakDuration = 5)

        scheduler.pendingDelayMillis shouldBe 25 * 60 * 1000L
    }

    "focus ending starts the break, notifies, and schedules the break end" {
        // Requirements: 4.4, 13.2
        val clock = FakeTimeSource()
        val scheduler = RecordingScheduler()
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(clock = clock, scheduler = scheduler, notifier = notifier)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)

        clock.advanceSeconds(25 * 60)
        manager.sync()

        val state = manager.sessionState.value
        state.shouldBeInstanceOf<FocusSessionState.Breaking>()
        state.elapsedSeconds shouldBe 0
        notifier.focusCompleted shouldBe listOf("Write essay")
        scheduler.pendingDelayMillis shouldBe 5 * 60 * 1000L
    }

    "break ending returns to idle, notifies, clears storage and cancels checks" {
        // Requirements: 4.5, 13.3
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        val scheduler = RecordingScheduler()
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(prefs, clock = clock, scheduler = scheduler, notifier = notifier)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)

        clock.advanceSeconds(25 * 60)
        manager.sync()
        clock.advanceSeconds(5 * 60)
        manager.sync()

        manager.sessionState.value shouldBe FocusSessionState.Idle
        notifier.breakCompletedCount shouldBe 1
        prefs.getFocusSession() shouldBe null
        scheduler.pendingDelayMillis shouldBe null
    }

    "repeated syncs never notify twice for the same phase end" {
        val clock = FakeTimeSource()
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(clock = clock, notifier = notifier)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)

        clock.advanceSeconds(25 * 60 + 10)
        repeat(5) { manager.sync() }

        notifier.focusCompleted.size shouldBe 1
    }

    "pausing freezes time and cancels the check; resuming reschedules the remainder" {
        // Requirements: 4.6, 4.7
        checkAll(PROPERTY_ITERATIONS, Arb.int(5..90), Arb.int(0..300), Arb.int(0..3600)) { focus, beforePause, whilePaused ->
            val clock = FakeTimeSource()
            val scheduler = RecordingScheduler()
            val manager = focusSessionManager(clock = clock, scheduler = scheduler)
            manager.startSession(task, focus, 5)
            val elapsedAtPause = beforePause % (focus * 60)

            clock.advanceSeconds(elapsedAtPause)
            manager.pauseSession()
            scheduler.pendingDelayMillis shouldBe null

            clock.advanceSeconds(whilePaused)
            manager.sync()
            (manager.sessionState.value as FocusSessionState.Paused).elapsedSeconds shouldBe elapsedAtPause

            manager.resumeSession()
            scheduler.pendingDelayMillis shouldBe (focus * 60 - elapsedAtPause) * 1000L
            (manager.sessionState.value as FocusSessionState.Focusing).elapsedSeconds shouldBe elapsedAtPause
        }
    }

    "a session survives process death" {
        // Requirements: 13.6
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        val tasks = taskRepositoryOf(task)
        focusSessionManager(prefs, tasks, clock).startSession(task, focusDuration = 25, breakDuration = 5)

        // New process: fresh manager over the same storage, ten minutes later
        clock.advanceSeconds(600)
        val scheduler = RecordingScheduler()
        val revived = focusSessionManager(prefs, tasks, clock, scheduler)
        revived.sync()

        val state = revived.sessionState.value as FocusSessionState.Focusing
        state.task shouldBe task
        state.elapsedSeconds shouldBe 600
        scheduler.pendingDelayMillis shouldBe 15 * 60 * 1000L
    }

    "a paused session survives process death without drifting" {
        // Requirements: 4.7, 13.6
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        val tasks = taskRepositoryOf(task)
        val manager = focusSessionManager(prefs, tasks, clock)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        clock.advanceSeconds(120)
        manager.pauseSession()

        clock.advanceSeconds(3600)
        val revived = focusSessionManager(prefs, tasks, clock)
        revived.sync()

        (revived.sessionState.value as FocusSessionState.Paused).elapsedSeconds shouldBe 120
    }

    "phases that ended while the app was dead are caught up in order" {
        // Requirements: 4.4, 4.5, 13.6
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        val tasks = taskRepositoryOf(task)
        focusSessionManager(prefs, tasks, clock).startSession(task, focusDuration = 25, breakDuration = 5)

        // Back 27 minutes later: focus ended at 25, so 2 minutes into the break
        clock.advanceSeconds(27 * 60)
        val notifier = RecordingNotifier()
        val revived = focusSessionManager(prefs, tasks, clock, notifier = notifier)
        revived.sync()
        (revived.sessionState.value as FocusSessionState.Breaking).elapsedSeconds shouldBe 120
        notifier.focusCompleted.size shouldBe 1

        // Back an hour later: both phases are long over
        clock.advanceSeconds(3600)
        val revivedAgain = focusSessionManager(prefs, tasks, clock, notifier = notifier)
        revivedAgain.sync()
        revivedAgain.sessionState.value shouldBe FocusSessionState.Idle
        notifier.breakCompletedCount shouldBe 1
    }

    "restoring a session whose task was deleted clears it" {
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        focusSessionManager(prefs, taskRepositoryOf(task), clock).startSession(task, 25, 5)

        val revived = focusSessionManager(prefs, taskRepositoryOf(), clock)
        revived.sync()

        revived.sessionState.value shouldBe FocusSessionState.Idle
        prefs.getFocusSession() shouldBe null
    }

    "a finished focus phase records its minutes once; ending early records nothing" {
        // Requirements: 4.9, 7.3
        val clock = FakeTimeSource()
        val performance = mockk<PerformanceRepository>(relaxed = true)
        val manager = focusSessionManager(clock = clock, performance = performance)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        val focusEndedAt = clock.now + 25 * 60 * 1000L

        clock.advanceSeconds(25 * 60 + 30)
        repeat(3) { manager.sync() }
        coVerify(exactly = 1) { performance.recordFocusSession("t-1", focusEndedAt, 25) }

        val early = focusSessionManager(clock = clock, performance = performance)
        early.startSession(task, focusDuration = 25, breakDuration = 5)
        clock.advanceSeconds(600)
        early.endSession()
        coVerify(exactly = 1) { performance.recordFocusSession(any(), any(), any()) }
    }

    "ending early clears storage so nothing is restored later" {
        // Requirements: 4.8
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        val tasks = taskRepositoryOf(task)
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(prefs, tasks, clock, notifier = notifier)
        manager.startSession(task, 25, 5)

        manager.endSession()

        prefs.getFocusSession() shouldBe null
        val revived = focusSessionManager(prefs, tasks, clock)
        revived.sync()
        revived.sessionState.value shouldBe FocusSessionState.Idle
        // Ending early is not a completion
        notifier.breakCompletedCount shouldBe 0
    }
})
