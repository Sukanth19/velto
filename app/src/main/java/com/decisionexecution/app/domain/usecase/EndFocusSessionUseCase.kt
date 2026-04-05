package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.service.FocusSessionManager
import javax.inject.Inject

/**
 * Use case for ending a focus session early.
 * 
 * Terminates the current focus session before completion,
 * transitioning back to idle state.
 * 
 * Requirements: 4.8
 */
class EndFocusSessionUseCase @Inject constructor(
    private val focusSessionManager: FocusSessionManager
) {
    /**
     * Ends the active focus session.
     */
    suspend operator fun invoke() {
        focusSessionManager.endSession()
    }
}
