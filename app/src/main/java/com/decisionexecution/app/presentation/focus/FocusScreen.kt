package com.decisionexecution.app.presentation.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * Full-screen focus timer with minimal UI. Break mode uses its own colour, label and a slow
 * breathing animation so it never reads as focus time.
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 12.6
 */
@Composable
fun FocusScreen(
    onGoHome: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phaseColor = when (val s = state) {
        is FocusUiState.Break -> MaterialTheme.colorScheme.tertiary
        is FocusUiState.Focus -> if (s.isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
        FocusUiState.Idle -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Soft glow of the phase colour behind the ring
            .background(
                Brush.radialGradient(
                    listOf(phaseColor.copy(alpha = 0.16f), Color.Transparent),
                    radius = 1400f
                )
            )
            .statusBarsPadding()
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state::class,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phase"
        ) { phase ->
            when (phase) {
                FocusUiState.Idle::class -> IdleContent(onGoHome)
                else -> when (val s = state) {
                    is FocusUiState.Focus -> FocusContent(s, phaseColor, viewModel::onPause, viewModel::onResume, viewModel::onEnd)
                    is FocusUiState.Break -> BreakContent(s, phaseColor, viewModel::onEnd)
                    FocusUiState.Idle -> IdleContent(onGoHome)
                }
            }
        }
    }
}

@Composable
private fun FocusContent(
    state: FocusUiState.Focus,
    color: Color,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        PhaseLabel(if (state.isPaused) "Paused" else "Focus", color)
        Text(
            state.taskTitle,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        TimerRing(state.remainingSeconds, state.totalSeconds, color)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            RoundAction(Icons.Rounded.Stop, "End session", filled = false) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onEnd()
            }
            if (state.isPaused) {
                RoundAction(Icons.Rounded.PlayArrow, "Resume", filled = true, large = true) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onResume()
                }
            } else {
                RoundAction(Icons.Rounded.Pause, "Pause", filled = true, large = true) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPause()
                }
            }
        }
    }
}

@Composable
private fun BreakContent(state: FocusUiState.Break, color: Color, onSkip: () -> Unit) {
    val breathing = rememberInfiniteTransition(label = "breathing")
    // One slow in-and-out cycle, roughly a calm breath
    val scale by breathing.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        PhaseLabel("Break", color)
        Text("Breathe in… breathe out", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Box(Modifier.scale(scale)) {
            TimerRing(state.remainingSeconds, state.totalSeconds, color, icon = Icons.Rounded.SelfImprovement)
        }
        OutlinedButton(onClick = onSkip) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text("Skip break")
        }
    }
}

@Composable
private fun PhaseLabel(text: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f), contentColor = color) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
    }
}

/**
 * Progress ring drawn by hand for rounded ends. The sweep animates over each one-second tick
 * so it moves continuously instead of jumping.
 */
@Composable
private fun TimerRing(remainingSeconds: Int, totalSeconds: Int, color: Color, icon: ImageVector? = null) {
    val target = if (totalSeconds > 0) 1f - remainingSeconds.toFloat() / totalSeconds else 0f
    val progress by animateFloatAsState(target, animationSpec = tween(1000, easing = LinearEasing), label = "progress")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .semantics { contentDescription = "${formatSeconds(remainingSeconds)} remaining" }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(32.dp)) }
            Text(formatSeconds(remainingSeconds), style = MaterialTheme.typography.displayLarge)
            Text(
                "of ${totalSeconds / 60} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    filled: Boolean,
    large: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (large) 80.dp else 60.dp
    if (filled) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(size)) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(if (large) 36.dp else 26.dp))
        }
    } else {
        OutlinedIconButton(onClick = onClick, modifier = Modifier.size(size).clip(CircleShape)) {
            Icon(icon, contentDescription = description)
        }
    }
}

@Composable
private fun IdleContent(onGoHome: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Text("Ready when you are", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Tap Just Start on Home and Velto picks the best task for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onGoHome) { Text("Go to Home") }
    }
}
