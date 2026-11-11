package com.decisionexecution.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import android.os.Build
import com.decisionexecution.app.domain.model.ThemePalette
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.ui.draw.alpha
import com.decisionexecution.app.domain.service.PlantHealth
import com.decisionexecution.app.domain.service.PlantStage
import com.decisionexecution.app.domain.service.PlantState
import com.decisionexecution.app.domain.service.PlantVariety
import com.decisionexecution.app.domain.service.isUnlocked
import com.decisionexecution.app.domain.service.nextReward
import com.decisionexecution.app.domain.service.unlockLevel
import com.decisionexecution.app.presentation.components.PlantCompanion
import com.decisionexecution.app.domain.model.FocusSound
import com.decisionexecution.app.presentation.theme.label
import com.decisionexecution.app.presentation.theme.swatch
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
import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.model.CalendarSettings
import com.decisionexecution.app.domain.model.DeviceCalendar
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
 * Focus/break durations, daily focus goal, appearance (palette, light/dark, pure black), reminders and backup. Sliders only save when released, not on every drag step.
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
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) viewModel.onCalendarAccessGranted() else viewModel.onCalendarAccessDenied()
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
            SectionTitle("Appearance")
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Colours", style = MaterialTheme.typography.labelLarge)
                PalettePicker(uiState.themePalette, uiState.level, viewModel::onThemePaletteChanged)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Plant", style = MaterialTheme.typography.labelLarge)
                PlantPicker(uiState.plantVariety, uiState.level, viewModel::onPlantVarietyChanged)
            }
            nextReward(uiState.level)?.let { next ->
                Text(
                    "Level ${next.level} unlocks ${next.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Mode", style = MaterialTheme.typography.labelLarge)
                ThemePicker(uiState.themeMode, viewModel::onThemeModeChanged)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pure black", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "True black backgrounds in dark mode, easier on OLED screens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Switch(
                    checked = uiState.pureBlack,
                    onCheckedChange = viewModel::onPureBlackChanged,
                    enabled = uiState.themeMode != ThemeMode.LIGHT
                )
            }

            SectionTitle("Focus")
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

            FocusSoundSetting(uiState.focusSound, viewModel::onFocusSoundChanged)

            DndSetting(enabled = uiState.dndDuringFocus, onChange = viewModel::onDndDuringFocusChanged)

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

            SectionTitle("Google Calendar")
            CalendarSetting(
                settings = uiState.calendar,
                error = uiState.calendarError,
                onLink = { calendarPermission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) },
                onUnlink = viewModel::onUnlinkCalendar,
                onReminderChange = viewModel::onCalendarReminderChanged
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

    uiState.calendarChoices?.let { calendars ->
        CalendarPickerDialog(calendars, onPick = viewModel::onCalendarPicked, onDismiss = viewModel::onCalendarPickerDismissed)
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

/**
 * Link a calendar on the phone (Google Calendar syncs it to the account) so tasks with due dates
 * can go on it, and pick when it reminds about them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarSetting(
    settings: CalendarSettings,
    error: String?,
    onLink: () -> Unit,
    onUnlink: () -> Unit,
    onReminderChange: (CalendarReminder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val calendar = settings.calendar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (calendar != null) Icons.Rounded.EventAvailable else Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(if (calendar != null) "Linked to ${calendar.name}" else "Not linked", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (calendar != null) calendar.accountName
                    else "Add tasks with a due date to your calendar, so it reminds you before tests and deadlines",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilledTonalButton(onClick = onLink, shapes = ButtonDefaults.shapes()) {
                Text(if (calendar != null) "Change calendar" else "Link Google Calendar")
            }
            if (calendar != null) OutlinedButton(onClick = onUnlink, shapes = ButtonDefaults.shapes()) { Text("Unlink") }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (calendar != null) {
            Text("Remind me", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CalendarReminder.entries.forEach { reminder ->
                    FilterChip(
                        selected = reminder == settings.reminder,
                        onClick = { onReminderChange(reminder) },
                        label = { Text(reminder.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarPickerDialog(calendars: List<DeviceCalendar>, onPick: (DeviceCalendar) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a calendar") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                calendars.forEach { calendar ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(calendar) }
                            .padding(vertical = Spacing.sm, horizontal = Spacing.xs)
                    ) {
                        Text(calendar.name, style = MaterialTheme.typography.titleSmall)
                        if (calendar.accountName != calendar.name) {
                            Text(
                                calendar.accountName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.sm))
}

/** Background sound while focusing; generated on the phone, so nothing to download */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusSoundSetting(selected: FocusSound, onSelect: (FocusSound) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Focus sound", style = MaterialTheme.typography.labelLarge)
        Text(
            "Plays while you focus and stops for breaks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FocusSound.entries.forEach { sound ->
                FilterChip(selected = sound == selected, onClick = { onSelect(sound) }, label = { Text(sound.label) })
            }
        }
    }
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

