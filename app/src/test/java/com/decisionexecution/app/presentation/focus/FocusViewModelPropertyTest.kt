package com.decisionexecution.app.presentation.focus

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import com.decisionexecution.app.testutil.taskRepositoryWithSteps
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.EndFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.ExtendFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.PauseFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.ResumeFocusSessionUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.mockk
import com.decisionexecution.app.testutil.FakeTimeSource
import com.decisionexecution.app.testutil.focusSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.UUID

/**
 * Property-based tests for FocusViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelPropertyTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "Focus UI Task Display" {
        // For any active focus session,
        // the focus UI state should contain the task title of the task being worked on.

        checkAll(PROPERTY_ITERATIONS, taskArb(), Arb.int(5..90), Arb.int(1..30), Arb.boolean()) { task, focus, brk, paused ->
            val clock = FakeTimeSource()
            val manager = focusSessionManager(clock = clock)
            val viewModel = createViewModel(manager)

            manager.startSession(task, focus, brk)
            if (paused) viewModel.onPause()

            val state = viewModel.uiState.value
            state.shouldBeInstanceOf<FocusUiState.Focus>()
            state.taskTitle shouldBe task.title
            state.isPaused shouldBe paused
        }
    }

    "Focus vs Break State Distinction" {
        // For any focus session, the UI state
        // during focus time should be distinguishable from the UI state during break time.

        checkAll(PROPERTY_ITERATIONS, taskArb(), Arb.int(5..90), Arb.int(1..30)) { task, focus, brk ->
            val clock = FakeTimeSource()
            val manager = focusSessionManager(clock = clock)
            val viewModel = createViewModel(manager)

            manager.startSession(task, focus, brk)
            val focusState = viewModel.uiState.value
            focusState.shouldBeInstanceOf<FocusUiState.Focus>()
            focusState.totalSeconds shouldBe focus * 60

            manager.transitionToBreak()
            val breakState = viewModel.uiState.value
            breakState.shouldBeInstanceOf<FocusUiState.Break>()
            breakState.totalSeconds shouldBe brk * 60

            (focusState == breakState) shouldBe false
        }
    }

    "remaining time is total minus elapsed and never negative" {
        checkAll(PROPERTY_ITERATIONS, taskArb(), Arb.int(5..90), Arb.int(0..10_000)) { task, focus, rawElapsed ->
            // Stay inside the focus phase; going past it correctly moves to the break
            val elapsed = rawElapsed % (focus * 60)
            val clock = FakeTimeSource()
            val manager = focusSessionManager(clock = clock)
            val viewModel = createViewModel(manager)

            manager.startSession(task, focus, 5)
            clock.advanceSeconds(elapsed)
            manager.sync()

            val state = viewModel.uiState.value as FocusUiState.Focus
            state.remainingSeconds shouldBe focus * 60 - elapsed
        }
    }

    "ending a session returns UI to idle" {
        checkAll(PROPERTY_ITERATIONS, taskArb()) { task ->
            val clock = FakeTimeSource()
            val manager = focusSessionManager(clock = clock)
            val viewModel = createViewModel(manager)

            manager.startSession(task, 25, 5)
            viewModel.onEnd()

            viewModel.uiState.value shouldBe FocusUiState.Idle
        }
    }
})

private fun createViewModel(manager: FocusSessionManager) = FocusViewModel(
    manager,
    PauseFocusSessionUseCase(manager),
    ResumeFocusSessionUseCase(manager),
    EndFocusSessionUseCase(manager),
    ExtendFocusSessionUseCase(manager),
    taskRepositoryWithSteps()
)

private fun taskArb(): Arb<Task> = arbitrary {
    Task(
        id = UUID.randomUUID().toString(),
        title = Arb.string(1..50).bind(),
        description = null,
        category = Arb.enum<TaskCategory>().bind(),
        urgency = Arb.int(1..5).bind(),
        importance = Arb.int(1..5).bind(),
        effort = Arb.enum<TaskEffort>().bind(),
        energyTag = Arb.enum<EnergyTag>().orNull().bind(),
        createdAt = Arb.long(1_000_000_000_000L..2_000_000_000_000L).bind()
    )
}
