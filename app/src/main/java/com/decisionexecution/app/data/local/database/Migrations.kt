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

/** Adds due dates and repeats to tasks, and a table of steps for each task. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `dueDate` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrence` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `subtasks` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`isDone` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`taskId`) " +
                "REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)")
    }
}

/** Adds time estimates, dodge counts and snoozing to tasks, and estimate vs actual time to completions. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `estimateMinutes` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `dodges` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `snoozedUntil` INTEGER")
        db.execSQL("ALTER TABLE `task_completions` ADD COLUMN `estimateMinutes` INTEGER")
        db.execSQL("ALTER TABLE `task_completions` ADD COLUMN `actualMinutes` INTEGER")
    }
}
