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
import com.decisionexecution.app.domain.service.PlantVariety
import androidx.compose.ui.graphics.Shape
import com.decisionexecution.app.presentation.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private val LeafGreen = Color(0xFF4FA866)
private val DryBrown = Color(0xFFB0905E)
private val Sunshine = Color(0xFFFFD54F)
private val Seeds = Color(0xFF6D4C2F)

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
                val colors = PlantColors(leaf, flower, droop)
                when (plant.variety) {
                    PlantVariety.CACTUS -> drawCactus(plant.stage, base, stemHeight, colors, petalShape)
                    PlantVariety.BONSAI -> drawBonsai(plant.stage, base, stemHeight, sway, colors, canopyOutline)
                    else -> {
                        val tip = drawStemAndLeaves(plant.stage, base, stemHeight, sway, colors)
                        if (plant.variety == PlantVariety.SUNFLOWER) drawSunflowerHead(plant.stage, tip, colors, petalShape)
                        else drawBlossomHead(plant.stage, tip, colors, petalShape, canopyOutline)
                    }
                }
            }
        }
    }
}

/** Colours for one frame: [droop] fades flowers towards dry brown */
private class PlantColors(val leaf: Color, val flower: Color, val droop: Float) {
    fun wilted(color: Color, amount: Float = 0.7f) = lerp(color, DryBrown, droop * amount)
}

/** Stem with leaf pairs; returns the tip, where a flower or canopy goes */
private fun DrawScope.drawStemAndLeaves(stage: PlantStage, base: Offset, stemHeight: Float, sway: Float, c: PlantColors): Offset {
    val w = size.width
    val h = size.height
    val tip = Offset(base.x, base.y - stemHeight)
    val stem = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(base.x + w * 0.06f * sin(sway), base.y - stemHeight * 0.5f, tip.x, tip.y)
    }
    drawPath(stem, c.leaf, style = Stroke(w * 0.035f, cap = StrokeCap.Round))
    val leafPairs = when (stage) {
        PlantStage.SAPLING, PlantStage.BLOOM -> 2
        else -> 1
    }
    repeat(leafPairs) { i ->
        val y = base.y - stemHeight * (0.45f + 0.35f * i)
        val leafSize = Size(w * (0.20f - 0.03f * i), h * 0.09f)
        // Leaves fold down as the plant wilts
        drawLeaf(Offset(base.x, y), leafSize, angle = -25f + 70f * c.droop, color = c.leaf, left = true)
        drawLeaf(Offset(base.x, y), leafSize, angle = 25f - 70f * c.droop, color = c.leaf, left = false)
    }
    return tip
}

private fun DrawScope.drawBlossomHead(stage: PlantStage, tip: Offset, c: PlantColors, petals: Shape, canopy: Shape) {
    val w = size.width
    when (stage) {
        PlantStage.BLOOM -> {
            val r = w * 0.15f * (1f - 0.35f * c.droop)
            translate(tip.x - r, tip.y - r) {
                drawOutline(petals.createOutline(Size(r * 2, r * 2), layoutDirection, this), c.wilted(c.flower))
            }
            drawCircle(c.wilted(Sunshine, 0.6f), r * 0.38f, tip)
        }
        PlantStage.TREE -> {
            val r = w * 0.27f * (1f - 0.2f * c.droop)
            translate(tip.x - r, tip.y - r * 1.3f) {
                drawOutline(canopy.createOutline(Size(r * 2, r * 2), layoutDirection, this), c.leaf)
            }
            // A couple of blossoms on a healthy tree
            if (c.droop < 0.5f) {
                drawCircle(c.flower, w * 0.03f, Offset(tip.x - r * 0.4f, tip.y - r * 0.5f))
                drawCircle(c.flower, w * 0.03f, Offset(tip.x + r * 0.45f, tip.y - r * 0.15f))
            }
        }
        else -> drawCircle(c.leaf, w * 0.03f, tip)
    }
}

/** A big yellow face once it blooms, and a bigger one again as a "tree" */
private fun DrawScope.drawSunflowerHead(stage: PlantStage, tip: Offset, c: PlantColors, petals: Shape) {
    val w = size.width
    val r = w * when (stage) {
        PlantStage.BLOOM -> 0.17f
        PlantStage.TREE -> 0.23f
        else -> 0f
    } * (1f - 0.3f * c.droop)
    if (r == 0f) {
        // A green bud waiting to open
        drawCircle(c.leaf, w * 0.045f, tip)
        return
    }
    translate(tip.x - r, tip.y - r) {
        drawOutline(petals.createOutline(Size(r * 2, r * 2), layoutDirection, this), c.wilted(Sunshine))
    }
    drawCircle(c.wilted(Seeds, 0.4f), r * 0.5f, tip)
    // A few seeds catching the light
    if (r > 0f) repeat(5) { i ->
        val angle = i * 72f * (Math.PI / 180f).toFloat()
        drawCircle(c.wilted(Sunshine, 0.5f).copy(alpha = 0.5f), r * 0.06f, Offset(tip.x + kotlin.math.cos(angle) * r * 0.28f, tip.y + sin(angle) * r * 0.28f))
    }
}

