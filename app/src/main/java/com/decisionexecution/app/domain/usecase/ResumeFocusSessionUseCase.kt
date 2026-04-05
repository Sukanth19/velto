package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.service.FocusSessionManager
import javax.inject.Inject

/**
 * Use case for resuming a paused focus session.
 * 
 * Resumes a previously paused focus session, continuing from
 * the point where it was paused.
 * 
 * Requirements: 4.7
 */
class ResumeFocusSessionUseCase @Inject constructor(
    private val focusSessionManager: FocusSessionManager
) {
    /**
     * Resumes the paused focus session.
     */
    suspend operator fun invoke() {
        focusSessionManager.resumeSession()
    }
}
