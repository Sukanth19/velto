package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

/**
 * Property-based tests for ScoringEngine.
 * Feature: decision-driven-execution-app
 */
class ScoringEnginePropertyTest : StringSpec({
    
    "Property 6: Score Calculation and Best Task Selection" {
        // Feature: decision-driven-execution-app, Property 6: For any non-empty list of tasks,
        // the best task returned by the scoring engine should have a score greater than or equal
        // to all other tasks in the list.
        // Validates: Requirements 2.1, 2.2
        
        checkAll(Arb.list(taskArb(), range = 1..100), 100) { tasks ->
            val scoringEngine = ScoringEngine()
            val bestTask = scoringEngine.getBestTask(tasks)
            
            bestTask shouldNotBe null
            val bestScore = scoringEngine.calculateScore(bestTask!!)
            
            tasks.forEach { task ->
                val taskScore = scoringEngine.calculateScore(task)
                bestScore shouldBeGreaterThanOrEqualTo taskScore
            }
        }
    }
    
    "Property 7: Top Three Task Ordering" {
        // Feature: decision-driven-execution-app, Property 7: For any list of tasks with at least
        // three tasks, the top three tasks returned should be in descending order by score.
        // Validates: Requirements 2.3
        
        checkAll(Arb.list(taskArb(), range = 3..100), 100) { tasks ->
            val scoringEngine = ScoringEngine()
            val topThree = scoringEngine.getTopThreeTasks(tasks)
            
            topThree.size shouldBe minOf(3, tasks.size)
            
            if (topThree.size >= 2) {
                val score1 = scoringEngine.calculateScore(topThree[0])
                val score2 = scoringEngine.calculateScore(topThree[1])
                score1 shouldBeGreaterThanOrEqualTo score2
            }
            
            if (topThree.size == 3) {
                val score2 = scoringEngine.calculateScore(topThree[1])
                val score3 = scoringEngine.calculateScore(topThree[2])
                score2 shouldBeGreaterThanOrEqualTo score3
            }
        }
    }
    
    "Property 8: Score Tiebreaker" {
        // Feature: decision-driven-execution-app, Property 8: For any set of tasks with identical
        // scores, the task with the earliest creation timestamp should be ranked higher.
        // Validates: Requirements 2.4
        
        checkAll(Arb.int(1..5), Arb.int(1..5), 100) { urgency, importance ->
            val scoringEngine = ScoringEngine()
            
            // Create tasks with identical attributes but different timestamps
            val baseTime = System.currentTimeMillis()
            val task1 = Task(
                id = UUID.randomUUID().toString(),
                title = "Task 1",
                description = null,
                category = TaskCategory.WORK,
                urgency = urgency,
                importance = importance,
                effort = TaskEffort.MEDIUM,
                energyTag = EnergyTag.LOW,
                createdAt = baseTime
            )
            
            val task2 = Task(
                id = UUID.randomUUID().toString(),
                title = "Task 2",
                description = null,
                category = TaskCategory.WORK,
                urgency = urgency,
                importance = importance,
                effort = TaskEffort.MEDIUM,
                energyTag = EnergyTag.LOW,
                createdAt = baseTime + 1000 // 1 second later
            )
            
            val task3 = Task(
                id = UUID.randomUUID().toString(),
                title = "Task 3",
                description = null,
                category = TaskCategory.WORK,
                urgency = urgency,
                importance = importance,
                effort = TaskEffort.MEDIUM,
                energyTag = EnergyTag.LOW,
                createdAt = baseTime + 2000 // 2 seconds later
            )
            
            // Verify all tasks have the same score
            val score1 = scoringEngine.calculateScore(task1)
            val score2 = scoringEngine.calculateScore(task2)
            val score3 = scoringEngine.calculateScore(task3)
            score1 shouldBe score2
            score2 shouldBe score3
            
            // Test with tasks in different orders
            val tasks = listOf(task3, task1, task2) // Shuffled order
            val bestTask = scoringEngine.getBestTask(tasks)
            
            // The oldest task (task1) should be selected
            bestTask shouldBe task1
            
            // Test top three ordering
            val topThree = scoringEngine.getTopThreeTasks(tasks)
            topThree[0] shouldBe task1 // Oldest first
            topThree[1] shouldBe task2 // Middle
            topThree[2] shouldBe task3 // Newest last
        }
    }
})

/**
 * Arbitrary generator for Task domain model.
 */
private fun taskArb(): Arb<Task> = arbitrary {
    Task(
        id = UUID.randomUUID().toString(),
        title = Arb.string(1..50).bind(),
        description = Arb.string(0..200).orNull().bind(),
        category = Arb.enum<TaskCategory>().bind(),
        urgency = Arb.int(1..5).bind(),
        importance = Arb.int(1..5).bind(),
        effort = Arb.enum<TaskEffort>().bind(),
        energyTag = Arb.enum<EnergyTag>().orNull().bind(),
        createdAt = Arb.long(1_000_000_000_000L..System.currentTimeMillis()).bind()
    )
}
