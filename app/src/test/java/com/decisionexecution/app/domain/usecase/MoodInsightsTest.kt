package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.FocusSessionRecord
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneOffset

class MoodInsightsTest : StringSpec({
    val zone = ZoneOffset.UTC
    val day = LocalDate.of(2026, 10, 1)
    fun session(date: LocalDate, minutes: Int) =
        FocusSessionRecord("t", date.atTime(10, 0).toInstant(zone).toEpochMilli(), minutes)

    "compares focus on high and low energy days" {
        val entries = listOf(
            MoodEntry(day, 4, 5, null),
            MoodEntry(day.plusDays(1), 4, 4, null),
            MoodEntry(day.plusDays(2), 2, 1, null)
        )
        val sessions = listOf(session(day, 100), session(day.plusDays(1), 80), session(day.plusDays(2), 30))

        val insights = buildMoodInsights(entries, sessions, zone)

        insights.highEnergyFocus shouldBe 90
        insights.lowEnergyFocus shouldBe 30
        insights.headline shouldBe "You focus 3.0x longer on high-energy days"
        insights.averageMood shouldBe 10.0 / 3
    }

    "needs both kinds of day before drawing a conclusion" {
        val insights = buildMoodInsights(listOf(MoodEntry(day, 3, 5, null)), emptyList(), zone)
        insights.lowEnergyFocus shouldBe null
        insights.headline shouldBe null
    }

    "a day with no focus counts as zero minutes" {
        val insights = buildMoodInsights(
            listOf(MoodEntry(day, 3, 5, null), MoodEntry(day.plusDays(1), 3, 1, null)),
            listOf(session(day.plusDays(1), 40)),
            zone
        )
        insights.highEnergyFocus shouldBe 0
        insights.headline shouldBe "Low energy doesn't stop you: 40m more focus on those days"
    }
})
