package com.decisionexecution.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.usecase.GetInsightsUseCase
import com.decisionexecution.app.domain.usecase.GetProgressUseCase
import com.decisionexecution.app.domain.usecase.GetPlantUseCase
import com.decisionexecution.app.domain.service.PlantState
import com.decisionexecution.app.domain.usecase.Progress
import com.decisionexecution.app.domain.usecase.Insights
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val insights: Insights? = null,
    val progress: Progress? = null,
    val plant: PlantState? = null,
    val error: String? = null
)

/**
 * Level, achievements, weekly focus time, completions and streak.
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    getInsightsUseCase: GetInsightsUseCase,
    getProgressUseCase: GetProgressUseCase,
    getPlantUseCase: GetPlantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getInsightsUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.toUserMessage("load your insights")) } }
                .collect { insights -> _uiState.update { it.copy(insights = insights, error = null) } }
        }
        viewModelScope.launch {
            // Progress is a bonus; if it fails the week still shows
            getProgressUseCase()
                .catch { }
                .collect { progress -> _uiState.update { it.copy(progress = progress) } }
        }
        viewModelScope.launch {
            getPlantUseCase().catch { }.collect { plant -> _uiState.update { it.copy(plant = plant) } }
        }
    }
}
