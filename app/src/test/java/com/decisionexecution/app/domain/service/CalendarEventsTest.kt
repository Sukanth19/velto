package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.localDate
import io.kotest.property.checkAll
import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CalendarEventsTest : StringSpec({

    val monday = LocalDate.of(2026, 11, 16)
    val studyForTest = Task(
        "t-1", "Study for test", "Chapters 4 to 6", TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, null, 1L,
        dueDate = monday
    )

    "a task with a due date becomes an all-day event on that date" {
        val event = calendarEventFor(studyForTest, CalendarReminder.MORNING_BEFORE)!!

        event.title shouldBe "Study for test"
        event.description shouldBe "Chapters 4 to 6"
        event.date shouldBe monday
        event.reminderMinutesBefore shouldBe 15 * 60
    }

    "a task without a due date has no event" {
        calendarEventFor(studyForTest.copy(dueDate = null), CalendarReminder.MORNING_BEFORE) shouldBe null
    }

    "a blank description is left out" {
        calendarEventFor(studyForTest.copy(description = "  "), CalendarReminder.NONE)!!.description shouldBe null
    }

    "no reminder means no reminder minutes" {
        calendarEventFor(studyForTest, CalendarReminder.NONE)!!.reminderMinutesBefore shouldBe null
    }

    "reminders count back from midnight at the start of the due date" {
        // The evening before at 6 PM is 6 hours before midnight
        CalendarReminder.EVENING_BEFORE.minutesBefore shouldBe 6 * 60
        // The day before at 9 AM is 15 hours before
        CalendarReminder.MORNING_BEFORE.minutesBefore shouldBe 15 * 60
        // Two days before at 9 AM is a day and 15 hours before
        CalendarReminder.TWO_DAYS_BEFORE.minutesBefore shouldBe 24 * 60 + 15 * 60
        CalendarReminder.WEEK_BEFORE.minutesBefore shouldBe 6 * 24 * 60 + 15 * 60
    }

    "every reminder fires before the due date starts" {
        CalendarReminder.entries.mapNotNull { it.minutesBefore }.forEach { (it > 0) shouldBe true }
    }

    "all-day events run from midnight UTC to midnight UTC the next day" {
        allDayStartMillis(monday) shouldBe Instant.parse("2026-11-16T00:00:00Z").toEpochMilli()
        allDayEndMillis(monday) shouldBe Instant.parse("2026-11-17T00:00:00Z").toEpochMilli()
    }

    "all-day times always land back on the same date and last exactly a day" {
        checkAll(PROPERTY_ITERATIONS, Arb.localDate(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31))) { date ->
            Instant.ofEpochMilli(allDayStartMillis(date)).atZone(ZoneOffset.UTC).toLocalDate() shouldBe date
            allDayEndMillis(date) - allDayStartMillis(date) shouldBe 24L * 60 * 60 * 1000
        }
    }
})
