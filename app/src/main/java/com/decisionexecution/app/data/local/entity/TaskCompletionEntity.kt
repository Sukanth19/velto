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
    val focusDuration: Int? // in minutes, null if completed without focus session
)
