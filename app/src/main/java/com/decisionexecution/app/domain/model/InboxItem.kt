package com.decisionexecution.app.domain.model

/**
 * A quick thought captured before deciding what it is. It later becomes a task, a habit, or gets binned.
 */
data class InboxItem(
    val id: String,
    val text: String,
    val createdAt: Long
)