/** A plump column that grows arms, then a flower on top */
private fun DrawScope.drawCactus(stage: PlantStage, base: Offset, stemHeight: Float, c: PlantColors, petals: Shape) {
    val w = size.width
    val body = lerp(c.leaf, Color(0xFF2E6B4A), 0.35f)
    val rib = lerp(body, Color.White, 0.25f)
    val bodyWidth = w * if (stage == PlantStage.SPROUT) 0.13f else 0.17f
    val height = stemHeight * 1.05f
    val top = base.y - height
    val corner = androidx.compose.ui.geometry.CornerRadius(bodyWidth / 2)
    drawRoundRect(body, Offset(base.x - bodyWidth / 2, top), Size(bodyWidth, height + bodyWidth / 2), corner)
    // Ribs down the body
    listOf(-0.22f, 0.22f).forEach { x ->
        drawLine(rib, Offset(base.x + bodyWidth * x, top + bodyWidth * 0.4f), Offset(base.x + bodyWidth * x, base.y), w * 0.008f, StrokeCap.Round)
    }
    fun arm(left: Boolean, at: Float, reach: Float) {
        val armWidth = bodyWidth * 0.62f
        val y = base.y - height * at
        val side = if (left) -1 else 1
        // Out sideways, then up; arms sag as the plant wilts
        val outX = base.x + side * (bodyWidth / 2 + w * reach)
        val rise = height * 0.3f * (1f - c.droop * 0.8f)
        drawLine(body, Offset(base.x, y), Offset(outX, y), armWidth, StrokeCap.Round)
        drawLine(body, Offset(outX, y), Offset(outX, y - rise), armWidth, StrokeCap.Round)
    }
    when (stage) {
        PlantStage.SAPLING -> arm(left = true, at = 0.45f, reach = 0.06f)
        PlantStage.BLOOM, PlantStage.TREE -> {
            arm(left = true, at = 0.42f, reach = 0.07f)
            arm(left = false, at = 0.62f, reach = 0.06f)
        }
        else -> Unit
    }
    if (stage == PlantStage.BLOOM || stage == PlantStage.TREE) {
        val r = w * (if (stage == PlantStage.TREE) 0.09f else 0.07f) * (1f - 0.35f * c.droop)
        translate(base.x - r, top - r * 0.9f) {
            drawOutline(petals.createOutline(Size(r * 2, r * 2), layoutDirection, this), c.wilted(lerp(c.flower, Color(0xFFFF6FA3), 0.5f)))
        }
        drawCircle(c.wilted(Sunshine, 0.6f), r * 0.35f, Offset(base.x, top + r * 0.1f))
    }
}

/** A curving trunk under clouds of foliage, with pink blossom when it blooms */
private fun DrawScope.drawBonsai(stage: PlantStage, base: Offset, stemHeight: Float, sway: Float, c: PlantColors, canopy: Shape) {
    val w = size.width
    if (stage == PlantStage.SPROUT) {
        drawStemAndLeaves(stage, base, stemHeight, sway, c)
        return
    }
    val bark = lerp(Color(0xFF6B4A33), c.leaf, 0.1f)
    val tip = Offset(base.x + w * 0.08f, base.y - stemHeight)
    val trunk = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(base.x - w * 0.14f, base.y - stemHeight * 0.35f, base.x + w * 0.18f, base.y - stemHeight * 0.6f, tip.x, tip.y)
    }
    drawPath(trunk, bark, style = Stroke(w * 0.055f, cap = StrokeCap.Round))
    // A side branch that holds the lower cloud
    val branchEnd = Offset(base.x - w * 0.17f, base.y - stemHeight * 0.55f)
    drawLine(bark, Offset(base.x - w * 0.03f, base.y - stemHeight * 0.4f), branchEnd, w * 0.03f, StrokeCap.Round)

    val scale = when (stage) {
        PlantStage.SAPLING -> 0.75f
        PlantStage.BLOOM -> 0.9f
        else -> 1.1f
    } * (1f - 0.25f * c.droop)
    fun cloud(center: Offset, r: Float) {
        val radius = r * scale
        translate(center.x - radius, center.y - radius * 0.8f) {
            drawOutline(canopy.createOutline(Size(radius * 2, radius * 1.6f), layoutDirection, this), c.leaf)
        }
    }
    cloud(tip, w * 0.2f)
    cloud(branchEnd, w * 0.13f)
    if (stage == PlantStage.TREE) cloud(Offset(tip.x + w * 0.16f, tip.y + w * 0.1f), w * 0.11f)
    if ((stage == PlantStage.BLOOM || stage == PlantStage.TREE) && c.droop < 0.5f) {
        val blossom = lerp(c.flower, Color(0xFFFFB3C7), 0.6f)
        listOf(Offset(-0.1f, -0.06f), Offset(0.07f, -0.1f), Offset(0.12f, 0.02f), Offset(-0.2f, 0.12f)).forEach { (dx, dy) ->
            drawCircle(blossom, w * 0.025f, Offset(tip.x + dx * w, tip.y + dy * w))
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
