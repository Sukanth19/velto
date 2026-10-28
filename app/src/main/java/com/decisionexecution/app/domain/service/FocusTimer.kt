package com.decisionexecution.app.domain.service

/**
 * Wall-clock source, injectable so timer logic can be tested without waiting.
 */
fun interface TimeSource {
    fun nowMillis(): Long
}

/**
 * Wakes the app when the current focus or break phase should end, even if the process is gone.
 * Implemented with WorkManager in the data layer.
 */
interface FocusTimerScheduler {
    /** Replaces any pending check with one that fires after [delayMillis]. */
    fun scheduleCheck(delayMillis: Long)
    fun cancel()
}

/**
 * Tells the user a phase ended. Implemented with system notifications in the presentation layer.
 */
interface FocusNotifier {
    fun onFocusComplete(taskTitle: String)
    fun onBreakComplete()
}
