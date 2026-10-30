package com.decisionexecution.app.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.usecase.GetInsightsUseCase
import com.decisionexecution.app.domain.usecase.GetWeeklyReviewUseCase
import com.decisionexecution.app.domain.usecase.WeeklyReview
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val review: WeeklyReview? = null,
    val streakDays: Int = 0,
    val error: String? = null
)

/**
 * The week in review: totals against last week, wins, focus hours and one suggestion.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    getWeeklyReviewUseCase: GetWeeklyReviewUseCase,
    getInsightsUseCase: GetInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(getWeeklyReviewUseCase(), getInsightsUseCase()) { review, insights -> review to insights.streakDays }
                .catch { e -> _uiState.update { it.copy(error = e.toUserMessage("load your week")) } }
                .collect { (review, streak) -> _uiState.update { it.copy(review = review, streakDays = streak, error = null) } }
        }
    }
}
