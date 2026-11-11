package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.repository.CalendarRepository
import com.decisionexecution.app.domain.service.calendarEventFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

enum class CalendarResult { ADDED, NOT_LINKED, NO_DUE_DATE, NO_PERMISSION, FAILED }

/**
 * Keeps tasks with due dates on the linked calendar. Adding is the user's choice per task; once a
 * task is on the calendar, its event follows the task when the date moves and goes when it's deleted.
 * A calendar problem never stops the task itself being saved, completed or deleted.
 */
class CalendarSyncUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {
    /** Puts the task on the linked calendar with the chosen reminder, or updates its event */
    suspend fun add(task: Task): CalendarResult {
        val settings = calendarRepository.settings().first()
        val calendar = settings.calendar ?: return CalendarResult.NOT_LINKED
        val event = calendarEventFor(task, settings.reminder) ?: return CalendarResult.NO_DUE_DATE
        return try {
            calendarRepository.saveEvent(task.id, calendar.id, event)
            CalendarResult.ADDED
        } catch (e: SecurityException) {
            CalendarResult.NO_PERMISSION
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CalendarResult.FAILED
        }
    }

    suspend fun remove(taskId: String) {
        quietly { calendarRepository.deleteEvent(taskId) }
    }

    /**
     * After a task changed: moves its event to the new date, or takes it off the calendar if the due
     * date was cleared. Tasks that were never added are left alone.
     */
    suspend fun refresh(task: Task) {
        if (!isOnCalendar(task.id)) return
        if (task.dueDate == null) remove(task.id) else add(task)
    }

    suspend fun isOnCalendar(taskId: String): Boolean = taskId in calendarRepository.tasksOnCalendar().first()

    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The task change matters more than its calendar copy
        }
    }
}
