package com.decisionexecution.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "task_completions")
data class TaskCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val taskTitle: String,
    val completedAt: Long,
    val focusDuration: Int?, // in minutes, null if completed without focus session
    /** What the user guessed, kept with the completion so estimates can be compared later */
    val estimateMinutes: Int? = null,
    /** Focus minutes actually spent on the task before this completion; not counted again in totals */
    val actualMinutes: Int? = null
)
