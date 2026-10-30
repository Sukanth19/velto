package com.decisionexecution.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.decisionexecution.app.data.local.entity.InboxItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_items WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getOpenItems(): Flow<List<InboxItemEntity>>

    @Insert
    suspend fun insert(item: InboxItemEntity)

    @Query("UPDATE inbox_items SET isArchived = 1 WHERE id = :itemId")
    suspend fun archive(itemId: String)
}
