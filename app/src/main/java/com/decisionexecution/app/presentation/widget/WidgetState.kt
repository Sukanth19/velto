package com.decisionexecution.app.presentation.widget

import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.presentation.components.label

/**
 * What the best-task widget shows. Kept free of Glance so it can be unit tested.
 * Requirements: 8.1, 8.3
 */
data class BestTaskWidgetState(
    val taskId: String?,
    val title: String,
    val details: String?
)

fun bestTaskWidgetState(task: Task?): BestTaskWidgetState =
    if (task == null) {
        BestTaskWidgetState(taskId = null, title = "No tasks available", details = null)
    } else {
        BestTaskWidgetState(
            taskId = task.id,
            title = task.title,
            details = "${task.category.label} · ${task.effort.label}"
        )
    }

/**
 * What the focus timer widget shows. The countdown itself is drawn by a system Chronometer,
 * so the widget only needs refreshing when the phase changes, not every minute.
 * Requirements: 8.2, 8.5
 */
sealed class TimerWidgetState {
    object Idle : TimerWidgetState()

    data class Active(
        val label: String,
        val title: String,
        val remainingMillis: Long,
        val isRunning: Boolean
    ) : TimerWidgetState()
}

fun timerWidgetState(state: FocusSessionState): TimerWidgetState = when (state) {
    is FocusSessionState.Idle -> TimerWidgetState.Idle
    is FocusSessionState.Focusing -> TimerWidgetState.Active(
        label = "Focus",
        title = state.task.title,
        remainingMillis = remainingMillis(state.totalSeconds, state.elapsedSeconds),
        isRunning = true
    )
    is FocusSessionState.Paused -> TimerWidgetState.Active(
        label = "Paused",
        title = state.task.title,
        remainingMillis = remainingMillis(state.totalSeconds, state.elapsedSeconds),
        isRunning = false
    )
    is FocusSessionState.Breaking -> TimerWidgetState.Active(
        label = "Break",
        title = "Step away for a moment",
        remainingMillis = remainingMillis(state.totalSeconds, state.elapsedSeconds),
        isRunning = true
    )
}

/**
 * Changes only when the widget needs redrawing: phase, pause state or task, not every tick.
 */
fun timerWidgetKey(state: FocusSessionState): Any = when (state) {
    is FocusSessionState.Idle -> "idle"
    is FocusSessionState.Focusing -> Triple("focus", state.task.id, state.totalSeconds)
    is FocusSessionState.Paused -> Triple("paused", state.task.id, state.elapsedSeconds)
    is FocusSessionState.Breaking -> Pair("break", state.totalSeconds)
}

private fun remainingMillis(totalSeconds: Int, elapsedSeconds: Int): Long =
    (totalSeconds - elapsedSeconds).coerceAtLeast(0) * 1000L
