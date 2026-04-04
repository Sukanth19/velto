package com.decisionexecution.app.di

import android.content.Context
import androidx.room.Room
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
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
        ).build()
    }
    
    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideTaskCompletionDao(database: AppDatabase): TaskCompletionDao = 
        database.taskCompletionDao()
}
