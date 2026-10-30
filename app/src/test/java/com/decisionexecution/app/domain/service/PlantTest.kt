package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.usecase.GetPlantUseCase
import com.decisionexecution.app.domain.usecase.daysSince
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class PlantTest : StringSpec({

    "the plant grows with level and never shrinks" {
        plantStageFor(1) shouldBe PlantStage.SEED
        plantStageFor(3) shouldBe PlantStage.SPROUT
        plantStageFor(7) shouldBe PlantStage.SAPLING
        plantStageFor(8) shouldBe PlantStage.BLOOM
        plantStageFor(40) shouldBe PlantStage.TREE
    }

    "health follows how recently you were active" {
        plantHealthFor(0) shouldBe PlantHealth.THRIVING
        plantHealthFor(1) shouldBe PlantHealth.OKAY
        plantHealthFor(3) shouldBe PlantHealth.THIRSTY
        plantHealthFor(4) shouldBe PlantHealth.WILTING
    }

    "a wilting plant says how long it has waited" {
        plantFor(5, 6).message shouldContain "6 days"
    }

    "no activity at all counts as the whole look-back" {
        val today = LocalDate.of(2026, 10, 30)
        daysSince(null, today) shouldBe GetPlantUseCase.LOOK_BACK_DAYS.toInt()
        daysSince(today.minusDays(2), today) shouldBe 2
        daysSince(today, today) shouldBe 0
    }
})
