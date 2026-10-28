package com.decisionexecution.app.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.presentation.components.TaskAttributeRow
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.theme.Spacing

/**
 * Tasks grouped by category with swipe actions and a category filter.
 * Swipe right completes; swipe left offers edit and delete.
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 12.5
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
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tasks · ${uiState.activeTaskCount} active") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CategoryFilter(selected = uiState.selectedCategory, onSelect = viewModel::onCategoryFilter)

            val sections = uiState.tasksByCategory.filterValues { it.isNotEmpty() }
            if (!uiState.isLoading && sections.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                    Text(
                        if (uiState.selectedCategory == null) "No active tasks. Tap + to add one."
                        else "No ${uiState.selectedCategory!!.label} tasks.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    sections.forEach { (category, tasks) ->
                        item(key = "header-$category") {
                            Text(
                                "${category.label} (${tasks.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xs
                                )
                            )
                        }
                        items(tasks, key = { it.id }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                onComplete = { viewModel.onTaskComplete(task.id) },
                                onShowActions = { actionsFor = task },
                                onClick = { onOpenTask(task.id) }
                            )
                        }
                    }
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
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        TaskCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: Task,
    onComplete: () -> Unit,
    onShowActions: () -> Unit,
    onClick: () -> Unit
) {
    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnShowActions by rememberUpdatedState(onShowActions)
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            when (value) {
                DismissValue.DismissedToEnd -> { currentOnComplete(); true }
                // Left swipe only reveals actions; the row snaps back
                DismissValue.DismissedToStart -> { currentOnShowActions(); false }
                DismissValue.Default -> false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            val toEnd = dismissState.dismissDirection == DismissDirection.StartToEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (toEnd) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = Spacing.lg),
                contentAlignment = if (toEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (toEnd) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (toEnd) "Complete" else "Edit or delete"
                )
            }
        },
        dismissContent = {
            ListItem(
                modifier = Modifier.clickable(onClick = onClick),
                headlineContent = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { TaskAttributeRow(task, Modifier.padding(top = Spacing.xs)) }
            )
        }
    )
}
