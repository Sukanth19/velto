package com.decisionexecution.app.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshes widgets as soon as what they show changes, while the app process is alive.
 * Requirements: 8.4, 8.5
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getBestTaskUseCase: GetBestTaskUseCase,
    private val focusSessionManager: FocusSessionManager
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            getBestTaskUseCase()
                .map(::bestTaskWidgetState)
                .distinctUntilChanged()
                .collect { safely { BestTaskWidget().updateAll(context) } }
        }
        scope.launch {
            focusSessionManager.sessionState
                .map(::timerWidgetKey)
                .distinctUntilChanged()
                // The first value is whatever the widget already shows
                .drop(1)
                .collect { safely { FocusTimerWidget().updateAll(context) } }
        }
    }

    // A widget refresh failing must never take the app down (Requirement 8.4)
    private suspend fun safely(block: suspend () -> Unit) {
        runCatching { block() }
    }
}

/**
 * Fallback refresh for the best-task widget when the app isn't running: the best task can
 * change without user action as tasks age, and the spec allows up to 15 minutes.
 * Requirements: 8.4
 */
class BestTaskWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        BestTaskWidget().updateAll(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "best_task_widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BestTaskWidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
