package com.decisionexecution.app.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.presentation.components.QuadrantBadge
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.components.quadrant
import com.decisionexecution.app.presentation.components.energyLabel
import com.decisionexecution.app.presentation.components.moodIcon
import com.decisionexecution.app.presentation.components.moodLabel
import com.decisionexecution.app.presentation.journal.MoodCheckInCard
import com.decisionexecution.app.presentation.progress.LevelBadge
import com.decisionexecution.app.presentation.theme.Spacing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Burning the midnight oil"
}

fun formatMinutes(minutes: Int): String =
    if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"

/**
 * Home: greeting with level and inbox, the next best task with one big Just Start, the mood
 * check-in, today's habits, daily goal and stats.
 */
@Composable
fun HomeScreen(
    onNavigateToFocus: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenInsights: () -> Unit = {},
    onOpenHabits: () -> Unit = {},
    onOpenInbox: () -> Unit = {},
    onOpenJournal: () -> Unit = {},
    onDecide: (minutes: Int) -> Unit = {},
    extrasViewModel: HomeExtrasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extras by extrasViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState.focusSessionStarted) {
        if (uiState.focusSessionStarted) {
            viewModel.onNavigatedToFocus()
            onNavigateToFocus()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            val retry = uiState.canRetry
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (retry) "Retry" else null,
                duration = if (retry) SnackbarDuration.Indefinite else SnackbarDuration.Short
            )
            viewModel.onErrorShown()
            if (retry && result == SnackbarResult.ActionPerformed) viewModel.onRetry()
        }
    }

    val scrollState = rememberScrollState()
    // Shrinks to a round + once you scroll, so it stops covering the cards underneath
    val fabExpanded by remember { derivedStateOf { scrollState.value < FAB_COLLAPSE_SCROLL } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                expanded = fabExpanded,
                icon = { Icon(Icons.Rounded.Add, contentDescription = "New task") },
                text = { Text("New task") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Header(
                streakDays = uiState.streakDays,
                extras = extras,
                onOpenSettings = onOpenSettings,
                onOpenInbox = onOpenInbox,
                onOpenProgress = onOpenInsights
            )

            AnimatedContent(
                targetState = uiState.isLoading to uiState.bestTask,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "hero"
            ) { (loading, task) ->
                when {
                    loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(64.dp))
                    }
                    task == null -> EmptyHero(onAddTask)
                    else -> HeroCard(
                        task = task,
                        onOpenTask = { onOpenTask(task.id) },
                        onJustStart = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onJustStartClicked()
                        }
                    )
                }
            }

            if (uiState.bestTask != null) PickForMeCard(onDecide)

            if (extras.moodLoaded) {
                val mood = extras.todayMood
                if (mood == null) {
                    MoodCheckInCard(existing = null, onSave = extrasViewModel::onSaveMood)
                } else {
                    MoodSummary(mood, onOpenJournal)
                }
            }
            HabitsSummary(extras.habitsDone, extras.habitsTotal, onOpenHabits)
            extras.plant?.let { PlantCard(it) }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenInsights) { Text("See your week") }
            }
            DailyGoalCard(focusMinutes = uiState.totalFocusTimeToday, goalMinutes = uiState.dailyFocusGoal)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatTile(Icons.Rounded.CheckCircle, uiState.completedTasksToday.toString(), "Done today", Modifier.weight(1f))
                StatTile(Icons.Rounded.Timer, formatMinutes(uiState.totalFocusTimeToday), "Focus", Modifier.weight(1f))
                StatTile(Icons.AutoMirrored.Rounded.TrendingUp, "${uiState.consistencyScore.roundToInt()}%", "Consistency", Modifier.weight(1f))
            }
            // Room for the extended FAB
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun Header(
    streakDays: Int,
    extras: HomeExtrasUiState,
    onOpenSettings: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenProgress: () -> Unit
) {
    val now = remember { LocalDateTime.now() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, MMM d")).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(greetingFor(now.hour), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(Spacing.sm))
            // Centred, because the tappable level chip reserves a 48dp touch target and the streak chip doesn't
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                extras.level?.let { LevelChip(it.level, it.title, onOpenProgress) }
                if (streakDays > 0) StreakChip(streakDays)
            }
        }
        BadgedBox(
            badge = { if (extras.inboxCount > 0) Badge { Text(extras.inboxCount.toString()) } }
        ) {
            IconButton(onClick = onOpenInbox, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Inbox, contentDescription = "Inbox, ${extras.inboxCount} items")
            }
        }
        IconButton(onClick = onOpenSettings, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun LevelChip(level: Int, title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT).padding(start = Spacing.xs, end = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            LevelBadge(level, Modifier.size(26.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MoodSummary(mood: com.decisionexecution.app.domain.model.MoodEntry, onOpenJournal: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onOpenJournal, shape = RoundedCornerShape(24.dp), color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(moodIcon(mood.mood), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Feeling ${moodLabel(mood.mood).lowercase()} · ${energyLabel(mood.energy).lowercase()}", style = MaterialTheme.typography.titleSmall)
                Text("Open your journal", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Can't decide: pick a time box and let Velto choose. */
@Composable
private fun PickForMeCard(onDecide: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Casino, contentDescription = null, tint = scheme.primary)
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text("Can't decide?", style = MaterialTheme.typography.titleSmall)
                    Text("Say how long you've got and Velto picks", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(15, 25, 45, 60).forEach { minutes ->
                    FilledTonalButton(
                        onClick = { onDecide(minutes) },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (minutes >= 60) "1h" else "${minutes}m") }
                }
            }
        }
    }
}

@Composable
private fun PlantCard(plant: com.decisionexecution.app.domain.service.PlantState) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.Bottom) {
            com.decisionexecution.app.presentation.components.PlantCompanion(plant, plantSize = 104.dp, talkative = false)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f).padding(bottom = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(plant.stage.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                Text(plant.message, style = MaterialTheme.typography.titleSmall)
                Text("Grows as you level up", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/** Today's habits as a small wavy ring; tapping goes to the Habits tab. */
@Composable
private fun HabitsSummary(done: Int, total: Int, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val progress by animateFloatAsState(fraction, WavyProgressIndicatorDefaults.ProgressAnimationSpec, label = "habits")
    Surface(onClick = onOpen, shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(52.dp),
                    color = scheme.tertiary,
                    trackColor = scheme.surfaceContainerHighest
                )
                Text(if (total == 0) "–" else "$done", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Habits", style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        total == 0 -> "Start one small daily habit"
                        done == total -> "All $total done today. Nice!"
                        else -> "$done of $total done today"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StreakChip(days: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT).padding(start = Spacing.xs, end = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(if (days == 1) "1-day streak" else "$days-day streak", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun HeroCard(task: Task, onOpenTask: () -> Unit, onJustStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer)))
            .clickable(onClick = onOpenTask)
            .padding(Spacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("UP NEXT", style = MaterialTheme.typography.labelSmall, color = scheme.onPrimaryContainer.copy(alpha = 0.7f))
                QuadrantBadge(task.quadrant)
                task.dueDate?.let { com.decisionexecution.app.presentation.components.DueChip(it, recurrence = task.recurrence) }
            }
            Text(
                task.title,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onPrimaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${task.category.label} · ${task.effort.label}" + (task.energyTag?.let { " · ${it.label}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(Spacing.sm))
            // Squishes to a squarer shape while pressed, then springs back
            Button(
                onClick = onJustStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shapes = ButtonDefaults.shapes(pressedShape = RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Just Start", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EmptyHero(onAddTask: () -> Unit) {
    OutlinedCard(
        onClick = onAddTask,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Nothing to do yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Add a task and Velto will pick the best one to start with.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            FilledTonalButton(onClick = onAddTask, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Add your first task")
            }
        }
    }
}

/**
 * Today's focus time against the daily goal, as a squiggly bar that flattens out once the goal is hit.
 */
@Composable
private fun DailyGoalCard(focusMinutes: Int, goalMinutes: Int) {
    val reached = goalMinutes > 0 && focusMinutes >= goalMinutes
    val target = if (goalMinutes > 0) (focusMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target, animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec, label = "goal")
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (reached) "Daily goal reached" else "Daily focus goal",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${formatMinutes(focusMinutes)} of ${formatMinutes(goalMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                if (reached) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = scheme.tertiary)
                } else {
                    Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                }
            }
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(14.dp),
                color = if (reached) scheme.tertiary else scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
                // A finished goal lies flat; work in progress wiggles
                amplitude = { if (reached) 0f else WavyProgressIndicatorDefaults.indicatorAmplitude(it) }
            )
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        // Read as one item ("3, Done today") rather than two fragments
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        // Three tiles share a phone's width, so keep the padding and type compact
        Column(Modifier.padding(Spacing.sm + Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Headless destination for the just_start deep link: starts the best task and moves on.
 */
@Composable
fun JustStartRoute(
    onStarted: () -> Unit,
    onFailed: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onJustStartClicked() }
    LaunchedEffect(uiState.focusSessionStarted, uiState.error) {
        when {
            uiState.focusSessionStarted -> {
                viewModel.onNavigatedToFocus()
                onStarted()
            }
            // Home shows its own empty state, so the error needs no extra surface here
            uiState.error != null -> onFailed()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(64.dp)) }
}

private const val FAB_COLLAPSE_SCROLL = 48

private val CHIP_HEIGHT = 34.dp
