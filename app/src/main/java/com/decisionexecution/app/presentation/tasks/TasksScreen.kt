package com.decisionexecution.app.presentation.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.EisenhowerQuadrant
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.presentation.components.QuadrantBadge
import com.decisionexecution.app.presentation.components.SectionHeader
import com.decisionexecution.app.presentation.components.TaskAttributeRow
import com.decisionexecution.app.presentation.components.color
import com.decisionexecution.app.presentation.components.colors
import com.decisionexecution.app.presentation.components.hint
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.components.quadrant
import com.decisionexecution.app.presentation.theme.Spacing

/**
 * Tasks as a list grouped by category, or as a priority matrix.
 * Swipe right or tap the circle to complete; swipe left for edit and delete; both can be undone.
 * Requirements: 6.1, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 12.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onOpenTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onAddTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var actionsFor by remember { mutableStateOf<Task?>(null) }

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
    LaunchedEffect(uiState.undo) {
        uiState.undo?.let { action ->
            val verb = if (action is UndoAction.Completed) "Completed" else "Deleted"
            val result = snackbarHostState.showSnackbar(
                message = "$verb \"${action.task.title}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.onUndo() else viewModel.onUndoDismissed()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Tasks")
                        Text(
                            if (uiState.activeTaskCount == 1) "1 active" else "${uiState.activeTaskCount} active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = { ViewModeToggle(uiState.viewMode, viewModel::onViewModeChange) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Rounded.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CategoryFilter(selected = uiState.selectedCategory, onSelect = viewModel::onCategoryFilter)

            val sections = uiState.tasksByCategory.filterValues { it.isNotEmpty() }
            AnimatedContent(
                targetState = uiState.viewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "viewMode"
            ) { mode ->
                when {
                    !uiState.isLoading && sections.isEmpty() -> EmptyTasks(uiState.selectedCategory, onAddTask)
                    mode == TasksViewMode.MATRIX -> MatrixView(uiState.matrix, onOpenTask)
                    else -> TaskList(
                        sections = sections,
                        onComplete = viewModel::onTaskComplete,
                        onShowActions = { actionsFor = it },
                        onOpenTask = onOpenTask
                    )
                }
            }
        }
    }

    actionsFor?.let { task ->
        AlertDialog(
            onDismissRequest = { actionsFor = null },
            title = { Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = { actionsFor = null; onEditTask(task.id) }) { Text("Edit") }
            },
            dismissButton = {
                TextButton(
                    onClick = { actionsFor = null; viewModel.onTaskDelete(task.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        )
    }
}

@Composable
private fun ViewModeToggle(mode: TasksViewMode, onChange: (TasksViewMode) -> Unit) {
    Row(
        Modifier
            .padding(end = Spacing.sm)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.xs)
    ) {
        listOf(
            Triple(TasksViewMode.LIST, Icons.Rounded.ViewAgenda, "List view"),
            Triple(TasksViewMode.MATRIX, Icons.Rounded.GridView, "Priority matrix")
        ).forEach { (value, icon, description) ->
            val selected = mode == value
            val background by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "toggle"
            )
            IconButton(
                onClick = { onChange(value) },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(background)
            ) {
                Icon(
                    icon,
                    contentDescription = description,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilter(selected: TaskCategory?, onSelect: (TaskCategory?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") }, shape = CircleShape)
        TaskCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category.label) },
                shape = CircleShape,
                leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(category.color())) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskList(
    sections: Map<TaskCategory, List<Task>>,
    onComplete: (String) -> Unit,
    onShowActions: (Task) -> Unit,
    onOpenTask: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize()
    ) {
        sections.forEach { (category, tasks) ->
            item(key = "header-$category") {
                SectionHeader(
                    category.label,
                    tasks.size,
                    accent = category.color(),
                    modifier = Modifier
                        .animateItemPlacement()
                        .padding(top = Spacing.md, bottom = Spacing.xs)
                )
            }
            items(tasks, key = { it.id }) { task ->
                SwipeableTaskCard(
                    task = task,
                    onComplete = { onComplete(task.id) },
                    onShowActions = { onShowActions(task) },
                    onClick = { onOpenTask(task.id) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: Task,
    onComplete: () -> Unit,
    onShowActions: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnShowActions by rememberUpdatedState(onShowActions)
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            when (value) {
                DismissValue.DismissedToEnd -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnComplete(); true
                }
                // Left swipe only reveals actions; the card snaps back
                DismissValue.DismissedToStart -> { currentOnShowActions(); false }
                DismissValue.Default -> false
            }
        }
    )
    val shape = RoundedCornerShape(20.dp)

    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        background = {
            val toEnd = dismissState.dismissDirection == DismissDirection.StartToEnd
            val container = if (toEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
            val tint = if (toEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(container)
                    .padding(horizontal = Spacing.lg),
                contentAlignment = if (toEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(if (toEnd) Icons.Rounded.Check else Icons.Rounded.Edit, contentDescription = null, tint = tint)
                    Text(if (toEnd) "Done" else "Edit or delete", color = tint, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissContent = {
            Surface(
                shape = shape,
                // Opaque so the swipe background only shows once the card moves
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable(onClick = onClick)
                    // Swipes aren't reachable with TalkBack, so offer the same actions in its menu
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction("Complete task") { currentOnComplete(); true },
                            CustomAccessibilityAction("Edit or delete") { currentOnShowActions(); true }
                        )
                    }
            ) {
                Row(
                    Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    CompleteButton(task.category.color()) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete()
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TaskAttributeRow(task)
                    }
                    QuadrantBadge(task.quadrant)
                }
            }
        }
    )
}

/** Hollow circle in the category colour; tapping it completes the task. */
@Composable
private fun CompleteButton(color: Color, onClick: () -> Unit) {
    Box(
        // 48dp touch target around a 26dp circle
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = "Complete task", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(26.dp).border(2.dp, color, CircleShape))
    }
}

