package com.decisionexecution.app.presentation.habits

import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitIcon
import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.presentation.widget.habitWidgetRows
import com.decisionexecution.app.testutil.InMemoryHabitRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModelTest : StringSpec({
    val today = LocalDate.of(2026, 10, 30)
    fun habit(id: String, vararg daysAgo: Long, on: LocalDate = today) =
        HabitWithHistory(Habit(id, "Habit $id", HabitIcon.SPARK, id.toLong()), daysAgo.map { on.minusDays(it) }.toSet())

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "rows show today, the streak and the last seven days" {
        val row = habitRows(listOf(habit("1", 0, 1, 2, 6)), today).single()
        row.doneToday shouldBe true
        row.streak shouldBe 3
        row.week shouldBe listOf(true, false, false, false, true, true, true)
    }

    "heatmap is the share of habits done each day, ending today" {
        val map = habitHeatmap(listOf(habit("1", 0, 1), habit("2", 0)), today, days = 3)
        map shouldBe listOf(0f, 0.5f, 1f)
    }

    "widget lists undone habits first" {
        habitWidgetRows(listOf(habit("1", 0), habit("2")), today).map { it.habitId } shouldBe listOf("2", "1")
    }

    // The view model works on the real today
    "checking a habit in marks it done and fires confetti" {
        val viewModel = HabitsViewModel(InMemoryHabitRepository(habit("1", on = LocalDate.now())))
        val id = viewModel.uiState.value.habits.single().habit.id

        viewModel.onToggle(id, true)

        viewModel.uiState.value.habits.single().doneToday shouldBe true
        viewModel.uiState.value.celebrateKey shouldBe 1
    }

    "adding and deleting habits updates the list" {
        val viewModel = HabitsViewModel(InMemoryHabitRepository())
        viewModel.onAdd("  Stretch ", HabitIcon.MOVE)
        viewModel.onAdd("   ", HabitIcon.MOVE)

        val added = viewModel.uiState.value.habits.single()
        added.habit.name shouldBe "Stretch"

        viewModel.onDelete(added.habit.id)
        viewModel.uiState.value.habits shouldBe emptyList()
    }
})
