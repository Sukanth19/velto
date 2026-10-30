package com.decisionexecution.app.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.usecase.GetMoodInsightsUseCase
import com.decisionexecution.app.domain.usecase.MoodInsights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class JournalUiState(
    val insights: MoodInsights? = null,
    val today: MoodEntry? = null,
    /** Every check-in this calendar year, for the year-in-pixels grid */
    val year: Map<LocalDate, MoodEntry> = emptyMap(),
    val savedKey: Int = 0
)

/**
 * Mood and energy check-ins, and what they say about when you focus best.
 */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    getMoodInsightsUseCase: GetMoodInsightsUseCase
) : ViewModel() {

    private val todayDate = LocalDate.now()
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMoodInsightsUseCase(todayDate).collect { insights ->
                _uiState.update { it.copy(insights = insights, today = insights.entries.find { e -> e.day == todayDate }) }
            }
        }
        viewModelScope.launch {
            moodRepository.getEntries(todayDate.withDayOfYear(1)).collect { entries ->
                _uiState.update { it.copy(year = entries.associateBy { e -> e.day }) }
            }
        }
    }

    fun onSave(mood: Int, energy: Int, note: String?) {
        _uiState.update { it.copy(savedKey = it.savedKey + 1) }
        viewModelScope.launch { moodRepository.save(MoodEntry(todayDate, mood, energy, note)) }
    }
}
