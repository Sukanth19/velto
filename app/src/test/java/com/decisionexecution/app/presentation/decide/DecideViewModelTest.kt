package com.decisionexecution.app.presentation.decide

import com.decisionexecution.app.testutil.noCalibration
import com.decisionexecution.app.testutil.stubPick
import com.decisionexecution.app.domain.usecase.DayPhaseSource

import androidx.lifecycle.SavedStateHandle
import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.usecase.PickTaskUseCase
import com.decisionexecution.app.domain.usecase.StartTimeboxedFocusUseCase
import com.decisionexecution.app.testutil.InMemoryMoodRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DecideViewModelTest : StringSpec({
    val report = Task("1", "Report", null, TaskCategory.WORK, 4, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK, 1)
    val email = Task("2", "Email", null, TaskCategory.WORK, 4, 3, TaskEffort.LOW, EnergyTag.LOW, 2)

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    fun viewModel(minutes: Int, start: StartTimeboxedFocusUseCase = mockk(relaxed = true)): DecideViewModel {
        val tasks = mockk<TaskRepository>()
        every { tasks.getAllTasks() } returns flowOf(listOf(report, email))
        return DecideViewModel(
            SavedStateHandle(mapOf(DecideViewModel.MINUTES_ARG to minutes)),
            PickTaskUseCase(tasks, InMemoryMoodRepository(), DayPhaseSource.NEUTRAL, noCalibration()),
            start
        )
    }

    "the time box from the link decides the first pick" {
        viewModel(15).uiState.value.pick shouldBe email
        viewModel(60).uiState.value.pick shouldBe report
    }

    "changing the time re-ranks" {
        val vm = viewModel(15)
        vm.onMinutesChange(60)
        vm.uiState.value.pick shouldBe report
    }

    "spin again walks down the list and wraps" {
        val vm = viewModel(60)
        vm.onSpinAgain()
        vm.uiState.value.pick shouldBe email
        vm.onSpinAgain()
        vm.uiState.value.pick shouldBe report
    }

    "starting uses exactly the chosen time box" {
        val start = mockk<StartTimeboxedFocusUseCase>(relaxed = true)
        val vm = viewModel(45, start)
        vm.onStart()
        coVerify { start(vm.uiState.value.pick!!, 45) }
        vm.uiState.value.started shouldBe true
    }
})
