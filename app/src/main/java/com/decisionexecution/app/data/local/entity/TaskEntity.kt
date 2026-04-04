package com.decisionexecution.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val category: String, // "SCHOOL", "PERSONAL", "WORK"
    val urgency: Int,      // 1-5
    val importance: Int,   // 1-5
    val effort: String,    // "LOW", "MEDIUM", "HIGH"
    val energyTag: String?, // "LOW", "DEEP_WORK", or null
    val createdAt: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
