package com.decisionexecution.app.presentation.taskdetail

import androidx.lifecycle.SavedStateHandle
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.DeleteTaskUseCase
import com.decisionexecution.app.domain.usecase.GetTaskByIdUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for TaskDetailViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    val task = Task("t-1", "Read chapter 3", null, TaskCategory.SCHOOL, 3, 4, TaskEffort.MEDIUM, EnergyTag.LOW, 1L)

    "loads the task by id" {
        val viewModel = createViewModel(task)
        viewModel.uiState.value.task shouldBe task
        viewModel.uiState.value.isLoading shouldBe false
    }

    "delete removes the task and finishes" {
        val delete = mockk<DeleteTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(task, delete = delete)

        viewModel.onDelete()

        coVerify { delete("t-1") }
        viewModel.uiState.value.isFinished shouldBe true
    }

    "complete records completion and finishes" {
        val complete = mockk<CompleteTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(task, complete = complete)

        viewModel.onComplete()

        coVerify { complete("t-1", any()) }
        viewModel.uiState.value.isFinished shouldBe true
    }

    "failed delete keeps the screen open with an error" {
        val delete = mockk<DeleteTaskUseCase>()
        coEvery { delete(any()) } throws IllegalStateException("locked")
        val viewModel = createViewModel(task, delete = delete)

        viewModel.onDelete()

        viewModel.uiState.value.isFinished shouldBe false
        viewModel.uiState.value.error shouldBe "Couldn't delete the task. Please try again."
    }
})

private fun createViewModel(
    task: Task?,
    complete: CompleteTaskUseCase = mockk(relaxed = true),
    delete: DeleteTaskUseCase = mockk(relaxed = true)
): TaskDetailViewModel {
    val getTask = mockk<GetTaskByIdUseCase>()
    every { getTask(any()) } returns flowOf(task)
    val handle = SavedStateHandle(mapOf(TaskDetailViewModel.TASK_ID_ARG to "t-1"))
    return TaskDetailViewModel(handle, getTask, complete, delete)
}
