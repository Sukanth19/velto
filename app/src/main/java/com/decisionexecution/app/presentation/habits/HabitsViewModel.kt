package com.decisionexecution.app.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitIcon
import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.presentation.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class HabitRow(
    val habit: Habit,
    val doneToday: Boolean,
    val streak: Int,
    /** The last 7 days, oldest first, ending today */
    val week: List<Boolean>
)

data class HabitsUiState(
    val habits: List<HabitRow> = emptyList(),
    /** Share of habits done per day over the heatmap window, oldest first, 0 to 1 */
    val heatmap: List<Float> = emptyList(),
    val isLoading: Boolean = true,
    /** Bumped on every check-in so the screen fires confetti */
    val celebrateKey: Int = 0,
    val error: String? = null
) {
    val doneToday: Int get() = habits.count { it.doneToday }
}

fun habitRows(habits: List<HabitWithHistory>, today: LocalDate): List<HabitRow> = habits.map { h ->
    HabitRow(
        habit = h.habit,
        doneToday = h.isDoneOn(today),
        streak = h.streak(today),
        week = (6 downTo 0).map { h.isDoneOn(today.minusDays(it.toLong())) }
    )
}

fun habitHeatmap(habits: List<HabitWithHistory>, today: LocalDate, days: Int): List<Float> {
    if (habits.isEmpty()) return List(days) { 0f }
    return (days - 1 downTo 0).map { back ->
        val day = today.minusDays(back.toLong())
        habits.count { it.isDoneOn(day) }.toFloat() / habits.size
    }
}

/**
 * Daily habits: check off today, see streaks, the week strip and a 12-week heatmap.
 */
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // A long look back so streaks longer than the heatmap still count
            habitRepository.getHabits(today.minusDays(HISTORY_DAYS))
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("load your habits")) } }
                .collect { habits ->
                    _uiState.update {
                        it.copy(
                            habits = habitRows(habits, today),
                            heatmap = habitHeatmap(habits, today, HEATMAP_DAYS),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onToggle(habitId: String, done: Boolean) {
        if (done) _uiState.update { it.copy(celebrateKey = it.celebrateKey + 1) }
        viewModelScope.launch {
            runCatching { habitRepository.setDone(habitId, today, done) }
                .onFailure { e -> _uiState.update { it.copy(error = e.toUserMessage("update the habit")) } }
        }
    }

    fun onAdd(name: String, icon: HabitIcon) {
        if (name.isBlank()) return
        viewModelScope.launch {
            habitRepository.addHabit(Habit(UUID.randomUUID().toString(), name.trim(), icon, System.currentTimeMillis()))
        }
    }

    fun onDelete(habitId: String) {
        viewModelScope.launch { habitRepository.deleteHabit(habitId) }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    companion object {
        const val HEATMAP_DAYS = 12 * 7
        const val HISTORY_DAYS = 400L
    }
}
