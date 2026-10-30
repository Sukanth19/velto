package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.decisionexecution.app.data.local.entity.FocusSessionEntity
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import com.decisionexecution.app.data.local.entity.InboxItemEntity
import com.decisionexecution.app.data.local.entity.MoodEntryEntity
import com.decisionexecution.app.data.local.entity.SubtaskEntity
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import com.decisionexecution.app.data.local.entity.TaskEntity

/** Whole-table reads and writes for export and import. Run imports inside a transaction. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM tasks") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM task_completions") suspend fun completions(): List<TaskCompletionEntity>
    @Query("SELECT * FROM focus_sessions") suspend fun focusSessions(): List<FocusSessionEntity>
    @Query("SELECT * FROM habits") suspend fun habits(): List<HabitEntity>
    @Query("SELECT * FROM habit_check_ins") suspend fun checkIns(): List<HabitCheckInEntity>
    @Query("SELECT * FROM inbox_items") suspend fun inboxItems(): List<InboxItemEntity>
    @Query("SELECT * FROM mood_entries") suspend fun moodEntries(): List<MoodEntryEntity>
    @Query("SELECT * FROM subtasks") suspend fun subtasks(): List<SubtaskEntity>

    @Insert suspend fun insertTasks(items: List<TaskEntity>)
    @Insert suspend fun insertCompletions(items: List<TaskCompletionEntity>)
    @Insert suspend fun insertFocusSessions(items: List<FocusSessionEntity>)
    @Insert suspend fun insertHabits(items: List<HabitEntity>)
    @Insert suspend fun insertCheckIns(items: List<HabitCheckInEntity>)
    @Insert suspend fun insertInboxItems(items: List<InboxItemEntity>)
    @Insert suspend fun insertMoodEntries(items: List<MoodEntryEntity>)
    @Insert suspend fun insertSubtasks(items: List<SubtaskEntity>)

    // Subtasks go before tasks: they reference them
    @Query("DELETE FROM subtasks") suspend fun clearSubtasks()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM task_completions") suspend fun clearCompletions()
    @Query("DELETE FROM focus_sessions") suspend fun clearFocusSessions()
    // Check-ins go first: they reference habits
    @Query("DELETE FROM habit_check_ins") suspend fun clearCheckIns()
    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM inbox_items") suspend fun clearInboxItems()
    @Query("DELETE FROM mood_entries") suspend fun clearMoodEntries()
}
