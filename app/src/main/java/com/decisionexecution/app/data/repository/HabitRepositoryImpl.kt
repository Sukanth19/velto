package com.decisionexecution.app.data.repository

import com.decisionexecution.app.data.local.dao.HabitDao
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitIcon
import com.decisionexecution.app.domain.model.HabitWithHistory
import com.decisionexecution.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {

    override fun getHabits(since: LocalDate): Flow<List<HabitWithHistory>> =
        combine(habitDao.getHabits(), habitDao.getCheckInsSince(since.toEpochDay())) { habits, checkIns ->
            val daysByHabit = checkIns.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.day) })
            habits.map { HabitWithHistory(it.toDomain(), daysByHabit[it.id].orEmpty().toSet()) }
        }

    override suspend fun addHabit(habit: Habit) {
        habitDao.insertHabit(HabitEntity(habit.id, habit.name, habit.icon.name, habit.createdAt))
    }

    override suspend fun deleteHabit(habitId: String) {
        habitDao.deleteCheckIns(habitId)
        habitDao.deleteHabit(habitId)
    }

    override suspend fun setDone(habitId: String, day: LocalDate, done: Boolean) {
        if (done) habitDao.insertCheckIn(HabitCheckInEntity(habitId, day.toEpochDay()))
        else habitDao.deleteCheckIn(habitId, day.toEpochDay())
    }

    private fun HabitEntity.toDomain() = Habit(
        id = id,
        name = name,
        // An icon from a newer app version falls back rather than crashing
        icon = HabitIcon.entries.find { it.name == icon } ?: HabitIcon.SPARK,
        createdAt = createdAt
    )
}
