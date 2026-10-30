package com.decisionexecution.app.presentation.widget

import android.content.Context
import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.components.quadrant
import com.decisionexecution.app.presentation.home.formatMinutes
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * What the best-task widget shows. Kept free of Glance so it can be tested without a launcher.
 * Requirements: 8.1, 8.3
 */
data class BestTaskWidgetState(
    val taskId: String?,
    val title: String,
    val details: String?
)

fun bestTaskWidgetState(task: Task?): BestTaskWidgetState =
    if (task == null) {
        BestTaskWidgetState(taskId = null, title = "No tasks available", details = null)
    } else {
        BestTaskWidgetState(
            taskId = task.id,
            title = task.title,
            details = "${task.category.label} · ${task.effort.label}"
        )
    }

/**
 * Last best-task state the widget showed, so a slow or failing load doesn't blank it.
 * Requirements: 8.4
 */
class BestTaskWidgetCache(context: Context) {
    private val prefs = context.getSharedPreferences("best_task_widget_cache", Context.MODE_PRIVATE)

    fun save(state: BestTaskWidgetState) {
        prefs.edit()
            .putString(KEY_TASK_ID, state.taskId)
            .putString(KEY_TITLE, state.title)
            .putString(KEY_DETAILS, state.details)
            .apply()
    }

    fun load(): BestTaskWidgetState? {
        val title = prefs.getString(KEY_TITLE, null) ?: return null
        return BestTaskWidgetState(prefs.getString(KEY_TASK_ID, null), title, prefs.getString(KEY_DETAILS, null))
    }

    private companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
        const val KEY_DETAILS = "details"
    }
}

/**
 * Loads fresh state, falling back to the cached state on failure or timeout, then to the empty state.
 */
suspend fun loadBestTaskWidgetState(
    cache: BestTaskWidgetCache,
    timeoutMillis: Long,
    load: suspend () -> BestTaskWidgetState
): BestTaskWidgetState {
    val fresh = runCatching { withTimeoutOrNull(timeoutMillis) { load() } }.getOrNull()
    if (fresh != null) {
        cache.save(fresh)
        return fresh
    }
    return cache.load() ?: bestTaskWidgetState(null)
}

/**
 * What the focus timer widget shows. The countdown itself is drawn by a system Chronometer,
 * so the widget only needs refreshing when the phase changes, not every minute.
 * Requirements: 8.2, 8.5
 */
sealed class TimerWidgetState {
    object Idle : TimerWidgetState()

    data class Active(
        val label: String,
        val title: String,
        val remainingMillis: Long,
        val isRunning: Boolean,
        /** Breaks only offer skip; pause and extend are for focus time */
        val isBreak: Boolean = false
    ) : TimerWidgetState()
}

fun timerWidgetState(state: FocusSessionState): TimerWidgetState = when (state) {
    is FocusSessionState.Idle -> TimerWidgetState.Idle
    is FocusSessionState.Focusing -> TimerWidgetState.Active(
        label = "Focus",
        title = state.task.title,
        remainingMillis = remainingMillis(state.totalSeconds, state.elapsedSeconds),
        isRunning = true
    )
    is FocusSessionState.Paused -> TimerWidgetState.Active(
        label = "Paused",
        title = state.task.title,
        remainingMillis = remainingMillis(state.totalSeconds, state.elapsedSeconds),
        isRunning = false
    )
    is FocusSessionState.Breaking -> TimerWidgetState.Active(
        label = "Break",
        title = "Step away for a moment",
        remainingMillis = remainingMillis(state.totalSeconds, state.elapsedSeconds),
        isRunning = true,
        isBreak = true
    )
}

/**
 * Changes only when the widget needs redrawing: phase, pause state, task or an extension, not every tick.
 */
fun timerWidgetKey(state: FocusSessionState): Any = when (state) {
    is FocusSessionState.Idle -> "idle"
    is FocusSessionState.Focusing -> Triple("focus", state.task.id, state.totalSeconds)
    is FocusSessionState.Paused -> listOf("paused", state.task.id, state.elapsedSeconds, state.totalSeconds)
    is FocusSessionState.Breaking -> Pair("break", state.totalSeconds)
}

private fun remainingMillis(totalSeconds: Int, elapsedSeconds: Int): Long =
    (totalSeconds - elapsedSeconds).coerceAtLeast(0) * 1000L

/**
 * What the Today widget shows: focus time against the daily goal, tasks done and the streak.
 */
data class TodayWidgetState(
    val focusMinutes: Int,
    val goalMinutes: Int,
    val completedTasks: Int,
    val streakDays: Int
) {
    val progress: Float get() = if (goalMinutes > 0) (focusMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f) else 0f
    val goalReached: Boolean get() = goalMinutes > 0 && focusMinutes >= goalMinutes
    val focusLabel: String get() = formatMinutes(focusMinutes)

    val goalLabel: String
        get() = if (goalReached) "Goal reached" else "${(progress * 100).toInt()}% of ${formatMinutes(goalMinutes)}"

    val streakLabel: String?
        get() = when (streakDays) {
            0 -> null
            1 -> "1-day streak"
            else -> "$streakDays-day streak"
        }
}

/**
 * One row of the top-tasks widget. Checking it off completes the task without opening the app.
 */
data class TopTaskRow(
    val taskId: String,
    val title: String,
    val quadrantLabel: String
)

fun topTaskRows(tasks: List<Task>): List<TopTaskRow> =
    tasks.take(TOP_TASKS_LIMIT).map { TopTaskRow(it.id, it.title, it.quadrant.label) }

const val TOP_TASKS_LIMIT = 3

/**
 * One habit on the habits widget, with whether it's been done today.
 */
data class HabitWidgetRow(
    val habitId: String,
    val name: String,
    val done: Boolean
)

/** Undone habits first, so what's left to do is what fits on a small widget */
fun habitWidgetRows(habits: List<HabitWithHistory>, today: LocalDate): List<HabitWidgetRow> =
    habits
        .map { HabitWidgetRow(it.habit.id, it.habit.name, it.isDoneOn(today)) }
        .sortedBy { it.done }
