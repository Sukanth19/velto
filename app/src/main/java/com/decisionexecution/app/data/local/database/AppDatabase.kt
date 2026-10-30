package com.decisionexecution.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.decisionexecution.app.data.local.dao.BackupDao
import com.decisionexecution.app.data.local.dao.HabitDao
import com.decisionexecution.app.data.local.dao.InboxDao
import com.decisionexecution.app.data.local.dao.MoodDao
import com.decisionexecution.app.data.local.dao.StatsDao
import com.decisionexecution.app.data.local.dao.SubtaskDao
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.FocusSessionDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.local.entity.TaskEntity
import com.decisionexecution.app.data.local.entity.FocusSessionEntity
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import com.decisionexecution.app.data.local.entity.InboxItemEntity
import com.decisionexecution.app.data.local.entity.MoodEntryEntity
import com.decisionexecution.app.data.local.entity.SubtaskEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskCompletionEntity::class,
        FocusSessionEntity::class,
        HabitEntity::class,
        HabitCheckInEntity::class,
        InboxItemEntity::class,
        MoodEntryEntity::class,
        SubtaskEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun habitDao(): HabitDao
    abstract fun inboxDao(): InboxDao
    abstract fun moodDao(): MoodDao
    abstract fun statsDao(): StatsDao
    abstract fun backupDao(): BackupDao
    abstract fun subtaskDao(): SubtaskDao
}
