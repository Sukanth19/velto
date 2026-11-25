package com.decisionexecution.app.ecosystem

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.decisionexecution.app.data.local.dao.HabitDao
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import com.decisionexecution.app.domain.model.HabitIcon
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EcosystemEntryPoint {
    fun habitDao(): HabitDao
}

/**
 * Answers pupdr — see [Ecosystem]. Writes go straight to the DAO (not the repository) so a change
 * that came from pupdr isn't echoed back to it. Only [ContentProvider.call] is supported.
 */
class EcosystemProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!Ecosystem.isTrusted(ctx, callingPackage)) throw SecurityException("not a pupdr ecosystem app")
        val links = HabitLinks(ctx)
        val x = extras ?: Bundle()
        if (method == Ecosystem.PING) return Bundle().apply { putInt(Ecosystem.RESULT, Ecosystem.VERSION) }
        if (!links.enabled) return null
        val dao = EntryPointAccessors.fromApplication(ctx.applicationContext, EcosystemEntryPoint::class.java).habitDao()
        val key = x.getString(Ecosystem.KEY)?.takeIf { it in Ecosystem.Habits.ALL } ?: return null
        val day = x.getLong(Ecosystem.DAY)
        return runBlocking {
            when (method) {
                Ecosystem.HABIT_SET -> {
                    val habitId = resolveHabit(ctx, dao, links, key)
                    if (x.getBoolean(Ecosystem.DONE)) dao.insertCheckIn(HabitCheckInEntity(habitId, day)) else dao.deleteCheckIn(habitId, day)
                    Bundle().apply { putBoolean(Ecosystem.RESULT, true) }
                }
                Ecosystem.HABIT_GET -> {
                    val habitId = links.habitFor(key)
                    val done = habitId != null && dao.getCheckInsSince(day).first().any { it.habitId == habitId && it.day == day }
                    Bundle().apply { putBoolean(Ecosystem.DONE, done) }
                }
                else -> null
            }
        }
    }

    /** The linked habit for [key]; links an existing habit with a matching name, or creates one. */
    private suspend fun resolveHabit(ctx: Context, dao: HabitDao, links: HabitLinks, key: String): String {
        val habits = dao.getHabits().first()
        links.habitFor(key)?.takeIf { id -> habits.any { it.id == id } }?.let { return it }
        val label = Ecosystem.Habits.label(key)
        val match = habits.firstOrNull { it.name.equals(label, ignoreCase = true) }
            ?: habits.firstOrNull { it.name.contains(label, ignoreCase = true) }
        val id = match?.id ?: UUID.randomUUID().toString().also { newId ->
            val icon = when (key) { Ecosystem.Habits.WATER -> HabitIcon.WATER; Ecosystem.Habits.WORKOUT -> HabitIcon.MOVE; else -> HabitIcon.SPARK }
            dao.insertHabit(HabitEntity(newId, label, icon.name, System.currentTimeMillis()))
        }
        links.link(key, id)
        return id
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?) = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
