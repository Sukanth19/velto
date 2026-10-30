package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.EisenhowerQuadrant
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.util.Locale

class DeadlinesTest : StringSpec({
    val today = LocalDate.of(2026, 10, 30) // a Friday
    fun task(urgency: Int = 1, importance: Int = 4, due: LocalDate? = null) =
        Task("t", "Essay", null, TaskCategory.SCHOOL, urgency, importance, TaskEffort.MEDIUM, null, 0, dueDate = due)

    "urgency climbs as the due date gets close" {
        effectiveUrgency(task(due = null), today) shouldBe 1
        effectiveUrgency(task(due = today.plusDays(10)), today) shouldBe 1
        effectiveUrgency(task(due = today.plusDays(3)), today) shouldBe 3
        effectiveUrgency(task(due = today.plusDays(1)), today) shouldBe 4
        effectiveUrgency(task(due = today), today) shouldBe 5
        effectiveUrgency(task(due = today.minusDays(4)), today) shouldBe 5
    }

    "a due date never lowers urgency the user set" {
        effectiveUrgency(task(urgency = 5, due = today.plusDays(30)), today) shouldBe 5
    }

    "an important task due tomorrow moves to do first and outranks a far one" {
        val dueTomorrow = task(due = today.plusDays(1))
        EisenhowerClassifier { today }.classify(dueTomorrow) shouldBe EisenhowerQuadrant.URGENT_IMPORTANT
        val engine = ScoringEngine { today }
        engine.getBestTask(listOf(task(urgency = 3), dueTomorrow.copy(id = "soon"))) shouldBe dueTomorrow.copy(id = "soon")
    }

    "due text reads naturally" {
        dueText(today.minusDays(2), today) shouldBe "Overdue"
        dueText(today, today) shouldBe "Due today"
        dueText(today.plusDays(1), today) shouldBe "Due tomorrow"
        dueText(today.plusDays(3), today, Locale.UK) shouldBe "Due Monday"
        dueText(today.plusDays(7), today, Locale.UK) shouldBe "Due 6 Nov"
    }

    "repeats move to the next date after today" {
        Recurrence.DAILY.next(today, today) shouldBe today.plusDays(1)
        // Friday's weekday task comes back on Monday
        Recurrence.WEEKDAYS.next(today, today) shouldBe today.plusDays(3)
        Recurrence.WEEKLY.next(today, today) shouldBe today.plusWeeks(1)
        Recurrence.MONTHLY.next(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 31)) shouldBe LocalDate.of(2026, 2, 28)
    }

    "finishing an overdue repeat skips the missed dates" {
        // Due two and a half weeks ago, weekly: next is the first Friday after today
        Recurrence.WEEKLY.next(today.minusDays(14), today) shouldBe today.plusWeeks(1)
        Recurrence.DAILY.next(today.minusDays(5), today) shouldBe today.plusDays(1)
    }
})
