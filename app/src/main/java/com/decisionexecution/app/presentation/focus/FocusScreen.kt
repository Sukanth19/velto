package com.decisionexecution.app.presentation.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.presentation.theme.Spacing

fun formatSeconds(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/**
 * Full-screen focus timer with minimal UI. Break mode uses a different color and label.
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 12.6
 */
@Composable
fun FocusScreen(
    onGoHome: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is FocusUiState.Idle -> IdleContent(onGoHome)
            is FocusUiState.Focus -> TimerContent(
                label = if (s.isPaused) "Paused" else "Focus",
                title = s.taskTitle,
                remainingSeconds = s.remainingSeconds,
                totalSeconds = s.totalSeconds,
                color = MaterialTheme.colorScheme.primary
            ) {
                if (s.isPaused) {
                    Button(onClick = viewModel::onResume) { Text("Resume") }
                } else {
                    FilledTonalButton(onClick = viewModel::onPause) { Text("Pause") }
                }
                OutlinedButton(onClick = viewModel::onEnd) { Text("End") }
            }
            is FocusUiState.Break -> TimerContent(
                label = "Break",
                title = "Step away for a moment",
                remainingSeconds = s.remainingSeconds,
                totalSeconds = s.totalSeconds,
                color = MaterialTheme.colorScheme.tertiary
            ) {
                OutlinedButton(onClick = viewModel::onEnd) { Text("Skip break") }
            }
        }
    }
}

@Composable
private fun TimerContent(
    label: String,
    title: String,
    remainingSeconds: Int,
    totalSeconds: Int,
    color: Color,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = color)
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
            val progress = if (totalSeconds > 0) 1f - remainingSeconds.toFloat() / totalSeconds else 0f
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 8.dp
            )
            Text(formatSeconds(remainingSeconds), style = MaterialTheme.typography.displayLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), content = actions)
    }
}

@Composable
private fun IdleContent(onGoHome: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text("No active session", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Tap Just Start on Home to focus on your best task.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onGoHome) { Text("Go to Home") }
    }
}
