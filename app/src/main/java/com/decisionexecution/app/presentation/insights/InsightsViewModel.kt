package com.decisionexecution.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.usecase.GetInsightsUseCase
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
    val error: String? = null
)

/**
 * Weekly focus time, completions and streak.
 * Requirements: 7.2, 7.3, 7.4
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    getInsightsUseCase: GetInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getInsightsUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.toUserMessage("load your insights")) } }
                .collect { insights -> _uiState.update { it.copy(insights = insights, error = null) } }
        }
    }
}
