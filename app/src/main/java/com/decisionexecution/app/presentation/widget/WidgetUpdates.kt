package com.decisionexecution.app.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import com.decisionexecution.app.domain.usecase.GetInsightsUseCase
import com.decisionexecution.app.domain.usecase.GetProductivityMetricsUseCase
import com.decisionexecution.app.domain.usecase.GetTopThreeTasksUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
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
    private val focusSessionManager: FocusSessionManager,
    private val getTopThreeTasksUseCase: GetTopThreeTasksUseCase,
    private val getProductivityMetricsUseCase: GetProductivityMetricsUseCase,
    private val getInsightsUseCase: GetInsightsUseCase,
    private val preferencesRepository: PreferencesRepository
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
        scope.launch {
            getTopThreeTasksUseCase()
                .map(::topTaskRows)
                .distinctUntilChanged()
                .collect { safely { TopTasksWidget().updateAll(context) } }
        }
        scope.launch {
            // Today's bounds are fixed when the app starts; the periodic refresh rolls them over at midnight
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            combine(
                getProductivityMetricsUseCase(start, end, 7),
                getInsightsUseCase(today, zone),
                preferencesRepository.getPreferences()
            ) { metrics, insights, prefs ->
                TodayWidgetState(metrics.totalFocusTime, prefs.dailyFocusGoalMinutes, metrics.completedTasks, insights.streakDays)
            }
                .distinctUntilChanged()
                .collect { safely { TodayWidget().updateAll(context) } }
        }
    }

    // A widget refresh failing must never take the app down (Requirement 8.4)
    private suspend fun safely(block: suspend () -> Unit) {
        runCatching { block() }
    }
}

/**
 * Fallback refresh for the best-task, today and top-tasks widgets when the app isn't running:
 * the best task can change without user action as tasks age, today's stats reset at midnight,
 * and the spec allows up to 15 minutes. Keeps its old name so already-scheduled work still resolves.
 * Requirements: 8.4
 */
class BestTaskWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        BestTaskWidget().updateAll(applicationContext)
        TodayWidget().updateAll(applicationContext)
        TopTasksWidget().updateAll(applicationContext)
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
