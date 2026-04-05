package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.FocusSessionData
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
    
    private var breakDurationSeconds: Int = 0
    
    suspend fun startSession(task: Task, focusDuration: Int, breakDuration: Int) {
        val focusDurationSeconds = focusDuration * 60
        breakDurationSeconds = breakDuration * 60
        
        // Save session state to DataStore
        preferencesRepository.saveFocusSession(
            FocusSessionData(
                taskId = task.id,
                taskTitle = task.title,
                startTimestamp = System.currentTimeMillis(),
                focusDurationSeconds = focusDurationSeconds,
                breakDurationSeconds = breakDurationSeconds,
                isPaused = false,
                pausedAtSeconds = null
            )
        )
        
        _sessionState.value = FocusSessionState.Focusing(
            task = task,
            elapsedSeconds = 0,
            totalSeconds = focusDurationSeconds
        )
        // TODO: Schedule WorkManager worker for background timing
    }
    
    suspend fun pauseSession() {
        val currentState = _sessionState.value
        if (currentState is FocusSessionState.Focusing) {
            // Save paused state to DataStore
            preferencesRepository.saveFocusSession(
                FocusSessionData(
                    taskId = currentState.task.id,
                    taskTitle = currentState.task.title,
                    startTimestamp = System.currentTimeMillis(),
                    focusDurationSeconds = currentState.totalSeconds,
                    breakDurationSeconds = breakDurationSeconds,
                    isPaused = true,
                    pausedAtSeconds = currentState.elapsedSeconds
                )
            )
            
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
            // Update DataStore to reflect resumed state
            preferencesRepository.saveFocusSession(
                FocusSessionData(
                    taskId = currentState.task.id,
                    taskTitle = currentState.task.title,
                    startTimestamp = System.currentTimeMillis(),
                    focusDurationSeconds = currentState.totalSeconds,
                    breakDurationSeconds = breakDurationSeconds,
                    isPaused = false,
                    pausedAtSeconds = null
                )
            )
            
            _sessionState.value = FocusSessionState.Focusing(
                task = currentState.task,
                elapsedSeconds = currentState.elapsedSeconds,
                totalSeconds = currentState.totalSeconds
            )
        }
    }
    
    suspend fun endSession() {
        // Clear session data from DataStore
        preferencesRepository.saveFocusSession(
            FocusSessionData(
                taskId = null,
                taskTitle = null,
                startTimestamp = null,
                focusDurationSeconds = null,
                breakDurationSeconds = null,
                isPaused = false,
                pausedAtSeconds = null
            )
        )
        
        _sessionState.value = FocusSessionState.Idle
        // TODO: Cancel WorkManager worker
    }
    
    suspend fun transitionToBreak() {
        val currentState = _sessionState.value
        if (currentState is FocusSessionState.Focusing) {
            _sessionState.value = FocusSessionState.Breaking(
                elapsedSeconds = 0,
                totalSeconds = breakDurationSeconds
            )
        }
    }
    
    suspend fun completeSession() {
        _sessionState.value = FocusSessionState.Idle
        
        // Clear session data from DataStore
        preferencesRepository.saveFocusSession(
            FocusSessionData(
                taskId = null,
                taskTitle = null,
                startTimestamp = null,
                focusDurationSeconds = null,
                breakDurationSeconds = null,
                isPaused = false,
                pausedAtSeconds = null
            )
        )
        // TODO: Record completion in PerformanceRepository
    }
}
