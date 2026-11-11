package com.decisionexecution.app.presentation.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.service.allDayEndMillis
import com.decisionexecution.app.domain.service.allDayStartMillis

/**
 * Opens the calendar app (Google Calendar on most phones) with the event filled in for the user to
 * save. It needs no calendar permission, so it works before a calendar is linked.
 */
fun calendarInsertIntent(event: CalendarEvent): Intent =
    Intent(Intent.ACTION_INSERT, Events.CONTENT_URI)
        .putExtra(Events.TITLE, event.title)
        .putExtra(Events.DESCRIPTION, event.description)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, allDayStartMillis(event.date))
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, allDayEndMillis(event.date))

/** False when there's no calendar app to open */
fun Context.openCalendarInsert(event: CalendarEvent): Boolean = try {
    startActivity(calendarInsertIntent(event))
    true
} catch (e: ActivityNotFoundException) {
    false
}
