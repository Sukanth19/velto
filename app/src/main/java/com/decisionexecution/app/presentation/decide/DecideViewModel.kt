package com.decisionexecution.app.presentation.decide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.service.PickContext
import com.decisionexecution.app.domain.usecase.PickTaskUseCase
import com.decisionexecution.app.domain.usecase.StartTimeboxedFocusUseCase
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecideUiState(
    val minutes: Int = DecideViewModel.DEFAULT_MINUTES,
    /** Energy picked on this screen; null means "use today's check-in" */
    val energyOverride: Int? = null,
    val context: PickContext? = null,
    val ranked: List<Task> = emptyList(),
    val pickIndex: Int = 0,
    /** Bumped for every spin so the reel animates again even when it lands on the same task */
    val spinKey: Int = 0,
    val isLoading: Boolean = true,
    val started: Boolean = false,
    val error: String? = null
) {
    val pick: Task? get() = ranked.getOrNull(pickIndex)
}

/**
 * "Can't decide": given a time box and energy, spin through the best fits and land on one.
 */
@HiltViewModel
class DecideViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pickTaskUseCase: PickTaskUseCase,
    private val startTimeboxedFocusUseCase: StartTimeboxedFocusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DecideUiState(minutes = savedStateHandle.get<Int>(MINUTES_ARG)?.takeIf { it > 0 } ?: DEFAULT_MINUTES)
    )
    val uiState: StateFlow<DecideUiState> = _uiState.asStateFlow()

    init {
        repick()
    }

    fun onMinutesChange(minutes: Int) {
        _uiState.update { it.copy(minutes = minutes) }
        repick()
    }

    fun onEnergyChange(energy: Int?) {
        _uiState.update { it.copy(energyOverride = energy) }
        repick()
    }

    /** Lands on the next-best fit, wrapping round */
    fun onSpinAgain() {
        // Passing over a pick counts as dodging it, so a task skipped again and again gets offered a shrink
        _uiState.value.pick?.let { passed -> viewModelScope.launch { runCatching { pickTaskUseCase.onPassedOver(passed) } } }
        _uiState.update {
            if (it.ranked.isEmpty()) it
            else it.copy(pickIndex = (it.pickIndex + 1) % it.ranked.size, spinKey = it.spinKey + 1)
        }
    }

    fun onStart() {
        val state = _uiState.value
        val task = state.pick ?: return
        viewModelScope.launch {
            try {
                startTimeboxedFocusUseCase(task, state.minutes)
                _uiState.update { it.copy(started = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("start a focus session")) }
            }
        }
    }

    fun onStartedHandled() = _uiState.update { it.copy(started = false) }

    fun reason(): String? {
        val state = _uiState.value
        val task = state.pick ?: return null
        return state.context?.let { pickTaskUseCase.reason(task, it) }
    }

    private fun repick() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val picks = pickTaskUseCase(state.minutes, state.energyOverride)
                _uiState.update {
                    it.copy(ranked = picks.ranked, context = picks.context, pickIndex = 0, spinKey = it.spinKey + 1, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("load your tasks")) }
            }
        }
    }

    companion object {
        const val MINUTES_ARG = "minutes"
        const val DEFAULT_MINUTES = 25
        val TIME_OPTIONS = listOf(15, 25, 45, 60)
    }
}
