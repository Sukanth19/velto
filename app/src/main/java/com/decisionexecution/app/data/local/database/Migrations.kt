package com.decisionexecution.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds focus_sessions so focus time is tracked separately from task completions. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, " +
                "`completedAt` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_completedAt` ON `focus_sessions` (`completedAt`)")
    }
}
