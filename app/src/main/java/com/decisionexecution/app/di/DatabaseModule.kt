package com.decisionexecution.app.di

import android.content.Context
import androidx.room.Room
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.FocusSessionDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.local.dao.BackupDao
import com.decisionexecution.app.data.local.dao.HabitDao
import com.decisionexecution.app.data.local.dao.InboxDao
import com.decisionexecution.app.data.local.dao.MoodDao
import com.decisionexecution.app.data.local.dao.StatsDao
import com.decisionexecution.app.data.local.database.MIGRATION_1_2
import com.decisionexecution.app.data.local.database.MIGRATION_2_3
import com.decisionexecution.app.data.local.database.MIGRATION_3_4
import com.decisionexecution.app.data.local.dao.SubtaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "decision_execution_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
    
    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideTaskCompletionDao(database: AppDatabase): TaskCompletionDao = 
        database.taskCompletionDao()
    
    @Provides
    fun provideFocusSessionDao(database: AppDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideHabitDao(database: AppDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideInboxDao(database: AppDatabase): InboxDao = database.inboxDao()

    @Provides
    fun provideMoodDao(database: AppDatabase): MoodDao = database.moodDao()

    @Provides
    fun provideStatsDao(database: AppDatabase): StatsDao = database.statsDao()

    @Provides
    fun provideSubtaskDao(database: AppDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideBackupDao(database: AppDatabase): BackupDao = database.backupDao()
}
