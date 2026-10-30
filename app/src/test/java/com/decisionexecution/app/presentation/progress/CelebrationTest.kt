package com.decisionexecution.app.presentation.progress

import com.decisionexecution.app.domain.service.Achievement
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CelebrationTest : StringSpec({

    "nothing new means nothing to celebrate" {
        pendingCelebration(3, setOf(Achievement.FIRST_STEP), celebratedLevel = 3, celebrated = setOf("FIRST_STEP")) shouldBe null
    }

    "a new level and unseen achievements are celebrated together" {
        val celebration = pendingCelebration(
            level = 4,
            unlocked = setOf(Achievement.IN_THE_ZONE, Achievement.FIRST_STEP),
            celebratedLevel = 3,
            celebrated = setOf("IN_THE_ZONE")
        )
        celebration shouldBe Celebration(newLevel = 4, achievements = listOf(Achievement.FIRST_STEP))
    }

    "achievements alone are still celebrated" {
        pendingCelebration(1, setOf(Achievement.HABIT_FORMING), 1, emptySet())?.newLevel shouldBe null
    }
})
