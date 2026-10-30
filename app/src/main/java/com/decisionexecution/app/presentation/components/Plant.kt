package com.decisionexecution.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.service.PlantHealth
import com.decisionexecution.app.domain.service.PlantStage
import com.decisionexecution.app.domain.service.PlantState
import com.decisionexecution.app.presentation.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private val LeafGreen = Color(0xFF4FA866)
private val DryBrown = Color(0xFFB0905E)

/**
 * The companion plant. It sways gently while healthy, droops and browns when neglected, and
 * bounces and says something when tapped.
 */
@Composable
fun PlantCompanion(plant: PlantState, modifier: Modifier = Modifier, plantSize: Dp = 120.dp, talkative: Boolean = true) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val bounce = remember { Animatable(1f) }
    var talking by remember { mutableStateOf(false) }

    val droop by animateFloatAsState(
        when (plant.health) {
            PlantHealth.THRIVING -> 0f
            PlantHealth.OKAY -> 0.15f
            PlantHealth.THIRSTY -> 0.5f
            PlantHealth.WILTING -> 1f
        },
        tween(900),
        label = "droop"
    )
    val sway by rememberInfiniteTransition(label = "sway").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )
    val scheme = MaterialTheme.colorScheme
    val leaf = lerp(lerp(LeafGreen, scheme.tertiary, 0.25f), DryBrown, droop * 0.8f)
    val flower = scheme.primary
    // Terracotta, nudged towards the theme so it still sits in dark mode and dynamic colour
    val pot = lerp(Color(0xFFC9714B), scheme.secondary, 0.2f)
    val potRim = lerp(Color(0xFFE08B63), scheme.secondaryContainer, 0.2f)
    val soil = lerp(Color(0xFF5B4636), scheme.onSurface, 0.15f)
    val canopyOutline = MaterialShapes.Cookie9Sided.toShape()
    val petalShape = MaterialShapes.Flower.toShape()

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (talkative) {
            AnimatedVisibility(talking, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Surface(shape = RoundedCornerShape(16.dp), color = scheme.inverseSurface, contentColor = scheme.inverseOnSurface) {
                    Text(
                        plant.message,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 220.dp).padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    )
                }
            }
        }
        Canvas(
            Modifier
                .size(plantSize)
                .scale(bounce.value)
                .semantics { contentDescription = "Your plant: ${plant.stage.label}. ${plant.message}" }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Poke the plant"
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    scope.launch {
                        bounce.snapTo(0.82f)
                        bounce.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
                    }
                    scope.launch {
                        talking = true
                        delay(3_000)
                        talking = false
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val soilY = h * 0.70f
            drawPot(w, h, soilY, pot, potRim, soil)

            // Neglect bends the whole plant over; health keeps it swaying upright
            val lean = sway * (4f - 2.5f * droop) + 38f * droop
            val stemHeight = h * when (plant.stage) {
                PlantStage.SEED -> 0f
                PlantStage.SPROUT -> 0.22f
                PlantStage.SAPLING -> 0.36f
                PlantStage.BLOOM -> 0.44f
                PlantStage.TREE -> 0.40f
            }
            val base = Offset(w / 2, soilY)
            if (plant.stage == PlantStage.SEED) {
                // Just a seed peeking out, with a tiny shoot when it's happy
                drawOval(leaf, Offset(base.x - w * 0.07f, soilY - h * 0.05f), Size(w * 0.14f, h * 0.08f))
                if (droop < 0.5f) {
                    drawLine(leaf, Offset(base.x, soilY - h * 0.04f), Offset(base.x + sway * 2f, soilY - h * 0.12f), w * 0.025f, StrokeCap.Round)
                }
                return@Canvas
            }
            rotate(lean, pivot = base) {
                val tip = Offset(base.x, soilY - stemHeight)
                val stem = Path().apply {
                    moveTo(base.x, base.y)
                    quadraticTo(base.x + w * 0.06f * sin(sway), base.y - stemHeight * 0.5f, tip.x, tip.y)
                }
                drawPath(stem, leaf, style = Stroke(w * 0.035f, cap = StrokeCap.Round))

                val leafPairs = when (plant.stage) {
                    PlantStage.SPROUT -> 1
                    PlantStage.SAPLING, PlantStage.BLOOM -> 2
                    else -> 1
                }
                repeat(leafPairs) { i ->
                    val y = soilY - stemHeight * (0.45f + 0.35f * i)
                    val leafSize = Size(w * (0.20f - 0.03f * i), h * 0.09f)
                    // Leaves fold down as the plant wilts
                    drawLeaf(Offset(base.x, y), leafSize, angle = -25f + 70f * droop, color = leaf, left = true)
                    drawLeaf(Offset(base.x, y), leafSize, angle = 25f - 70f * droop, color = leaf, left = false)
                }

                when (plant.stage) {
                    PlantStage.BLOOM -> {
                        val r = w * 0.15f * (1f - 0.35f * droop)
                        translate(tip.x - r, tip.y - r) {
                            drawOutline(petalShape.createOutline(Size(r * 2, r * 2), layoutDirection, this), lerp(flower, DryBrown, droop * 0.7f))
                        }
                        drawCircle(lerp(Color(0xFFFFD54F), DryBrown, droop * 0.6f), r * 0.38f, tip)
                    }
                    PlantStage.TREE -> {
                        val r = w * 0.27f * (1f - 0.2f * droop)
                        translate(tip.x - r, tip.y - r * 1.3f) {
                            drawOutline(canopyOutline.createOutline(Size(r * 2, r * 2), layoutDirection, this), leaf)
                        }
                        // A couple of blossoms on a healthy tree
                        if (droop < 0.5f) {
                            drawCircle(flower, w * 0.03f, Offset(tip.x - r * 0.4f, tip.y - r * 0.5f))
                            drawCircle(flower, w * 0.03f, Offset(tip.x + r * 0.45f, tip.y - r * 0.15f))
                        }
                    }
                    else -> drawCircle(leaf, w * 0.03f, tip)
                }
            }
        }
    }
}

private fun DrawScope.drawPot(w: Float, h: Float, soilY: Float, pot: Color, rim: Color, soil: Color) {
    val top = soilY - h * 0.02f
    val body = Path().apply {
        moveTo(w * 0.27f, top)
        lineTo(w * 0.73f, top)
        lineTo(w * 0.66f, h * 0.97f)
        lineTo(w * 0.34f, h * 0.97f)
        close()
    }
    drawPath(body, pot)
    drawRoundRect(rim, Offset(w * 0.23f, top - h * 0.03f), Size(w * 0.54f, h * 0.08f), androidx.compose.ui.geometry.CornerRadius(h * 0.03f))
    drawOval(soil, Offset(w * 0.29f, top - h * 0.015f), Size(w * 0.42f, h * 0.035f))
}

private fun DrawScope.drawLeaf(at: Offset, size: Size, angle: Float, color: Color, left: Boolean) {
    rotate(if (left) 180f - angle else angle, pivot = at) {
        drawOval(color, Offset(at.x, at.y - size.height / 2), size)
    }
}
