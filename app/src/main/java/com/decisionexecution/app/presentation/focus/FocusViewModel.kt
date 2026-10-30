package com.decisionexecution.app.presentation.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.usecase.EndFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.ExtendFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.PauseFocusSessionUseCase
import com.decisionexecution.app.domain.usecase.ResumeFocusSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
 * ViewModel for the Focus screen: mirrors the session manager and exposes pause, resume, extend, end.
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusSessionManager: FocusSessionManager,
    private val pauseFocusSessionUseCase: PauseFocusSessionUseCase,
    private val resumeFocusSessionUseCase: ResumeFocusSessionUseCase,
    private val endFocusSessionUseCase: EndFocusSessionUseCase,
    private val extendFocusSessionUseCase: ExtendFocusSessionUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<FocusUiState> = focusSessionManager.sessionState
        .map { it.toUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            focusSessionManager.sessionState.value.toUiState()
        )

    /** Steps of the task being focused on, so the screen can show the next one */
    @OptIn(ExperimentalCoroutinesApi::class)
    val steps: StateFlow<List<Subtask>> = focusSessionManager.sessionState
        .map { state ->
            when (state) {
                is FocusSessionState.Focusing -> state.task.id
                is FocusSessionState.Paused -> state.task.id
                else -> null
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else taskRepository.getSubtasks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onStepDone(step: Subtask) {
        viewModelScope.launch { runCatching { taskRepository.setSubtaskDone(step.id, true) } }
    }

    init {
        // Refresh from the clock once a second while the Focus screen exists; the background
        // check handles phase ends when it doesn't
        viewModelScope.launch {
            while (isActive) {
                focusSessionManager.sync()
                delay(TICK_MILLIS)
            }
        }
    }

    fun onPause() {
        viewModelScope.launch { pauseFocusSessionUseCase() }
    }

    fun onResume() {
        viewModelScope.launch { resumeFocusSessionUseCase() }
    }

    fun onExtend() {
        viewModelScope.launch { extendFocusSessionUseCase() }
    }

    fun onEnd() {
        viewModelScope.launch { endFocusSessionUseCase() }
    }

    companion object {
        const val TICK_MILLIS = 1000L
    }
}
