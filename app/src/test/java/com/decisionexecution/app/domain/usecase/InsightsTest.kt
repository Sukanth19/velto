package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.repository.FocusSessionRecord
import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.time.LocalDate
import java.time.ZoneId

/**
 * Week and streak calculation for Insights.
 */
class InsightsTest : StringSpec({

    val zone = ZoneId.of("Asia/Kolkata")
    val today = LocalDate.of(2026, 10, 28)
    fun at(daysAgo: Long, hour: Int = 12) = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    fun focus(daysAgo: Long, minutes: Int = 25) = FocusSessionRecord("t", at(daysAgo), minutes)

    "no activity means an empty week and no streak" {
        val insights = buildInsights(emptyList(), emptyList(), today, zone)
        insights.streakDays shouldBe 0
        insights.week.size shouldBe 7
        insights.week.last().date shouldBe today
        insights.week.first().date shouldBe today.minusDays(6)
        insights.bestDay shouldBe null
    }

    "consecutive days count up to today" {
        val insights = buildInsights(listOf(at(0), at(1), at(2)), emptyList(), today, zone)
        insights.streakDays shouldBe 3
    }

    "nothing done yet today keeps yesterday's streak alive" {
        buildInsights(listOf(at(1), at(2)), emptyList(), today, zone).streakDays shouldBe 2
    }

    "a missed day ends the streak" {
        buildInsights(listOf(at(0), at(2), at(3)), emptyList(), today, zone).streakDays shouldBe 1
        buildInsights(listOf(at(2), at(3)), emptyList(), today, zone).streakDays shouldBe 0
    }

    "a focus session alone counts as an active day" {
        buildInsights(emptyList(), listOf(focus(0), focus(1)), today, zone).streakDays shouldBe 2
    }

    "days are split at local midnight, not UTC" {
        // 00:30 local is still the previous day in UTC for India
        val justAfterMidnight = today.atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        buildInsights(listOf(justAfterMidnight), emptyList(), today, zone).week.last().completedTasks shouldBe 1
    }

    "week totals add up and ignore older activity" {
        checkAll(PROPERTY_ITERATIONS, Arb.list(Arb.int(0..20), 0..30), Arb.list(Arb.int(0..20), 0..30)) { completionDays, focusDays ->
            val insights = buildInsights(
                completionDays.map { at(it.toLong()) },
                focusDays.map { focus(it.toLong(), 10) },
                today,
                zone
            )
            insights.weekCompletedTasks shouldBe completionDays.count { it <= 6 }
            insights.weekFocusMinutes shouldBe focusDays.count { it <= 6 } * 10
        }
    }

    "best day prefers focus time" {
        val insights = buildInsights(listOf(at(1), at(1), at(1)), listOf(focus(3, 50)), today, zone)
        insights.bestDay?.date shouldBe today.minusDays(3)
    }
})
