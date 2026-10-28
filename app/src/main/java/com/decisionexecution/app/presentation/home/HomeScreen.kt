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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.presentation.components.QuadrantBadge
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.components.quadrant
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
 * Home: greeting, the next best task with one big Just Start, streak and today's stats.
 * Requirements: 12.4, 7.7
 */
@Composable
fun HomeScreen(
    onNavigateToFocus: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenInsights: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New task") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Header(streakDays = uiState.streakDays, onOpenSettings = onOpenSettings)

            AnimatedContent(
                targetState = uiState.isLoading to uiState.bestTask,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "hero"
            ) { (loading, task) ->
                when {
                    loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenInsights) { Text("See your week") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatTile(Icons.Rounded.CheckCircle, uiState.completedTasksToday.toString(), "Done today", Modifier.weight(1f))
                StatTile(Icons.Rounded.Timer, formatMinutes(uiState.totalFocusTimeToday), "Focus", Modifier.weight(1f))
                StatTile(Icons.Rounded.TrendingUp, "${uiState.consistencyScore.roundToInt()}%", "Consistency", Modifier.weight(1f))
            }
            // Room for the extended FAB
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun Header(streakDays: Int, onOpenSettings: () -> Unit) {
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
            if (streakDays > 0) {
                Spacer(Modifier.height(Spacing.sm))
                StreakChip(streakDays)
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
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
            Modifier.padding(start = Spacing.sm, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
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
            Button(
                onClick = onJustStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Just Start", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            FilledTonalButton(onClick = onAddTask) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Add your first task")
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        // Read as one item ("3, Done today") rather than two fragments
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(20.dp),
        // Material 3 1.1 has no surfaceContainerLow; a light tint of surfaceVariant reads the same
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
 * Requirements: 3.1, 8.6
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
