package com.decisionexecution.app.data.backup

import androidx.room.withTransaction
import com.decisionexecution.app.data.local.dao.BackupDao
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.local.entity.FocusSessionEntity
import com.decisionexecution.app.data.local.entity.HabitCheckInEntity
import com.decisionexecution.app.data.local.entity.HabitEntity
import com.decisionexecution.app.data.local.entity.InboxItemEntity
import com.decisionexecution.app.data.local.entity.MoodEntryEntity
import com.decisionexecution.app.data.local.entity.SubtaskEntity
import com.decisionexecution.app.data.local.entity.TaskCompletionEntity
import com.decisionexecution.app.data.local.entity.TaskEntity
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.ThemePalette
import com.decisionexecution.app.domain.repository.BackupRepository
import com.decisionexecution.app.domain.repository.BackupSummary
import com.decisionexecution.app.domain.repository.InvalidBackupException
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

/**
 * Plain JSON with a format number, so a backup stays readable by later versions of the app.
 * The running focus session and device-only state (widget caches, DND bookkeeping) aren't included.
 */
class BackupRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val backupDao: BackupDao,
    private val preferencesRepository: PreferencesRepository,
    private val progressRepository: ProgressRepository
) : BackupRepository {

    override suspend fun export(): String {
        val prefs = preferencesRepository.getPreferences().first()
        return JSONObject()
            .put("app", APP)
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
            .put("tasks", backupDao.tasks().toJson { it.toJson() })
            .put("completions", backupDao.completions().toJson { it.toJson() })
            .put("focusSessions", backupDao.focusSessions().toJson { it.toJson() })
            .put("habits", backupDao.habits().toJson { it.toJson() })
            .put("checkIns", backupDao.checkIns().toJson { JSONObject().put("habitId", it.habitId).put("day", it.day) })
            .put("inbox", backupDao.inboxItems().toJson { it.toJson() })
            .put("moods", backupDao.moodEntries().toJson { it.toJson() })
            .put("subtasks", backupDao.subtasks().toJson { it.toJson() })
            .put(
                "preferences", JSONObject()
                    .put("focusDurationMinutes", prefs.focusDurationMinutes)
                    .put("breakDurationMinutes", prefs.breakDurationMinutes)
                    .put("themeMode", prefs.themeMode.name)
                    .put("themePalette", prefs.themePalette.name)
                    .put("pureBlack", prefs.pureBlack)
                    .put("dailyFocusGoalMinutes", prefs.dailyFocusGoalMinutes)
            )
            .put(
                "progress", JSONObject()
                    .put("celebratedLevel", progressRepository.getCelebratedLevel().first())
                    .put("celebratedAchievements", JSONArray(progressRepository.getCelebratedAchievements().first().toList()))
            )
            .toString(2)
    }

    override suspend fun import(json: String): BackupSummary {
        // Parse everything up front so a broken file never leaves the database half-replaced
        val backup = try {
            parse(JSONObject(json))
        } catch (e: JSONException) {
            throw InvalidBackupException("That file isn't a Velto backup", e)
        }
        database.withTransaction {
            backupDao.clearCheckIns()
            backupDao.clearHabits()
            backupDao.clearCompletions()
            backupDao.clearFocusSessions()
            backupDao.clearSubtasks()
            backupDao.clearTasks()
            backupDao.clearInboxItems()
            backupDao.clearMoodEntries()
            backupDao.insertTasks(backup.tasks)
            backupDao.insertCompletions(backup.completions)
            backupDao.insertFocusSessions(backup.focusSessions)
            backupDao.insertHabits(backup.habits)
            backupDao.insertCheckIns(backup.checkIns)
            backupDao.insertInboxItems(backup.inbox)
            backupDao.insertMoodEntries(backup.moods)
            backupDao.insertSubtasks(backup.subtasks)
        }
        backup.preferences?.let { p ->
            val current = preferencesRepository.getPreferences().first()
            preferencesRepository.savePreferences(
                current.copy(
                    focusDurationMinutes = p.optInt("focusDurationMinutes", current.focusDurationMinutes),
                    breakDurationMinutes = p.optInt("breakDurationMinutes", current.breakDurationMinutes),
                    themeMode = ThemeMode.entries.find { it.name == p.optString("themeMode") } ?: current.themeMode,
                    themePalette = ThemePalette.entries.find { it.name == p.optString("themePalette") } ?: current.themePalette,
                    pureBlack = p.optBoolean("pureBlack", current.pureBlack),
                    dailyFocusGoalMinutes = p.optInt("dailyFocusGoalMinutes", current.dailyFocusGoalMinutes)
                )
            )
        }
        // Rewards already celebrated before the backup shouldn't pop up all over again
        backup.progress?.let { p ->
            val achievements = p.optJSONArray("celebratedAchievements")?.let { a -> (0 until a.length()).map(a::getString).toSet() }
            progressRepository.markCelebrated(p.optInt("celebratedLevel", 1), achievements.orEmpty())
        }
        return BackupSummary(backup.tasks.size, backup.habits.size, backup.moods.size)
    }

    private class Backup(
        val tasks: List<TaskEntity>,
        val completions: List<TaskCompletionEntity>,
        val focusSessions: List<FocusSessionEntity>,
        val habits: List<HabitEntity>,
        val checkIns: List<HabitCheckInEntity>,
        val inbox: List<InboxItemEntity>,
        val moods: List<MoodEntryEntity>,
        val subtasks: List<SubtaskEntity>,
        val preferences: JSONObject?,
        val progress: JSONObject?
    )

    private fun parse(root: JSONObject): Backup {
        if (root.optString("app") != APP) throw InvalidBackupException("That file isn't a Velto backup")
        val format = root.optInt("format", -1)
        if (format !in 1..FORMAT) {
            throw InvalidBackupException("This backup is from a newer version of Velto. Update the app to restore it.")
        }
        val habits = root.list("habits") {
            HabitEntity(it.getString("id"), it.getString("name"), it.getString("icon"), it.getLong("createdAt"))
        }
        val habitIds = habits.map { it.id }.toSet()
        val tasks = root.list("tasks") {
                TaskEntity(
                    id = it.getString("id"),
                    title = it.getString("title"),
                    description = it.optStringOrNull("description"),
                    category = it.getString("category"),
                    urgency = it.getInt("urgency"),
                    importance = it.getInt("importance"),
                    effort = it.getString("effort"),
                    energyTag = it.optStringOrNull("energyTag"),
                    createdAt = it.getLong("createdAt"),
                    isCompleted = it.optBoolean("isCompleted", false),
                    completedAt = if (it.isNull("completedAt")) null else it.getLong("completedAt"),
                    // Format 1 backups have neither
                    dueDate = if (it.isNull("dueDate")) null else it.getLong("dueDate"),
                    recurrence = it.optStringOrNull("recurrence")
                )
            }
        val taskIds = tasks.map { it.id }.toSet()
        return Backup(
            tasks = tasks,
            completions = root.list("completions") {
                TaskCompletionEntity(
                    it.getString("id"), it.getString("taskId"), it.getString("taskTitle"), it.getLong("completedAt"),
                    if (it.isNull("focusDuration")) null else it.getInt("focusDuration")
                )
            },
            focusSessions = root.list("focusSessions") {
                FocusSessionEntity(it.getString("id"), it.getString("taskId"), it.getLong("completedAt"), it.getInt("durationMinutes"))
            },
            habits = habits,
            // A check-in for a habit that isn't in the file would break the foreign key; drop it
            checkIns = root.list("checkIns") { HabitCheckInEntity(it.getString("habitId"), it.getLong("day")) }
                .filter { it.habitId in habitIds }
                .distinct(),
            inbox = root.list("inbox") {
                InboxItemEntity(it.getString("id"), it.getString("text"), it.getLong("createdAt"), it.optBoolean("isArchived", false))
            },
            moods = root.list("moods") {
                MoodEntryEntity(it.getLong("day"), it.getInt("mood"), it.getInt("energy"), it.optStringOrNull("note"), it.getLong("loggedAt"))
            },
            // Like check-ins, a step for a task that isn't in the file is dropped
            subtasks = root.list("subtasks") {
                SubtaskEntity(it.getString("id"), it.getString("taskId"), it.getString("title"), it.optBoolean("isDone", false), it.optInt("position", 0))
            }.filter { it.taskId in taskIds },
            preferences = root.optJSONObject("preferences"),
            progress = root.optJSONObject("progress")
        )
    }

    private companion object {
        const val APP = "velto"
        /** Bump when the shape changes, and keep reading older formats. 2 added due dates, repeats and steps */
        const val FORMAT = 2
    }
}

