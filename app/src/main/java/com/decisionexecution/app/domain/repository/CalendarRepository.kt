package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.CalendarSettings
import com.decisionexecution.app.domain.model.DeviceCalendar
import kotlinx.coroutines.flow.Flow

/**
 * Puts tasks on a calendar on the phone (Google Calendar syncs them to the user's account) and
 * remembers which event belongs to which task. Calls that touch the calendar throw
 * [SecurityException] when calendar access hasn't been granted.
 */
interface CalendarRepository {
    fun settings(): Flow<CalendarSettings>

    /** Calendars the user can add events to, Google ones first */
    suspend fun writableCalendars(): List<DeviceCalendar>

    /** Links [calendar], or unlinks with null. Events already added stay where they are */
    suspend fun link(calendar: DeviceCalendar?)

    suspend fun setReminder(reminder: CalendarReminder)

    /** Ids of the tasks that have an event on the calendar */
    fun tasksOnCalendar(): Flow<Set<String>>

    /** Adds the task's event to [calendarId], or updates the one it already has there */
    suspend fun saveEvent(taskId: String, calendarId: Long, event: CalendarEvent)

    /** Deletes the task's event, if it has one */
    suspend fun deleteEvent(taskId: String)
}
