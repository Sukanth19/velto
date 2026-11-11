package com.decisionexecution.app.testutil

import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.CalendarSettings
import com.decisionexecution.app.domain.model.DeviceCalendar
import com.decisionexecution.app.domain.repository.CalendarRepository
import com.decisionexecution.app.domain.usecase.CalendarSyncUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

val GOOGLE_CALENDAR = DeviceCalendar(id = 7, name = "Personal", accountName = "student@gmail.com", isGoogle = true)

/** A calendar kept in memory: what [events] holds is what the calendar app would show */
class FakeCalendarRepository(
    linked: DeviceCalendar? = null,
    reminder: CalendarReminder = CalendarReminder.MORNING_BEFORE,
    var calendars: List<DeviceCalendar> = listOf(GOOGLE_CALENDAR)
) : CalendarRepository {
    private val settings = MutableStateFlow(CalendarSettings(linked, reminder))
    private val stored = MutableStateFlow<Map<String, Pair<Long, CalendarEvent>>>(emptyMap())

    /** Set to make every calendar call fail like a revoked permission or a broken provider */
    var failWith: Exception? = null

    val events: Map<String, CalendarEvent> get() = stored.value.mapValues { it.value.second }
    fun calendarOf(taskId: String): Long? = stored.value[taskId]?.first

    override fun settings(): Flow<CalendarSettings> = settings
    override fun tasksOnCalendar(): Flow<Set<String>> = stored.map { it.keys }

    override suspend fun writableCalendars(): List<DeviceCalendar> {
        failWith?.let { throw it }
        return calendars
    }

    override suspend fun link(calendar: DeviceCalendar?) = settings.update { it.copy(calendar = calendar) }
    override suspend fun setReminder(reminder: CalendarReminder) = settings.update { it.copy(reminder = reminder) }

    override suspend fun saveEvent(taskId: String, calendarId: Long, event: CalendarEvent) {
        failWith?.let { throw it }
        stored.value = stored.value + (taskId to (calendarId to event))
    }

    override suspend fun deleteEvent(taskId: String) {
        failWith?.let { throw it }
        stored.value = stored.value - taskId
    }

    private inline fun MutableStateFlow<CalendarSettings>.update(change: (CalendarSettings) -> CalendarSettings) {
        value = change(value)
    }
}

/** Calendar sync with nothing linked, for tests that don't care about the calendar */
fun noCalendarSync() = CalendarSyncUseCase(FakeCalendarRepository())
