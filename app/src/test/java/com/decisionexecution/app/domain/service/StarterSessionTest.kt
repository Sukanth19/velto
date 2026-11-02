package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.testutil.FakeTimeSource
import com.decisionexecution.app.testutil.RecordingNotifier
import com.decisionexecution.app.testutil.focusSessionManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

/** Two-minute starters from Shrink it, and how focus counts towards dodging */
class StarterSessionTest : StringSpec({

    val task = Task("t-1", "Tax return", null, TaskCategory.PERSONAL, 3, 5, TaskEffort.HIGH, null, 1, dodges = 4)
    fun tasks(): TaskRepository = mockk<TaskRepository>(relaxed = true).also {
        every { it.getTaskById(any()) } returns flowOf(task)
    }

    "a starter runs two minutes, skips the break and asks to keep going" {
        val clock = FakeTimeSource()
        val repo = tasks()
        val notifier = RecordingNotifier()
        val performance = mockk<PerformanceRepository>(relaxed = true)
        val manager = focusSessionManager(tasks = repo, clock = clock, notifier = notifier, performance = performance)

        manager.startStarter(task)
        (manager.sessionState.value as FocusSessionState.Focusing).let {
            it.totalSeconds shouldBe 120
            it.isStarter shouldBe true
        }

        clock.advanceSeconds(125)
        manager.sync()

        manager.sessionState.value shouldBe FocusSessionState.Idle
        manager.starterFinished.value shouldBe task
        notifier.starterCompletions shouldBe listOf("Tax return")
        notifier.breakCompletedCount shouldBe 0
        coVerify { performance.recordFocusSession("t-1", any(), 2) }
        coVerify { repo.clearDodges("t-1") }

        manager.dismissStarter()
        manager.starterFinished.value shouldBe null
    }

    "keeping going from a starter is a normal session with a break" {
        val clock = FakeTimeSource()
        val manager = focusSessionManager(tasks = tasks(), clock = clock)
        manager.startStarter(task)
        clock.advanceSeconds(120)
        manager.sync()

        manager.startSession(task, 25, 5)
        manager.starterFinished.value shouldBe null
        clock.advanceSeconds(25 * 60)
        manager.sync()
        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
    }

    "quitting before halfway is a dodge, past halfway it isn't" {
        val clock = FakeTimeSource()
        val repo = tasks()
        val manager = focusSessionManager(tasks = repo, clock = clock)

        manager.startSession(task, 20, 5)
        clock.advanceSeconds(5 * 60)
        manager.endSession()
        coVerify(exactly = 1) { repo.addDodge("t-1") }

        manager.startSession(task, 20, 5)
        clock.advanceSeconds(15 * 60)
        manager.endSession()
        coVerify(exactly = 1) { repo.addDodge("t-1") }
    }

    "a full focus phase clears past dodges" {
        val clock = FakeTimeSource()
        val repo = tasks()
        val manager = focusSessionManager(tasks = repo, clock = clock)
        manager.startSession(task, 25, 5)
        clock.advanceSeconds(25 * 60)
        manager.sync()
        coVerify { repo.clearDodges("t-1") }
    }
})
