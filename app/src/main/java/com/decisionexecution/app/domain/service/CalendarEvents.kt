package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.Task
import java.time.LocalDate
import java.time.ZoneOffset

/** The all-day event for a task on its due date, or null if it has no due date to put on a calendar */
fun calendarEventFor(task: Task, reminder: CalendarReminder): CalendarEvent? {
    val due = task.dueDate ?: return null
    return CalendarEvent(
        title = task.title,
        description = task.description?.takeIf { it.isNotBlank() },
        date = due,
        reminderMinutesBefore = reminder.minutesBefore
    )
}

/**
 * Calendars store all-day events from midnight UTC at the start of the day to midnight UTC at the
 * start of the next, whatever the phone's time zone, so the event lands on the right date everywhere.
 */
fun allDayStartMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun allDayEndMillis(date: LocalDate): Long = allDayStartMillis(date.plusDays(1))
