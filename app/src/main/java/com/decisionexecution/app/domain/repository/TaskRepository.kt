package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(id: String): Flow<Task?>
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun completeTask(taskId: String, completionTimestamp: Long)
    suspend fun uncompleteTask(taskId: String)
    /** The user passed on it once more */
    suspend fun addDodge(taskId: String)
    /** The user got stuck in, so forget past dodges and any snooze */
    suspend fun clearDodges(taskId: String)
    /** Not now: counts as a dodge and keeps it out of Just Start until [until] */
    suspend fun snooze(taskId: String, until: LocalDate)
    fun getTasksByCategory(category: TaskCategory): Flow<List<Task>>

    /** Steps of a task, in order */
    fun getSubtasks(taskId: String): Flow<List<Subtask>>
    suspend fun setSubtaskDone(subtaskId: String, done: Boolean)
    /** Replaces all of a task's steps, keeping ids and done state of the ones passed in */
    suspend fun replaceSubtasks(taskId: String, subtasks: List<Subtask>)
    /** Leaves only [doneIds] ticked; used to reset a repeating task and to undo that */
    suspend fun setDoneSubtasks(taskId: String, doneIds: Set<String>)
}
