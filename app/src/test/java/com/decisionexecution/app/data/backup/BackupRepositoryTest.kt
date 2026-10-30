package com.decisionexecution.app.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.local.entity.FocusSessionEntity
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import com.decisionexecution.app.data.local.entity.InboxItemEntity
import com.decisionexecution.app.data.local.entity.MoodEntryEntity
import com.decisionexecution.app.data.local.entity.SubtaskEntity
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import com.decisionexecution.app.data.local.entity.TaskEntity
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.repository.InvalidBackupException
import com.decisionexecution.app.testutil.InMemoryPreferencesRepository
import com.decisionexecution.app.testutil.InMemoryProgressRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Export from one database, import into a fresh one, and everything comes back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRepositoryTest {

    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @Before
    fun setUp() {
        source = database()
        target = database()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private fun repository(db: AppDatabase, prefs: InMemoryPreferencesRepository = InMemoryPreferencesRepository(), progress: InMemoryProgressRepository = InMemoryProgressRepository()) =
        BackupRepositoryImpl(db, db.backupDao(), prefs, progress)

    @Test
    fun `everything survives an export and import`(): Unit = runBlocking {
        val dao = source.backupDao()
        dao.insertTasks(
            listOf(
                TaskEntity("t-1", "Write essay", "Draft first", "SCHOOL", 4, 5, "HIGH", "DEEP_WORK", 10, dueDate = 20_100, recurrence = "WEEKLY"),
                TaskEntity("t-2", "Done thing", null, "WORK", 2, 2, "LOW", null, 11, isCompleted = true, completedAt = 99)
            )
        )
        dao.insertCompletions(listOf(TaskCompletionEntity("c-1", "t-2", "Done thing", 99, null)))
        dao.insertFocusSessions(listOf(FocusSessionEntity("f-1", "t-1", 50, 25)))
        dao.insertHabits(listOf(HabitEntity("h-1", "Read \"daily\"", "READ", 1)))
        dao.insertCheckIns(listOf(HabitCheckInEntity("h-1", 20_000), HabitCheckInEntity("h-1", 20_001)))
        dao.insertInboxItems(listOf(InboxItemEntity("i-1", "Idea, with commas\nand lines", 5, false)))
        dao.insertMoodEntries(listOf(MoodEntryEntity(20_001, 4, 2, "Tired but ok", 7)))
        dao.insertSubtasks(listOf(SubtaskEntity("s-1", "t-1", "Outline", true, 0), SubtaskEntity("s-2", "t-1", "Draft", false, 1)))
        val prefs = InMemoryPreferencesRepository().apply {
            savePreferences(getPreferences().first().copy(focusDurationMinutes = 40, themeMode = ThemeMode.DARK))
        }
        val progress = InMemoryProgressRepository().apply { markCelebrated(4, setOf("FIRST_STEP")) }

        val json = repository(source, prefs, progress).export()

        val restoredPrefs = InMemoryPreferencesRepository()
        val restoredProgress = InMemoryProgressRepository()
        val summary = repository(target, restoredPrefs, restoredProgress).import(json)

        summary.tasks shouldBe 2
        summary.habits shouldBe 1
        summary.moodEntries shouldBe 1
        val restored = target.backupDao()
        restored.tasks().sortedBy { it.id } shouldBe dao.tasks().sortedBy { it.id }
        restored.completions() shouldBe dao.completions()
        restored.focusSessions() shouldBe dao.focusSessions()
        restored.habits() shouldBe dao.habits()
        restored.checkIns().toSet() shouldBe dao.checkIns().toSet()
        restored.inboxItems() shouldBe dao.inboxItems()
        restored.moodEntries() shouldBe dao.moodEntries()
        restored.subtasks().sortedBy { it.position } shouldBe dao.subtasks().sortedBy { it.position }
        restoredPrefs.getPreferences().first().focusDurationMinutes shouldBe 40
        restoredPrefs.getPreferences().first().themeMode shouldBe ThemeMode.DARK
        restoredProgress.getCelebratedLevel().first() shouldBe 4
        restoredProgress.getCelebratedAchievements().first() shouldBe setOf("FIRST_STEP")
    }

    @Test
    fun `a format 1 backup from before due dates and steps still imports`(): Unit = runBlocking {
        val json = """
            {"app": "velto", "format": 1, "tasks": [
              {"id": "t-1", "title": "Old task", "description": null, "category": "WORK", "urgency": 3,
               "importance": 3, "effort": "LOW", "energyTag": null, "createdAt": 1, "isCompleted": false, "completedAt": null}
            ]}
        """.trimIndent()

        repository(target).import(json)

        target.backupDao().tasks().single().let {
            it.title shouldBe "Old task"
            it.dueDate shouldBe null
            it.recurrence shouldBe null
        }
    }

    @Test
    fun `import replaces what was there`(): Unit = runBlocking {
        target.backupDao().insertTasks(listOf(TaskEntity("old", "Old task", null, "WORK", 1, 1, "LOW", null, 1)))
        val json = repository(source).export()

        repository(target).import(json)

        target.backupDao().tasks() shouldBe emptyList()
    }

    @Test
    fun `a file that isn't a backup changes nothing`(): Unit = runBlocking {
        target.backupDao().insertTasks(listOf(TaskEntity("keep", "Keep me", null, "WORK", 1, 1, "LOW", null, 1)))

        shouldThrow<InvalidBackupException> { repository(target).import("{\"hello\": 1}") }
        shouldThrow<InvalidBackupException> { repository(target).import("not json at all") }
        shouldThrow<InvalidBackupException> { repository(target).import("{\"app\":\"velto\",\"format\":99}") }
            .message shouldContain "newer version"

        target.backupDao().tasks().map { it.id } shouldBe listOf("keep")
    }
}
