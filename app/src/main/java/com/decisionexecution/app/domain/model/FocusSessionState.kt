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
