package com.decisionexecution.app.domain.service

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class ProgressionTest : StringSpec({

    "XP adds up every kind of activity" {
        xpFor(LifetimeStats(tasksCompleted = 2, focusMinutes = 30, habitCheckIns = 3, moodEntries = 4, inboxCaptured = 5)) shouldBe
            2 * 20 + 30 + 3 * 10 + 4 * 5 + 5 * 2
    }

    "levels start at 1 and each one takes 50 XP more than the last" {
        levelFor(0).level shouldBe 1
        levelFor(99).level shouldBe 1
        levelFor(100).level shouldBe 2
        levelFor(249).level shouldBe 2
        levelFor(250).let {
            it.level shouldBe 3
            it.xpIntoLevel shouldBe 0
            it.xpForNextLevel shouldBe 200
        }
    }

    "XP into the level always fits inside the level" {
        checkAll(PROPERTY_ITERATIONS, Arb.int(0..200_000)) { xp ->
            val level = levelFor(xp)
            level.xpIntoLevel shouldBeGreaterThanOrEqual 0
            level.xpIntoLevel shouldBeLessThan level.xpForNextLevel
            // Rebuilding the total from the level gives back the XP
            (1 until level.level).sumOf(::xpToCompleteLevel) + level.xpIntoLevel shouldBe xp
        }
    }

    "titles move up with the level" {
        levelTitle(1) shouldBe "Spark"
        levelTitle(7) shouldBe "Mover"
        levelTitle(30) shouldBe "Legend"
    }

    "achievements unlock from their thresholds" {
        unlockedAchievements(LifetimeStats()) shouldBe emptySet()
        unlockedAchievements(LifetimeStats(tasksCompleted = 10, habitsCreated = 1, bestHabitStreak = 7)) shouldContainExactlyInAnyOrder
            listOf(Achievement.FIRST_STEP, Achievement.GETTING_THINGS_DONE, Achievement.HABIT_FORMING, Achievement.WEEK_STRONG)
    }
})
