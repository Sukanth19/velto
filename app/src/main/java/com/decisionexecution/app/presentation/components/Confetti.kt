package com.decisionexecution.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val round: Boolean
)

/**
 * A one-shot burst of confetti from [origin] (fractions of the canvas). Each new [burstKey]
 * fires another burst; 0 means none yet. Doesn't take touches, so it can sit over anything.
 */
@Composable
fun ConfettiBurst(burstKey: Int, modifier: Modifier = Modifier, origin: Offset = Offset(0.5f, 0.35f)) {
    if (burstKey == 0) return
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primaryContainer, scheme.tertiaryContainer, scheme.error)
    val pieces = remember(burstKey) {
        val random = Random(burstKey)
        List(PIECES) {
            Piece(
                // Mostly upwards, fanning out to the sides
                angle = (-90f + random.nextFloat() * 160f - 80f) * (Math.PI.toFloat() / 180f),
                speed = 0.6f + random.nextFloat() * 0.9f,
                spin = random.nextFloat() * 720f - 360f,
                color = palette[random.nextInt(palette.size)],
                width = 8f + random.nextFloat() * 10f,
                height = 6f + random.nextFloat() * 8f,
                round = random.nextFloat() < 0.35f
            )
        }
    }
    val time = remember(burstKey) { Animatable(0f) }
    LaunchedEffect(burstKey) { time.animateTo(1f, tween(DURATION_MILLIS, easing = LinearEasing)) }

    Canvas(modifier.fillMaxSize()) {
        val t = time.value
        if (t >= 1f) return@Canvas
        val start = Offset(size.width * origin.x, size.height * origin.y)
        val reach = size.minDimension
        pieces.forEach { piece ->
            // Launch outwards, then gravity pulls everything down
            val x = start.x + cos(piece.angle) * piece.speed * reach * t
            val y = start.y + sin(piece.angle) * piece.speed * reach * t + GRAVITY * reach * t * t
            val alpha = (1f - t).coerceIn(0f, 1f) * 1.4f
            val color = piece.color.copy(alpha = alpha.coerceAtMost(1f))
            rotate(piece.spin * t, Offset(x, y)) {
                if (piece.round) drawCircle(color, piece.height / 2, Offset(x, y))
                else drawRect(color, Offset(x - piece.width / 2, y - piece.height / 2), Size(piece.width, piece.height))
            }
        }
    }
}

private const val PIECES = 70
private const val DURATION_MILLIS = 1600
private const val GRAVITY = 1.2f
