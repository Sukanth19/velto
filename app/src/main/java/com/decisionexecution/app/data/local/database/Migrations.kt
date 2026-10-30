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

/** Adds habits, the quick-capture inbox and the mood journal. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit_check_ins` (`habitId` TEXT NOT NULL, `day` INTEGER NOT NULL, " +
                "PRIMARY KEY(`habitId`, `day`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_check_ins_day` ON `habit_check_ins` (`day`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `inbox_items` (`id` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mood_entries` (`day` INTEGER NOT NULL, `mood` INTEGER NOT NULL, " +
                "`energy` INTEGER NOT NULL, `note` TEXT, `loggedAt` INTEGER NOT NULL, PRIMARY KEY(`day`))"
        )
    }
}
