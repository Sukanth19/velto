package com.decisionexecution.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import android.text.format.DateFormat
import com.decisionexecution.app.domain.model.Reminder
import com.decisionexecution.app.domain.service.formatMinuteOfDay
import java.time.LocalDate
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.presentation.home.formatMinutes
import com.decisionexecution.app.presentation.theme.Spacing
import kotlin.math.roundToInt

/**
 * Focus/break durations, daily focus goal and theme. Sliders only save when released, not on every drag step.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.onExport(it, context.contentResolver) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }
    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onBackupMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            DurationSlider(
                title = "Focus duration",
                value = uiState.focusDuration,
                range = FOCUS_DURATION_RANGE,
                onCommit = viewModel::onFocusDurationChanged
            )
            DurationSlider(
                title = "Break duration",
                value = uiState.breakDuration,
                range = BREAK_DURATION_RANGE,
                onCommit = viewModel::onBreakDurationChanged
            )
            DurationSlider(
                title = "Daily focus goal",
                value = uiState.dailyFocusGoal,
                range = DAILY_FOCUS_GOAL_RANGE,
                // Quarter-hour steps; nobody sets a goal of 97 minutes
                stepMinutes = 15,
                format = ::formatMinutes,
                onCommit = viewModel::onDailyFocusGoalChanged
            )
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            DndSetting(enabled = uiState.dndDuringFocus, onChange = viewModel::onDndDuringFocusChanged)

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                ThemePicker(uiState.themeMode, viewModel::onThemeModeChanged)
            }

            SectionTitle("Reminders")
            ReminderSetting(
                title = "Habits left",
                description = "An evening nudge, only if habits are still unchecked",
                reminder = uiState.habitReminder,
                onChange = viewModel::onHabitReminderChanged
            )
            ReminderSetting(
                title = "Mood check-in",
                description = "A morning nudge, skipped once you've logged today",
                reminder = uiState.moodReminder,
                onChange = viewModel::onMoodReminderChanged
            )
            ReminderSetting(
                title = "Week in review",
                description = "Sunday evening, when there's a week to look back on",
                reminder = uiState.weeklyReviewReminder,
                onChange = viewModel::onWeeklyReviewReminderChanged
            )

            SectionTitle("Your data")
            Text(
                "Everything stays on this phone and is included in your Android backup. " +
                    "Export a file to keep a copy yourself or move to a phone without Google backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch("velto-backup-${LocalDate.now()}.json") },
                    enabled = !uiState.backupBusy,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Export")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    enabled = !uiState.backupBusy,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Import")
                }
            }
            if (uiState.backupBusy) LinearWavyProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.lg))
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace everything?") },
            text = { Text("Your tasks, habits, journal and progress on this phone will be replaced by the backup. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onImport(uri, context.contentResolver)
                        pendingImport = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.sm))
}

/** A daily reminder: on/off, and a time that opens a picker when tapped. */
@Composable
private fun ReminderSetting(title: String, description: String, reminder: Reminder, onChange: (Reminder) -> Unit) {
    val context = LocalContext.current
    val use24Hour = remember { DateFormat.is24HourFormat(context) }
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { picking = true }, enabled = reminder.enabled) {
            Text(formatMinuteOfDay(reminder.minuteOfDay, use24Hour))
        }
        Switch(checked = reminder.enabled, onCheckedChange = { onChange(reminder.copy(enabled = it)) })
    }
    if (picking) {
        val state = rememberTimePickerState(reminder.minuteOfDay / 60, reminder.minuteOfDay % 60, use24Hour)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(title) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(reminder.copy(minuteOfDay = state.hour * 60 + state.minute))
                    picking = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } }
        )
    }
}

/**
 * Do Not Disturb while focusing. Android needs the user to grant DND access in system settings,
 * so turning it on without access sends them there first.
 */
@Composable
private fun DndSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val notificationManager = remember { context.getSystemService(NotificationManager::class.java) }
    var hasAccess by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    // Re-check when coming back from the system settings screen
    LifecycleResumeEffect(Unit) {
        hasAccess = notificationManager.isNotificationPolicyAccessGranted
        onPauseOrDispose { }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Silence notifications while focusing", style = MaterialTheme.typography.labelLarge)
            Text(
                if (enabled && !hasAccess) "Needs Do Not Disturb access. Tap the switch to grant it."
                else "Turns on Do Not Disturb during focus and back off for breaks",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled && !hasAccess) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(
            checked = enabled && hasAccess,
            onCheckedChange = { on ->
                if (on && !hasAccess) {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
                onChange(on)
            }
        )
    }
}

/** Connected button group: the picked option morphs into a pill. */
@Composable
private fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
        Triple(ThemeMode.SYSTEM, "System", Icons.Rounded.BrightnessAuto)
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            ToggleButton(
                checked = selected == mode,
                onCheckedChange = { onSelect(mode) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                // Three to a row on a phone; the default padding wraps "System"
                contentPadding = PaddingValues(horizontal = Spacing.sm)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DurationSlider(
    title: String,
    value: Int,
    range: IntRange,
    onCommit: (Int) -> Unit,
    stepMinutes: Int = 1,
    format: (Int) -> String = { "$it min" }
) {
    // Local value while dragging; reset whenever the stored value changes
    var dragValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                format(dragValue.roundToInt()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = dragValue,
            onValueChange = { dragValue = (it / stepMinutes).roundToInt() * stepMinutes.toFloat() },
            onValueChangeFinished = { onCommit(dragValue.roundToInt()) },
            // Continuous track and rounding; per-minute tick marks are too dense to read
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}