/**
 * A row of colour swatches, one per palette. Picking one recolours the app straight away, so the
 * screen itself is the preview. The wallpaper option only exists where Material You does (Android 12+).
 */
@Composable
private fun PalettePicker(selected: ThemePalette, level: Int, onSelect: (ThemePalette) -> Unit) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val wallpaper = remember(dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            listOf(scheme.primary, scheme.primaryContainer, scheme.tertiary)
        } else null
    }
    val palettes = ThemePalette.entries.filter { it != ThemePalette.WALLPAPER || wallpaper != null }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        palettes.forEach { palette ->
            val colors = if (palette == ThemePalette.WALLPAPER) wallpaper!! else palette.swatch(dark)
            PaletteSwatch(palette.label, colors, selected == palette, lockedUntil = palette.unlockLevel.takeIf { it > level }) { onSelect(palette) }
        }
    }
}

/** Companion plants: tap one to plant it on Home. Locked ones show the level they come at. */
@Composable
private fun PlantPicker(selected: PlantVariety, level: Int, onSelect: (PlantVariety) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        PlantVariety.entries.forEach { variety ->
            val locked = !variety.isUnlocked(level)
            val isSelected = variety == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(selected = isSelected, enabled = !locked, role = Role.RadioButton) { onSelect(variety) }
            ) {
                Column(
                    Modifier.padding(Spacing.sm).alpha(if (locked) 0.45f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PlantCompanion(
                            PlantState(PlantStage.BLOOM, PlantHealth.THRIVING, 0, variety),
                            plantSize = 64.dp,
                            talkative = false
                        )
                        if (locked) Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        if (locked) "Level ${variety.unlockLevel}" else variety.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(label: String, colors: List<Color>, selected: Boolean, lockedUntil: Int? = null, onClick: () -> Unit) {
    // The picked swatch squircles and grows a ring, matching the expressive shape morphs elsewhere
    val corner by animateDpAsState(if (selected) 18.dp else 28.dp, label = "swatchCorner")
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, label = "swatchRing")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, enabled = lockedUntil == null, role = Role.RadioButton, onClick = onClick)
            .padding(Spacing.xs)
            .alpha(if (lockedUntil != null) 0.45f else 1f)
    ) {
        Box(
            Modifier
                .size(56.dp)
                // A zero-width border still draws a hairline, so leave it off entirely when unselected
                .then(if (ring > 0.dp) Modifier.border(ring, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(corner + 4.dp)) else Modifier)
                .padding(5.dp)
                .clip(RoundedCornerShape(corner)),
            contentAlignment = Alignment.Center
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(colors[0]))
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.fillMaxHeight().weight(1f).background(colors[1]))
                    Box(Modifier.fillMaxHeight().weight(1f).background(colors[2]))
                }
            }
            if (selected || lockedUntil != null) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (lockedUntil != null) Icons.Rounded.Lock else Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            lockedUntil?.let { "Level $it" } ?: label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
