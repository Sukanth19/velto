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

data class FocusSessionData(
    val taskId: String?,
    val taskTitle: String?,
    val startTimestamp: Long?,
    val focusDurationSeconds: Int?,
    val breakDurationSeconds: Int?,
    val isPaused: Boolean = false,
    val pausedAtSeconds: Int? = null
)

sealed class JustStartResult {
    data class Success(val task: Task) : JustStartResult()
    object NoTasksAvailable : JustStartResult()
}
