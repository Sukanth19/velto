package com.decisionexecution.app.domain.model

import java.time.LocalDate

/** A calendar on the phone, such as a Google account's calendar synced by the Google Calendar app */
data class DeviceCalendar(
    val id: Long,
    val name: String,
    /** The account it syncs with, like an email address; the same as [name] for a local calendar */
    val accountName: String,
    val isGoogle: Boolean
)

/**
 * When the calendar app should remind about a task. Tasks are all-day events on their due date,
 * so each reminder is a number of days before at a time of day.
 */
enum class CalendarReminder(val label: String, val daysBefore: Int, val minuteOfDay: Int) {
    NONE("No reminder", 0, 0),
    EVENING_BEFORE("The evening before, 6 PM", 1, 18 * 60),
    MORNING_BEFORE("The day before, 9 AM", 1, 9 * 60),
    TWO_DAYS_BEFORE("Two days before, 9 AM", 2, 9 * 60),
    WEEK_BEFORE("A week before, 9 AM", 7, 9 * 60);

    /**
     * Minutes before the start of the all-day event, which is midnight at the start of the due
     * date; null for no reminder. The day before at 9 AM is 15 hours before that midnight.
     */
    val minutesBefore: Int?
        get() = if (this == NONE) null else daysBefore * 24 * 60 - minuteOfDay
}

/** Where tasks go on the calendar; [calendar] is null until the user links one */
data class CalendarSettings(
    val calendar: DeviceCalendar? = null,
    val reminder: CalendarReminder = CalendarReminder.MORNING_BEFORE
) {
    val isLinked: Boolean get() = calendar != null
}

/** An all-day calendar event for a task, ready to write to a calendar */
data class CalendarEvent(
    val title: String,
    val description: String?,
    val date: LocalDate,
    val reminderMinutesBefore: Int?
)
