package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.model.EnergyTag

class ScoringEngine {
    
    private companion object {
        const val URGENCY_WEIGHT = 2.0
        const val IMPORTANCE_WEIGHT = 3.0
        const val EFFORT_MODIFIER = -0.5
    }
    
    fun calculateScore(task: Task): Double {
        val urgencyScore = URGENCY_WEIGHT * task.urgency
        val importanceScore = IMPORTANCE_WEIGHT * task.importance
        val effortValue = when (task.effort) {
            TaskEffort.LOW -> 1
            TaskEffort.MEDIUM -> 2
            TaskEffort.HIGH -> 3
        }
        val effortScore = EFFORT_MODIFIER * effortValue
        val energyBonus = when (task.energyTag) {
            EnergyTag.DEEP_WORK -> 1.0
            EnergyTag.LOW -> 0.5
            null -> 0.0
        }
        
        return urgencyScore + importanceScore + effortScore + energyBonus
    }
    
    fun getBestTask(tasks: List<Task>): Task? {
        if (tasks.isEmpty()) return null
        
        return tasks.maxWithOrNull(compareBy<Task> { calculateScore(it) }
            .thenBy { -it.createdAt }) // Older tasks first (negative for descending)
    }
    
    fun getTopThreeTasks(tasks: List<Task>): List<Task> {
        return tasks.sortedWith(
            compareByDescending<Task> { calculateScore(it) }
                .thenBy { it.createdAt } // Older tasks first
        ).take(3)
    }
}
