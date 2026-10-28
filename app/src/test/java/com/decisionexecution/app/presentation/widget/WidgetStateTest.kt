package com.decisionexecution.app.presentation.widget

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import com.decisionexecution.app.domain.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * What the widgets display and when they need redrawing.
 * Feature: decision-driven-execution-app
 */
class WidgetStateTest : StringSpec({

    val task = Task("t-1", "Write essay", null, TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK, 1L)

    "best task widget shows title, category and effort" {
        // Requirements: 8.1
        val state = bestTaskWidgetState(task)
        state.taskId shouldBe "t-1"
        state.title shouldBe "Write essay"
        state.details shouldBe "School · High effort"
    }

    "best task widget shows an empty state with no task to open" {
        // Requirements: 8.1, 8.4
        val state = bestTaskWidgetState(null)
        state.taskId shouldBe null
        state.title shouldBe "No tasks available"
    }

    "timer widget is idle with no session" {
        // Requirements: 8.2
        timerWidgetState(FocusSessionState.Idle) shouldBe TimerWidgetState.Idle
    }

    "timer widget remaining time matches the session for any elapsed time" {
        // Requirements: 8.2, 8.5
        checkAll(PROPERTY_ITERATIONS, Arb.int(1..5400), Arb.int(0..6000)) { total, elapsed ->
            val running = timerWidgetState(FocusSessionState.Focusing(task, elapsed, total))
            running.shouldBeInstanceOf<TimerWidgetState.Active>()
            running.remainingMillis shouldBe maxOf(0, total - elapsed) * 1000L
            running.isRunning shouldBe true
            running.title shouldBe "Write essay"

            val paused = timerWidgetState(FocusSessionState.Paused(task, elapsed, total))
            (paused as TimerWidgetState.Active).isRunning shouldBe false
        }
    }

    "timer widget tells break apart from focus" {
        // Requirements: 5.6, 8.2
        val focus = timerWidgetState(FocusSessionState.Focusing(task, 0, 1500)) as TimerWidgetState.Active
        val brk = timerWidgetState(FocusSessionState.Breaking(0, 300)) as TimerWidgetState.Active
        focus.label shouldBe "Focus"
        brk.label shouldBe "Break"
    }

    "timer widget only redraws on phase changes, not every tick" {
        // Requirements: 8.5
        checkAll(PROPERTY_ITERATIONS, Arb.int(0..1499), Arb.int(0..1499)) { a, b ->
            timerWidgetKey(FocusSessionState.Focusing(task, a, 1500)) shouldBe
                timerWidgetKey(FocusSessionState.Focusing(task, b, 1500))
        }
        val focusing = timerWidgetKey(FocusSessionState.Focusing(task, 100, 1500))
        focusing shouldNotBe timerWidgetKey(FocusSessionState.Paused(task, 100, 1500))
        focusing shouldNotBe timerWidgetKey(FocusSessionState.Breaking(0, 300))
        focusing shouldNotBe timerWidgetKey(FocusSessionState.Idle)
    }
})
