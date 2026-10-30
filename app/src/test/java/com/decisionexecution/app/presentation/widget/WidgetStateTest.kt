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
 */
class WidgetStateTest : StringSpec({

    val task = Task("t-1", "Write essay", null, TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK, 1L)

    "best task widget shows title, category and effort" {
        val state = bestTaskWidgetState(task)
        state.taskId shouldBe "t-1"
        state.title shouldBe "Write essay"
        state.details shouldBe "School · High effort"
    }

    "best task widget shows an empty state with no task to open" {
        val state = bestTaskWidgetState(null)
        state.taskId shouldBe null
        state.title shouldBe "No tasks available"
    }

    "timer widget is idle with no session" {
        timerWidgetState(FocusSessionState.Idle) shouldBe TimerWidgetState.Idle
    }

    "timer widget remaining time matches the session for any elapsed time" {
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
        val focus = timerWidgetState(FocusSessionState.Focusing(task, 0, 1500)) as TimerWidgetState.Active
        val brk = timerWidgetState(FocusSessionState.Breaking(0, 300)) as TimerWidgetState.Active
        focus.label shouldBe "Focus"
        brk.label shouldBe "Break"
    }

    "timer widget only redraws on phase changes, not every tick" {
        checkAll(PROPERTY_ITERATIONS, Arb.int(0..1499), Arb.int(0..1499)) { a, b ->
            timerWidgetKey(FocusSessionState.Focusing(task, a, 1500)) shouldBe
                timerWidgetKey(FocusSessionState.Focusing(task, b, 1500))
        }
        val focusing = timerWidgetKey(FocusSessionState.Focusing(task, 100, 1500))
        focusing shouldNotBe timerWidgetKey(FocusSessionState.Paused(task, 100, 1500))
        focusing shouldNotBe timerWidgetKey(FocusSessionState.Breaking(0, 300))
        focusing shouldNotBe timerWidgetKey(FocusSessionState.Idle)
    }

    "timer widget redraws when a paused session is extended" {
        timerWidgetKey(FocusSessionState.Paused(task, 100, 1500)) shouldNotBe
            timerWidgetKey(FocusSessionState.Paused(task, 100, 1800))
    }

    "timer widget offers break controls only during a break" {
        (timerWidgetState(FocusSessionState.Breaking(0, 300)) as TimerWidgetState.Active).isBreak shouldBe true
        (timerWidgetState(FocusSessionState.Focusing(task, 0, 1500)) as TimerWidgetState.Active).isBreak shouldBe false
    }

    "today widget tracks progress towards the daily goal" {
        val halfway = TodayWidgetState(focusMinutes = 60, goalMinutes = 120, completedTasks = 2, streakDays = 1)
        halfway.progress shouldBe 0.5f
        halfway.goalReached shouldBe false
        halfway.goalLabel shouldBe "50% of 2h 00m"
        halfway.streakLabel shouldBe "1-day streak"

        val over = TodayWidgetState(focusMinutes = 150, goalMinutes = 120, completedTasks = 4, streakDays = 0)
        over.progress shouldBe 1f
        over.goalLabel shouldBe "Goal reached"
        over.streakLabel shouldBe null

        TodayWidgetState(0, 0, 0, 0).progress shouldBe 0f
    }

    "top tasks widget shows at most three rows" {
        val tasks = (1..5).map { task.copy(id = "t-$it", title = "Task $it") }
        topTaskRows(tasks).map { it.taskId } shouldBe listOf("t-1", "t-2", "t-3")
    }
})
