package com.decisionexecution.app.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.EndFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.PauseFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.ResumeFocusSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the Focus screen renders. Focus and break are separate types so the UI can't confuse them.
 */
sealed class FocusUiState {
    object Idle : FocusUiState()

    data class Focus(
        val taskTitle: String,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val isPaused: Boolean
    ) : FocusUiState()

    data class Break(
        val remainingSeconds: Int,
        val totalSeconds: Int
    ) : FocusUiState()
}

fun FocusSessionState.toUiState(): FocusUiState = when (this) {
    is FocusSessionState.Idle -> FocusUiState.Idle
    is FocusSessionState.Focusing -> FocusUiState.Focus(
        taskTitle = task.title,
        remainingSeconds = (totalSeconds - elapsedSeconds).coerceAtLeast(0),
        totalSeconds = totalSeconds,
        isPaused = false
    )
    is FocusSessionState.Paused -> FocusUiState.Focus(
        taskTitle = task.title,
        remainingSeconds = (totalSeconds - elapsedSeconds).coerceAtLeast(0),
        totalSeconds = totalSeconds,
        isPaused = true
    )
    is FocusSessionState.Breaking -> FocusUiState.Break(
        remainingSeconds = (totalSeconds - elapsedSeconds).coerceAtLeast(0),
        totalSeconds = totalSeconds
    )
}

/**
 * ViewModel for the Focus screen: mirrors the session manager and exposes pause, resume, end.
 *
 * Requirements: 4.6, 4.7, 4.8, 5.1, 5.2, 5.3, 5.4, 5.6
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    focusSessionManager: FocusSessionManager,
    private val pauseFocusSessionUseCase: PauseFocusSessionUseCase,
    private val resumeFocusSessionUseCase: ResumeFocusSessionUseCase,
    private val endFocusSessionUseCase: EndFocusSessionUseCase
) : ViewModel() {

    val uiState: StateFlow<FocusUiState> = focusSessionManager.sessionState
        .map { it.toUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            focusSessionManager.sessionState.value.toUiState()
        )

    fun onPause() {
        viewModelScope.launch { pauseFocusSessionUseCase() }
    }

    fun onResume() {
        viewModelScope.launch { resumeFocusSessionUseCase() }
    }

    fun onEnd() {
        viewModelScope.launch { endFocusSessionUseCase() }
    }
}
