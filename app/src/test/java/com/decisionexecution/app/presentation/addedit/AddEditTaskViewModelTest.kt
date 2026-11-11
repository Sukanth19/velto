package com.decisionexecution.app.presentation.addedit

import com.decisionexecution.app.domain.usecase.CalendarSyncUseCase
import com.decisionexecution.app.testutil.noCalibration
import com.decisionexecution.app.testutil.FakeCalendarRepository
import com.decisionexecution.app.testutil.GOOGLE_CALENDAR
import java.time.LocalDate
import com.decisionexecution.app.testutil.stubPick
import com.decisionexecution.app.domain.usecase.DayPhaseSource

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import com.decisionexecution.app.testutil.taskRepositoryWithSteps
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTaskViewModelTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "blank title is rejected and nothing is saved" {
        val create = mockk<CreateTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(create = create)

        viewModel.onTitleChange("   ")
        viewModel.onSave()

        viewModel.uiState.value.titleError shouldNotBe null
        viewModel.uiState.value.isSaved shouldBe false
        coVerify(exactly = 0) { create(any()) }
    }

    "new task is created with the form values" {
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
        checkAll(PROPERTY_ITERATIONS, Arb.int(-50..50), Arb.int(-50..50)) { urgency, importance ->
            val viewModel = createViewModel()
            viewModel.onUrgencyChange(urgency)
            viewModel.onImportanceChange(importance)

            viewModel.uiState.value.urgency shouldBe urgency.coerceIn(1, 5)
            viewModel.uiState.value.importance shouldBe importance.coerceIn(1, 5)
        }
    }

    "save failure surfaces an error" {
        val create = mockk<CreateTaskUseCase>()
        coEvery { create(any()) } throws IllegalStateException("disk full")
        val viewModel = createViewModel(create = create)

        viewModel.onTitleChange("Task")
        viewModel.onSave()

        viewModel.uiState.value.error shouldBe "Couldn't save the task. Please try again."
        viewModel.uiState.value.isSaved shouldBe false
    }

    "with a linked calendar, a new task with a due date goes on it by default" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR, reminder = CalendarReminder.EVENING_BEFORE)
        val monday = LocalDate.of(2026, 11, 16)
        val viewModel = createViewModel(calendar = calendar)

        viewModel.uiState.value.calendarLinked shouldBe true
        viewModel.uiState.value.addToCalendar shouldBe true
        viewModel.onTitleChange("Study for test")
        viewModel.onDueDateChange(monday)
        viewModel.onSave()

        viewModel.uiState.value.isSaved shouldBe true
        val event = calendar.events.values.single()
        event.title shouldBe "Study for test"
        event.date shouldBe monday
        event.reminderMinutesBefore shouldBe 6 * 60
        calendar.calendarOf(calendar.events.keys.single()) shouldBe GOOGLE_CALENDAR.id
    }

    "turning the calendar switch off keeps the task off the calendar" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val viewModel = createViewModel(calendar = calendar)

        viewModel.onTitleChange("Study for test")
        viewModel.onDueDateChange(LocalDate.of(2026, 11, 16))
        viewModel.onAddToCalendarChange(false)
        viewModel.onSave()

        viewModel.uiState.value.isSaved shouldBe true
        calendar.events shouldBe emptyMap()
    }

    "a task without a due date isn't put on the calendar" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val viewModel = createViewModel(calendar = calendar)

        viewModel.onTitleChange("Someday task")
        viewModel.onSave()

        viewModel.uiState.value.isSaved shouldBe true
        calendar.events shouldBe emptyMap()
    }

    "without a linked calendar nothing is added and the switch is off" {
        val calendar = FakeCalendarRepository(linked = null)
        val viewModel = createViewModel(calendar = calendar)

        viewModel.uiState.value.calendarLinked shouldBe false
        viewModel.uiState.value.addToCalendar shouldBe false
        viewModel.onTitleChange("Study for test")
        viewModel.onDueDateChange(LocalDate.of(2026, 11, 16))
        viewModel.onSave()

        calendar.events shouldBe emptyMap()
    }

    "editing a task on the calendar moves its event to the new date" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val existing = Task("t-1", "Study for test", null, TaskCategory.SCHOOL, 3, 4, TaskEffort.MEDIUM, null, 1L, dueDate = LocalDate.of(2026, 11, 16))
        CalendarSyncUseCase(calendar).add(existing)
        val viewModel = createViewModel(taskId = "t-1", existing = existing, calendar = calendar)

        viewModel.uiState.value.addToCalendar shouldBe true
        viewModel.onDueDateChange(LocalDate.of(2026, 11, 18))
        viewModel.onSave()

        calendar.events.getValue("t-1").date shouldBe LocalDate.of(2026, 11, 18)
    }

    "switching the calendar off when editing removes the event" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val existing = Task("t-1", "Study for test", null, TaskCategory.SCHOOL, 3, 4, TaskEffort.MEDIUM, null, 1L, dueDate = LocalDate.of(2026, 11, 16))
        CalendarSyncUseCase(calendar).add(existing)
        val viewModel = createViewModel(taskId = "t-1", existing = existing, calendar = calendar)

        viewModel.onAddToCalendarChange(false)
        viewModel.onSave()

        calendar.events shouldBe emptyMap()
    }

    "a calendar failure still saves the task" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR).apply { failWith = SecurityException("revoked") }
        val create = mockk<CreateTaskUseCase>(relaxed = true)
        val viewModel = createViewModel(create = create, calendar = calendar)

        viewModel.onTitleChange("Study for test")
        viewModel.onDueDateChange(LocalDate.of(2026, 11, 16))
        viewModel.onSave()

        coVerify { create(any()) }
        viewModel.uiState.value.isSaved shouldBe true
        viewModel.uiState.value.error shouldBe null
    }
})

private fun createViewModel(
    taskId: String? = null,
    existing: Task? = null,
    create: CreateTaskUseCase = mockk(relaxed = true),
    update: UpdateTaskUseCase = mockk(relaxed = true),
    calendar: FakeCalendarRepository = FakeCalendarRepository()
): AddEditTaskViewModel {
    val getTask = mockk<GetTaskByIdUseCase>()
    every { getTask(any()) } returns flowOf(existing)
    val handle = SavedStateHandle(mapOf(AddEditTaskViewModel.TASK_ID_ARG to taskId))
    return AddEditTaskViewModel(handle, getTask, create, update, mockk(relaxed = true), taskRepositoryWithSteps(), noCalibration(), calendar, CalendarSyncUseCase(calendar))
}