private fun <T> List<T>.toJson(map: (T) -> JSONObject): JSONArray = JSONArray().also { array -> forEach { array.put(map(it)) } }

private fun <T> JSONObject.list(key: String, map: (JSONObject) -> T): List<T> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { map(array.getJSONObject(it)) }
}

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

private fun TaskEntity.toJson() = JSONObject()
    .put("id", id).put("title", title).put("description", description ?: JSONObject.NULL)
    .put("category", category).put("urgency", urgency).put("importance", importance)
    .put("effort", effort).put("energyTag", energyTag ?: JSONObject.NULL).put("createdAt", createdAt)
    .put("isCompleted", isCompleted).put("completedAt", completedAt ?: JSONObject.NULL)
    .put("dueDate", dueDate ?: JSONObject.NULL).put("recurrence", recurrence ?: JSONObject.NULL)

private fun TaskCompletionEntity.toJson() = JSONObject()
    .put("id", id).put("taskId", taskId).put("taskTitle", taskTitle).put("completedAt", completedAt)
    .put("focusDuration", focusDuration ?: JSONObject.NULL)

private fun FocusSessionEntity.toJson() = JSONObject()
    .put("id", id).put("taskId", taskId).put("completedAt", completedAt).put("durationMinutes", durationMinutes)

private fun HabitEntity.toJson() = JSONObject().put("id", id).put("name", name).put("icon", icon).put("createdAt", createdAt)

private fun InboxItemEntity.toJson() = JSONObject().put("id", id).put("text", text).put("createdAt", createdAt).put("isArchived", isArchived)

private fun MoodEntryEntity.toJson() = JSONObject()
    .put("day", day).put("mood", mood).put("energy", energy).put("note", note ?: JSONObject.NULL).put("loggedAt", loggedAt)

private fun SubtaskEntity.toJson() = JSONObject()
    .put("id", id).put("taskId", taskId).put("title", title).put("isDone", isDone).put("position", position)
