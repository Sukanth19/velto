package com.decisionexecution.app.presentation.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.usecase.DayStats
import com.decisionexecution.app.domain.usecase.Insights
import com.decisionexecution.app.domain.usecase.Progress
import com.decisionexecution.app.presentation.progress.AchievementGrid
import com.decisionexecution.app.presentation.progress.LevelCard
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.decisionexecution.app.presentation.home.formatMinutes
import com.decisionexecution.app.presentation.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Level and achievements, a way into the journal, then the last 7 days: streak, totals,
 * a focus-time chart and completed tasks per day.
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    onOpenJournal: () -> Unit = {},
    onOpenReview: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { LargeTopAppBar(title = { Text("Progress") }) }) { padding ->
        val insights = uiState.insights
        when {
            insights != null -> InsightsContent(insights, uiState.progress, uiState.plant, onOpenJournal, onOpenReview, Modifier.padding(padding))
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, textAlign = TextAlign.Center, modifier = Modifier.padding(Spacing.xl))
            }
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun InsightsContent(
    insights: Insights,
    progress: Progress?,
    plant: com.decisionexecution.app.domain.service.PlantState?,
    onOpenJournal: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Tap it: it bounces and tells you how it's doing
        plant?.let {
            com.decisionexecution.app.presentation.components.PlantCompanion(
                it,
                plantSize = 180.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        progress?.let { LevelCard(it.level) }
        LinkCard(Icons.Rounded.CalendarViewWeek, "Week in review", "Wins, trends and one thing to try", onOpenReview)
        LinkCard(Icons.AutoMirrored.Rounded.MenuBook, "Mood & energy journal", "See what fuels your best days", onOpenJournal)
        progress?.let { AchievementGrid(it.unlocked) }
        Text("Your week", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Spacing.sm))
        StreakCard(insights.streakDays)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TotalTile(Icons.Rounded.Timer, formatMinutes(insights.weekFocusMinutes), "Focused this week", Modifier.weight(1f))
            TotalTile(Icons.Rounded.CheckCircle, insights.weekCompletedTasks.toString(), "Tasks done", Modifier.weight(1f))
        }
        ChartCard(insights.week)
        insights.bestDay?.let { best ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text("Best day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            "${dayName(best.date, TextStyle.FULL)}: ${formatMinutes(best.focusMinutes)} focus, ${best.completedTasks} done",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun LinkCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun StreakCard(days: Int) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(MaterialShapes.SoftBurst.toShape()).background(scheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = scheme.onTertiary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    if (days == 1) "1 day" else "$days days",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    when {
                        days == 0 -> "Finish a task or a focus session to start a streak"
                        days < 3 -> "Current streak. Keep it going!"
                        else -> "Current streak. You're on a roll"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TotalTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(week: List<DayStats>) {
    val maxMinutes = (week.maxOfOrNull { it.focusMinutes } ?: 0).coerceAtLeast(30)
    val today = LocalDate.now()
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("Focus time", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                week.forEach { day ->
                    Bar(day, maxMinutes, isToday = day.date == today, modifier = Modifier.weight(1f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Text("Tasks done", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val active = day.completedTasks > 0
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .semantics { contentDescription = "${dayName(day.date, TextStyle.FULL)}: ${day.completedTasks} done" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.completedTasks.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bar(day: DayStats, maxMinutes: Int, isToday: Boolean, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(day.focusMinutes.toFloat() / maxMinutes, tween(700), label = "bar")
    val color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    Column(
        modifier.fillMaxHeight().semantics(mergeDescendants = true) {
            contentDescription = "${dayName(day.date, TextStyle.FULL)}: ${formatMinutes(day.focusMinutes)} focus"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (day.focusMinutes > 0) {
            Text(formatMinutes(day.focusMinutes), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Spacer(Modifier.height(Spacing.xs))
        }
        Box(
            Modifier
                .width(22.dp)
                .fillMaxHeight(fraction.coerceIn(0.02f, 1f) * 0.8f)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(color)
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (isToday) "Today" else dayName(day.date, TextStyle.SHORT),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun dayName(date: LocalDate, style: TextStyle) = date.dayOfWeek.getDisplayName(style, Locale.getDefault())
