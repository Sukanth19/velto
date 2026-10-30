package com.decisionexecution.app.presentation.common

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Raw database errors never reach the user verbatim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorMessagesTest {

    @Test
    fun `validation messages pass through`() {
        IllegalArgumentException("Urgency must be between 1 and 5").toUserMessage("save the task") shouldBe
            "Urgency must be between 1 and 5"
    }

    @Test
    fun `storage errors suggest freeing space`() {
        val expected = "Couldn't save the task: your phone is low on storage. Free up some space and try again."
        SQLiteFullException("database or disk is full (code 13)").toUserMessage("save the task") shouldBe expected
        SQLiteDiskIOException("disk I/O error (code 10)").toUserMessage("save the task") shouldBe expected
    }

    @Test
    fun `constraint errors explain the conflict`() {
        SQLiteConstraintException("UNIQUE constraint failed: tasks.id").toUserMessage("save the task") shouldBe
            "Couldn't save the task: it conflicts with something already saved."
    }

    @Test
    fun `other database errors hide the sqlite details`() {
        val message = SQLiteException("database is locked (code 5)").toUserMessage("load your tasks")
        message shouldBe "Couldn't load your tasks. Please try again."
        message shouldNotContain "code 5"
    }
}
