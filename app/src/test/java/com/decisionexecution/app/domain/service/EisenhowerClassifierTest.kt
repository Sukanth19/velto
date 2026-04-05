package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Unit tests for EisenhowerClassifier with specific examples.
 */
class EisenhowerClassifierTest : StringSpec({
    
    "should classify task with urgency 4 and importance 4 as URGENT_IMPORTANT" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 4, importance = 4)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.URGENT_IMPORTANT
    }
    
    "should classify task with urgency 5 and importance 5 as URGENT_IMPORTANT" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 5, importance = 5)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.URGENT_IMPORTANT
    }
    
    "should classify task with urgency 3 and importance 4 as NOT_URGENT_IMPORTANT" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 3, importance = 4)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.NOT_URGENT_IMPORTANT
    }
    
    "should classify task with urgency 1 and importance 5 as NOT_URGENT_IMPORTANT" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 1, importance = 5)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.NOT_URGENT_IMPORTANT
    }
    
    "should classify task with urgency 4 and importance 3 as URGENT_NOT_IMPORTANT" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 4, importance = 3)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.URGENT_NOT_IMPORTANT
    }
    
    "should classify task with urgency 5 and importance 1 as URGENT_NOT_IMPORTANT" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 5, importance = 1)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.URGENT_NOT_IMPORTANT
    }
    
    "should classify task with urgency 3 and importance 3 as NEITHER" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 3, importance = 3)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.NEITHER
    }
    
    "should classify task with urgency 1 and importance 1 as NEITHER" {
        val classifier = EisenhowerClassifier()
        val task = createTask(urgency = 1, importance = 1)
        
        classifier.classify(task) shouldBe EisenhowerQuadrant.NEITHER
    }
    
    "should group empty list into empty map" {
        val classifier = EisenhowerClassifier()
        val tasks = emptyList<Task>()
        
        val grouped = classifier.groupByQuadrant(tasks)
        
        grouped.isEmpty() shouldBe true
    }
    
    "should group tasks correctly by quadrant" {
        val classifier = EisenhowerClassifier()
        val tasks = listOf(
            createTask(urgency = 5, importance = 5), // URGENT_IMPORTANT
            createTask(urgency = 4, importance = 4), // URGENT_IMPORTANT
            createTask(urgency = 2, importance = 5), // NOT_URGENT_IMPORTANT
            createTask(urgency = 5, importance = 2), // URGENT_NOT_IMPORTANT
            createTask(urgency = 1, importance = 1)  // NEITHER
        )
        
        val grouped = classifier.groupByQuadrant(tasks)
        
        grouped[EisenhowerQuadrant.URGENT_IMPORTANT]?.size shouldBe 2
        grouped[EisenhowerQuadrant.NOT_URGENT_IMPORTANT]?.size shouldBe 1
        grouped[EisenhowerQuadrant.URGENT_NOT_IMPORTANT]?.size shouldBe 1
        grouped[EisenhowerQuadrant.NEITHER]?.size shouldBe 1
    }
    
    "should handle all tasks in same quadrant" {
        val classifier = EisenhowerClassifier()
        val tasks = listOf(
            createTask(urgency = 5, importance = 5),
            createTask(urgency = 4, importance = 4),
            createTask(urgency = 4, importance = 5)
        )
        
        val grouped = classifier.groupByQuadrant(tasks)
        
        grouped.size shouldBe 1
        grouped[EisenhowerQuadrant.URGENT_IMPORTANT]?.size shouldBe 3
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
