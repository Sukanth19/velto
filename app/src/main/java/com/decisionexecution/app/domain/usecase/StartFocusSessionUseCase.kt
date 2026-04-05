package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.service.FocusSessionManager
import javax.inject.Inject

/**
 * Use case for starting a focus session.
 * 
 * Initiates a Pomodoro-based focus session for a specific task with
 * configurable focus and break durations.
 * 
 * Requirements: 4.6
 */
class StartFocusSessionUseCase @Inject constructor(
    private val focusSessionManager: FocusSessionManager
) {
    /**
     * Starts a focus session.
     * 
     * @param task The task to focus on
     * @param focusDuration Focus duration in minutes
     * @param breakDuration Break duration in minutes
     */
    suspend operator fun invoke(task: Task, focusDuration: Int, breakDuration: Int) {
        focusSessionManager.startSession(task, focusDuration, breakDuration)
    }
}
