package com.decisionexecution.app.presentation.taskdetail

import com.decisionexecution.app.domain.usecase.CalendarSyncUseCase
import androidx.lifecycle.SavedStateHandle
import com.decisionexecution.app.testutil.taskRepositoryWithSteps
import com.decisionexecution.app.testutil.FakeCalendarRepository
import com.decisionexecution.app.testutil.GOOGLE_CALENDAR
import java.time.LocalDate
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

    val test = task.copy(title = "Study for test", dueDate = LocalDate.of(2026, 11, 16))

    "add to calendar puts the task on the linked calendar" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val viewModel = createViewModel(test, calendar = calendar)
        viewModel.uiState.value.calendarLinked shouldBe true
        viewModel.uiState.value.onCalendar shouldBe false

        viewModel.onAddToCalendar()

        calendar.events.getValue("t-1").date shouldBe LocalDate.of(2026, 11, 16)
        viewModel.uiState.value.onCalendar shouldBe true
        viewModel.uiState.value.calendarMessage shouldBe "Added to your calendar"
    }

    "remove takes it back off the calendar" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val viewModel = createViewModel(test, calendar = calendar)
        viewModel.onAddToCalendar()

        viewModel.onRemoveFromCalendar()

        calendar.events shouldBe emptyMap()
        viewModel.uiState.value.onCalendar shouldBe false
        viewModel.uiState.value.calendarMessage shouldBe "Removed from your calendar"
    }

    "lost calendar access says to link again" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR).apply { failWith = SecurityException("revoked") }
        val viewModel = createViewModel(test, calendar = calendar)

        viewModel.onAddToCalendar()

        viewModel.uiState.value.onCalendar shouldBe false
        viewModel.uiState.value.calendarMessage shouldBe "Velto can't reach your calendar. Link it again in Settings"
    }

    "without a linked calendar it says to link one" {
        val viewModel = createViewModel(test, calendar = FakeCalendarRepository(linked = null))

        viewModel.uiState.value.calendarLinked shouldBe false
        viewModel.onAddToCalendar()

        viewModel.uiState.value.calendarMessage shouldBe "Link a calendar in Settings first"
    }
})

private fun createViewModel(
    task: Task?,
    complete: CompleteTaskUseCase = mockk(relaxed = true),
    delete: DeleteTaskUseCase = mockk(relaxed = true),
    calendar: FakeCalendarRepository = FakeCalendarRepository()
): TaskDetailViewModel {
    val getTask = mockk<GetTaskByIdUseCase>()
    every { getTask(any()) } returns flowOf(task)
    val handle = SavedStateHandle(mapOf(TaskDetailViewModel.TASK_ID_ARG to "t-1"))
    return TaskDetailViewModel(handle, getTask, complete, delete, taskRepositoryWithSteps(), calendar, CalendarSyncUseCase(calendar))
}
