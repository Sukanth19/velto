package com.decisionexecution.app.ecosystem

import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Which Velto habit stands for each shared habit key (creatine, water, workout), and whether syncing
 * with pupdr is on. Kept in its own SharedPreferences so the database schema doesn't change.
 */
class HabitLinks(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    fun habitFor(key: String): String? = prefs.getString("link.$key", null)
    fun link(key: String, habitId: String) { prefs.edit().putString("link.$key", habitId).apply() }
    fun keyFor(habitId: String): String? = Ecosystem.Habits.ALL.firstOrNull { habitFor(it) == habitId }
    fun linkedKeys(): List<String> = Ecosystem.Habits.ALL.filter { habitFor(it) != null }

    companion object {
        const val PREFS = "ecosystem_links"
        private const val KEY_ENABLED = "sync_pupdr"
    }
}

/** Velto's outgoing side: tells pupdr when a linked habit is ticked or unticked here. */
object VeltoBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun pupdrInstalled(context: Context) = Ecosystem.isInstalled(context, Ecosystem.PUPDR)

    fun habitChanged(context: Context, habitId: String, epochDay: Long, done: Boolean) {
        val links = HabitLinks(context)
        if (!links.enabled) return
        val key = links.keyFor(habitId) ?: return
        val app = context.applicationContext
        scope.launch {
            Ecosystem.call(app, Ecosystem.PUPDR, Ecosystem.HABIT_SET, Bundle().apply {
                putString(Ecosystem.KEY, key)
                putLong(Ecosystem.DAY, epochDay)
                putBoolean(Ecosystem.DONE, done)
            })
        }
    }
}
