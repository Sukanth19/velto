package com.decisionexecution.app.domain.service

import com.decisionexecution.app.domain.model.FocusSessionData
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the focus session state machine (Idle -> Focusing <-> Paused -> Breaking -> Idle).
 *
 * The persisted [FocusSessionData] is the source of truth and elapsed time is always computed
 * from the clock, so the timer stays accurate in the background and across process death.
 * [sync] brings the in-memory state up to date: the Focus screen calls it every second while
 * visible, and a scheduled background check calls it when a phase should end.
 *
 * Requirements: 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 13.1, 13.6
 */
@Singleton
class FocusSessionManager @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val taskRepository: TaskRepository,
    private val performanceRepository: PerformanceRepository,
    private val scheduler: FocusTimerScheduler,
    private val notifier: FocusNotifier,
    private val timeSource: TimeSource
) {
    private val _sessionState = MutableStateFlow<FocusSessionState>(FocusSessionState.Idle)
    val sessionState: StateFlow<FocusSessionState> = _sessionState.asStateFlow()

    // Ticks from the UI and background checks can overlap; transitions must happen once
    private val mutex = Mutex()
    private var session: FocusSessionData? = null
    private var task: Task? = null
    private var restored = false

    suspend fun startSession(task: Task, focusDuration: Int, breakDuration: Int) = mutex.withLock {
        val now = timeSource.nowMillis()
        restored = true
        this.task = task
        save(
            FocusSessionData(
                taskId = task.id,
                taskTitle = task.title,
                startTimestamp = now,
                focusDurationSeconds = focusDuration * 60,
                breakDurationSeconds = breakDuration * 60,
                isPaused = false,
                pausedAtSeconds = 0,
                isOnBreak = false
            )
        )
        scheduler.scheduleCheck(focusDuration * 60 * 1000L)
        publish(now)
    }

    suspend fun pauseSession() = mutex.withLock {
        val current = session ?: return@withLock
        if (current.isPaused || current.isOnBreak) return@withLock
        val now = timeSource.nowMillis()
        save(current.copy(isPaused = true, startTimestamp = null, pausedAtSeconds = elapsedSeconds(current, now)))
        scheduler.cancel()
        publish(now)
    }

    suspend fun resumeSession() = mutex.withLock {
        val current = session ?: return@withLock
        if (!current.isPaused) return@withLock
        val now = timeSource.nowMillis()
        val resumed = current.copy(isPaused = false, startTimestamp = now)
        save(resumed)
        scheduler.scheduleCheck(remainingMillis(resumed, now))
        publish(now)
    }

    /**
     * Adds time to the current focus phase, running or paused. Breaks can't be extended; skipping
     * them is the only control there.
     */
    suspend fun extendSession(minutes: Int) = mutex.withLock {
        val current = session ?: return@withLock
        if (current.isOnBreak || minutes <= 0) return@withLock
        val now = timeSource.nowMillis()
        val extended = current.copy(focusDurationSeconds = (current.focusDurationSeconds ?: 0) + minutes * 60)
        save(extended)
        if (!extended.isPaused) scheduler.scheduleCheck(remainingMillis(extended, now))
        publish(now)
    }

    suspend fun endSession() = mutex.withLock {
        clear()
        scheduler.cancel()
    }

    suspend fun transitionToBreak() = mutex.withLock {
        val current = session ?: return@withLock
        if (current.isOnBreak) return@withLock
        val now = timeSource.nowMillis()
        startBreak(current, now, now)
        publish(now)
    }

    suspend fun completeSession() = mutex.withLock {
        if (session != null) finish()
    }

    /**
     * Recomputes elapsed time from the clock, restoring a persisted session after process death
     * and catching up on any phases that ended while nothing was watching.
     */
    suspend fun sync() = mutex.withLock {
        val now = timeSource.nowMillis()
        if (!restored) restore(now)

        var current = session ?: return@withLock
        while (!current.isPaused) {
            val total = phaseTotalSeconds(current)
            if (elapsedSeconds(current, now) < total) break
            // When the phase actually ended, which may be well before now
            val endedAt = current.startTimestamp!! + (total - (current.pausedAtSeconds ?: 0)) * 1000L
            if (current.isOnBreak) {
                finish()
                return@withLock
            }
            startBreak(current, endedAt, now)
            current = session!!
        }
        publish(now)
    }

    private suspend fun restore(now: Long) {
        restored = true
        val stored = preferencesRepository.getFocusSession() ?: return
        val storedTask = stored.taskId?.let { taskRepository.getTaskById(it).first() }
        if (storedTask == null || stored.focusDurationSeconds == null || stored.breakDurationSeconds == null) {
            // Task was deleted or data is incomplete; nothing sensible to resume
            preferencesRepository.clearFocusSession()
            return
        }
        task = storedTask
        session = stored
        // The pending background check may have been lost with the process
        if (!stored.isPaused) scheduler.scheduleCheck(remainingMillis(stored, now))
    }

    private suspend fun startBreak(current: FocusSessionData, breakStartedAt: Long, now: Long) {
        val onBreak = current.copy(
            isOnBreak = true,
            isPaused = false,
            startTimestamp = breakStartedAt,
            pausedAtSeconds = 0
        )
        save(onBreak)
        current.taskId?.let { taskId ->
            performanceRepository.recordFocusSession(taskId, breakStartedAt, (current.focusDurationSeconds ?: 0) / 60)
        }
        notifier.onFocusComplete(task?.title ?: current.taskTitle.orEmpty())
        scheduler.scheduleCheck(remainingMillis(onBreak, now))
    }

    private suspend fun finish() {
        clear()
        scheduler.cancel()
        notifier.onBreakComplete()
    }

    private suspend fun clear() {
        session = null
        task = null
        restored = true
        preferencesRepository.clearFocusSession()
        _sessionState.value = FocusSessionState.Idle
    }

    private suspend fun save(data: FocusSessionData) {
        session = data
        preferencesRepository.saveFocusSession(data)
    }

    private fun publish(now: Long) {
        val current = session
        val currentTask = task
        if (current == null || currentTask == null) {
            _sessionState.value = FocusSessionState.Idle
            return
        }
        val total = phaseTotalSeconds(current)
        val elapsed = elapsedSeconds(current, now).coerceAtMost(total)
        _sessionState.value = when {
            current.isOnBreak -> FocusSessionState.Breaking(elapsed, total)
            current.isPaused -> FocusSessionState.Paused(currentTask, elapsed, total)
            else -> FocusSessionState.Focusing(currentTask, elapsed, total)
        }
    }

    private fun phaseTotalSeconds(data: FocusSessionData): Int =
        (if (data.isOnBreak) data.breakDurationSeconds else data.focusDurationSeconds) ?: 0

    private fun elapsedSeconds(data: FocusSessionData, now: Long): Int {
        val banked = data.pausedAtSeconds ?: 0
        val start = data.startTimestamp
        return if (data.isPaused || start == null) banked else banked + ((now - start) / 1000).toInt()
    }

    private fun remainingMillis(data: FocusSessionData, now: Long): Long {
        val banked = (data.pausedAtSeconds ?: 0) * 1000L
        val running = data.startTimestamp?.takeUnless { data.isPaused }?.let { now - it } ?: 0L
        return (phaseTotalSeconds(data) * 1000L - banked - running).coerceAtLeast(0L)
    }
}
