package com.decisionexecution.app.presentation.common

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException

/**
 * Turns an exception into a message a user can act on, instead of raw SQLite or developer text.
 * [action] completes "Couldn't ...", e.g. "save the task".
 *
 * Requirements: 10.6, 12.4
 */
fun Throwable.toUserMessage(action: String): String = when (this) {
    // Domain validation messages (Task's require() checks) are already written for users
    is IllegalArgumentException -> message ?: "Couldn't $action: some values aren't valid."
    is SQLiteFullException, is SQLiteDiskIOException ->
        "Couldn't $action: your phone is low on storage. Free up some space and try again."
    is SQLiteConstraintException -> "Couldn't $action: it conflicts with something already saved."
    is SQLiteException -> "Couldn't $action. Please try again."
    else -> "Couldn't $action. Please try again."
}