@Composable
private fun MatrixView(matrix: Map<EisenhowerQuadrant, List<Task>>, onOpenTask: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        listOf(
            EisenhowerQuadrant.URGENT_IMPORTANT to EisenhowerQuadrant.NOT_URGENT_IMPORTANT,
            EisenhowerQuadrant.URGENT_NOT_IMPORTANT to EisenhowerQuadrant.NEITHER
        ).forEach { (left, right) ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QuadrantCell(left, matrix[left].orEmpty(), onOpenTask, Modifier.weight(1f).fillMaxHeight())
                QuadrantCell(right, matrix[right].orEmpty(), onOpenTask, Modifier.weight(1f).fillMaxHeight())
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun QuadrantCell(
    quadrant: EisenhowerQuadrant,
    tasks: List<Task>,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (container, content) = quadrant.colors()
    Surface(modifier = modifier.heightIn(min = 180.dp), shape = RoundedCornerShape(24.dp), color = container, contentColor = content) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(quadrant.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(tasks.size.toString(), style = MaterialTheme.typography.titleMedium)
            }
            Text(quadrant.hint, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.7f))
            Spacer(Modifier.height(Spacing.xs))
            if (tasks.isEmpty()) {
                Text("Nothing here", style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.6f))
            }
            tasks.take(MATRIX_PREVIEW).forEach { task ->
                Text(
                    "• ${task.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenTask(task.id) }
                        .padding(vertical = Spacing.xs)
                )
            }
            if (tasks.size > MATRIX_PREVIEW) {
                Text("+${tasks.size - MATRIX_PREVIEW} more", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private const val MATRIX_PREVIEW = 4

@Composable
private fun EmptyTasks(selected: TaskCategory?, onAddTask: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(if (selected == null) "All clear" else "No ${selected.label} tasks", style = MaterialTheme.typography.titleLarge)
        Text(
            "Add something and it'll show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        FilledTonalButton(onClick = onAddTask) { Text("Add a task") }
    }
}
