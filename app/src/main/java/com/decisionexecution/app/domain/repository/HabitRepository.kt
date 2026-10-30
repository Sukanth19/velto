package com.decisionexecution.app.domain.repository

import com.decisionexecution.app.domain.model.Habit
import com.decisionexecution.app.domain.model.HabitWithHistory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    /** Every habit with its check-ins since [since], oldest habit first */
    fun getHabits(since: LocalDate): Flow<List<HabitWithHistory>>
    suspend fun addHabit(habit: Habit)
    suspend fun deleteHabit(habitId: String)
    suspend fun setDone(habitId: String, day: LocalDate, done: Boolean)
}
