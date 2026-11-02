package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.ThemePalette
import com.decisionexecution.app.presentation.progress.pendingCelebration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RewardsTest : StringSpec({

    "you start with wallpaper and violet colours and the blossom" {
        ThemePalette.entries.filter { it.isUnlocked(1) } shouldBe listOf(ThemePalette.WALLPAPER, ThemePalette.VIOLET)
        PlantVariety.entries.filter { it.isUnlocked(1) } shouldBe listOf(PlantVariety.BLOSSOM)
    }

    "everything is unlocked by level 10" {
        ThemePalette.entries.all { it.isUnlocked(10) } shouldBe true
        PlantVariety.entries.all { it.isUnlocked(10) } shouldBe true
        nextReward(10) shouldBe null
    }

    "levelling up from 3 to 6 brings everything in between" {
        rewardsBetween(3, 6).map { it.description } shouldBe listOf("Sunflower plant", "Sunset colours", "Cactus plant")
        nextReward(6) shouldBe Reward(7, "Rose colours")
    }

    "a level-up celebration lists what it unlocked" {
        pendingCelebration(level = 5, unlocked = emptySet(), celebratedLevel = 4, celebrated = emptySet())!!
            .rewards.map { it.description } shouldBe listOf("Sunset colours")
    }

    "a plant you haven't unlocked falls back to the blossom" {
        plantFor(level = 3, daysSinceActive = 0, variety = PlantVariety.BONSAI).variety shouldBe PlantVariety.BLOSSOM
        plantFor(level = 9, daysSinceActive = 0, variety = PlantVariety.BONSAI).variety shouldBe PlantVariety.BONSAI
    }
})
