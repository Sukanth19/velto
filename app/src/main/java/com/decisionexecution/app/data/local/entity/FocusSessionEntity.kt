package com.decisionexecution.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A finished focus phase. Kept apart from task completions so focus time
 * doesn't count as a completed task.
 */
@Entity(tableName = "focus_sessions", indices = [Index("completedAt")])
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val completedAt: Long,
    val durationMinutes: Int
)
