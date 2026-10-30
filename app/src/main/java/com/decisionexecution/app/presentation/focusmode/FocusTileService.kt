package com.decisionexecution.app.presentation.focusmode

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.decisionexecution.app.MainActivity
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.JustStartResult
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.JustStartUseCase
import com.decisionexecution.app.presentation.navigation.DeepLinks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile: tap to Just Start, pause, resume, or skip a break, without opening the app.
 */
@AndroidEntryPoint
class FocusTileService : TileService() {

    @Inject lateinit var focusSessionManager: FocusSessionManager
    @Inject lateinit var justStartUseCase: JustStartUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            runCatching { focusSessionManager.sync() }
            render(focusSessionManager.sessionState.value)
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            runCatching { focusSessionManager.sync() }
            when (focusSessionManager.sessionState.value) {
                is FocusSessionState.Idle -> {
                    // With nothing to start, open the app so the user can add a task
                    if (runCatching { justStartUseCase() }.getOrNull() !is JustStartResult.Success) openApp(DeepLinks.HOME)
                }
                is FocusSessionState.Focusing -> focusSessionManager.pauseSession()
                is FocusSessionState.Paused -> focusSessionManager.resumeSession()
                is FocusSessionState.Breaking -> focusSessionManager.endSession()
            }
            render(focusSessionManager.sessionState.value)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(state: FocusSessionState) {
        val tile = qsTile ?: return
        tile.state = when (state) {
            is FocusSessionState.Focusing, is FocusSessionState.Breaking -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = when (state) {
            is FocusSessionState.Idle -> "Just Start"
            is FocusSessionState.Focusing -> "Focusing"
            is FocusSessionState.Paused -> "Paused"
            is FocusSessionState.Breaking -> "Break"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (state) {
                is FocusSessionState.Idle -> "Tap to focus"
                is FocusSessionState.Focusing -> "${minutesLeft(state.totalSeconds, state.elapsedSeconds)} min left"
                is FocusSessionState.Paused -> "Tap to resume"
                is FocusSessionState.Breaking -> "Tap to skip"
            }
        }
        tile.updateTile()
    }

    private fun openApp(link: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link), this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
