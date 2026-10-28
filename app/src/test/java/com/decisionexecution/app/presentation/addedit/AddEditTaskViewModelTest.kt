package com.decisionexecution.app.presentation.addedit

import androidx.lifecycle.SavedStateHandle
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.usecase.CreateTaskUseCase
import com.decisionexecution.app.domain.usecase.GetTaskByIdUseCase
import com.decisionexecution.app.domain.usecase.UpdateTaskUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for AddEditTaskViewModel.
 * Feature: decision-driven-execution-app
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTaskViewModelTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "blank title is rejected and nothing is saved" {
        // Requirements: 1.1
        val create = mockk<CreateTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(create = create)

        viewModel.onTitleChange("   ")
        viewModel.onSave()

        viewModel.uiState.value.titleError shouldNotBe null
        viewModel.uiState.value.isSaved shouldBe false
        coVerify(exactly = 0) { create(any()) }
    }

    "new task is created with the form values" {
        // Requirements: 1.1, 1.4, 1.5, 1.6
        val create = mockk<CreateTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(create = create)

        viewModel.onTitleChange("  Study for exam ")
        viewModel.onDescriptionChange("")
        viewModel.onCategoryChange(TaskCategory.SCHOOL)
        viewModel.onUrgencyChange(5)
        viewModel.onImportanceChange(4)
        viewModel.onEffortChange(TaskEffort.HIGH)
        viewModel.onEnergyTagChange(EnergyTag.DEEP_WORK)
        viewModel.onSave()

        val saved = slot<Task>()
        coVerify { create(capture(saved)) }
        saved.captured.title shouldBe "Study for exam"
        saved.captured.description shouldBe null
        saved.captured.category shouldBe TaskCategory.SCHOOL
        saved.captured.urgency shouldBe 5
        saved.captured.importance shouldBe 4
        saved.captured.effort shouldBe TaskEffort.HIGH
        saved.captured.energyTag shouldBe EnergyTag.DEEP_WORK
        viewModel.uiState.value.isSaved shouldBe true
    }

    "editing loads the task and updates it keeping id and createdAt" {
        // Requirements: 1.7
        val existing = Task("t-1", "Old", "desc", TaskCategory.WORK, 2, 3, TaskEffort.LOW, null, 1_600_000_000_000L)
        val update = mockk<UpdateTaskUseCase>(relaxed = true)
        val create = mockk<CreateTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(taskId = "t-1", existing = existing, create = create, update = update)

        viewModel.uiState.value.isEditing shouldBe true
        viewModel.uiState.value.title shouldBe "Old"
        viewModel.uiState.value.urgency shouldBe 2

        viewModel.onTitleChange("New")
        viewModel.onSave()

        coVerify(exactly = 0) { create(any()) }
        coVerify { update(existing.copy(title = "New")) }
    }

    "urgency and importance are clamped to 1-5 for any slider input" {
        // Requirements: 1.2, 1.3
        checkAll(100, Arb.int(-50..50), Arb.int(-50..50)) { urgency, importance ->
            val viewModel = createViewModel()
            viewModel.onUrgencyChange(urgency)
            viewModel.onImportanceChange(importance)

            viewModel.uiState.value.urgency shouldBe urgency.coerceIn(1, 5)
            viewModel.uiState.value.importance shouldBe importance.coerceIn(1, 5)
        }
    }

    "save failure surfaces an error" {
        // Requirements: 12.4
        val create = mockk<CreateTaskUseCase>()
        coEvery { create(any()) } throws IllegalStateException("disk full")
        val viewModel = createViewModel(create = create)

        viewModel.onTitleChange("Task")
        viewModel.onSave()

        viewModel.uiState.value.error shouldBe "disk full"
        viewModel.uiState.value.isSaved shouldBe false
    }
})

private fun createViewModel(
    taskId: String? = null,
    existing: Task? = null,
    create: CreateTaskUseCase = mockk(relaxed = true),
    update: UpdateTaskUseCase = mockk(relaxed = true)
): AddEditTaskViewModel {
    val getTask = mockk<GetTaskByIdUseCase>()
    every { getTask(any()) } returns flowOf(existing)
    val handle = SavedStateHandle(mapOf(AddEditTaskViewModel.TASK_ID_ARG to taskId))
    return AddEditTaskViewModel(handle, getTask, create, update)
}
