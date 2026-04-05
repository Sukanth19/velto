package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.service.FocusSessionManager
import javax.inject.Inject

/**
 * Use case for pausing an active focus session.
 * 
 * Pauses the current focus session, preserving the remaining time
 * so it can be resumed later from the same point.
 * 
 * Requirements: 4.7
 */
class PauseFocusSessionUseCase @Inject constructor(
    private val focusSessionManager: FocusSessionManager
) {
    /**
     * Pauses the active focus session.
     */
    suspend operator fun invoke() {
        focusSessionManager.pauseSession()
    }
}
