package com.decisionexecution.app.di

import android.content.Context
import androidx.room.Room
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.local.dao.TaskDao
import com.decisionexecution.app.data.local.dao.FocusSessionDao
import com.decisionexecution.app.data.local.dao.TaskCompletionDao
import com.decisionexecution.app.data.local.database.MIGRATION_1_2
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
        ).addMigrations(MIGRATION_1_2).build()
    }
    
    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideTaskCompletionDao(database: AppDatabase): TaskCompletionDao = 
        database.taskCompletionDao()
    
    @Provides
    fun provideFocusSessionDao(database: AppDatabase): FocusSessionDao = database.focusSessionDao()
}
