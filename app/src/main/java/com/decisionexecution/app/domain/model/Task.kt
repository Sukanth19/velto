package com.decisionexecution.app.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val category: TaskCategory,
    val urgency: Int,
    val importance: Int,
    val effort: TaskEffort,
    val energyTag: EnergyTag?,
    val createdAt: Long
) {
    init {
        require(urgency in 1..5) { "Urgency must be between 1 and 5" }
        require(importance in 1..5) { "Importance must be between 1 and 5" }
    }
}

enum class TaskCategory { SCHOOL, PERSONAL, WORK }
enum class TaskEffort { LOW, MEDIUM, HIGH }
enum class EnergyTag { LOW, DEEP_WORK }
