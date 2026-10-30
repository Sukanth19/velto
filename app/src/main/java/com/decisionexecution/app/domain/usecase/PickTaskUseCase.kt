package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.repository.TaskRepository
import com.decisionexecution.app.domain.service.PickContext
import com.decisionexecution.app.domain.service.TaskPicker
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Ranked choices for a given amount of time, using today's logged energy when there is one.
 *
 * @property ranked Best fit first; the spinner lands on the first, "spin again" walks down the list
 */
data class TaskPicks(
    val ranked: List<Task>,
    val context: PickContext
)

class PickTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val moodRepository: MoodRepository
) {
    private val picker = TaskPicker()

    suspend operator fun invoke(minutes: Int, energy: Int? = null, today: LocalDate = LocalDate.now()): TaskPicks {
        val context = PickContext(minutes, energy ?: moodRepository.getEntry(today).first()?.energy)
        return TaskPicks(picker.rank(taskRepository.getAllTasks().first(), context), context)
    }

    fun reason(task: Task, context: PickContext): String = picker.reason(task, context)
}

/**
 * Starts a focus session of exactly the chosen length, with the usual break after it.
 */
class StartTimeboxedFocusUseCase @Inject constructor(
    private val startFocusSessionUseCase: StartFocusSessionUseCase,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(task: Task, minutes: Int) {
        startFocusSessionUseCase(task, minutes, preferencesRepository.getPreferences().first().breakDurationMinutes)
    }
}
