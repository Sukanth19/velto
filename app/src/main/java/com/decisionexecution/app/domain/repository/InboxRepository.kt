package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow

interface InboxRepository {
    /** Newest first */
    fun getItems(): Flow<List<InboxItem>>
    suspend fun add(item: InboxItem)
    suspend fun delete(itemId: String)
}
