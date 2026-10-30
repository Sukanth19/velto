package com.decisionexecution.app.presentation.focusmode

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.FocusSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Side effects of the focus timer outside the app: Do Not Disturb while focusing (if the user
 * opted in and granted access) and keeping the Quick Settings tile current.
 */
@Singleton
class FocusAutomation @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusSessionManager: FocusSessionManager,
    private val preferencesRepository: PreferencesRepository
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    // Survives process death, so DND we turned on is always turned back off
    private val state = context.getSharedPreferences("focus_automation", Context.MODE_PRIVATE)

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                focusSessionManager.sessionState.map { it is FocusSessionState.Focusing }.distinctUntilChanged(),
                preferencesRepository.getPreferences().map { it.dndDuringFocus }.distinctUntilChanged()
            ) { focusing, enabled -> focusing && enabled }
                .distinctUntilChanged()
                .collect { wantQuiet -> runCatching { applyDnd(wantQuiet) } }
        }
        scope.launch {
            focusSessionManager.sessionState
                .map { tileKey(it) }
                .distinctUntilChanged()
                .collect { runCatching { TileService.requestListeningState(context, ComponentName(context, FocusTileService::class.java)) } }
        }
    }

    fun hasDndAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    private fun applyDnd(wantQuiet: Boolean) {
        if (!hasDndAccess()) return
        val weTurnedItOn = state.getBoolean(KEY_WE_ENABLED, false)
        if (wantQuiet) {
            // Leave a DND the user set themselves alone, and don't claim it as ours
            if (notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                state.edit().putBoolean(KEY_WE_ENABLED, true).apply()
            }
        } else if (weTurnedItOn) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            state.edit().putBoolean(KEY_WE_ENABLED, false).apply()
        }
    }

    private companion object {
        const val KEY_WE_ENABLED = "dnd_enabled_by_velto"
    }
}

/** Changes whenever the tile should look different: phase, pause, task, or roughly each minute */
internal fun tileKey(state: FocusSessionState): Any = when (state) {
    is FocusSessionState.Idle -> "idle"
    is FocusSessionState.Focusing -> listOf("focus", state.task.id, minutesLeft(state.totalSeconds, state.elapsedSeconds))
    is FocusSessionState.Paused -> listOf("paused", state.task.id)
    is FocusSessionState.Breaking -> listOf("break", minutesLeft(state.totalSeconds, state.elapsedSeconds))
}

/** Whole minutes left, rounded up, so the tile reads "1 min left" until the very end */
internal fun minutesLeft(totalSeconds: Int, elapsedSeconds: Int): Int = ((totalSeconds - elapsedSeconds).coerceAtLeast(0) + 59) / 60
