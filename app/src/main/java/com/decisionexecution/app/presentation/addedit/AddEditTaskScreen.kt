package com.decisionexecution.app.presentation.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.service.dueLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.service.ESTIMATE_CHOICES
import com.decisionexecution.app.domain.service.EstimateCalibration
import com.decisionexecution.app.presentation.home.formatMinutes
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.theme.Spacing
import kotlin.math.roundToInt

/**
 * Form for creating or editing a task.
 * The spec calls for dropdowns; with only 2-3 options per field, chips need one tap instead of two.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onDone: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onDone()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit task" else "New task") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                },
                actions = { TextButton(onClick = viewModel::onSave) { Text("Save") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            ChoiceGroup("Category", TaskCategory.entries, uiState.category, { it.label }, viewModel::onCategoryChange)

            RatingSlider("Urgency", uiState.urgency, viewModel::onUrgencyChange)
            RatingSlider("Importance", uiState.importance, viewModel::onImportanceChange)

            ChoiceGroup(
                "Effort",
                TaskEffort.entries,
                uiState.effort,
                { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                viewModel::onEffortChange
            )

            EstimatePicker(uiState.estimateMinutes, uiState.calibration, viewModel::onEstimateChange)

            DueDatePicker(uiState.dueDate, viewModel::onDueDateChange)
            ChoiceGroup(
                "Repeat",
                listOf<Recurrence?>(null) + Recurrence.entries,
                uiState.recurrence,
                { it?.label ?: "Never" },
                viewModel::onRecurrenceChange
            )
            if (uiState.dueDate != null || uiState.recurrence != null) {
                CalendarToggle(uiState.calendarLinked, uiState.addToCalendar, viewModel::onAddToCalendarChange)
            }

            StepsEditor(
                steps = uiState.steps,
                draft = uiState.stepDraft,
                onDraftChange = viewModel::onStepDraftChange,
                onAdd = viewModel::onAddStep,
                onRemove = viewModel::onRemoveStep
            )

            ChoiceGroup(
                "Energy",
                listOf<EnergyTag?>(null) + EnergyTag.entries,
                uiState.energyTag,
                { it?.label ?: "Any" },
                viewModel::onEnergyTagChange
            )
        }
    }
}

/**
 * How long the user thinks it'll take. Once Velto knows how their guesses usually turn out, it
 * says what to really plan for.
 */
@Composable
private fun EstimatePicker(estimate: Int?, calibration: EstimateCalibration?, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ChoiceGroup(
            "How long will it take?",
            listOf<Int?>(null) + ESTIMATE_CHOICES,
            estimate,
            { it?.let(::formatMinutes) ?: "Not sure" },
            onChange
        )
        if (calibration != null && !calibration.isAccurate) {
            Text(
                calibration.summary + (estimate?.let { ", so plan for about ${formatMinutes(calibration.likelyMinutes(it))}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Put a task with a due date on the linked calendar, so the calendar app reminds about it.
 * Without a linked calendar it points to where to link one.
 */
@Composable
private fun CalendarToggle(linked: Boolean, checked: Boolean, onChange: (Boolean) -> Unit) {
    if (!linked) {
        Text(
            "Link Google Calendar in Settings to add tasks like this to your calendar with a reminder",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Add to Google Calendar", style = MaterialTheme.typography.labelLarge)
            Text(
                "An all-day event on the due date, with your reminder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Quick picks for the common cases, and a calendar for anything else */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DueDatePicker(due: LocalDate?, onChange: (LocalDate?) -> Unit) {
    val today = remember { LocalDate.now() }
    var showCalendar by remember { mutableStateOf(false) }
    val quick = listOf<Pair<String, LocalDate?>>(
        "None" to null,
        "Today" to today,
        "Tomorrow" to today.plusDays(1),
        "Next week" to today.plusWeeks(1)
    )
    val custom = due != null && quick.none { it.second == due }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Due", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            quick.forEach { (label, date) ->
                FilterChip(selected = due == date, onClick = { onChange(date) }, label = { Text(label) })
            }
            FilterChip(
                selected = custom,
                onClick = { showCalendar = true },
                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text(if (custom) dueLabel(due!!, today) else "Pick a date") }
            )
        }
    }
    if (showCalendar) {
        val zone = ZoneOffset.UTC
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: today).atStartOfDay(zone).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                // Due dates in the past aren't useful to set
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate().isBefore(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) }
                    showCalendar = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Cancel") } }
        ) { DatePicker(state) }
    }
}

/** Break the task into steps; Focus shows the next one while you work */
@Composable
private fun StepsEditor(
    steps: List<Subtask>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Steps", style = MaterialTheme.typography.labelLarge)
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                Text(step.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(step.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove step ${step.title}")
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text(if (steps.isEmpty()) "First step, e.g. open the doc" else "Next step") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            trailingIcon = {
                IconButton(onClick = onAdd, enabled = draft.isNotBlank()) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add step")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

@Composable
private fun RatingSlider(title: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text("$value / 5", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3
        )
    }
}
