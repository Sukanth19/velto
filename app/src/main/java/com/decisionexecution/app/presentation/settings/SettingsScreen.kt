package com.decisionexecution.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.presentation.theme.Spacing
import kotlin.math.roundToInt

/**
 * Focus/break durations and theme. Sliders only save when released, not on every drag step.
 * Requirements: 11.1, 11.2, 11.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
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
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Column {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.onThemeModeChanged(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.themeMode == mode, onClick = null)
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            when (mode) {
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.SYSTEM -> "Follow system"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationSlider(title: String, value: Int, range: IntRange, onCommit: (Int) -> Unit) {
    // Local value while dragging; reset whenever the stored value changes
    var dragValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                "${dragValue.roundToInt()} min",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = { onCommit(dragValue.roundToInt()) },
            // Continuous track and rounding; per-minute tick marks are too dense to read
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}
