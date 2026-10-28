package com.decisionexecution.app.di

import android.content.Context
import androidx.work.WorkManager
import com.decisionexecution.app.data.work.WorkManagerFocusTimerScheduler
import com.decisionexecution.app.domain.service.FocusNotifier
import com.decisionexecution.app.domain.service.FocusTimerScheduler
import com.decisionexecution.app.domain.service.TimeSource
import com.decisionexecution.app.presentation.notifications.NotificationHelper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FocusTimerModule {

    @Binds
    abstract fun bindFocusTimerScheduler(impl: WorkManagerFocusTimerScheduler): FocusTimerScheduler

    @Binds
    abstract fun bindFocusNotifier(impl: NotificationHelper): FocusNotifier

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)

        @Provides
        fun provideTimeSource(): TimeSource = TimeSource { System.currentTimeMillis() }
    }
}
