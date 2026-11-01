package com.decisionexecution.app.testutil

import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.UserPreferences
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.FocusNotifier
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.service.FocusTimerScheduler
import com.decisionexecution.app.domain.service.TimeSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** Clock the test moves by hand. */
class FakeTimeSource(var now: Long = 1_700_000_000_000L) : TimeSource {
    override fun nowMillis(): Long = now
    fun advanceSeconds(seconds: Int) {
        now += seconds * 1000L
    }
}

/** Remembers the last scheduled check instead of talking to WorkManager. */
class RecordingScheduler : FocusTimerScheduler {
    var pendingDelayMillis: Long? = null
        private set
    var scheduleCount = 0
        private set

    override fun scheduleCheck(delayMillis: Long) {
        pendingDelayMillis = delayMillis
        scheduleCount++
    }

    override fun cancel() {
        pendingDelayMillis = null
    }
}

class RecordingNotifier : FocusNotifier {
    val focusCompleted = mutableListOf<String>()
    var breakCompletedCount = 0
        private set

    override fun onFocusComplete(taskTitle: String) {
        focusCompleted += taskTitle
    }

    override fun onBreakComplete() {
        breakCompletedCount++
    }
}

class InMemoryPreferencesRepository : PreferencesRepository {
    private var preferences = UserPreferences()
    private var focusSessionData: FocusSessionData? = null

    override fun getPreferences(): Flow<UserPreferences> = MutableStateFlow(preferences)

    override suspend fun savePreferences(preferences: UserPreferences) {
        this.preferences = preferences
    }

    override suspend fun updateFocusDuration(minutes: Int) {
        preferences = preferences.copy(focusDurationMinutes = minutes)
    }

    override suspend fun updateBreakDuration(minutes: Int) {
        preferences = preferences.copy(breakDurationMinutes = minutes)
    }

    override suspend fun updateDailyFocusGoal(minutes: Int) {
        preferences = preferences.copy(dailyFocusGoalMinutes = minutes)
    }

    override suspend fun updateDndDuringFocus(enabled: Boolean) {
        preferences = preferences.copy(dndDuringFocus = enabled)
    }

    override suspend fun updateHabitReminder(reminder: com.decisionexecution.app.domain.model.Reminder) {
        preferences = preferences.copy(habitReminder = reminder)
    }

    override suspend fun updateMoodReminder(reminder: com.decisionexecution.app.domain.model.Reminder) {
        preferences = preferences.copy(moodReminder = reminder)
    }

    override suspend fun updateWeeklyReviewReminder(reminder: com.decisionexecution.app.domain.model.Reminder) {
        preferences = preferences.copy(weeklyReviewReminder = reminder)
    }

    override suspend fun updateThemeMode(mode: com.decisionexecution.app.domain.model.ThemeMode) {
        preferences = preferences.copy(themeMode = mode)
    }

    override suspend fun updateThemePalette(palette: com.decisionexecution.app.domain.model.ThemePalette) {
        preferences = preferences.copy(themePalette = palette)
    }

    override suspend fun updatePureBlack(enabled: Boolean) {
        preferences = preferences.copy(pureBlack = enabled)
    }

    override suspend fun saveFocusSession(sessionData: FocusSessionData) {
        focusSessionData = sessionData
    }

    override suspend fun getFocusSession(): FocusSessionData? = focusSessionData

    override suspend fun clearFocusSession() {
        focusSessionData = null
    }
}

/** Task repository that knows exactly the given tasks, for session restore. */
fun taskRepositoryOf(vararg tasks: Task): TaskRepository {
    val repository = mockk<TaskRepository>(relaxed = true)
    every { repository.getTaskById(any()) } answers { flowOf(tasks.find { it.id == firstArg() }) }
    return repository
}

fun focusSessionManager(
    preferences: PreferencesRepository = InMemoryPreferencesRepository(),
    tasks: TaskRepository = taskRepositoryOf(),
    clock: TimeSource = FakeTimeSource(),
    scheduler: FocusTimerScheduler = RecordingScheduler(),
    notifier: FocusNotifier = RecordingNotifier(),
    performance: PerformanceRepository = mockk(relaxed = true)
) = FocusSessionManager(preferences, tasks, performance, scheduler, notifier, clock)

/** Insights use case with no activity, for screens that only need it to exist. */
fun emptyInsights(): com.decisionexecution.app.domain.usecase.GetInsightsUseCase {
    val useCase = mockk<com.decisionexecution.app.domain.usecase.GetInsightsUseCase>()
    every { useCase(any(), any()) } returns flowOf(
        com.decisionexecution.app.domain.usecase.Insights(week = emptyList(), streakDays = 0)
    )
    return useCase
}
