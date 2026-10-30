package com.decisionexecution.app.testutil

import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.domain.model.InboxItem
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.repository.HabitRepository
import com.decisionexecution.app.domain.repository.InboxRepository
import com.decisionexecution.app.domain.repository.MoodRepository
import com.decisionexecution.app.domain.repository.ProgressRepository
import com.decisionexecution.app.domain.service.LifetimeStats
import com.decisionexecution.app.domain.usecase.GetPlantUseCase
import com.decisionexecution.app.domain.usecase.GetProgressUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class InMemoryHabitRepository(vararg initial: HabitWithHistory) : HabitRepository {
    private val habits = MutableStateFlow(initial.map { it.habit })
    private val checkIns = MutableStateFlow(initial.flatMap { h -> h.doneDays.map { h.habit.id to it } }.toSet())

    override fun getHabits(since: LocalDate): Flow<List<HabitWithHistory>> =
        combine(habits, checkIns) { list, done ->
            list.map { habit -> HabitWithHistory(habit, done.filter { it.first == habit.id && it.second >= since }.map { it.second }.toSet()) }
        }

    override suspend fun addHabit(habit: Habit) = habits.update { it + habit }

    override suspend fun deleteHabit(habitId: String) {
        habits.update { list -> list.filterNot { it.id == habitId } }
        checkIns.update { set -> set.filterNot { it.first == habitId }.toSet() }
    }

    override suspend fun setDone(habitId: String, day: LocalDate, done: Boolean) =
        checkIns.update { if (done) it + (habitId to day) else it - (habitId to day) }
}

class InMemoryInboxRepository(vararg initial: InboxItem) : InboxRepository {
    val items = MutableStateFlow(initial.toList())

    override fun getItems(): Flow<List<InboxItem>> = items.map { list -> list.sortedByDescending { it.createdAt } }
    override suspend fun add(item: InboxItem) = items.update { it + item }
    override suspend fun delete(itemId: String) = items.update { list -> list.filterNot { it.id == itemId } }
}

class InMemoryMoodRepository(vararg initial: MoodEntry) : MoodRepository {
    val entries = MutableStateFlow(initial.associateBy { it.day })

    override fun getEntries(since: LocalDate): Flow<List<MoodEntry>> =
        entries.map { map -> map.values.filter { it.day >= since }.sortedByDescending { it.day } }

    override fun getEntry(day: LocalDate): Flow<MoodEntry?> = entries.map { it[day] }
    override suspend fun save(entry: MoodEntry) = entries.update { it + (entry.day to entry) }
}

class InMemoryProgressRepository(stats: LifetimeStats = LifetimeStats()) : ProgressRepository {
    val stats = MutableStateFlow(stats)
    private val level = MutableStateFlow(1)
    private val achievements = MutableStateFlow(emptySet<String>())

    override fun getLifetimeStats(): Flow<LifetimeStats> = stats
    override fun getCelebratedLevel(): Flow<Int> = level
    override fun getCelebratedAchievements(): Flow<Set<String>> = achievements

    override suspend fun markCelebrated(level: Int, achievementIds: Set<String>) {
        this.level.update { maxOf(it, level) }
        achievements.update { it + achievementIds }
    }
}

fun progressUseCase(stats: LifetimeStats = LifetimeStats()) =
    GetProgressUseCase(InMemoryProgressRepository(stats), emptyInsights())

fun plantUseCase(
    stats: LifetimeStats = LifetimeStats(),
    habits: HabitRepository = InMemoryHabitRepository(),
    moods: MoodRepository = InMemoryMoodRepository()
) = GetPlantUseCase(progressUseCase(stats), emptyInsights(), habits, moods)
