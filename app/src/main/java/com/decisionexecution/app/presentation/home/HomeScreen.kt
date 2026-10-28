package com.decisionexecution.app.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.presentation.components.TaskAttributeRow
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.theme.Spacing
import kotlin.math.roundToInt

/**
 * Home: one big "Just Start" button, the next best task, and minimal stats.
 * Requirements: 12.4, 7.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFocus: () -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
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
            Spacer(Modifier.height(Spacing.sm))

            Button(
                onClick = viewModel::onJustStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Just Start", style = MaterialTheme.typography.headlineSmall)
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                BestTaskCard(task = uiState.bestTask, onOpenTask = onOpenTask, onAddTask = onAddTask)
            }

            ProductivityStats(
                completedToday = uiState.completedTasksToday,
                focusMinutesToday = uiState.totalFocusTimeToday,
                consistencyScore = uiState.consistencyScore
            )
        }
    }
}

@Composable
private fun BestTaskCard(task: Task?, onOpenTask: (String) -> Unit, onAddTask: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            "Next best task",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (task == null) {
            OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onAddTask)) {
                Column(Modifier.padding(Spacing.md)) {
                    Text("Nothing to do yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Add a task and it will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onOpenTask(task.id) }) {
                Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        task.category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    TaskAttributeRow(task)
                }
            }
        }
    }
}

@Composable
private fun ProductivityStats(completedToday: Int, focusMinutesToday: Int, consistencyScore: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatTile("Done today", completedToday.toString(), Modifier.weight(1f))
        StatTile("Focus", "${focusMinutesToday}m", Modifier.weight(1f))
        StatTile("Consistency", "${consistencyScore.roundToInt()}%", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        // Read as one item ("3, Done today") rather than two fragments
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
