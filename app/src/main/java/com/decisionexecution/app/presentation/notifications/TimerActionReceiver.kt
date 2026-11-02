package com.decisionexecution.app.presentation.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.decisionexecution.app.domain.service.FocusSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pause, resume and stop buttons on the live timer notification. The manager re-posts the
 * notification itself once the session changes.
 */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {

    @Inject lateinit var focusSessionManager: FocusSessionManager

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                // The process may have just started for this tap; load the persisted session first
                focusSessionManager.sync()
                when (intent.action) {
                    ACTION_PAUSE -> focusSessionManager.pauseSession()
                    ACTION_RESUME -> focusSessionManager.resumeSession()
                    ACTION_STOP -> focusSessionManager.endSession()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.decisionexecution.app.timer.PAUSE"
        const val ACTION_RESUME = "com.decisionexecution.app.timer.RESUME"
        const val ACTION_STOP = "com.decisionexecution.app.timer.STOP"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
