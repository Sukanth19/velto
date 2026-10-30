package com.decisionexecution.app.presentation.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.HabitIcon
import com.decisionexecution.app.presentation.components.ConfettiBurst
import com.decisionexecution.app.presentation.components.badgeShape
import com.decisionexecution.app.presentation.components.imageVector
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Habits: one tap to check off today, streaks, the week at a glance and a 12-week heatmap.
 * Long-press a habit to delete it.
 */
@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HabitRow?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Box {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text("Habits")
                            if (uiState.habits.isNotEmpty()) {
                                Text(
                                    "${uiState.doneToday} of ${uiState.habits.size} done today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add habit")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
                uiState.habits.isEmpty() -> EmptyHabits(Modifier.padding(padding)) { adding = true }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    item(key = "heatmap") { HeatmapCard(uiState.heatmap) }
                    items(uiState.habits, key = { it.habit.id }) { row ->
                        HabitCard(
                            row = row,
                            onToggle = { viewModel.onToggle(row.habit.id, it) },
                            onLongPress = { deleting = row },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
        ConfettiBurst(uiState.celebrateKey)
    }

    if (adding) {
        AddHabitDialog(
            onDismiss = { adding = false },
            onAdd = { name, icon ->
                viewModel.onAdd(name, icon)
                adding = false
            }
        )
    }
    deleting?.let { row ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${row.habit.name}\"?") },
            text = { Text("Its streak and history go with it.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onDelete(row.habit.id); deleting = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    row: HabitRow,
    onToggle: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val badge by animateColorAsState(if (row.doneToday) scheme.primary else scheme.primaryContainer, label = "badge")
    val badgeContent by animateColorAsState(if (row.doneToday) scheme.onPrimary else scheme.onPrimaryContainer, label = "badgeContent")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = { onToggle(!row.doneToday) }, onLongClick = onLongPress),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerLow
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                Modifier.size(52.dp).clip(row.habit.icon.badgeShape.toShape()).background(badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(row.habit.icon.imageVector, contentDescription = null, tint = badgeContent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(row.habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (row.streak > 0) scheme.tertiary else scheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (row.streak == 1) "1 day" else "${row.streak} days",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                WeekStrip(row.week)
            }
            FilledIconToggleButton(
                checked = row.doneToday,
                onCheckedChange = onToggle,
                shapes = IconButtonDefaults.toggleableShapes(),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = if (row.doneToday) "Mark ${row.habit.name} not done" else "Mark ${row.habit.name} done"
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(week: List<Boolean>) {
    val today = LocalDate.now()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        week.forEachIndexed { index, done ->
            val day = today.minusDays((week.size - 1 - index).toLong())
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** GitHub-style grid: one column per week, darker squares for days with more habits done. */
@Composable
private fun HeatmapCard(heatmap: List<Float>) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Last 12 weeks", style = MaterialTheme.typography.titleSmall)
            val weeks = heatmap.chunked(7)
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = "Habit activity over the last 12 weeks" },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { share ->
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        if (share == 0f) scheme.surfaceContainerHighest
                                        else scheme.primary.copy(alpha = 0.25f + 0.75f * share)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHabits(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).clip(MaterialShapes.Flower.toShape()).background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text("Build a streak", style = MaterialTheme.typography.titleLarge)
        Text(
            "Small things every day: drink water, read, stretch. Check them off and watch the grid fill up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.md))
        Button(onClick = onAdd, shapes = ButtonDefaults.shapes()) { Text("Add a habit") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onAdd: (String, HabitIcon) -> Unit, initialName: String = "") {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(HabitIcon.SPARK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Drink water") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    HabitIcon.entries.forEach { option ->
                        FilledTonalIconToggleButton(
                            checked = icon == option,
                            onCheckedChange = { icon = option },
                            shapes = IconButtonDefaults.toggleableShapes()
                        ) {
                            Icon(option.imageVector, contentDescription = option.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, icon) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
