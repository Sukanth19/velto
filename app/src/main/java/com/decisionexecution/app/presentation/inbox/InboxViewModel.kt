package com.decisionexecution.app.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitIcon
import com.decisionexecution.app.domain.model.InboxItem
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.InboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class InboxUiState(
    val items: List<InboxItem> = emptyList(),
    val draft: String = "",
    val isLoading: Boolean = true,
    /** Bumped when the inbox is emptied, for a little celebration */
    val inboxZeroKey: Int = 0,
    val message: String? = null
)

/**
 * Capture first, decide later: each thought becomes a task, a habit, or gets binned.
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            inboxRepository.getItems().collect { items ->
                _uiState.update { state ->
                    val emptied = !state.isLoading && state.items.isNotEmpty() && items.isEmpty()
                    state.copy(
                        items = items,
                        isLoading = false,
                        inboxZeroKey = if (emptied) state.inboxZeroKey + 1 else state.inboxZeroKey
                    )
                }
            }
        }
    }

    fun onDraftChange(value: String) = _uiState.update { it.copy(draft = value) }

    fun onCapture() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(draft = "") }
        viewModelScope.launch {
            inboxRepository.add(InboxItem(UUID.randomUUID().toString(), text, System.currentTimeMillis()))
        }
    }

    fun onDismiss(item: InboxItem) {
        viewModelScope.launch { inboxRepository.delete(item.id) }
    }

    fun onMakeHabit(item: InboxItem) {
        viewModelScope.launch {
            habitRepository.addHabit(Habit(UUID.randomUUID().toString(), item.text, HabitIcon.SPARK, System.currentTimeMillis()))
            inboxRepository.delete(item.id)
            _uiState.update { it.copy(message = "Added \"${item.text}\" to your habits") }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}
