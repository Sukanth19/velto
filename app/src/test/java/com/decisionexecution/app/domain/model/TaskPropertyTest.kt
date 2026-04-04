package com.decisionexecution.app.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

/**
 * Property-based tests for Task domain model validation.
 * Feature: decision-driven-execution-app
 */
class TaskPropertyTest : StringSpec({
    
    "Property 1: Task Attribute Validation - urgency and importance must be 1-5 inclusive" {
        // Feature: decision-driven-execution-app, Property 1: For any task creation attempt,
        // urgency and importance values must be between 1 and 5 inclusive, and values outside
        // this range must be rejected with an error.
        // Validates: Requirements 1.2, 1.3
        
        checkAll<Int, Int>(100) { urgency, importance ->
            val validUrgency = urgency in 1..5
            val validImportance = importance in 1..5
            
            if (validUrgency && validImportance) {
                // Should successfully create task with valid values
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    description = "Test Description",
                    category = TaskCategory.WORK,
                    urgency = urgency,
                    importance = importance,
                    effort = TaskEffort.MEDIUM,
                    energyTag = EnergyTag.LOW,
                    createdAt = System.currentTimeMillis()
                )
                
                task.urgency shouldBe urgency
                task.importance shouldBe importance
            } else {
                // Should throw IllegalArgumentException for invalid values
                shouldThrow<IllegalArgumentException> {
                    Task(
                        id = UUID.randomUUID().toString(),
                        title = "Test Task",
                        description = "Test Description",
                        category = TaskCategory.WORK,
                        urgency = urgency,
                        importance = importance,
                        effort = TaskEffort.MEDIUM,
                        energyTag = EnergyTag.LOW,
                        createdAt = System.currentTimeMillis()
                    )
                }
            }
        }
    }
    
    "Property 1: Task Attribute Validation - valid urgency values (1-5) are accepted" {
        // Feature: decision-driven-execution-app, Property 1
        // Validates: Requirements 1.2
        
        checkAll(Arb.int(1..5), Arb.int(1..5), 100) { urgency, importance ->
            val task = Task(
                id = UUID.randomUUID().toString(),
                title = "Test Task",
                description = null,
                category = TaskCategory.PERSONAL,
                urgency = urgency,
                importance = importance,
                effort = TaskEffort.LOW,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            
            task.urgency shouldBe urgency
            task.importance shouldBe importance
        }
    }
    
    "Property 1: Task Attribute Validation - invalid urgency values are rejected" {
        // Feature: decision-driven-execution-app, Property 1
        // Validates: Requirements 1.2
        
        val invalidUrgencyValues = Arb.choice(
            Arb.int(Int.MIN_VALUE..0),
            Arb.int(6..Int.MAX_VALUE)
        )
        
        checkAll(invalidUrgencyValues, Arb.int(1..5), 100) { urgency, importance ->
            shouldThrow<IllegalArgumentException> {
                Task(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    description = null,
                    category = TaskCategory.SCHOOL,
                    urgency = urgency,
                    importance = importance,
                    effort = TaskEffort.HIGH,
                    energyTag = EnergyTag.DEEP_WORK,
                    createdAt = System.currentTimeMillis()
                )
            }
        }
    }
    
    "Property 1: Task Attribute Validation - invalid importance values are rejected" {
        // Feature: decision-driven-execution-app, Property 1
        // Validates: Requirements 1.3
        
        val invalidImportanceValues = Arb.choice(
            Arb.int(Int.MIN_VALUE..0),
            Arb.int(6..Int.MAX_VALUE)
        )
        
        checkAll(Arb.int(1..5), invalidImportanceValues, 100) { urgency, importance ->
            shouldThrow<IllegalArgumentException> {
                Task(
                    id = UUID.randomUUID().toString(),
                    title = "Test Task",
                    description = "Description",
                    category = TaskCategory.WORK,
                    urgency = urgency,
                    importance = importance,
                    effort = TaskEffort.MEDIUM,
                    energyTag = null,
                    createdAt = System.currentTimeMillis()
                )
            }
        }
    }
})
