package com.decisionexecution.app.domain.model

sealed class FocusSessionState {
    object Idle : FocusSessionState()
    
    data class Focusing(
        val task: Task,
        val elapsedSeconds: Int,
        val totalSeconds: Int
    ) : FocusSessionState()
    
    data class Paused(
        val task: Task,
        val elapsedSeconds: Int,
        val totalSeconds: Int
    ) : FocusSessionState()
    
    data class Breaking(
        val elapsedSeconds: Int,
        val totalSeconds: Int
    ) : FocusSessionState()
}

/**
 * Persisted focus session. Elapsed time is derived from the clock so it survives process death:
 * elapsed = pausedAtSeconds + (now - startTimestamp) while running, or pausedAtSeconds while paused.
 *
 * @property startTimestamp When the current running stretch began; null while paused
 * @property pausedAtSeconds Seconds of the current phase already counted before [startTimestamp]
 * @property isOnBreak Whether the current phase is the break rather than focus time
 */
data class FocusSessionData(
    val taskId: String?,
    val taskTitle: String?,
    val startTimestamp: Long?,
    val focusDurationSeconds: Int?,
    val breakDurationSeconds: Int?,
    val isPaused: Boolean = false,
    val pausedAtSeconds: Int? = null,
    val isOnBreak: Boolean = false
)

sealed class JustStartResult {
    data class Success(val task: Task) : JustStartResult()
    object NoTasksAvailable : JustStartResult()
}
