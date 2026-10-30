package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.Task
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Urgency as of [today]: the urgency the user set, raised as the due date gets close, so a
 * forgotten deadline climbs the list without anyone re-rating it.
 */
fun effectiveUrgency(task: Task, today: LocalDate): Int {
    val due = task.dueDate ?: return task.urgency
    val floor = when (daysUntil(due, today)) {
        in Long.MIN_VALUE..0L -> 5
        1L -> 4
        in 2L..3L -> 3
        else -> 1
    }
    return maxOf(task.urgency, floor)
}

fun daysUntil(due: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, due)

fun isOverdue(task: Task, today: LocalDate): Boolean = task.dueDate?.isBefore(today) == true

fun isDueToday(task: Task, today: LocalDate): Boolean = task.dueDate == today

/** "Overdue", "Today", "Tomorrow", a weekday this week, or "6 Nov" further out */
fun dueLabel(due: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String {
    val days = daysUntil(due, today)
    return when {
        days < 0 -> if (days == -1L) "Yesterday" else "Overdue"
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days < 7 -> due.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        else -> "${due.dayOfMonth} ${due.month.getDisplayName(TextStyle.SHORT, locale)}"
    }
}

/** The due date as a phrase for chips and widgets: "Overdue", "Due today", "Due Friday", "Due 6 Nov" */
fun dueText(due: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (daysUntil(due, today)) {
    in Long.MIN_VALUE..-1L -> "Overdue"
    0L -> "Due today"
    1L -> "Due tomorrow"
    else -> "Due ${dueLabel(due, today, locale)}"
}
