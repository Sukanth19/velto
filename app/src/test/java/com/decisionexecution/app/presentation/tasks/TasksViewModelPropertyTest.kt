package com.decisionexecution.app.presentation.tasks

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.usecase.CompleteTaskUseCase
import com.decisionexecution.app.domain.usecase.DeleteTaskUseCase
import com.decisionexecution.app.domain.usecase.GetAllTasksUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.UUID

/**
 * Property-based tests for task grouping, sorting, and filtering.
 * Feature: decision-driven-execution-app
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelPropertyTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "Property 25: Task Grouping by Category" {
        // Feature: decision-driven-execution-app, Property 25: For any list of tasks, grouping by
        // category should result in each task appearing in exactly one category group matching
        // its category attribute.
        // Validates: Requirements 9.1

        checkAll(PROPERTY_ITERATIONS, Arb.list(taskArb(), 0..50)) { tasks ->
            val grouped = groupTasksByCategory(tasks, ScoringEngine())

            grouped.values.flatten() shouldContainExactlyInAnyOrder tasks
            grouped.forEach { (category, group) ->
                group.forEach { it.category shouldBe category }
            }
        }
    }

    "Property 26: Task Sorting by Score Within Category" {
        // Feature: decision-driven-execution-app, Property 26: For any list of tasks within a
        // single category, the tasks should be ordered in descending order by their priority score.
        // Validates: Requirements 9.2

        checkAll(PROPERTY_ITERATIONS, Arb.list(taskArb(), 0..50)) { tasks ->
            val scoringEngine = ScoringEngine()
            val grouped = groupTasksByCategory(tasks, scoringEngine)

            grouped.values.forEach { group ->
                group.zipWithNext { a, b ->
                    scoringEngine.calculateScore(a) shouldBeGreaterThanOrEqualTo scoringEngine.calculateScore(b)
                }
            }
        }
    }

    "Property 27: Category Filtering" {
        // Feature: decision-driven-execution-app, Property 27: For any list of tasks and any
        // category, filtering by that category should return only tasks with that category attribute.
        // Validates: Requirements 9.6

        checkAll(PROPERTY_ITERATIONS, Arb.list(taskArb(), 0..50), Arb.enum<TaskCategory>()) { tasks, category ->
            val filtered = filterByCategory(groupTasksByCategory(tasks, ScoringEngine()), category)

            filtered.keys shouldBe setOf(category)
            filtered.values.flatten() shouldContainExactlyInAnyOrder tasks.filter { it.category == category }
        }
    }

    "Property 27: Category Filtering - ViewModel applies selected filter" {
        // Feature: decision-driven-execution-app, Property 27
        // Validates: Requirements 9.6

        checkAll(PROPERTY_ITERATIONS, Arb.list(taskArb(), 0..30), Arb.enum<TaskCategory>()) { tasks, category ->
            val viewModel = createViewModel(tasks)

            viewModel.onCategoryFilter(category)
            viewModel.uiState.value.tasksByCategory.values.flatten()
                .forEach { it.category shouldBe category }

            viewModel.onCategoryFilter(null)
            viewModel.uiState.value.tasksByCategory.values.flatten() shouldContainExactlyInAnyOrder tasks
        }
    }

    "Property 28: Active Task Count" {
        // Feature: decision-driven-execution-app, Property 28: For any list of active tasks,
        // the displayed count should equal the number of tasks in the list.
        // Validates: Requirements 9.7

        checkAll(PROPERTY_ITERATIONS, Arb.list(taskArb(), 0..50), Arb.enum<TaskCategory>().orNull()) { tasks, category ->
            val viewModel = createViewModel(tasks)
            viewModel.onCategoryFilter(category)

            // Count reflects all active tasks, not just the filtered view
            viewModel.uiState.value.activeTaskCount shouldBe tasks.size
        }
    }
})

private fun createViewModel(tasks: List<Task>): TasksViewModel {
    val getAllTasks = mockk<GetAllTasksUseCase>()
    every { getAllTasks() } returns flowOf(tasks)
    return TasksViewModel(
        getAllTasks,
        mockk<CompleteTaskUseCase>(relaxed = true),
        mockk<DeleteTaskUseCase>(relaxed = true),
        ScoringEngine()
    )
}

private fun taskArb(): Arb<Task> = arbitrary {
    Task(
        id = UUID.randomUUID().toString(),
        title = Arb.string(1..50).bind(),
        description = Arb.string(0..100).orNull().bind(),
        category = Arb.enum<TaskCategory>().bind(),
        urgency = Arb.int(1..5).bind(),
        importance = Arb.int(1..5).bind(),
        effort = Arb.enum<TaskEffort>().bind(),
        energyTag = Arb.enum<EnergyTag>().orNull().bind(),
        createdAt = Arb.long(1_000_000_000_000L..2_000_000_000_000L).bind()
    )
}
