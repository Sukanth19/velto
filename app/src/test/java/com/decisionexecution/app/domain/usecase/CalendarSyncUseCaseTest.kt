package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.testutil.FakeCalendarRepository
import com.decisionexecution.app.testutil.GOOGLE_CALENDAR
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class CalendarSyncUseCaseTest : StringSpec({

    val monday = LocalDate.of(2026, 11, 16)
    val studyForTest = Task("t-1", "Study for test", null, TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, null, 1L, dueDate = monday)

    "adds the task to the linked calendar with the chosen reminder" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR, reminder = CalendarReminder.TWO_DAYS_BEFORE)

        CalendarSyncUseCase(calendar).add(studyForTest) shouldBe CalendarResult.ADDED

        val event = calendar.events.getValue("t-1")
        event.date shouldBe monday
        event.reminderMinutesBefore shouldBe CalendarReminder.TWO_DAYS_BEFORE.minutesBefore
        calendar.calendarOf("t-1") shouldBe GOOGLE_CALENDAR.id
    }

    "adding twice updates the one event instead of making a second" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val sync = CalendarSyncUseCase(calendar)

        sync.add(studyForTest)
        sync.add(studyForTest.copy(title = "Study for the maths test"))

        calendar.events.size shouldBe 1
        calendar.events.getValue("t-1").title shouldBe "Study for the maths test"
    }

    "nothing is added without a linked calendar" {
        val calendar = FakeCalendarRepository(linked = null)

        CalendarSyncUseCase(calendar).add(studyForTest) shouldBe CalendarResult.NOT_LINKED
        calendar.events shouldBe emptyMap()
    }

    "nothing is added without a due date" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)

        CalendarSyncUseCase(calendar).add(studyForTest.copy(dueDate = null)) shouldBe CalendarResult.NO_DUE_DATE
        calendar.events shouldBe emptyMap()
    }

    "missing calendar access is reported, not thrown" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR).apply { failWith = SecurityException("no access") }

        CalendarSyncUseCase(calendar).add(studyForTest) shouldBe CalendarResult.NO_PERMISSION
    }

    "any other calendar failure is reported, not thrown" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR).apply { failWith = IllegalStateException("provider died") }

        CalendarSyncUseCase(calendar).add(studyForTest) shouldBe CalendarResult.FAILED
    }

    "refresh moves the event when the due date moves" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val sync = CalendarSyncUseCase(calendar)
        sync.add(studyForTest)

        sync.refresh(studyForTest.copy(dueDate = monday.plusDays(2)))

        calendar.events.getValue("t-1").date shouldBe monday.plusDays(2)
    }

    "refresh takes the event off when the due date is cleared" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val sync = CalendarSyncUseCase(calendar)
        sync.add(studyForTest)

        sync.refresh(studyForTest.copy(dueDate = null))

        calendar.events shouldBe emptyMap()
    }

    "refresh leaves tasks the user never added alone" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)

        CalendarSyncUseCase(calendar).refresh(studyForTest)

        calendar.events shouldBe emptyMap()
    }

    "remove and refresh swallow calendar failures" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val sync = CalendarSyncUseCase(calendar)
        sync.add(studyForTest)
        calendar.failWith = SecurityException("revoked")

        sync.remove("t-1")
        sync.refresh(studyForTest.copy(dueDate = monday.plusDays(1)))

        sync.isOnCalendar("t-1") shouldBe true
    }
})
