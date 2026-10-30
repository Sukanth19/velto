package com.decisionexecution.app.data.repository

import com.decisionexecution.app.data.local.dao.InboxDao
import com.decisionexecution.app.data.local.entity.InboxItemEntity
import com.decisionexecution.app.domain.model.InboxItem
import com.decisionexecution.app.domain.repository.InboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InboxRepositoryImpl @Inject constructor(
    private val inboxDao: InboxDao
) : InboxRepository {

    override fun getItems(): Flow<List<InboxItem>> =
        inboxDao.getOpenItems().map { items -> items.map { InboxItem(it.id, it.text, it.createdAt) } }

    override suspend fun add(item: InboxItem) {
        inboxDao.insert(InboxItemEntity(item.id, item.text, item.createdAt, isArchived = false))
    }

    // Archived rather than removed so it still counts towards the capture achievement
    override suspend fun delete(itemId: String) {
        inboxDao.archive(itemId)
    }
}
