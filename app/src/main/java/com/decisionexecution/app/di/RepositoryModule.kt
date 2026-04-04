package com.decisionexecution.app.di

import android.content.Context
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.repository.TaskRepositoryImpl
import com.decisionexecution.app.data.repository.PreferencesRepositoryImpl
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    fun providePreferencesRepository(
        @ApplicationContext context: Context
    ): PreferencesRepository = PreferencesRepositoryImpl(context)
}
