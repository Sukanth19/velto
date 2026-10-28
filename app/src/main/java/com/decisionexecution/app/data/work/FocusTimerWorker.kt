package com.decisionexecution.app.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.service.FocusTimerScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs when a focus or break phase should have ended. The session manager works out what
 * happened from the clock, moves to the next phase, notifies, and schedules the next check.
 *
 * Requirements: 4.4, 4.5, 13.1, 13.2, 13.3, 13.6
 */
@HiltWorker
class FocusTimerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val focusSessionManager: FocusSessionManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        focusSessionManager.sync()
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "focus_timer"
        private const val MAX_ATTEMPTS = 3
    }
}

/**
 * Schedules a single check for when the current phase ends, instead of polling.
 * The spec's 1-minute PeriodicWorkRequest isn't possible: WorkManager clamps periodic work to 15 minutes.
 */
@Singleton
class WorkManagerFocusTimerScheduler @Inject constructor(
    private val workManager: WorkManager
) : FocusTimerScheduler {

    override fun scheduleCheck(delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<FocusTimerWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(FocusTimerWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(FocusTimerWorker.UNIQUE_WORK_NAME)
    }
}
