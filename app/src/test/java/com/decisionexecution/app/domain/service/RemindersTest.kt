package com.decisionexecution.app.domain.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime

class RemindersTest : StringSpec({
    val morning = LocalDateTime.of(2026, 10, 30, 8, 30)

    "a reminder later today fires today" {
        delayUntil(morning, 20 * 60) shouldBe Duration.ofHours(11).plusMinutes(30)
    }

    "a reminder that's already passed fires tomorrow" {
        delayUntil(morning, 8 * 60) shouldBe Duration.ofHours(23).plusMinutes(30)
    }

    "exactly now counts as passed, so it never fires twice" {
        delayUntil(morning, 8 * 60 + 30) shouldBe Duration.ofDays(1)
    }

    "habit reminder only when something's left" {
        habitReminderMessage(emptyList(), 3) shouldBe null
        habitReminderMessage(listOf("Read"), 3)?.title shouldBe "One habit to go"
        habitReminderMessage(listOf("Read"), 1)?.title shouldBe "Keep your streak going"
        habitReminderMessage(listOf("Read", "Run", "Water", "Stretch"), 4)?.text shouldBe "Read, Run, Water and more"
    }

    "mood reminder is skipped once logged" {
        moodReminderMessage(loggedToday = true) shouldBe null
        moodReminderMessage(loggedToday = false)?.title shouldBe "How are you feeling?"
    }

    "times show in the user's clock format" {
        formatMinuteOfDay(20 * 60, use24Hour = false) shouldBe "8:00 PM"
        formatMinuteOfDay(0, use24Hour = false) shouldBe "12:00 AM"
        formatMinuteOfDay(12 * 60 + 5, use24Hour = false) shouldBe "12:05 PM"
        formatMinuteOfDay(9 * 60 + 5, use24Hour = true) shouldBe "09:05"
    }
})
