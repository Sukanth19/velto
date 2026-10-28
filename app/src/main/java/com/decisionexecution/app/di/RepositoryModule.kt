package com.decisionexecution.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.decisionexecution.app.data.local.dao.FocusSessionDao
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.repository.PerformanceRepositoryImpl
import com.decisionexecution.app.data.repository.TaskRepositoryImpl
import com.decisionexecution.app.data.repository.PreferencesRepositoryImpl
import com.decisionexecution.app.domain.repository.PerformanceRepository
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
        taskCompletionDao: TaskCompletionDao
    ): TaskRepository = TaskRepositoryImpl(taskDao, taskCompletionDao)
    
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
}
