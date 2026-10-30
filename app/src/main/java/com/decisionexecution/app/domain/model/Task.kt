package com.decisionexecution.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val category: TaskCategory,
    val urgency: Int,
    val importance: Int,
    val effort: TaskEffort,
    val energyTag: EnergyTag?,
    val createdAt: Long,
    val dueDate: LocalDate? = null,
    /** A repeating task moves to its next date when completed instead of closing */
    val recurrence: Recurrence? = null
) {
    init {
        require(urgency in 1..5) { "Urgency must be between 1 and 5" }
        require(importance in 1..5) { "Importance must be between 1 and 5" }
    }
}

enum class TaskCategory { SCHOOL, PERSONAL, WORK }
enum class TaskEffort { LOW, MEDIUM, HIGH }
enum class EnergyTag { LOW, DEEP_WORK }

enum class Recurrence {
    DAILY, WEEKDAYS, WEEKLY, MONTHLY;

    /**
     * The next due date after completing: one step on from [from], then further until it's
     * after [today], so finishing an overdue weekly task doesn't leave it overdue.
     */
    fun next(from: LocalDate, today: LocalDate): LocalDate {
        var date = step(from)
        while (!date.isAfter(today)) date = step(date)
        return date
    }

    private fun step(date: LocalDate): LocalDate = when (this) {
        DAILY -> date.plusDays(1)
        WEEKLY -> date.plusWeeks(1)
        MONTHLY -> date.plusMonths(1)
        WEEKDAYS -> {
            var next = date.plusDays(1)
            while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) next = next.plusDays(1)
            next
        }
    }
}

/** One step of a task, in order */
data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean,
    val position: Int
)
