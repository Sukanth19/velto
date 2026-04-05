package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

/**
 * Property-based tests for EisenhowerClassifier.
 * Feature: decision-driven-execution-app
 */
class EisenhowerClassifierPropertyTest : StringSpec({
    
    "Property 16: Eisenhower Urgent and Important Classification" {
        // Feature: decision-driven-execution-app, Property 16: For any task with urgency >= 4
        // and importance >= 4, the Eisenhower classifier should classify it as Urgent and Important.
        // Validates: Requirements 6.2
        
        checkAll(Arb.int(4..5), Arb.int(4..5), 100) { urgency, importance ->
            val classifier = EisenhowerClassifier()
            val task = createTask(urgency = urgency, importance = importance)
            
            val quadrant = classifier.classify(task)
            
            quadrant shouldBe EisenhowerQuadrant.URGENT_IMPORTANT
        }
    }
    
    "Property 17: Eisenhower Not Urgent but Important Classification" {
        // Feature: decision-driven-execution-app, Property 17: For any task with urgency <= 3
        // and importance >= 4, the Eisenhower classifier should classify it as Not Urgent but Important.
        // Validates: Requirements 6.3
        
        checkAll(Arb.int(1..3), Arb.int(4..5), 100) { urgency, importance ->
            val classifier = EisenhowerClassifier()
            val task = createTask(urgency = urgency, importance = importance)
            
            val quadrant = classifier.classify(task)
            
            quadrant shouldBe EisenhowerQuadrant.NOT_URGENT_IMPORTANT
        }
    }
    
    "Property 18: Eisenhower Urgent but Not Important Classification" {
        // Feature: decision-driven-execution-app, Property 18: For any task with urgency >= 4
        // and importance <= 3, the Eisenhower classifier should classify it as Urgent but Not Important.
        // Validates: Requirements 6.4
        
        checkAll(Arb.int(4..5), Arb.int(1..3), 100) { urgency, importance ->
            val classifier = EisenhowerClassifier()
            val task = createTask(urgency = urgency, importance = importance)
            
            val quadrant = classifier.classify(task)
            
            quadrant shouldBe EisenhowerQuadrant.URGENT_NOT_IMPORTANT
        }
    }
    
    "Property 19: Eisenhower Neither Classification" {
        // Feature: decision-driven-execution-app, Property 19: For any task with urgency <= 3
        // and importance <= 3, the Eisenhower classifier should classify it as Neither Urgent nor Important.
        // Validates: Requirements 6.5
        
        checkAll(Arb.int(1..3), Arb.int(1..3), 100) { urgency, importance ->
            val classifier = EisenhowerClassifier()
            val task = createTask(urgency = urgency, importance = importance)
            
            val quadrant = classifier.classify(task)
            
            quadrant shouldBe EisenhowerQuadrant.NEITHER
        }
    }
    
    "Property 20: Eisenhower Quadrant Counts" {
        // Feature: decision-driven-execution-app, Property 20: For any list of tasks,
        // the sum of task counts across all four Eisenhower quadrants should equal the total number of tasks.
        // Validates: Requirements 6.7
        
        checkAll(Arb.list(taskArb(), range = 0..100), 100) { tasks ->
            val classifier = EisenhowerClassifier()
            val groupedTasks = classifier.groupByQuadrant(tasks)
            
            val urgentImportantCount = groupedTasks[EisenhowerQuadrant.URGENT_IMPORTANT]?.size ?: 0
            val notUrgentImportantCount = groupedTasks[EisenhowerQuadrant.NOT_URGENT_IMPORTANT]?.size ?: 0
            val urgentNotImportantCount = groupedTasks[EisenhowerQuadrant.URGENT_NOT_IMPORTANT]?.size ?: 0
            val neitherCount = groupedTasks[EisenhowerQuadrant.NEITHER]?.size ?: 0
            
            val totalInQuadrants = urgentImportantCount + notUrgentImportantCount + 
                                   urgentNotImportantCount + neitherCount
            
            totalInQuadrants shouldBe tasks.size
        }
    }
})

/**
 * Helper function to create a task with specific urgency and importance values.
 */
private fun createTask(urgency: Int, importance: Int): Task {
    return Task(
        id = UUID.randomUUID().toString(),
        title = "Test Task",
        description = null,
        category = TaskCategory.WORK,
        urgency = urgency,
        importance = importance,
        effort = TaskEffort.MEDIUM,
        energyTag = null,
        createdAt = System.currentTimeMillis()
    )
}

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
