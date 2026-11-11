package com.decisionexecution.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.SharedPreferences
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.core.content.edit
import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.CalendarSettings
import com.decisionexecution.app.domain.model.DeviceCalendar
import com.decisionexecution.app.domain.repository.CalendarRepository
import com.decisionexecution.app.domain.service.allDayEndMillis
import com.decisionexecution.app.domain.service.allDayStartMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Writes task events through Android's calendar provider, which Google Calendar syncs to the
 * user's Google account, so there's no sign-in or network code here.
 *
 * The link and the task-to-event ids live in plain shared preferences on purpose: those aren't
 * in Android backup (see data_extraction_rules.xml), and event ids mean nothing on another phone.
 */
class CalendarRepositoryImpl(
    private val resolver: ContentResolver,
    private val prefs: SharedPreferences,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : CalendarRepository {

    private val settings = MutableStateFlow(readSettings())
    private val linkedTasks = MutableStateFlow(readLinkedTasks())

    override fun settings(): Flow<CalendarSettings> = settings.asStateFlow()

    override fun tasksOnCalendar(): Flow<Set<String>> = linkedTasks.asStateFlow()

    override suspend fun writableCalendars(): List<DeviceCalendar> = withContext(io) {
        val projection = arrayOf(
            Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE, Calendars.CALENDAR_ACCESS_LEVEL
        )
        val calendars = mutableListOf<DeviceCalendar>()
        resolver.query(Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                // Holidays, birthdays and calendars shared read-only can't take new events
                if (cursor.getInt(4) < Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                val account = cursor.getString(2).orEmpty()
                calendars += DeviceCalendar(
                    id = cursor.getLong(0),
                    name = cursor.getString(1) ?: account,
                    accountName = account,
                    isGoogle = cursor.getString(3) == GOOGLE_ACCOUNT_TYPE
                )
            }
        }
        calendars.sortedWith(compareByDescending<DeviceCalendar> { it.isGoogle }.thenBy { it.accountName }.thenBy { it.name })
    }

    override suspend fun link(calendar: DeviceCalendar?) {
        prefs.edit {
            if (calendar == null) {
                remove(KEY_CALENDAR_ID); remove(KEY_CALENDAR_NAME); remove(KEY_CALENDAR_ACCOUNT); remove(KEY_CALENDAR_GOOGLE)
            } else {
                putLong(KEY_CALENDAR_ID, calendar.id)
                putString(KEY_CALENDAR_NAME, calendar.name)
                putString(KEY_CALENDAR_ACCOUNT, calendar.accountName)
                putBoolean(KEY_CALENDAR_GOOGLE, calendar.isGoogle)
            }
        }
        settings.update { it.copy(calendar = calendar) }
    }

    override suspend fun setReminder(reminder: CalendarReminder) {
        prefs.edit { putString(KEY_REMINDER, reminder.name) }
        settings.update { it.copy(reminder = reminder) }
    }

    override suspend fun saveEvent(taskId: String, calendarId: Long, event: CalendarEvent) = withContext(io) {
        val existing = storedEvent(taskId)
        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, calendarId)
            put(Events.TITLE, event.title)
            put(Events.DESCRIPTION, event.description)
            put(Events.DTSTART, allDayStartMillis(event.date))
            put(Events.DTEND, allDayEndMillis(event.date))
            put(Events.ALL_DAY, 1)
            put(Events.EVENT_TIMEZONE, "UTC")
        }
        val updated = existing != null && existing.calendarId == calendarId &&
            resolver.update(Events.CONTENT_URI, values, MATCH_EVENT, existing.matchArgs()) > 0
        val eventId = if (updated) {
            existing!!.eventId
        } else {
            // Linked to a different calendar since, so the old copy goes rather than leaving a duplicate
            existing?.let { resolver.delete(Events.CONTENT_URI, MATCH_EVENT, it.matchArgs()) }
            val uri = resolver.insert(Events.CONTENT_URI, values) ?: error("The calendar didn't take the event")
            ContentUris.parseId(uri)
        }
        replaceReminder(eventId, event.reminderMinutesBefore)
        remember(taskId, StoredEvent(calendarId, eventId))
    }

    override suspend fun deleteEvent(taskId: String) = withContext(io) {
        val existing = storedEvent(taskId) ?: return@withContext
        resolver.delete(Events.CONTENT_URI, MATCH_EVENT, existing.matchArgs())
        forget(taskId)
    }

    private fun replaceReminder(eventId: Long, minutesBefore: Int?) {
        resolver.delete(Reminders.CONTENT_URI, "${Reminders.EVENT_ID}=?", arrayOf(eventId.toString()))
        if (minutesBefore == null) return
        resolver.insert(Reminders.CONTENT_URI, ContentValues().apply {
            put(Reminders.EVENT_ID, eventId)
            put(Reminders.MINUTES, minutesBefore)
            put(Reminders.METHOD, Reminders.METHOD_ALERT)
        })
    }

    private data class StoredEvent(val calendarId: Long, val eventId: Long) {
        // Matching the calendar too means a stale id can never touch someone else's event
        fun matchArgs() = arrayOf(eventId.toString(), calendarId.toString(), "0")
        fun encode() = "$calendarId:$eventId"
    }

    private fun storedEvent(taskId: String): StoredEvent? {
        val parts = prefs.getString(EVENT_PREFIX + taskId, null)?.split(':') ?: return null
        val calendarId = parts.getOrNull(0)?.toLongOrNull() ?: return null
        val eventId = parts.getOrNull(1)?.toLongOrNull() ?: return null
        return StoredEvent(calendarId, eventId)
    }

    private fun remember(taskId: String, event: StoredEvent) {
        prefs.edit { putString(EVENT_PREFIX + taskId, event.encode()) }
        linkedTasks.update { it + taskId }
    }

    private fun forget(taskId: String) {
        prefs.edit { remove(EVENT_PREFIX + taskId) }
        linkedTasks.update { it - taskId }
    }

    private fun readSettings(): CalendarSettings {
        val defaults = CalendarSettings()
        val calendar = if (prefs.contains(KEY_CALENDAR_ID)) {
            DeviceCalendar(
                id = prefs.getLong(KEY_CALENDAR_ID, 0),
                name = prefs.getString(KEY_CALENDAR_NAME, null).orEmpty(),
                accountName = prefs.getString(KEY_CALENDAR_ACCOUNT, null).orEmpty(),
                isGoogle = prefs.getBoolean(KEY_CALENDAR_GOOGLE, false)
            )
        } else null
        val reminder = prefs.getString(KEY_REMINDER, null)
            ?.let { name -> CalendarReminder.entries.find { it.name == name } } ?: defaults.reminder
        return CalendarSettings(calendar, reminder)
    }

    private fun readLinkedTasks(): Set<String> =
        prefs.all.keys.filter { it.startsWith(EVENT_PREFIX) }.map { it.removePrefix(EVENT_PREFIX) }.toSet()

    companion object {
        const val PREFS_NAME = "calendar"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val KEY_CALENDAR_ID = "calendar_id"
        private const val KEY_CALENDAR_NAME = "calendar_name"
        private const val KEY_CALENDAR_ACCOUNT = "calendar_account"
        private const val KEY_CALENDAR_GOOGLE = "calendar_google"
        private const val KEY_REMINDER = "reminder"
        private const val EVENT_PREFIX = "event_"
        private val MATCH_EVENT = "${Events._ID}=? AND ${Events.CALENDAR_ID}=? AND ${Events.DELETED}=?"
    }
}
