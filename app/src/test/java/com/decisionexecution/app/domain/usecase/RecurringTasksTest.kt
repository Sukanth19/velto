package com.decisionexecution.app.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.repository.PerformanceRepositoryImpl
import com.decisionexecution.app.data.repository.TaskRepositoryImpl
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
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
 * Completing and undoing tasks with due dates, repeats and steps, against a real database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurringTasksTest {

    private lateinit var db: AppDatabase
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var complete: CompleteTaskUseCase
    private lateinit var undo: UndoCompleteTaskUseCase
    private val today = LocalDate.of(2026, 10, 30)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tasks = TaskRepositoryImpl(db.taskDao(), db.taskCompletionDao(), db.subtaskDao())
        val performance = PerformanceRepositoryImpl(db.taskCompletionDao(), db.focusSessionDao())
        complete = CompleteTaskUseCase(tasks, performance)
        undo = UndoCompleteTaskUseCase(tasks, performance)
    }

    @After
    fun tearDown() = db.close()

    private fun task(id: String, recurrence: Recurrence?, due: LocalDate?) =
        Task(id, "Water plants", null, TaskCategory.PERSONAL, 3, 3, TaskEffort.LOW, null, 1, dueDate = due, recurrence = recurrence)

    private suspend fun steps(taskId: String, vararg done: Boolean) = tasks.replaceSubtasks(
        taskId,
        done.mapIndexed { i, isDone -> Subtask("$taskId-s$i", taskId, "Step $i", isDone, i) }
    )

    @Test
    fun `a repeating task moves on with its steps reset, and undo puts it all back`(): Unit = runBlocking {
        val weekly = task("w", Recurrence.WEEKLY, today)
        tasks.insertTask(weekly)
        steps("w", true, false)

        val receipt = complete("w", today = today)

        // Still active, due next week, steps cleared, and the completion counted
        tasks.getAllTasks().first().single().dueDate shouldBe today.plusWeeks(1)
        tasks.getSubtasks("w").first().map { it.isDone } shouldBe listOf(false, false)
        db.taskCompletionDao().getCompletionsInPeriod(0, Long.MAX_VALUE).first().size shouldBe 1

        undo(receipt)

        tasks.getAllTasks().first().single() shouldBe weekly
        tasks.getSubtasks("w").first().map { it.isDone } shouldBe listOf(true, false)
        db.taskCompletionDao().getCompletionsInPeriod(0, Long.MAX_VALUE).first().size shouldBe 0
    }

    @Test
    fun `a one-off task closes and comes back on undo, keeping its due date and steps`(): Unit = runBlocking {
        val once = task("o", null, today.plusDays(2))
        tasks.insertTask(once)
        steps("o", true)

        val receipt = complete("o", today = today)
        tasks.getAllTasks().first() shouldBe emptyList()

        undo(receipt)
        tasks.getAllTasks().first().single() shouldBe once
        tasks.getSubtasks("o").first().single().isDone shouldBe true
    }

    @Test
    fun `deleting a task removes its steps`(): Unit = runBlocking {
        tasks.insertTask(task("d", null, null))
        steps("d", false, false)

        tasks.deleteTask("d")

        db.backupDao().subtasks() shouldBe emptyList()
    }
}
