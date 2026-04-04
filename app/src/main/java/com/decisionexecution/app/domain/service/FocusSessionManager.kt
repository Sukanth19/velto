package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusSessionManager @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    private val _sessionState = MutableStateFlow<FocusSessionState>(FocusSessionState.Idle)
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()
    
    suspend fun startSession(task: Task, focusDuration: Int, breakDuration: Int) {
        _sessionState.value = FocusSessionState.Focusing(
            task = task,
            elapsedSeconds = 0,
            totalSeconds = focusDuration * 60
        )
        // TODO: Schedule WorkManager worker for background timing
    }
    
    suspend fun pauseSession() {
        val currentState = _sessionState.value
        if (currentState is FocusSessionState.Focusing) {
            _sessionState.value = FocusSessionState.Paused(
                task = currentState.task,
                elapsedSeconds = currentState.elapsedSeconds,
                totalSeconds = currentState.totalSeconds
            )
        }
    }
    
    suspend fun resumeSession() {
        val currentState = _sessionState.value
        if (currentState is FocusSessionState.Paused) {
            _sessionState.value = FocusSessionState.Focusing(
                task = currentState.task,
                elapsedSeconds = currentState.elapsedSeconds,
                totalSeconds = currentState.totalSeconds
            )
        }
    }
    
    suspend fun endSession() {
        _sessionState.value = FocusSessionState.Idle
        // TODO: Cancel WorkManager worker
    }
}
