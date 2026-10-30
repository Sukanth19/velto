package com.decisionexecution.app.domain.repository

/**
 * Everything the user has made, as one portable JSON document.
 */
interface BackupRepository {
    suspend fun export(): String

    /**
     * Replaces all current data with the backup. Nothing changes if the file isn't a valid backup.
     *
     * @throws InvalidBackupException when the file isn't a Velto backup this version can read
     */
    suspend fun import(json: String): BackupSummary
}

data class BackupSummary(
    val tasks: Int,
    val habits: Int,
    val moodEntries: Int
)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
