package com.decisionexecution.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.FocusSessionDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.local.entity.TaskEntity
import com.decisionexecution.app.data.local.entity.FocusSessionEntity
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity

@Database(
    entities = [TaskEntity::class, TaskCompletionEntity::class, FocusSessionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun focusSessionDao(): FocusSessionDao
}
