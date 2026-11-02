package com.decisionexecution.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.JustStartResult
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.usecase.GetBestTaskUseCase
import com.decisionexecution.app.domain.usecase.GetInsightsUseCase
import com.decisionexecution.app.domain.usecase.GetProductivityMetricsUseCase
import com.decisionexecution.app.domain.usecase.JustStartUseCase
import com.decisionexecution.app.domain.usecase.GetEstimateCalibrationUseCase
import com.decisionexecution.app.domain.usecase.ShrinkTaskUseCase
import com.decisionexecution.app.domain.usecase.SnoozeTaskUseCase
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val bestTask: Task? = null,
    /** Why this task suits the time of day, when that's what tipped it */
    val pickReason: String? = null,
    /** How long the best task will likely take, corrected by the user's estimate history */
    val likelyMinutes: Int? = null,
    val completedTasksToday: Int = 0,
    val totalFocusTimeToday: Int = 0,
    val consistencyScore: Double = 0.0,
    val streakDays: Int = 0,
    val dailyFocusGoal: Int = 120,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** The error came from loading, so the UI can offer Retry */
    val canRetry: Boolean = false,
    val focusSessionStarted: Boolean = false
)

/**
 * ViewModel for the Home screen: next best task, today's stats, and the Just Start action.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBestTaskUseCase: GetBestTaskUseCase,
    private val justStartUseCase: JustStartUseCase,
    private val getProductivityMetricsUseCase: GetProductivityMetricsUseCase,
    private val getInsightsUseCase: GetInsightsUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val snoozeTaskUseCase: SnoozeTaskUseCase,
    private val shrinkTaskUseCase: ShrinkTaskUseCase,
    private val getEstimateCalibrationUseCase: GetEstimateCalibrationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private var loadJob: Job? = null

    private fun loadHomeData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, canRetry = false) }

            val zone = ZoneId.systemDefault()
            val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            combine(
                combine(getBestTaskUseCase.pick(), getEstimateCalibrationUseCase()) { pick, calibration -> pick to calibration },
                getProductivityMetricsUseCase(startOfToday, endOfToday, CONSISTENCY_DAYS),
                getInsightsUseCase(),
                preferencesRepository.getPreferences()
            ) { (pick, calibration), metrics, insights, prefs ->
                val estimate = pick?.task?.estimateMinutes
                _uiState.value.copy(
                    bestTask = pick?.task,
                    pickReason = pick?.reason,
                    likelyMinutes = estimate?.let { calibration?.likelyMinutes(it) ?: it },
                    completedTasksToday = metrics.completedTasks,
                    totalFocusTimeToday = metrics.totalFocusTime,
                    consistencyScore = metrics.consistencyScore,
                    streakDays = insights.streakDays,
                    dailyFocusGoal = prefs.dailyFocusGoalMinutes,
                    isLoading = false
                )
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("load today's data"), canRetry = true) }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onJustStartClicked() {
        viewModelScope.launch {
            try {
                when (justStartUseCase()) {
                    is JustStartResult.Success -> {
                        _uiState.update { it.copy(focusSessionStarted = true) }
                    }
                    is JustStartResult.NoTasksAvailable -> {
                        _uiState.update { it.copy(error = NO_TASKS_ERROR) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("start a focus session")) }
            }
        }
    }

    /** Not now: tomorrow's problem. Home moves on to the next best task. */
    fun onNotNow() {
        val task = _uiState.value.bestTask ?: return
        viewModelScope.launch {
            runCatching { snoozeTaskUseCase(task) }
                .onFailure { e -> _uiState.update { it.copy(error = e.toUserMessage("snooze the task")) } }
        }
    }

    /** Shrink it: add a tiny first step and start a two-minute starter on it */
    fun onShrink(firstStep: String?) {
        val task = _uiState.value.bestTask ?: return
        viewModelScope.launch {
            try {
                shrinkTaskUseCase(task, firstStep)
                _uiState.update { it.copy(focusSessionStarted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage("start a focus session")) }
            }
        }
    }

    fun onNavigatedToFocus() {
        _uiState.update { it.copy(focusSessionStarted = false) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null, canRetry = false) }
    }

    fun onRetry() = loadHomeData()

    companion object {
        const val CONSISTENCY_DAYS = 7
        const val NO_TASKS_ERROR = "No tasks available. Create a task first."
    }
}
