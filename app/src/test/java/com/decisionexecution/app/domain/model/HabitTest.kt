package com.decisionexecution.app.domain.model

import com.decisionexecution.app.data.repository.longestRun
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class HabitTest : StringSpec({
    val today = LocalDate.of(2026, 10, 30)
    fun history(vararg daysAgo: Long) =
        HabitWithHistory(Habit("h", "Read", HabitIcon.READ, 0), daysAgo.map { today.minusDays(it) }.toSet())

    "streak counts consecutive days up to today" {
        history(0, 1, 2, 4).streak(today) shouldBe 3
    }

    "an unchecked today doesn't break yesterday's streak" {
        history(1, 2, 3).streak(today) shouldBe 3
    }

    "a missed yesterday resets the streak" {
        history(2, 3, 4).streak(today) shouldBe 0
    }

    "longest run finds the best stretch ever, ignoring duplicates and order" {
        longestRun(listOf(10, 3, 4, 5, 5, 11, 12, 13, 14)) shouldBe 5
        longestRun(emptyList()) shouldBe 0
    }
})
