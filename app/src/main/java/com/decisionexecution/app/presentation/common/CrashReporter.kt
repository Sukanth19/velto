package com.decisionexecution.app.presentation.common

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

/**
 * Keeps the last crash on the phone so the next launch can offer to send it. Nothing is
 * uploaded anywhere; the user decides whether to share it.
 */
class CrashReporter(private val context: Context) {

    private val file: File get() = File(context.filesDir, "last_crash.txt")

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Never let writing the report hide the original crash
            runCatching { file.writeText(report(error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun pendingReport(): String? = runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear() {
        runCatching { file.delete() }
    }

    private fun report(error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
        return buildString {
            appendLine("Velto $version crashed at ${LocalDateTime.now()}")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            // Long traces get cut so the report still fits in an issue or a message
            append(trace.take(MAX_TRACE_CHARS))
        }
    }

    companion object {
        private const val MAX_TRACE_CHARS = 6_000
    }
}
