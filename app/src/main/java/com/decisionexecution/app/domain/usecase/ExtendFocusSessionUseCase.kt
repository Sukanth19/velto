package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.service.FocusSessionManager
import javax.inject.Inject

/**
 * Adds a few minutes to the running or paused focus phase, for when the task needs just a bit more.
 */
class ExtendFocusSessionUseCase @Inject constructor(
    private val focusSessionManager: FocusSessionManager
) {
    suspend operator fun invoke(minutes: Int = DEFAULT_MINUTES) {
        focusSessionManager.extendSession(minutes)
    }

    companion object {
        const val DEFAULT_MINUTES = 5
    }
}
