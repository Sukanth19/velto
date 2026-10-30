package com.decisionexecution.app.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.usecase.ExtendFocusSessionUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Pause, resume, extend and stop buttons on the focus timer widget. Runs in the app process,
 * so the in-app Focus screen and notifications follow along.
 */
class TimerControlAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val manager = context.widgetEntryPoint().focusSessionManager()
        // The process may have just started for this tap; load the persisted session first
        manager.sync()
        when (parameters[COMMAND]) {
            PAUSE -> manager.pauseSession()
            RESUME -> manager.resumeSession()
            EXTEND -> manager.extendSession(ExtendFocusSessionUseCase.DEFAULT_MINUTES)
            STOP -> manager.endSession()
        }
        FocusTimerWidget().update(context, glanceId)
    }

    companion object {
        val COMMAND = ActionParameters.Key<String>("command")
        const val PAUSE = "pause"
        const val RESUME = "resume"
        const val EXTEND = "extend"
        const val STOP = "stop"
    }
}

/**
 * Completes a task from the top-tasks widget's checkbox, then redraws every task widget.
 */
class CompleteTopTaskAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK_ID] ?: return
        runCatching { context.widgetEntryPoint().completeTaskUseCase()(taskId) }
        TopTasksWidget().update(context, glanceId)
        runCatching {
            BestTaskWidget().updateAll(context)
            TodayWidget().updateAll(context)
        }
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
    }
}

/**
 * Checks a habit in or out for today from the habits widget.
 */
class ToggleHabitAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HABIT_ID] ?: return
        runCatching { context.widgetEntryPoint().habitRepository().setDone(habitId, LocalDate.now(), parameters[DONE] ?: true) }
        HabitsWidget().update(context, glanceId)
        runCatching { LevelWidget().updateAll(context) }
    }

    companion object {
        val HABIT_ID = ActionParameters.Key<String>("habit_id")
        val DONE = ActionParameters.Key<Boolean>("done")
    }
}

/**
 * Logs today's mood from the mood widget, keeping any energy and note already logged.
 */
class LogMoodAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mood = parameters[MOOD] ?: return
        runCatching {
            val repository = context.widgetEntryPoint().moodRepository()
            val today = LocalDate.now()
            val existing = repository.getEntry(today).first()
            repository.save(MoodEntry(today, mood, existing?.energy ?: DEFAULT_ENERGY, existing?.note))
        }
        MoodWidget().update(context, glanceId)
        runCatching { LevelWidget().updateAll(context) }
    }

    companion object {
        val MOOD = ActionParameters.Key<Int>("mood")
        private const val DEFAULT_ENERGY = 3
    }
}
