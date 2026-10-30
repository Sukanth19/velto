package com.decisionexecution.app.presentation.home

import com.decisionexecution.app.testutil.emptyInsights
import com.decisionexecution.app.testutil.InMemoryPreferencesRepository
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import com.decisionexecution.app.domain.usecase.GetProductivityMetricsUseCase
import com.decisionexecution.app.domain.usecase.JustStartUseCase
import com.decisionexecution.app.domain.usecase.ProductivityMetrics
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for HomeViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "should load best task and today's metrics on init" {
        val task = createTestTask()
        val viewModel = createViewModel(
            bestTask = task,
            metrics = ProductivityMetrics(completedTasks = 3, totalFocusTime = 75, consistencyScore = 42.5)
        )

        val state = viewModel.uiState.value
        state.bestTask shouldBe task
        state.completedTasksToday shouldBe 3
        state.totalFocusTimeToday shouldBe 75
        state.consistencyScore shouldBe 42.5
        state.isLoading shouldBe false
        state.error shouldBe null
    }

    "Just Start with no tasks should show error" {
        val justStart = mockk<JustStartUseCase>()
        coEvery { justStart() } returns JustStartResult.NoTasksAvailable
        val viewModel = createViewModel(bestTask = null, justStartUseCase = justStart)

        viewModel.onJustStartClicked()

        viewModel.uiState.value.error shouldBe HomeViewModel.NO_TASKS_ERROR
        viewModel.uiState.value.focusSessionStarted shouldBe false
    }

    "Just Start with tasks should trigger session without error" {
        val task = createTestTask()
        val justStart = mockk<JustStartUseCase>()
        coEvery { justStart() } returns JustStartResult.Success(task)
        val viewModel = createViewModel(bestTask = task, justStartUseCase = justStart)

        viewModel.onJustStartClicked()

        coVerify(exactly = 1) { justStart() }
        viewModel.uiState.value.error shouldBe null
        viewModel.uiState.value.focusSessionStarted shouldBe true

        viewModel.onNavigatedToFocus()
        viewModel.uiState.value.focusSessionStarted shouldBe false
    }

    "Just Start failure should surface error instead of crashing" {
        val justStart = mockk<JustStartUseCase>()
        coEvery { justStart() } throws IllegalStateException("database unavailable")
        val viewModel = createViewModel(bestTask = createTestTask(), justStartUseCase = justStart)

        viewModel.onJustStartClicked()

        viewModel.uiState.value.error shouldBe "Couldn't start a focus session. Please try again."
    }

    "a failed load offers retry, and retrying loads the data" {
        val task = createTestTask()
        var attempts = 0
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } answers {
            attempts++
            if (attempts == 1) flow { throw IllegalStateException("db closed") } else flowOf(task)
        }
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(0, 0, 0.0))
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, emptyInsights(), InMemoryPreferencesRepository())

        viewModel.uiState.value.canRetry shouldBe true
        viewModel.uiState.value.error shouldBe "Couldn't load today's data. Please try again."

        viewModel.onRetry()

        viewModel.uiState.value.bestTask shouldBe task
        viewModel.uiState.value.error shouldBe null
        viewModel.uiState.value.canRetry shouldBe false
    }

    "onErrorShown should clear error" {
        val justStart = mockk<JustStartUseCase>()
        coEvery { justStart() } returns JustStartResult.NoTasksAvailable
        val viewModel = createViewModel(bestTask = null, justStartUseCase = justStart)

        viewModel.onJustStartClicked()
        viewModel.onErrorShown()

        viewModel.uiState.value.error shouldBe null
    }
})

private fun createViewModel(
    bestTask: Task?,
    metrics: ProductivityMetrics = ProductivityMetrics(0, 0, 0.0),
    justStartUseCase: JustStartUseCase = mockk(relaxed = true)
): HomeViewModel {
    val getBestTask = mockk<GetBestTaskUseCase>()
    every { getBestTask() } returns flowOf(bestTask)
    val getMetrics = mockk<GetProductivityMetricsUseCase>()
    every { getMetrics(any(), any(), any()) } returns flowOf(metrics)
    return HomeViewModel(getBestTask, justStartUseCase, getMetrics, emptyInsights(), InMemoryPreferencesRepository())
}

private fun createTestTask(): Task = Task(
    id = "task-1",
    title = "Write report",
    description = null,
    category = TaskCategory.WORK,
    urgency = 4,
    importance = 5,
    effort = TaskEffort.MEDIUM,
    energyTag = EnergyTag.DEEP_WORK,
    createdAt = 1_700_000_000_000L
)
