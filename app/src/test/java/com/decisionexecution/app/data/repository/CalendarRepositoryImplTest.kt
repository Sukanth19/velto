package com.decisionexecution.app.data.repository

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.test.core.app.ApplicationProvider
import com.decisionexecution.app.domain.model.CalendarEvent
import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.DeviceCalendar
import com.decisionexecution.app.domain.service.allDayEndMillis
import com.decisionexecution.app.domain.service.allDayStartMillis
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The calendar repository against a stand-in for Android's calendar provider, checking the rows
 * Google Calendar would sync.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var provider: FakeCalendarProvider
    private val monday = LocalDate.of(2026, 11, 16)
    private val event = CalendarEvent("Study for test", "Chapters 4 to 6", monday, reminderMinutesBefore = 15 * 60)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = Robolectric.buildContentProvider(FakeCalendarProvider::class.java).create(CalendarContract.AUTHORITY).get()
        provider.addCalendar(1, "Holidays in India", "holiday@group.v.calendar.google.com", "com.google", Calendars.CAL_ACCESS_READ)
        provider.addCalendar(2, "Phone", "Phone", "LOCAL", Calendars.CAL_ACCESS_OWNER)
        provider.addCalendar(3, "student@gmail.com", "student@gmail.com", "com.google", Calendars.CAL_ACCESS_OWNER)
    }

    private fun repository() = CalendarRepositoryImpl(
        context.contentResolver,
        context.getSharedPreferences(CalendarRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE),
        Dispatchers.Unconfined
    )

    @Test
    fun listsOnlyCalendarsThatTakeEventsGoogleFirst() = runBlocking<Unit> {
        val calendars = repository().writableCalendars()

        calendars.map { it.id } shouldBe listOf(3L, 2L)
        calendars.first() shouldBe DeviceCalendar(3, "student@gmail.com", "student@gmail.com", isGoogle = true)
    }

    @Test
    fun linkAndReminderAreRememberedAcrossRestarts() = runBlocking<Unit> {
        val google = repository().writableCalendars().first()
        repository().apply {
            link(google)
            setReminder(CalendarReminder.EVENING_BEFORE)
        }

        val settings = repository().settings().first()
        settings.calendar shouldBe google
        settings.reminder shouldBe CalendarReminder.EVENING_BEFORE
    }

    @Test
    fun unlinkingForgetsTheCalendar() = runBlocking<Unit> {
        val repo = repository()
        repo.link(repo.writableCalendars().first())

        repo.link(null)

        repository().settings().first().calendar shouldBe null
    }

    @Test
    fun savingWritesAnAllDayEventWithAReminder() = runBlocking<Unit> {
        val repo = repository()

        repo.saveEvent("t-1", 3, event)

        val row = provider.events.values.single()
        row[Events.CALENDAR_ID] shouldBe 3L
        row[Events.TITLE] shouldBe "Study for test"
        row[Events.DESCRIPTION] shouldBe "Chapters 4 to 6"
        row[Events.ALL_DAY] shouldBe 1
        row[Events.DTSTART] shouldBe allDayStartMillis(monday)
        row[Events.DTEND] shouldBe allDayEndMillis(monday)
        row[Events.EVENT_TIMEZONE] shouldBe "UTC"
        val reminder = provider.reminders.values.single()
        reminder[Reminders.EVENT_ID] shouldBe provider.events.keys.single()
        reminder[Reminders.MINUTES] shouldBe 15 * 60
        reminder[Reminders.METHOD] shouldBe Reminders.METHOD_ALERT
        repo.tasksOnCalendar().first() shouldBe setOf("t-1")
    }

    @Test
    fun savingAgainUpdatesTheSameEventAndReplacesTheReminder() = runBlocking<Unit> {
        val repo = repository()
        repo.saveEvent("t-1", 3, event)

        repo.saveEvent("t-1", 3, event.copy(date = monday.plusDays(1), reminderMinutesBefore = 6 * 60))

        provider.events.size shouldBe 1
        provider.events.values.single()[Events.DTSTART] shouldBe allDayStartMillis(monday.plusDays(1))
        provider.reminders.values.map { it[Reminders.MINUTES] } shouldBe listOf(6 * 60)
    }

    @Test
    fun noReminderLeavesNoReminderRow() = runBlocking<Unit> {
        repository().saveEvent("t-1", 3, event.copy(reminderMinutesBefore = null))

        provider.reminders shouldBe emptyMap()
    }

    @Test
    fun anEventDeletedInTheCalendarAppIsAddedAgain() = runBlocking<Unit> {
        val repo = repository()
        repo.saveEvent("t-1", 3, event)
        provider.events.clear()

        repo.saveEvent("t-1", 3, event)

        provider.events.size shouldBe 1
    }

    @Test
    fun movingToAnotherCalendarDeletesTheOldCopy() = runBlocking<Unit> {
        val repo = repository()
        repo.saveEvent("t-1", 3, event)

        repo.saveEvent("t-1", 2, event)

        provider.events.values.map { it[Events.CALENDAR_ID] } shouldBe listOf(2L)
    }

    @Test
    fun deletingRemovesTheEventAndForgetsTheTask() = runBlocking<Unit> {
        val repo = repository()
        repo.saveEvent("t-1", 3, event)

        repo.deleteEvent("t-1")

        provider.events shouldBe emptyMap()
        repo.tasksOnCalendar().first() shouldBe emptySet()
        repository().tasksOnCalendar().first() shouldBe emptySet()
    }

    @Test
    fun theTaskToEventLinkSurvivesARestart() = runBlocking<Unit> {
        repository().saveEvent("t-1", 3, event)

        val restarted = repository()
        restarted.tasksOnCalendar().first() shouldBe setOf("t-1")
        restarted.saveEvent("t-1", 3, event.copy(title = "Revise for test"))

        provider.events.size shouldBe 1
        provider.events.values.single()[Events.TITLE] shouldBe "Revise for test"
    }

    @Test
    fun aStaleEventIdNeverTouchesAnotherCalendarsEvent() = runBlocking<Unit> {
        val repo = repository()
        repo.saveEvent("t-1", 3, event)
        val id = provider.events.keys.single()
        // Something else now owns that id in a different calendar
        provider.events[id] = mutableMapOf(Events._ID to id, Events.CALENDAR_ID to 2L, Events.TITLE to "Dentist")

        repo.deleteEvent("t-1")

        provider.events.getValue(id)[Events.TITLE] shouldBe "Dentist"
    }

    @Test
    fun deletingATaskThatWasNeverAddedDoesNothing() = runBlocking<Unit> {
        repository().deleteEvent("never-added")

        provider.deletes shouldBe 0
    }
}

