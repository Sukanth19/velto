package com.decisionexecution.app.presentation.decide

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.presentation.components.ConfettiBurst
import com.decisionexecution.app.presentation.components.QuadrantBadge
import com.decisionexecution.app.presentation.components.energyLabel
import com.decisionexecution.app.presentation.components.label
import com.decisionexecution.app.presentation.components.quadrant
import com.decisionexecution.app.presentation.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Pick for me: choose a time box (and optionally energy), watch the reel spin through your best
 * fits, then start a focus session of exactly that length.
 */
@Composable
fun DecideScreen(
    onBack: () -> Unit,
    onStarted: () -> Unit,
    viewModel: DecideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState.started) {
        if (uiState.started) {
            viewModel.onStartedHandled()
            onStarted()
        }
    }

    // The reel: flick through candidates, slowing down, then land on the pick
    var shown by remember { mutableStateOf<Task?>(null) }
    var spinning by remember { mutableStateOf(false) }
    val landed = remember { Animatable(1f) }
    LaunchedEffect(uiState.spinKey) {
        val pick = uiState.pick ?: run { shown = null; return@LaunchedEffect }
        val reel = uiState.ranked.take(REEL_SIZE)
        spinning = true
        if (reel.size > 1) {
            var wait = 45L
            repeat(SPIN_STEPS) { step ->
                shown = reel[(uiState.pickIndex + step + 1) % reel.size]
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                delay(wait)
                wait = (wait * 1.18).toLong()
            }
        }
        shown = pick
        spinning = false
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        landed.snapTo(0.85f)
        landed.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pick for me") },
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
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                OptionGroup(
                    title = "How long have you got?",
                    options = DecideViewModel.TIME_OPTIONS,
                    selected = uiState.minutes,
                    label = { if (it >= 60) "${it / 60}h" else "${it}m" },
                    onSelect = viewModel::onMinutesChange
                )
                OptionGroup(
                    title = "Energy" + (uiState.context?.energy?.takeIf { uiState.energyOverride == null }?.let { " (from today's check-in)" } ?: ""),
                    options = listOf(2, 3, 4),
                    selected = uiState.energyOverride ?: uiState.context?.energy?.let { it.coerceIn(2, 4) },
                    label = { energyLabel(it) },
                    onSelect = { viewModel.onEnergyChange(if (it == uiState.energyOverride) null else it) }
                )

                when {
                    uiState.isLoading -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                    uiState.ranked.isEmpty() -> Text(
                        "No tasks to pick from yet. Add a few and come back.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.xl)
                    )
                    else -> {
                        Reel(shown, spinning, landed.value, if (spinning) null else viewModel.reason())
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedButton(
                                onClick = viewModel::onSpinAgain,
                                enabled = !spinning && uiState.ranked.size > 1,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(Spacing.sm))
                                Text("Spin again")
                            }
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onStart()
                                },
                                enabled = !spinning,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1.4f).height(56.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(Spacing.sm))
                                Text("Start ${uiState.minutes} min")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
        ConfettiBurst(if (spinning || uiState.pick == null) 0 else uiState.spinKey, origin = androidx.compose.ui.geometry.Offset(0.5f, 0.55f))
    }
}

@Composable
private fun Reel(task: Task?, spinning: Boolean, landedScale: Float, reason: String?) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (spinning) scheme.surfaceContainerHigh else scheme.primaryContainer,
        contentColor = if (spinning) scheme.onSurface else scheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .scale(landedScale)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(
            Modifier.padding(Spacing.lg).heightIn(min = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically)
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .rotate(if (spinning) 20f else 0f)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(if (spinning) scheme.outlineVariant else scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Casino, contentDescription = null, tint = if (spinning) scheme.onSurface else scheme.onPrimary)
            }
            AnimatedContent(
                targetState = task,
                transitionSpec = { slideInVertically { -it } togetherWith slideOutVertically { it } },
                label = "reel"
            ) { shown ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        shown?.title ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    shown?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            QuadrantBadge(it.quadrant)
                            Text("${it.category.label} · ${it.effort.label}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Text(
                reason ?: "Deciding…",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OptionGroup(
    title: String,
    options: List<Int>,
    selected: Int?,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            options.forEachIndexed { index, value ->
                ToggleButton(
                    checked = selected == value,
                    onCheckedChange = { onSelect(value) },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                    modifier = Modifier.weight(1f)
                ) { Text(label(value), maxLines = 1) }
            }
        }
    }
}

private const val REEL_SIZE = 5
private const val SPIN_STEPS = 12
