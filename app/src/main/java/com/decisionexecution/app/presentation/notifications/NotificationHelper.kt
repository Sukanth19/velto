package com.decisionexecution.app.presentation.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.decisionexecution.app.MainActivity
import com.decisionexecution.app.R
import com.decisionexecution.app.domain.service.FocusNotifier
import com.decisionexecution.app.domain.service.ReminderMessage
import com.decisionexecution.app.domain.service.TimerNotice
import com.decisionexecution.app.presentation.navigation.DeepLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Focus and break completion notifications, plus the daily habit and mood reminders. Silently skips posting when the user denied the
 * permission; the timer keeps running either way.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) : FocusNotifier {

    init {
        createChannel()
    }

    override fun onFocusComplete(taskTitle: String) {
        show(
            id = FOCUS_COMPLETE_ID,
            title = "Focus complete",
            text = if (taskTitle.isBlank()) "Time for a break." else "Nice work on \"$taskTitle\". Time for a break.",
            deepLink = DeepLinks.FOCUS
        )
    }

    override fun onBreakComplete() {
        // The break notification replaces the focus one instead of stacking
        NotificationManagerCompat.from(context).cancel(FOCUS_COMPLETE_ID)
        show(
            id = BREAK_COMPLETE_ID,
            title = "Break's over",
            text = "Ready for the next task?",
            deepLink = DeepLinks.HOME
        )
    }

    override fun onStarterComplete(taskTitle: String) {
        show(
            id = FOCUS_COMPLETE_ID,
            title = "Two minutes done",
            text = if (taskTitle.isBlank()) "You started. Keep going?" else "You started \"$taskTitle\". Keep going?",
            deepLink = DeepLinks.FOCUS
        )
    }

    /**
     * The ongoing timer: counts down by itself in the shade and on the lock screen, and on Android 16+
     * asks to be promoted to a Live Update, which puts it in a status bar chip. Silent; the
     * completion alerts are separate notifications on the louder channel.
     */
    override fun onTimerChanged(timer: TimerNotice?) {
        val manager = NotificationManagerCompat.from(context)
        if (timer == null) {
            lastTimerNotification = null
            manager.cancel(TIMER_ID)
            return
        }
        if (!canPostNotifications()) return
        val title = when {
            timer.isBreak -> "Break"
            else -> timer.taskTitle
        }
        val remaining = (timer.totalSeconds - timer.elapsedSeconds).coerceAtLeast(0)
        val text = when {
            timer.isPaused -> "Paused · ${formatClock(remaining)} left"
            timer.isBreak -> "Breathe. Back to it soon"
            timer.isStarter -> "Just two minutes. That's all"
            else -> "Focusing"
        }
        val builder = NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent(TIMER_ID, DeepLinks.FOCUS))
            .setRequestPromotedOngoing(true)
        timer.endsAtMillis?.let { endsAt ->
            builder.setWhen(endsAt).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        } ?: builder.setShowWhen(false)
        when {
            timer.isBreak -> builder.addAction(R.drawable.ic_widget_skip, "Skip break", timerAction(TimerActionReceiver.ACTION_STOP))
            timer.isPaused -> {
                builder.addAction(R.drawable.ic_widget_play, "Resume", timerAction(TimerActionReceiver.ACTION_RESUME))
                builder.addAction(R.drawable.ic_widget_stop, "End", timerAction(TimerActionReceiver.ACTION_STOP))
            }
            else -> {
                builder.addAction(R.drawable.ic_widget_pause, "Pause", timerAction(TimerActionReceiver.ACTION_PAUSE))
                builder.addAction(R.drawable.ic_widget_stop, "End", timerAction(TimerActionReceiver.ACTION_STOP))
            }
        }
        val notification = builder.build()
        lastTimerNotification = notification
        try {
            manager.notify(TIMER_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post; the in-app timer still works
        }
    }

    /** The timer notification's id, for the focus sound service to share instead of adding a second one */
    val timerNotificationId: Int get() = TIMER_ID

    @Volatile private var lastTimerNotification: android.app.Notification? = null

    /** Whether a session is still showing its timer */
    val hasTimer: Boolean get() = lastTimerNotification != null

    /** The timer notification as last posted, or a plain one if none has been (a foreground service needs one) */
    fun timerNotification(): android.app.Notification = lastTimerNotification
        ?: NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focusing")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent(TIMER_ID, DeepLinks.FOCUS))
            .build()

    private fun timerAction(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, TimerActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openIntent(requestCode: Int, deepLink: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun formatClock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

    /** Gentle daily nudges on their own channel, so they can be muted without losing timer alerts */
    fun showReminder(id: Int, message: ReminderMessage, deepLink: String) {
        show(id, message.title, message.text, deepLink, REMINDER_CHANNEL_ID)
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun show(id: Int, title: String, text: String, deepLink: String, channelId: String = CHANNEL_ID) {
        if (!canPostNotifications()) return

        val pendingIntent = openIntent(id, deepLink)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(if (channelId == CHANNEL_ID) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post; nothing to do
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Focus timer", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Tells you when focus time and breaks end"
        }
        val reminders = NotificationChannel(REMINDER_CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Habits left in the evening and the morning mood check-in"
        }
        val timer = NotificationChannel(TIMER_CHANNEL_ID, "Live timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "The running focus or break timer, with pause and stop"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, reminders, timer))
    }

    companion object {
        const val CHANNEL_ID = "focus_timer"
        const val REMINDER_CHANNEL_ID = "reminders"
        const val TIMER_CHANNEL_ID = "live_timer"
        private const val TIMER_ID = 1003
        private const val FOCUS_COMPLETE_ID = 1001
        private const val BREAK_COMPLETE_ID = 1002
    }
}
