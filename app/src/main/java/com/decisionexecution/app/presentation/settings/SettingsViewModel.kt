package com.decisionexecution.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.UserPreferences
import com.decisionexecution.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val focusDuration: Int = 25,
    val breakDuration: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyFocusGoal: Int = 120,
    val dndDuringFocus: Boolean = false,
    val error: String? = null
)

val FOCUS_DURATION_RANGE = 5..90
val BREAK_DURATION_RANGE = 1..30
val DAILY_FOCUS_GOAL_RANGE = 15..480

fun isValidFocusDuration(minutes: Int): Boolean = minutes in FOCUS_DURATION_RANGE

fun isValidBreakDuration(minutes: Int): Boolean = minutes in BREAK_DURATION_RANGE

/**
 * ViewModel for the Settings screen. Rejects out-of-range durations instead of saving them.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.getPreferences().collect { prefs ->
                _uiState.update {
                    it.copy(
                        focusDuration = prefs.focusDurationMinutes,
                        breakDuration = prefs.breakDurationMinutes,
                        themeMode = prefs.themeMode,
                        dailyFocusGoal = prefs.dailyFocusGoalMinutes,
                        dndDuringFocus = prefs.dndDuringFocus
                    )
                }
            }
        }
    }

    fun onFocusDurationChanged(minutes: Int) {
        if (!isValidFocusDuration(minutes)) {
            _uiState.update {
                it.copy(error = "Focus duration must be ${FOCUS_DURATION_RANGE.first}-${FOCUS_DURATION_RANGE.last} minutes")
            }
            return
        }
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch { preferencesRepository.updateFocusDuration(minutes) }
    }

    fun onBreakDurationChanged(minutes: Int) {
        if (!isValidBreakDuration(minutes)) {
            _uiState.update {
                it.copy(error = "Break duration must be ${BREAK_DURATION_RANGE.first}-${BREAK_DURATION_RANGE.last} minutes")
            }
            return
        }
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch { preferencesRepository.updateBreakDuration(minutes) }
    }

    fun onDailyFocusGoalChanged(minutes: Int) {
        viewModelScope.launch { preferencesRepository.updateDailyFocusGoal(minutes.coerceIn(DAILY_FOCUS_GOAL_RANGE)) }
    }

    fun onDndDuringFocusChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateDndDuringFocus(enabled) }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch {
            val current = _uiState.value
            preferencesRepository.savePreferences(
                UserPreferences(
                    focusDurationMinutes = current.focusDuration,
                    breakDurationMinutes = current.breakDuration,
                    themeMode = mode,
                    dailyFocusGoalMinutes = current.dailyFocusGoal,
                    dndDuringFocus = current.dndDuringFocus
                )
            )
        }
    }
}
