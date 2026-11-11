package com.decisionexecution.app.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.repository.PerformanceRepositoryImpl
import com.decisionexecution.app.data.repository.TaskRepositoryImpl
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.testutil.FakeCalendarRepository
import com.decisionexecution.app.testutil.GOOGLE_CALENDAR
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * A task's calendar event follows it through completing, undoing and deleting, against a real database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarTaskLifecycleTest {

    private lateinit var db: AppDatabase
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var calendar: FakeCalendarRepository
    private lateinit var sync: CalendarSyncUseCase
    private lateinit var complete: CompleteTaskUseCase
    private lateinit var undo: UndoCompleteTaskUseCase
    private lateinit var delete: DeleteTaskUseCase
    private val today = LocalDate.of(2026, 11, 11)
    private val monday = LocalDate.of(2026, 11, 16)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tasks = TaskRepositoryImpl(db.taskDao(), db.taskCompletionDao(), db.subtaskDao())
        val performance = PerformanceRepositoryImpl(db.taskCompletionDao(), db.focusSessionDao())
        calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        sync = CalendarSyncUseCase(calendar)
        complete = CompleteTaskUseCase(tasks, performance, sync)
        undo = UndoCompleteTaskUseCase(tasks, performance, sync)
        delete = DeleteTaskUseCase(tasks, sync)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun deletingATaskRemovesItsEvent() = runBlocking<Unit> {
        val task = task("Study for test", monday)
        tasks.insertTask(task)
        sync.add(task)

        delete(task.id)

        calendar.events shouldBe emptyMap()
    }

    @Test
    fun deletingStillWorksWhenTheCalendarFails() = runBlocking<Unit> {
        val task = task("Study for test", monday)
        tasks.insertTask(task)
        sync.add(task)
        calendar.failWith = SecurityException("revoked")

        delete(task.id)

        tasks.getTaskById(task.id).first() shouldBe null
    }

    @Test
    fun completingAOneOffTaskLeavesItsEventAsARecord() = runBlocking<Unit> {
        val task = task("Study for test", monday)
        tasks.insertTask(task)
        sync.add(task)

        complete(task.id, today = today)

        calendar.events.getValue(task.id).date shouldBe monday
    }

    @Test
    fun completingARepeatingTaskMovesItsEventToTheNextDate() = runBlocking<Unit> {
        val task = task("Weekly quiz revision", monday, Recurrence.WEEKLY)
        tasks.insertTask(task)
        sync.add(task)

        complete(task.id, today = monday)

        calendar.events.getValue(task.id).date shouldBe monday.plusWeeks(1)
    }

    @Test
    fun undoingPutsTheRepeatingEventBack() = runBlocking<Unit> {
        val task = task("Weekly quiz revision", monday, Recurrence.WEEKLY)
        tasks.insertTask(task)
        sync.add(task)

        val receipt = complete(task.id, today = monday)
        undo(receipt)

        calendar.events.getValue(task.id).date shouldBe monday
    }

    @Test
    fun repeatingTasksNotOnTheCalendarStayOff() = runBlocking<Unit> {
        val task = task("Weekly quiz revision", monday, Recurrence.WEEKLY)
        tasks.insertTask(task)

        complete(task.id, today = monday)

        calendar.events shouldBe emptyMap()
    }

    private fun task(title: String, due: LocalDate, recurrence: Recurrence? = null) = Task(
        id = title.lowercase().replace(' ', '-'),
        title = title,
        description = null,
        category = TaskCategory.SCHOOL,
        urgency = 3,
        importance = 4,
        effort = TaskEffort.MEDIUM,
        energyTag = null,
        createdAt = 1L,
        dueDate = due,
        recurrence = recurrence
    )
}
