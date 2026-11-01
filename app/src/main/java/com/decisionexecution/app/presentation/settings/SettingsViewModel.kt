package com.decisionexecution.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.ContentResolver
import android.net.Uri
import com.decisionexecution.app.domain.model.Reminder
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.ThemePalette
import com.decisionexecution.app.domain.repository.BackupRepository
import com.decisionexecution.app.domain.repository.InvalidBackupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val themePalette: ThemePalette = ThemePalette.WALLPAPER,
    val pureBlack: Boolean = false,
    val dailyFocusGoal: Int = 120,
    val dndDuringFocus: Boolean = false,
    val habitReminder: Reminder = UserPreferences().habitReminder,
    val moodReminder: Reminder = UserPreferences().moodReminder,
    val weeklyReviewReminder: Reminder = UserPreferences().weeklyReviewReminder,
    /** Result of the last export or import, shown once */
    val backupMessage: String? = null,
    val backupBusy: Boolean = false,
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
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository
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
                        themePalette = prefs.themePalette,
                        pureBlack = prefs.pureBlack,
                        dailyFocusGoal = prefs.dailyFocusGoalMinutes,
                        dndDuringFocus = prefs.dndDuringFocus,
                        habitReminder = prefs.habitReminder,
                        moodReminder = prefs.moodReminder,
                        weeklyReviewReminder = prefs.weeklyReviewReminder
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

    fun onHabitReminderChanged(reminder: Reminder) {
        viewModelScope.launch { preferencesRepository.updateHabitReminder(reminder) }
    }

    fun onMoodReminderChanged(reminder: Reminder) {
        viewModelScope.launch { preferencesRepository.updateMoodReminder(reminder) }
    }

    fun onWeeklyReviewReminderChanged(reminder: Reminder) {
        viewModelScope.launch { preferencesRepository.updateWeeklyReviewReminder(reminder) }
    }

    /** Writes the backup to a file the user picked with the system file picker */
    fun onExport(uri: Uri, resolver: ContentResolver) = runBackup("Couldn't save the backup") {
        val json = backupRepository.export()
        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json) } ?: error("Can't write there")
        }
        "Backup saved"
    }

    fun onImport(uri: Uri, resolver: ContentResolver) = runBackup("Couldn't restore that backup") {
        val json = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Can't read that file")
        }
        val summary = backupRepository.import(json)
        "Restored ${summary.tasks} tasks, ${summary.habits} habits and ${summary.moodEntries} journal entries"
    }

    fun onBackupMessageShown() = _uiState.update { it.copy(backupMessage = null) }

    private fun runBackup(failure: String, block: suspend () -> String) {
        _uiState.update { it.copy(backupBusy = true) }
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: InvalidBackupException) {
                e.message ?: failure
            } catch (e: Exception) {
                failure
            }
            _uiState.update { it.copy(backupBusy = false, backupMessage = message) }
        }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.updateThemeMode(mode) }
    }

    fun onThemePaletteChanged(palette: ThemePalette) {
        viewModelScope.launch { preferencesRepository.updateThemePalette(palette) }
    }

    fun onPureBlackChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updatePureBlack(enabled) }
    }
}
