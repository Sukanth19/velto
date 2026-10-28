package com.decisionexecution.app.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migrates real databases built from the exported schemas in app/schemas.
 * Requirements: 7.6, 10.1
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun `migrating 1 to 2 keeps tasks and completions and adds focus sessions`() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, title, description, category, urgency, importance, effort, energyTag, createdAt, isCompleted, completedAt) " +
                    "VALUES ('t-1', 'Keep me', NULL, 'WORK', 3, 4, 'LOW', NULL, 1, 0, NULL)"
            )
            db.execSQL(
                "INSERT INTO task_completions (id, taskId, taskTitle, completedAt, focusDuration) " +
                    "VALUES ('c-1', 't-0', 'Done', 2, NULL)"
            )
        }

        // Validates the migrated schema against 2.json
        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT title FROM tasks").use { it.moveToFirst(); it.getString(0) shouldBe "Keep me" }
        db.query("SELECT COUNT(*) FROM task_completions").use { it.moveToFirst(); it.getInt(0) shouldBe 1 }
        db.execSQL("INSERT INTO focus_sessions (id, taskId, completedAt, durationMinutes) VALUES ('f-1', 't-1', 3, 25)")
        db.query("SELECT durationMinutes FROM focus_sessions").use { it.moveToFirst(); it.getInt(0) shouldBe 25 }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
