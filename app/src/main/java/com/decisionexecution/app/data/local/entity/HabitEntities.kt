package com.decisionexecution.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val createdAt: Long
)

/** One row per habit per day it was done; [day] is the epoch day in the user's time zone. */
@Entity(
    tableName = "habit_check_ins",
    primaryKeys = ["habitId", "day"],
    foreignKeys = [
        ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("day")]
)
data class HabitCheckInEntity(
    val habitId: String,
    val day: Long
)

/** Processed items stay as archived rows so lifetime capture counts survive clearing the inbox. */
@Entity(tableName = "inbox_items")
data class InboxItemEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val isArchived: Boolean
)

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey val day: Long,
    val mood: Int,
    val energy: Int,
    val note: String?,
    val loggedAt: Long
)
