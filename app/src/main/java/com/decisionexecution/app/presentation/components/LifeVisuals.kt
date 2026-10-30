package com.decisionexecution.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import com.decisionexecution.app.domain.model.HabitIcon

val HabitIcon.imageVector: ImageVector
    get() = when (this) {
        HabitIcon.SPARK -> Icons.Rounded.AutoAwesome
        HabitIcon.WATER -> Icons.Rounded.WaterDrop
        HabitIcon.READ -> Icons.AutoMirrored.Rounded.MenuBook
        HabitIcon.MOVE -> Icons.AutoMirrored.Rounded.DirectionsRun
        HabitIcon.MIND -> Icons.Rounded.SelfImprovement
        HabitIcon.SLEEP -> Icons.Rounded.Bedtime
        HabitIcon.FOOD -> Icons.Rounded.Restaurant
        HabitIcon.CODE -> Icons.Rounded.Code
        HabitIcon.MUSIC -> Icons.Rounded.MusicNote
    }

val HabitIcon.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

/** Each habit gets its own expressive badge shape, so the list doesn't read as a column of circles */
val HabitIcon.badgeShape: RoundedPolygon
    get() = when (this) {
        HabitIcon.SPARK -> MaterialShapes.Sunny
        HabitIcon.WATER -> MaterialShapes.Cookie6Sided
        HabitIcon.READ -> MaterialShapes.Clover4Leaf
        HabitIcon.MOVE -> MaterialShapes.Burst
        HabitIcon.MIND -> MaterialShapes.Flower
        HabitIcon.SLEEP -> MaterialShapes.Ghostish
        HabitIcon.FOOD -> MaterialShapes.Cookie9Sided
        HabitIcon.CODE -> MaterialShapes.Pentagon
        HabitIcon.MUSIC -> MaterialShapes.SoftBurst
    }

/** Mood 1-5 as a face, saddest first */
fun moodIcon(mood: Int): ImageVector = when (mood) {
    1 -> Icons.Rounded.SentimentVeryDissatisfied
    2 -> Icons.Rounded.SentimentDissatisfied
    3 -> Icons.Rounded.SentimentNeutral
    4 -> Icons.Rounded.SentimentSatisfied
    else -> Icons.Rounded.SentimentVerySatisfied
}

fun moodLabel(mood: Int): String = when (mood) {
    1 -> "Awful"
    2 -> "Meh"
    3 -> "Okay"
    4 -> "Good"
    else -> "Great"
}

fun energyLabel(energy: Int): String = when (energy) {
    1 -> "Drained"
    2 -> "Low"
    3 -> "Steady"
    4 -> "Charged"
    else -> "Buzzing"
}
