package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TaskPickerTest : StringSpec({
    val picker = TaskPicker()
    fun task(id: String, effort: TaskEffort, energy: EnergyTag?, urgency: Int = 3, importance: Int = 3) =
        Task(id, "Task $id", null, TaskCategory.WORK, urgency, importance, effort, energy, id.toLong())

    val bigReport = task("1", TaskEffort.HIGH, EnergyTag.DEEP_WORK, urgency = 4, importance = 5)
    val quickEmail = task("2", TaskEffort.LOW, EnergyTag.LOW, urgency = 4, importance = 3)
    val tasks = listOf(bigReport, quickEmail)

    "with plenty of time the important deep work wins" {
        picker.rank(tasks, PickContext(minutes = 60)).first() shouldBe bigReport
    }

    "with little time a quick win beats a big task" {
        picker.rank(tasks, PickContext(minutes = 15)).first() shouldBe quickEmail
    }

    "low energy steers away from deep work even with time to spare" {
        picker.rank(tasks, PickContext(minutes = 45, energy = 1)).first() shouldBe quickEmail
    }

    "reasons match the situation" {
        picker.reason(quickEmail, PickContext(15)) shouldBe "Quick win that fits in 15 minutes"
        picker.reason(quickEmail, PickContext(25, energy = 1)) shouldBe "Gentle one for a low-energy day"
        picker.reason(bigReport, PickContext(25, energy = 5)) shouldBe "You're charged. Perfect for deep work"
    }
})
