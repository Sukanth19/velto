package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MoodRepository {
    /** Entries from [since] on, newest first */
    fun getEntries(since: LocalDate): Flow<List<MoodEntry>>
    fun getEntry(day: LocalDate): Flow<MoodEntry?>
    /** Replaces any entry already logged for that day */
    suspend fun save(entry: MoodEntry)
}
