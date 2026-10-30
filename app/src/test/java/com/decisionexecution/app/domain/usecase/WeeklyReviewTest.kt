package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitIcon
import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.FocusSessionRecord
import com.decisionexecution.app.domain.repository.TaskCompletion
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate
import java.time.ZoneOffset

class WeeklyReviewTest : StringSpec({
    val zone = ZoneOffset.UTC
    val today = LocalDate.of(2026, 10, 30)
    fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()
    fun done(title: String, day: LocalDate, hour: Int = 12) = TaskCompletion(title, "t-$title", title, at(day, hour), null)
    // completedAt is when the session ended, so a 9:00 start for 60 minutes ends at 10:00
    fun focus(day: LocalDate, startHour: Int, minutes: Int) =
        FocusSessionRecord("t", at(day, startHour) + minutes * 60_000L, minutes)

    "this week is compared with the week before" {
        val review = buildWeeklyReview(
            today, zone,
            completions = listOf(done("A", today), done("B", today.minusDays(2)), done("Old", today.minusDays(9))),
            sessions = listOf(focus(today, 9, 60), focus(today.minusDays(8), 9, 30)),
            habits = emptyList(),
            moods = emptyList()
        )
        review.thisWeek.tasksDone shouldBe 2
        review.lastWeek.tasksDone shouldBe 1
        review.thisWeek.focusMinutes shouldBe 60
        review.focusChange shouldBe 100
        review.tasksChange shouldBe 100
        review.wins shouldBe listOf("A", "B")
        review.bestDay shouldBe today
    }

    "habit rate counts only the days each habit existed" {
        val created = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val habit = HabitWithHistory(Habit("h", "Read", HabitIcon.READ, created), setOf(today, today.minusDays(1)))
        val review = buildWeeklyReview(today, zone, emptyList(), emptyList(), listOf(habit), emptyList())
        review.thisWeek.habitCheckIns shouldBe 2
        // 2 of the 3 days since it was made
        review.thisWeek.habitRate shouldBe 2f / 3
    }

    "a repeating task done several times is one win" {
        val review = buildWeeklyReview(
            today, zone,
            completions = listOf(done("Water plants", today), done("Water plants", today.minusDays(1))),
            sessions = emptyList(), habits = emptyList(), moods = emptyList()
        )
        review.wins shouldBe listOf("Water plants")
    }

    "focus hours use when sessions started, and need an hour of data" {
        focusByHour(listOf(focus(today, 9, 60)), zone)[9] shouldBe 60
        bestFocusWindow(focusByHour(listOf(focus(today, 9, 30)), zone)) shouldBe null
        val window = bestFocusWindow(focusByHour(listOf(focus(today, 9, 50), focus(today, 10, 40), focus(today, 20, 25)), zone))
        window shouldBe FocusWindow(9, 2, 90)
    }

    "one busy hour gives a one-hour window, not a tie towards the empty hour before it" {
        bestFocusWindow(focusByHour(listOf(focus(today, 9, 50), focus(today, 9, 30)), zone)) shouldBe FocusWindow(9, 1, 80)
    }

    "the best window can wrap past midnight" {
        val window = bestFocusWindow(focusByHour(listOf(focus(today, 23, 45), focus(today.plusDays(1), 0, 45)), zone))
        window?.startHour shouldBe 23
        window?.endHour shouldBe 1
    }

    "suggestions pick the weakest spot" {
        val empty = PeriodTotals(0, 0, 0, null, null)
        suggestionFor(empty, empty, null) shouldContain "Fresh start"
        suggestionFor(PeriodTotals(3, 100, 2, 0.2f, null), empty, null) shouldContain "Habits slipped"
        suggestionFor(PeriodTotals(3, 50, 0, null, null), PeriodTotals(3, 200, 0, null, null), null) shouldContain "Focus dipped"
        suggestionFor(PeriodTotals(3, 200, 0, null, null), empty, FocusWindow(9, 2, 120)) shouldContain "9am–11am"
    }

    "mood average covers only this week" {
        val review = buildWeeklyReview(
            today, zone, emptyList(), emptyList(), emptyList(),
            moods = listOf(MoodEntry(today, 5, 3, null), MoodEntry(today.minusDays(1), 3, 3, null), MoodEntry(today.minusDays(10), 1, 1, null))
        )
        review.thisWeek.averageMood shouldBe 4.0
        review.lastWeek.averageMood shouldBe 1.0
    }

    "hours read like a clock" {
        formatHour(0) shouldBe "12am"
        formatHour(9) shouldBe "9am"
        formatHour(12) shouldBe "12pm"
        formatHour(18) shouldBe "6pm"
    }
})
