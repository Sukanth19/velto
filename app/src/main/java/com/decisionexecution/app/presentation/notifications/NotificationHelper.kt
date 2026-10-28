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
import com.decisionexecution.app.presentation.navigation.DeepLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Focus and break completion notifications. Silently skips posting when the user denied the
 * permission; the timer keeps running either way.
 *
 * Requirements: 13.2, 13.3, 13.4, 13.5
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

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun show(id: Int, title: String, text: String, deepLink: String) {
        if (!canPostNotifications()) return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "focus_timer"
        private const val FOCUS_COMPLETE_ID = 1001
        private const val BREAK_COMPLETE_ID = 1002
    }
}
