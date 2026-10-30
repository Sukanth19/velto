package com.decisionexecution.app.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.decisionexecution.app.domain.model.Reminder
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.delayUntil
import com.decisionexecution.app.domain.service.habitReminderMessage
import com.decisionexecution.app.domain.service.moodReminderMessage
import com.decisionexecution.app.presentation.navigation.DeepLinks
import com.decisionexecution.app.presentation.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class ReminderKind { HABITS, MOOD }

/**
 * Fires at the reminder time, posts only if there's something left to do, then books tomorrow's.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: PreferencesRepository,
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository,
    private val notificationHelper: NotificationHelper,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KEY_KIND)?.let { name -> ReminderKind.entries.find { it.name == name } } ?: return Result.failure()
        val prefs = preferencesRepository.getPreferences().first()
        val reminder = if (kind == ReminderKind.HABITS) prefs.habitReminder else prefs.moodReminder
        if (!reminder.enabled) return Result.success()

        runCatching {
            val today = LocalDate.now()
            when (kind) {
                ReminderKind.HABITS -> {
                    val habits = habitRepository.getHabits(today).first()
                    habitReminderMessage(habits.filterNot { it.isDoneOn(today) }.map { it.habit.name }, habits.size)
                        ?.let { notificationHelper.showReminder(HABITS_NOTIFICATION_ID, it, DeepLinks.HABITS) }
                }
                ReminderKind.MOOD -> {
                    moodReminderMessage(moodRepository.getEntry(today).first() != null)
                        ?.let { notificationHelper.showReminder(MOOD_NOTIFICATION_ID, it, DeepLinks.JOURNAL) }
                }
            }
        }
        // Chained one-shots rather than periodic work, so the time doesn't drift by hours
        reminderScheduler.schedule(kind, reminder, afterRun = true)
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
        private const val HABITS_NOTIFICATION_ID = 2001
        private const val MOOD_NOTIFICATION_ID = 2002
    }
}

@Singleton
class ReminderScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    /**
     * Books the next run of [kind], or cancels it when turned off. [afterRun] is for the worker
     * booking its own successor, which has to queue behind itself instead of replacing itself.
     */
    fun schedule(kind: ReminderKind, reminder: Reminder, afterRun: Boolean = false) {
        val name = uniqueName(kind)
        if (!reminder.enabled) {
            workManager.cancelUniqueWork(name)
            return
        }
        // After a run, look from a minute ahead so a run that fired a little early books tomorrow, not today again
        val lookAhead = if (afterRun) 60_000L else 0L
        val delay = lookAhead + delayUntil(LocalDateTime.now().plusNanos(lookAhead * 1_000_000), reminder.minuteOfDay).toMillis()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_KIND to kind.name))
            .build()
        workManager.enqueueUniqueWork(
            name,
            if (afterRun) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun uniqueName(kind: ReminderKind) = "reminder_${kind.name.lowercase()}"
}
