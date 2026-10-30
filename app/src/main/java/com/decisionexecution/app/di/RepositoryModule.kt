package com.decisionexecution.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.decisionexecution.app.data.local.dao.FocusSessionDao
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.backup.BackupRepositoryImpl
import com.decisionexecution.app.data.local.dao.BackupDao
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.local.dao.HabitDao
import com.decisionexecution.app.data.local.dao.InboxDao
import com.decisionexecution.app.data.local.dao.MoodDao
import com.decisionexecution.app.data.local.dao.StatsDao
import com.decisionexecution.app.data.local.dao.SubtaskDao
import com.decisionexecution.app.data.repository.HabitRepositoryImpl
import com.decisionexecution.app.data.repository.InboxRepositoryImpl
import com.decisionexecution.app.data.repository.MoodRepositoryImpl
import com.decisionexecution.app.data.repository.PerformanceRepositoryImpl
import com.decisionexecution.app.data.repository.ProgressRepositoryImpl
import com.decisionexecution.app.data.repository.TaskRepositoryImpl
import com.decisionexecution.app.data.repository.PreferencesRepositoryImpl
import com.decisionexecution.app.domain.repository.BackupRepository
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.InboxRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.ProgressRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        taskCompletionDao: TaskCompletionDao,
        subtaskDao: SubtaskDao
    ): TaskRepository = TaskRepositoryImpl(taskDao, taskCompletionDao, subtaskDao)
    
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore
    
    @Provides
    @Singleton
    fun providePreferencesRepository(
        dataStore: DataStore<Preferences>
    ): PreferencesRepository = PreferencesRepositoryImpl(dataStore)
    
    @Provides
    @Singleton
    fun providePerformanceRepository(
        taskCompletionDao: TaskCompletionDao,
        focusSessionDao: FocusSessionDao
    ): PerformanceRepository = PerformanceRepositoryImpl(taskCompletionDao, focusSessionDao)

    @Provides
    @Singleton
    fun provideHabitRepository(habitDao: HabitDao): HabitRepository = HabitRepositoryImpl(habitDao)

    @Provides
    @Singleton
    fun provideInboxRepository(inboxDao: InboxDao): InboxRepository = InboxRepositoryImpl(inboxDao)

    @Provides
    @Singleton
    fun provideMoodRepository(moodDao: MoodDao): MoodRepository = MoodRepositoryImpl(moodDao)

    @Provides
    @Singleton
    fun provideProgressRepository(
        statsDao: StatsDao,
        habitDao: HabitDao,
        dataStore: DataStore<Preferences>
    ): ProgressRepository = ProgressRepositoryImpl(statsDao, habitDao, dataStore)

    @Provides
    @Singleton
    fun provideBackupRepository(
        database: AppDatabase,
        backupDao: BackupDao,
        preferencesRepository: PreferencesRepository,
        progressRepository: ProgressRepository
    ): BackupRepository = BackupRepositoryImpl(database, backupDao, preferencesRepository, progressRepository)
}
