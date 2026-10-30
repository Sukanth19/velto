package com.decisionexecution.app.presentation.taskdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.service.effectiveUrgency
import com.decisionexecution.app.presentation.components.DueChip
import com.decisionexecution.app.presentation.components.StepRow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.theme.Spacing
import java.text.DateFormat
import java.util.Date

/**
 * Shows every attribute of a task with edit, delete and complete actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    uiState.task?.let { task ->
                        IconButton(onClick = { onEdit(task.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val task = uiState.task
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            task == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Task not found")
            }
            else -> TaskDetailContent(task, uiState.steps, uiState.error, viewModel::onComplete, viewModel::onToggleStep, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; viewModel.onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TaskDetailContent(
    task: Task,
    steps: List<Subtask>,
    error: String?,
    onComplete: () -> Unit,
    onToggleStep: (String, Boolean) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(task.title, style = MaterialTheme.typography.headlineMedium)
        task.dueDate?.let { DueChip(it, recurrence = task.recurrence) }
        task.description?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (steps.isNotEmpty()) StepsCard(steps, onToggleStep)
        HorizontalDivider()
        task.recurrence?.let { DetailRow("Repeats", it.label) }
        DetailRow("Category", task.category.label)
        val urgencyNow = effectiveUrgency(task, LocalDate.now())
        // Show when a close due date is pushing it up
        DetailRow("Urgency", if (urgencyNow > task.urgency) "$urgencyNow / 5 (due soon)" else "${task.urgency} / 5")
        DetailRow("Importance", "${task.importance} / 5")
        DetailRow("Effort", task.effort.label)
        DetailRow("Energy", task.energyTag?.label ?: "Any")
        DetailRow("Created", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(task.createdAt)))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(Spacing.sm))
        Button(onClick = onComplete, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (task.recurrence != null) "Done, see you next time" else "Mark complete")
        }
    }
}

@Composable
private fun StepsCard(steps: List<Subtask>, onToggle: (String, Boolean) -> Unit) {
    val done = steps.count { it.isDone }
    val progress by animateFloatAsState(done.toFloat() / steps.size, WavyProgressIndicatorDefaults.ProgressAnimationSpec, label = "steps")
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Steps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$done of ${steps.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs))
            steps.forEach { step -> StepRow(step, onToggle = { onToggle(step.id, it) }) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
