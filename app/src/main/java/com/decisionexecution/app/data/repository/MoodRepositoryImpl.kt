package com.decisionexecution.app.data.repository

import com.decisionexecution.app.data.local.dao.MoodDao
import com.decisionexecution.app.data.local.entity.MoodEntryEntity
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class MoodRepositoryImpl @Inject constructor(
    private val moodDao: MoodDao
) : MoodRepository {

    override fun getEntries(since: LocalDate): Flow<List<MoodEntry>> =
        moodDao.getEntriesSince(since.toEpochDay()).map { entries -> entries.map { it.toDomain() } }

    override fun getEntry(day: LocalDate): Flow<MoodEntry?> =
        moodDao.getEntry(day.toEpochDay()).map { it?.toDomain() }

    override suspend fun save(entry: MoodEntry) {
        moodDao.upsert(MoodEntryEntity(entry.day.toEpochDay(), entry.mood, entry.energy, entry.note, System.currentTimeMillis()))
    }

    private fun MoodEntryEntity.toDomain() = MoodEntry(LocalDate.ofEpochDay(day), mood, energy, note)
}
