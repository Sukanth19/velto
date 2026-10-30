package com.decisionexecution.app.domain.service

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** How long from [now] until the next [minuteOfDay]: later today, or tomorrow if it's passed */
fun delayUntil(now: LocalDateTime, minuteOfDay: Int): Duration {
    val today = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
    val next = if (today.isAfter(now)) today else today.plusDays(1)
    return Duration.between(now, next)
}

/** How long until the next [day] at [minuteOfDay], for weekly reminders */
fun delayUntilWeekly(now: LocalDateTime, day: DayOfWeek, minuteOfDay: Int): Duration {
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    var next = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day)).atTime(time)
    if (!next.isAfter(now)) next = next.plusWeeks(1)
    return Duration.between(now, next)
}

fun weeklyReviewMessage(tasksDone: Int, focusLabel: String, isEmpty: Boolean): ReminderMessage? =
    if (isEmpty) null
    else ReminderMessage(
        "Your week in review is ready",
        "${if (tasksDone == 1) "1 task" else "$tasksDone tasks"} done and $focusLabel of focus. See what stood out"
    )

/** What a reminder says, or null when there's nothing worth interrupting for */
data class ReminderMessage(val title: String, val text: String)

fun habitReminderMessage(habitsLeft: List<String>, totalHabits: Int): ReminderMessage? = when {
    habitsLeft.isEmpty() -> null
    habitsLeft.size == 1 && totalHabits > 1 ->
        ReminderMessage("One habit to go", "Just \"${habitsLeft.single()}\" left. Finish the set!")
    habitsLeft.size == 1 -> ReminderMessage("Keep your streak going", "Don't forget \"${habitsLeft.single()}\" today")
    else -> ReminderMessage(
        "${habitsLeft.size} habits left today",
        habitsLeft.take(3).joinToString(", ") + if (habitsLeft.size > 3) " and more" else ""
    )
}

fun moodReminderMessage(loggedToday: Boolean): ReminderMessage? =
    if (loggedToday) null else ReminderMessage("How are you feeling?", "Take five seconds to check in. Your plant likes it too")

/** Displays a minute of the day as "8:00 PM" or "20:00" */
fun formatMinuteOfDay(minuteOfDay: Int, use24Hour: Boolean): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    if (use24Hour) return "%02d:%02d".format(hour, minute)
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(h12, minute, if (hour < 12) "AM" else "PM")
}
