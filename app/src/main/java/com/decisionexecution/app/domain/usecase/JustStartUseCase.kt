package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.JustStartResult
import com.decisionexecution.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for the "Just Start" flow.
 * 
 * Orchestrates the one-tap execution flow: gets the best task, loads user preferences,
 * and starts a focus session automatically without requiring additional user input.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */
class JustStartUseCase @Inject constructor(
    private val getBestTaskUseCase: GetBestTaskUseCase,
    private val startFocusSessionUseCase: StartFocusSessionUseCase,
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Executes the Just Start flow.
     * 
     * @return JustStartResult.Success with the selected task, or JustStartResult.NoTasksAvailable
     */
    suspend operator fun invoke(): JustStartResult {
        // Get the best task
        val bestTask = getBestTaskUseCase().first() 
            ?: return JustStartResult.NoTasksAvailable
        
        // Load user preferences for focus/break durations
        val preferences = preferencesRepository.getPreferences().first()
        
        // Start focus session with the best task
        startFocusSessionUseCase(
            task = bestTask,
            focusDuration = preferences.focusDurationMinutes,
            breakDuration = preferences.breakDurationMinutes
        )
        
        return JustStartResult.Success(bestTask)
    }
}
