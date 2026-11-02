package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.repository.EstimateSample
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.usecase.DayPhaseSource
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/** Time-of-day picks, snoozing and learning from estimates */
class SmarterPicksTest : StringSpec({

    val today = LocalDate.of(2026, 11, 2)
    val deep = Task("deep", "Write the report", null, TaskCategory.WORK, 3, 4, TaskEffort.HIGH, EnergyTag.DEEP_WORK, 1)
    val easy = Task("easy", "Reply to emails", null, TaskCategory.WORK, 3, 4, TaskEffort.LOW, EnergyTag.LOW, 2)

    fun hours(vararg minutesAt: Pair<Int, Int>) = IntArray(24).also { a -> minutesAt.forEach { (h, m) -> a[h] = m } }
    // Mornings are when this person gets things done; mid-afternoon they work but barely
    val history = hours(9 to 120, 10 to 100, 11 to 60, 14 to 10, 16 to 40, 20 to 30)

    "the best focus window is peak time" {
        dayPhase(history, hour = 9, energy = null) shouldBe DayPhase.PEAK
        dayPhase(history, hour = 10, energy = null) shouldBe DayPhase.PEAK
    }

    "an hour they work in but rarely get much done is a slump" {
        dayPhase(history, hour = 14, energy = null) shouldBe DayPhase.SLUMP
    }

    "hours with no history say nothing" {
        dayPhase(history, hour = 3, energy = null) shouldBe DayPhase.NEUTRAL
        dayPhase(IntArray(24), hour = 9, energy = null) shouldBe DayPhase.NEUTRAL
    }

    "low energy logged today is a slump even at peak time" {
        dayPhase(history, hour = 9, energy = 1) shouldBe DayPhase.SLUMP
    }

    "peak time puts deep work first and a slump puts the easy task first" {
        fun best(phase: DayPhase) = GetBestTaskUseCase(repo(deep, easy), ScoringEngine { today }, DayPhaseSource { flowOf(phase) })
        best(DayPhase.PEAK).pick { today }.first()!!.let {
            it.task shouldBe deep
            it.reason shouldBe "Your peak focus hours. Hard stuff first"
        }
        best(DayPhase.SLUMP).pick { today }.first()!!.task shouldBe easy
    }

    "a snoozed task is skipped until its day comes, unless it's all there is" {
        // Would win on priority, so the snooze is the only thing keeping it back
        val snoozed = deep.copy(importance = 5, urgency = 5, snoozedUntil = today.plusDays(1))
        val useCase = GetBestTaskUseCase(repo(snoozed, easy), ScoringEngine { today }, DayPhaseSource.NEUTRAL)
        useCase.pick { today }.first()!!.task shouldBe easy
        useCase.pick { today.plusDays(1) }.first()!!.task shouldBe snoozed

        GetBestTaskUseCase(repo(snoozed), ScoringEngine { today }, DayPhaseSource.NEUTRAL).pick { today }.first()!!.task shouldBe snoozed
    }

    "three dodges means it needs shrinking" {
        deep.copy(dodges = 2).needsShrinking shouldBe false
        deep.copy(dodges = 3).needsShrinking shouldBe true
    }

    "calibration needs three samples and uses the median" {
        calibrate(listOf(EstimateSample(30, 60), EstimateSample(30, 45))) shouldBe null
        val c = calibrate(listOf(EstimateSample(30, 60), EstimateSample(30, 45), EstimateSample(20, 200)))!!
        // Ratios 2.0, 1.5 and 10 (capped at 4): the median ignores the one marathon
        c.ratio shouldBe (2.0 plusOrMinus 0.001)
        c.summary shouldBe "You usually take 2× your estimate"
        c.likelyMinutes(25) shouldBe 50
    }

    "fast finishers and accurate guessers get their own wording" {
        calibrate(List(3) { EstimateSample(60, 42) })!!.summary shouldBe "You usually finish in 70% of your estimate"
        calibrate(List(3) { EstimateSample(60, 63) })!!.summary shouldBe "Your estimates are spot on"
        calibrate(List(3) { EstimateSample(10, 16) })!!.summary shouldBe "You usually take 1.6× your estimate"
    }

    "pick for me prefers a task whose real length fits the time box" {
        // Says 15 minutes but this person takes 3x as long, so it won't fit in 20
        val looksQuick = easy.copy(id = "quick", estimateMinutes = 15)
        val honest = easy.copy(id = "honest", estimateMinutes = 30, createdAt = 3)
        val slow = EstimateCalibration(ratio = 3.0, samples = 5)
        val picker = TaskPicker(ScoringEngine { today })
        val withHistory = PickContext(minutes = 20, calibration = slow)
        picker.score(looksQuick, withHistory) shouldBe (picker.score(honest, withHistory) plusOrMinus 0.001)
        picker.rank(listOf(looksQuick, honest), PickContext(minutes = 20)).first() shouldBe looksQuick
        picker.reason(looksQuick, PickContext(minutes = 60, calibration = slow)) shouldBe "Should take about 45 minutes. It fits"
    }
})

private fun repo(vararg tasks: Task): TaskRepository = mockk<TaskRepository>().also {
    every { it.getAllTasks() } returns flowOf(tasks.toList())
}
