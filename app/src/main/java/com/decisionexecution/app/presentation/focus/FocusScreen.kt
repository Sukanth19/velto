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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.foundation.shape.RoundedCornerShape
import com.decisionexecution.app.domain.model.Subtask
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.usecase.ExtendFocusSessionUseCase
import com.decisionexecution.app.presentation.theme.Spacing

fun formatSeconds(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/**
 * Full-screen focus timer with minimal UI. Break mode uses its own colour, label and a slow
 * breathing animation so it never reads as focus time.
 */
@Composable
fun FocusScreen(
    onGoHome: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
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
                    is FocusUiState.Focus -> FocusContent(
                        s, steps, phaseColor, viewModel::onPause, viewModel::onResume, viewModel::onEnd, viewModel::onExtend, viewModel::onStepDone
                    )
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
    steps: List<Subtask>,
    color: Color,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onExtend: () -> Unit,
    onStepDone: (Subtask) -> Unit
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
        if (steps.isNotEmpty()) NextStepCard(steps, onStepDone)
        // The wave goes flat while paused, so the ring itself shows the timer has stopped
        TimerRing(state.remainingSeconds, state.totalSeconds, color, wavy = !state.isPaused)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnd()
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = "End session")
            }
            PlayPauseButton(
                running = !state.isPaused,
                onToggle = { run ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (run) onResume() else onPause()
                }
            )
            FilledTonalButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExtend()
                },
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = Spacing.md),
                modifier = Modifier.height(IconButtonDefaults.mediumContainerSize().height)
            ) {
                Text("+${ExtendFocusSessionUseCase.DEFAULT_MINUTES} min", maxLines = 1)
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
            // Longer, slower waves than focus: the break should feel calm
            TimerRing(
                state.remainingSeconds,
                state.totalSeconds,
                color,
                icon = Icons.Rounded.SelfImprovement,
                wavelength = 64.dp,
                waveSpeedFactor = 0.4f
            )
        }
        OutlinedButton(onClick = onSkip, shapes = ButtonDefaults.shapes()) {
            Icon(Icons.Rounded.SkipNext, contentDescription = null)
            Spacer(Modifier.width(Spacing.sm))
            Text("Skip break")
        }
    }
}

/** The next unticked step, so "Just Start" really starts with step one */
@Composable
private fun NextStepCard(steps: List<Subtask>, onDone: (Subtask) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val next = steps.firstOrNull { !it.isDone }
    val done = steps.count { it.isDone }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (next == null) "ALL STEPS DONE" else "STEP ${done + 1} OF ${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                AnimatedContent(targetState = next?.title, label = "step") { title ->
                    Text(
                        title ?: "Wrap up, or keep going",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (next != null) {
                FilledTonalIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDone(next)
                    },
                    shapes = IconButtonDefaults.shapes()
                ) { Icon(Icons.Rounded.Check, contentDescription = "Step done: ${next.title}") }
            }
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
 * Squiggly Material 3 Expressive progress ring. The sweep animates over each one-second tick so
 * it moves continuously instead of jumping, and the wave keeps travelling while time runs.
 */
@Composable
private fun TimerRing(
    remainingSeconds: Int,
    totalSeconds: Int,
    color: Color,
    icon: ImageVector? = null,
    wavy: Boolean = true,
    wavelength: androidx.compose.ui.unit.Dp = 44.dp,
    waveSpeedFactor: Float = 1f
) {
    val target = if (totalSeconds > 0) 1f - remainingSeconds.toFloat() / totalSeconds else 0f
    val progress by animateFloatAsState(target, animationSpec = tween(1000, easing = LinearEasing), label = "progress")
    // Ease the wave in and out rather than snapping between flat and wavy
    val amplitude by animateFloatAsState(if (wavy) WAVE_AMPLITUDE else 0f, animationSpec = tween(600), label = "amplitude")
    val strokeWidth = with(LocalDensity.current) { 10.dp.toPx() }
    val stroke = remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .semantics { contentDescription = "${formatSeconds(remainingSeconds)} remaining" }
    ) {
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = 10.dp,
            amplitude = { amplitude },
            wavelength = wavelength,
            waveSpeed = if (wavy) wavelength * waveSpeedFactor else 0.dp
        )
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

/** Fraction of the indicator's full wave height; full height looks noisy behind the digits */
private const val WAVE_AMPLITUDE = 0.75f

/**
 * Big play/pause toggle. Running it's a rounded square, paused it springs back to a circle,
 * following the expressive toggle shape convention. Filled either way, since it's the main action.
 */
@Composable
private fun PlayPauseButton(running: Boolean, onToggle: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FilledIconToggleButton(
        checked = running,
        onCheckedChange = onToggle,
        shapes = IconButtonDefaults.toggleableShapes(),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            checkedContainerColor = scheme.primary,
            checkedContentColor = scheme.onPrimary
        ),
        modifier = Modifier.size(IconButtonDefaults.largeContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
    ) {
        Icon(
            if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (running) "Pause" else "Resume",
            modifier = Modifier.size(IconButtonDefaults.largeIconSize)
        )
    }
}

@Composable
private fun IdleContent(onGoHome: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        // Slowly spinning cookie, the expressive take on an empty-state badge
        val spin by rememberInfiniteTransition(label = "spin").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
            label = "rotation"
        )
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(112.dp)
                    .rotate(spin)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
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
        Button(onClick = onGoHome, shapes = ButtonDefaults.shapes()) { Text("Go to Home") }
    }
}
