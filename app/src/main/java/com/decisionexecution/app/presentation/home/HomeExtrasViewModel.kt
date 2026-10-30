package com.decisionexecution.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.InboxRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.service.LevelProgress
import com.decisionexecution.app.domain.service.PlantState
import com.decisionexecution.app.domain.usecase.GetPlantUseCase
import com.decisionexecution.app.domain.usecase.GetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeExtrasUiState(
    val level: LevelProgress? = null,
    val habitsDone: Int = 0,
    val habitsTotal: Int = 0,
    val todayMood: MoodEntry? = null,
    val moodLoaded: Boolean = false,
    val inboxCount: Int = 0,
    val plant: PlantState? = null
)

/**
 * Everything on Home beyond tasks: level, the plant, today's habits, the mood check-in and the inbox count.
 * Each source loads on its own so one failing doesn't blank the rest.
 */
@HiltViewModel
class HomeExtrasViewModel @Inject constructor(
    getProgressUseCase: GetProgressUseCase,
    habitRepository: HabitRepository,
    inboxRepository: InboxRepository,
    private val moodRepository: MoodRepository,
    getPlantUseCase: GetPlantUseCase
) : ViewModel() {

    private val today = LocalDate.now()
    private val _uiState = MutableStateFlow(HomeExtrasUiState())
    val uiState: StateFlow<HomeExtrasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getProgressUseCase().catch { }.collect { p -> _uiState.update { it.copy(level = p.level) } }
        }
        viewModelScope.launch {
            getPlantUseCase(today).catch { }.collect { plant -> _uiState.update { it.copy(plant = plant) } }
        }
        viewModelScope.launch {
            habitRepository.getHabits(today).catch { }.collect { habits ->
                _uiState.update { it.copy(habitsDone = habits.count { h -> h.isDoneOn(today) }, habitsTotal = habits.size) }
            }
        }
        viewModelScope.launch {
            inboxRepository.getItems().catch { }.collect { items -> _uiState.update { it.copy(inboxCount = items.size) } }
        }
        viewModelScope.launch {
            moodRepository.getEntry(today).catch { }.collect { entry ->
                _uiState.update { it.copy(todayMood = entry, moodLoaded = true) }
            }
        }
    }

    fun onSaveMood(mood: Int, energy: Int, note: String?) {
        viewModelScope.launch { moodRepository.save(MoodEntry(today, mood, energy, note)) }
    }
}