/**
 * Just enough of Android's calendar provider: calendars, events and reminders tables, with
 * selections of the form "a=? AND b=?". A missing `deleted` column counts as 0, like the real one.
 */
class FakeCalendarProvider : ContentProvider() {
    private val calendars = linkedMapOf<Long, MutableMap<String, Any?>>()
    val events = linkedMapOf<Long, MutableMap<String, Any?>>()
    val reminders = linkedMapOf<Long, MutableMap<String, Any?>>()
    var deletes = 0
    private var nextId = 100L

    fun addCalendar(id: Long, name: String, account: String, accountType: String, access: Int) {
        calendars[id] = mutableMapOf(
            Calendars._ID to id, Calendars.CALENDAR_DISPLAY_NAME to name, Calendars.ACCOUNT_NAME to account,
            Calendars.ACCOUNT_TYPE to accountType, Calendars.CALENDAR_ACCESS_LEVEL to access
        )
    }

    private fun table(uri: Uri) = when (uri.pathSegments.first()) {
        "calendars" -> calendars
        "events" -> events
        "reminders" -> reminders
        else -> error("Unknown table $uri")
    }

    private fun matches(row: Map<String, Any?>, selection: String?, args: Array<out String>?): Boolean {
        if (selection == null) return true
        return selection.split(" AND ").withIndex().all { (i, clause) ->
            val column = clause.removeSuffix("=?").trim()
            val value = row[column] ?: if (column == Events.DELETED) 0 else null
            value?.toString() == args!![i]
        }
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sortOrder: String?): Cursor {
        val rows = table(uri).values.filter { matches(it, selection, args) }
        val columns = projection ?: rows.firstOrNull()?.keys?.toTypedArray() ?: emptyArray()
        return MatrixCursor(columns).apply { rows.forEach { row -> addRow(columns.map { row[it] }) } }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val id = nextId++
        table(uri)[id] = values!!.keySet().associateWith { values.get(it) }.toMutableMap().apply { put("_id", id) }
        return ContentUris.withAppendedId(uri, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int {
        val rows = table(uri).values.filter { matches(it, selection, args) }
        rows.forEach { row -> values!!.keySet().forEach { row[it] = values.get(it) } }
        return rows.size
    }

    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int {
        deletes++
        val table = table(uri)
        val ids = table.filterValues { matches(it, selection, args) }.keys
        ids.forEach(table::remove)
        return ids.size
    }

    override fun getType(uri: Uri): String? = null
}
