package com.decisionexecution.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.decisionexecution.app.R
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.presentation.components.moodLabel
import com.decisionexecution.app.presentation.navigation.DeepLinks
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** The shared 2x2 card: rounded surface, tap goes to [link] unless a child handles it first. */
@Composable
private fun SmallCard(context: Context, link: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(deepLinkAction(context, link))
    ) { content() }
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text.uppercase(),
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )
}

/**
 * Today's habits with checkboxes; ticking one checks it in without opening the app.
 */
class HabitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rows = runCatching {
            val today = LocalDate.now()
            habitWidgetRows(context.widgetEntryPoint().habitRepository().getHabits(today).first(), today)
        }.getOrElse { emptyList() }

        provideContent {
            GlanceTheme {
                SmallCard(context, DeepLinks.HABITS) {
                    CardLabel(if (rows.isEmpty()) "Habits" else "Habits · ${rows.count { it.done }}/${rows.size}")
                    Spacer(GlanceModifier.height(4.dp))
                    if (rows.isEmpty()) {
                        Text("Add a habit in Velto", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
                    }
                    rows.take(HABITS_SHOWN).forEach { row ->
                        CheckBox(
                            checked = row.done,
                            onCheckedChange = actionRunCallback<ToggleHabitAction>(
                                actionParametersOf(
                                    ToggleHabitAction.HABIT_ID to row.habitId,
                                    ToggleHabitAction.DONE to !row.done
                                )
                            ),
                            text = row.name,
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp)
                        )
                    }
                    if (rows.size > HABITS_SHOWN) {
                        Text("+${rows.size - HABITS_SHOWN} more", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                    }
                }
            }
        }
    }

    private companion object {
        const val HABITS_SHOWN = 3
    }
}

/**
 * Five faces; tapping one logs today's mood. Keeps today's energy if it was already logged.
 */
class MoodWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = runCatching { context.widgetEntryPoint().moodRepository().getEntry(LocalDate.now()).first() }.getOrNull()

        provideContent {
            GlanceTheme {
                SmallCard(context, DeepLinks.JOURNAL) {
                    CardLabel("Mood")
                    Text(
                        today?.let { "Feeling ${moodLabel(it.mood).lowercase()}" } ?: "How's today?",
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        MoodEntry.SCALE.forEach { mood ->
                            val selected = today?.mood == mood
                            Box(
                                GlanceModifier
                                    .size(30.dp)
                                    .cornerRadius(15.dp)
                                    .background(if (selected) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.widgetBackground)
                                    .clickable(actionRunCallback<LogMoodAction>(actionParametersOf(LogMoodAction.MOOD to mood))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(MOOD_EMOJI[mood - 1], style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center))
                            }
                        }
                    }
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }

    private companion object {
        val MOOD_EMOJI = listOf("😫", "😕", "😐", "🙂", "😄")
    }
}

/**
 * One big button into the inbox capture field, with how much is waiting to be sorted.
 */
class CaptureWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val count = runCatching { context.widgetEntryPoint().inboxRepository().getItems().first().size }.getOrElse { 0 }

        provideContent {
            GlanceTheme {
                SmallCard(context, DeepLinks.CAPTURE) {
                    CardLabel("Inbox")
                    Text(
                        when (count) {
                            0 -> "All sorted"
                            1 -> "1 to sort"
                            else -> "$count to sort"
                        },
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircleIconButton(
                            imageProvider = ImageProvider(R.drawable.ic_widget_add),
                            contentDescription = "Capture a thought",
                            onClick = deepLinkAction(context, DeepLinks.CAPTURE),
                            backgroundColor = GlanceTheme.colors.primary,
                            contentColor = GlanceTheme.colors.onPrimary,
                            modifier = GlanceModifier.size(64.dp)
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

/**
 * Current level, its title and an XP bar towards the next.
 */
class LevelWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val level = runCatching { context.widgetEntryPoint().getProgressUseCase()().first().level }.getOrNull()

        provideContent {
            GlanceTheme {
                SmallCard(context, DeepLinks.PROGRESS) {
                    CardLabel("Level")
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            GlanceModifier.size(48.dp).cornerRadius(16.dp).background(GlanceTheme.colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (level?.level ?: 1).toString(),
                                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(GlanceModifier.size(8.dp))
                        Text(
                            level?.title ?: "Spark",
                            maxLines = 1,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    LinearProgressIndicator(
                        progress = level?.fraction ?: 0f,
                        modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
                        color = GlanceTheme.colors.primary,
                        backgroundColor = GlanceTheme.colors.secondaryContainer
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        level?.let { "${it.xpToGo} XP to level ${it.level + 1}" } ?: "Earn XP by getting things done",
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

class HabitsWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

class MoodWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoodWidget()
}

class CaptureWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaptureWidget()
}

class LevelWidgetReceiver : RefreshedWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LevelWidget()
}
