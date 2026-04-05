package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.EisenhowerQuadrant
import com.decisionexecution.app.domain.model.Task

/**
 * Domain service for classifying tasks into Eisenhower Matrix quadrants.
 * 
 * The Eisenhower Matrix organizes tasks based on urgency and importance:
 * - URGENT_IMPORTANT: urgency >= 4 AND importance >= 4
 * - NOT_URGENT_IMPORTANT: urgency <= 3 AND importance >= 4
 * - URGENT_NOT_IMPORTANT: urgency >= 4 AND importance <= 3
 * - NEITHER: urgency <= 3 AND importance <= 3
 */
class EisenhowerClassifier {
    
    /**
     * Classifies a single task into an Eisenhower quadrant based on its urgency and importance.
     * 
     * @param task The task to classify
     * @return The EisenhowerQuadrant the task belongs to
     */
    fun classify(task: Task): EisenhowerQuadrant {
        return when {
            task.urgency >= 4 && task.importance >= 4 -> EisenhowerQuadrant.URGENT_IMPORTANT
            task.urgency <= 3 && task.importance >= 4 -> EisenhowerQuadrant.NOT_URGENT_IMPORTANT
            task.urgency >= 4 && task.importance <= 3 -> EisenhowerQuadrant.URGENT_NOT_IMPORTANT
            else -> EisenhowerQuadrant.NEITHER
        }
    }
    
    /**
     * Groups a list of tasks by their Eisenhower quadrant.
     * 
     * @param tasks The list of tasks to group
     * @return A map where keys are EisenhowerQuadrant values and values are lists of tasks
     */
    fun groupByQuadrant(tasks: List<Task>): Map<EisenhowerQuadrant, List<Task>> {
        return tasks.groupBy { classify(it) }
    }
}
