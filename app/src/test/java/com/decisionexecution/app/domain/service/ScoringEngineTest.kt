package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Unit tests for ScoringEngine edge cases.
 * Feature: decision-driven-execution-app
 */
class ScoringEngineTest : StringSpec({
    
    "should return null when task list is empty" {
        // Requirements: 2.5
        val scoringEngine = ScoringEngine()
        val bestTask = scoringEngine.getBestTask(emptyList())
        
        bestTask shouldBe null
    }
    
    "should return empty list when getting top three from empty list" {
        // Requirements: 2.5
        val scoringEngine = ScoringEngine()
        val topThree = scoringEngine.getTopThreeTasks(emptyList())
        
        topThree shouldBe emptyList()
    }
    
    "should return single task when list contains only one task" {
        // Requirements: 2.4
        val scoringEngine = ScoringEngine()
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = "Only Task",
            description = "The only task in the list",
            category = TaskCategory.WORK,
            urgency = 3,
            importance = 4,
            effort = TaskEffort.MEDIUM,
            energyTag = EnergyTag.LOW,
            createdAt = System.currentTimeMillis()
        )
        
        val bestTask = scoringEngine.getBestTask(listOf(task))
        bestTask shouldBe task
        
        val topThree = scoringEngine.getTopThreeTasks(listOf(task))
        topThree shouldBe listOf(task)
    }
    
    "should use timestamp tiebreaker when scores are identical" {
        // Requirements: 2.4
        val scoringEngine = ScoringEngine()
        val baseTime = System.currentTimeMillis()
        
        // Create three tasks with identical scores but different timestamps
        val oldestTask = Task(
            id = "1",
            title = "Oldest Task",
            description = null,
            category = TaskCategory.PERSONAL,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.LOW,
            energyTag = null,
            createdAt = baseTime - 10000 // 10 seconds ago
        )
        
        val middleTask = Task(
            id = "2",
            title = "Middle Task",
            description = null,
            category = TaskCategory.PERSONAL,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.LOW,
            energyTag = null,
            createdAt = baseTime - 5000 // 5 seconds ago
        )
        
        val newestTask = Task(
            id = "3",
            title = "Newest Task",
            description = null,
            category = TaskCategory.PERSONAL,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.LOW,
            energyTag = null,
            createdAt = baseTime // now
        )
        
        // Verify all tasks have the same score
        val score1 = scoringEngine.calculateScore(oldestTask)
        val score2 = scoringEngine.calculateScore(middleTask)
        val score3 = scoringEngine.calculateScore(newestTask)
        score1 shouldBe score2
        score2 shouldBe score3
        
        // Test with tasks in random order
        val tasks = listOf(newestTask, oldestTask, middleTask)
        
        // Best task should be the oldest
        val bestTask = scoringEngine.getBestTask(tasks)
        bestTask shouldBe oldestTask
        
        // Top three should be ordered by timestamp (oldest first)
        val topThree = scoringEngine.getTopThreeTasks(tasks)
        topThree[0] shouldBe oldestTask
        topThree[1] shouldBe middleTask
        topThree[2] shouldBe newestTask
    }
    
    "should calculate correct score with all factors" {
        // Requirements: 2.1
        val scoringEngine = ScoringEngine()
        
        // Test with all factors present
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = "Complex Task",
            description = "Task with all attributes",
            category = TaskCategory.SCHOOL,
            urgency = 5,
            importance = 4,
            effort = TaskEffort.HIGH,
            energyTag = EnergyTag.DEEP_WORK,
            createdAt = System.currentTimeMillis()
        )
        
        // Expected score: (2.0 * 5) + (3.0 * 4) + (-0.5 * 3) + 1.0
        // = 10.0 + 12.0 - 1.5 + 1.0 = 21.5
        val score = scoringEngine.calculateScore(task)
        score shouldBe 21.5
    }
    
    "should calculate correct score with minimal factors" {
        // Requirements: 2.1
        val scoringEngine = ScoringEngine()
        
        // Test with minimal factors (no energy tag)
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = "Simple Task",
            description = null,
            category = TaskCategory.PERSONAL,
            urgency = 1,
            importance = 1,
            effort = TaskEffort.LOW,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        // Expected score: (2.0 * 1) + (3.0 * 1) + (-0.5 * 1) + 0.0
        // = 2.0 + 3.0 - 0.5 + 0.0 = 4.5
        val score = scoringEngine.calculateScore(task)
        score shouldBe 4.5
    }
    
    "should prioritize importance over urgency" {
        // Requirements: 2.1
        val scoringEngine = ScoringEngine()
        
        val urgentTask = Task(
            id = "urgent",
            title = "Urgent Task",
            description = null,
            category = TaskCategory.WORK,
            urgency = 5,
            importance = 1,
            effort = TaskEffort.MEDIUM,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        val importantTask = Task(
            id = "important",
            title = "Important Task",
            description = null,
            category = TaskCategory.WORK,
            urgency = 1,
            importance = 5,
            effort = TaskEffort.MEDIUM,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        val urgentScore = scoringEngine.calculateScore(urgentTask)
        val importantScore = scoringEngine.calculateScore(importantTask)
        
        // Important task should have higher score due to higher weight (3.0 vs 2.0)
        // Urgent: (2.0 * 5) + (3.0 * 1) + (-0.5 * 2) = 10 + 3 - 1 = 12
        // Important: (2.0 * 1) + (3.0 * 5) + (-0.5 * 2) = 2 + 15 - 1 = 16
        urgentScore shouldBe 12.0
        importantScore shouldBe 16.0
        
        val bestTask = scoringEngine.getBestTask(listOf(urgentTask, importantTask))
        bestTask shouldBe importantTask
    }
    
    "should penalize high effort tasks" {
        // Requirements: 2.1
        val scoringEngine = ScoringEngine()
        
        val lowEffortTask = Task(
            id = "low",
            title = "Low Effort",
            description = null,
            category = TaskCategory.WORK,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.LOW,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        val highEffortTask = Task(
            id = "high",
            title = "High Effort",
            description = null,
            category = TaskCategory.WORK,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.HIGH,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        val lowScore = scoringEngine.calculateScore(lowEffortTask)
        val highScore = scoringEngine.calculateScore(highEffortTask)
        
        // Low effort should score higher (less penalty)
        // Low: (2.0 * 3) + (3.0 * 3) + (-0.5 * 1) = 6 + 9 - 0.5 = 14.5
        // High: (2.0 * 3) + (3.0 * 3) + (-0.5 * 3) = 6 + 9 - 1.5 = 13.5
        lowScore shouldBe 14.5
        highScore shouldBe 13.5
        
        val bestTask = scoringEngine.getBestTask(listOf(highEffortTask, lowEffortTask))
        bestTask shouldBe lowEffortTask
    }
    
    "should apply energy bonus correctly" {
        // Requirements: 2.1
        val scoringEngine = ScoringEngine()
        
        val noEnergyTask = Task(
            id = "none",
            title = "No Energy Tag",
            description = null,
            category = TaskCategory.WORK,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.MEDIUM,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        val lowEnergyTask = Task(
            id = "low",
            title = "Low Energy",
            description = null,
            category = TaskCategory.WORK,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.MEDIUM,
            energyTag = EnergyTag.LOW,
            createdAt = System.currentTimeMillis()
        )
        
        val deepWorkTask = Task(
            id = "deep",
            title = "Deep Work",
            description = null,
            category = TaskCategory.WORK,
            urgency = 3,
            importance = 3,
            effort = TaskEffort.MEDIUM,
            energyTag = EnergyTag.DEEP_WORK,
            createdAt = System.currentTimeMillis()
        )
        
        val noEnergyScore = scoringEngine.calculateScore(noEnergyTask)
        val lowEnergyScore = scoringEngine.calculateScore(lowEnergyTask)
        val deepWorkScore = scoringEngine.calculateScore(deepWorkTask)
        
        // Base: (2.0 * 3) + (3.0 * 3) + (-0.5 * 2) = 6 + 9 - 1 = 14.0
        // Low energy adds 0.5: 14.5
        // Deep work adds 1.0: 15.0
        noEnergyScore shouldBe 14.0
        lowEnergyScore shouldBe 14.5
        deepWorkScore shouldBe 15.0
    }
})
