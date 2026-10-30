package com.decisionexecution.app.presentation.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.graphics.shapes.RoundedPolygon
import com.decisionexecution.app.domain.service.Achievement
import com.decisionexecution.app.domain.service.LevelProgress
import com.decisionexecution.app.presentation.theme.Spacing

val Achievement.icon: ImageVector
    get() = when (this) {
        Achievement.FIRST_STEP -> Icons.Rounded.Flag
        Achievement.GETTING_THINGS_DONE -> Icons.Rounded.CheckCircle
        Achievement.CENTURION -> Icons.Rounded.MilitaryTech
        Achievement.IN_THE_ZONE -> Icons.Rounded.Timer
        Achievement.DEEP_DIVER -> Icons.Rounded.Diamond
        Achievement.HABIT_FORMING -> Icons.Rounded.Spa
        Achievement.WEEK_STRONG -> Icons.Rounded.LocalFireDepartment
        Achievement.UNSTOPPABLE -> Icons.Rounded.AllInclusive
        Achievement.ON_A_ROLL -> Icons.Rounded.Bolt
        Achievement.DEAR_DIARY -> Icons.AutoMirrored.Rounded.MenuBook
        Achievement.BRAIN_DUMP -> Icons.Rounded.Inbox
        Achievement.LEVEL_TEN -> Icons.Rounded.AutoAwesome
    }

private val badgeShapes = listOf(
    MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Burst,
    MaterialShapes.Flower, MaterialShapes.Cookie6Sided, MaterialShapes.SoftBurst, MaterialShapes.Pentagon,
    MaterialShapes.Gem, MaterialShapes.Puffy, MaterialShapes.Clover8Leaf, MaterialShapes.VerySunny
)

val Achievement.shape: RoundedPolygon get() = badgeShapes[ordinal % badgeShapes.size]

/**
 * Level with a wavy XP bar towards the next one.
 */
@Composable
fun LevelCard(level: LevelProgress, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val progress by animateFloatAsState(level.fraction, WavyProgressIndicatorDefaults.ProgressAnimationSpec, label = "xp")
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(level.level, Modifier.size(72.dp))
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(level.title, style = MaterialTheme.typography.headlineMedium)
                    Text("${level.totalXp} XP total", style = MaterialTheme.typography.bodyMedium)
                }
            }
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(14.dp),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest
            )
            Text(
                "${level.xpToGo} XP to level ${level.level + 1}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(MaterialTheme.colorScheme.primary)
            .semantics { contentDescription = "Level $level" },
        contentAlignment = Alignment.Center
    ) {
        // Sized from the badge, so the number sits centred in a 26dp chip and an 80dp dialog alike
        val fontSize = with(LocalDensity.current) { (maxHeight * if (level >= 10) 0.38f else 0.48f).toSp() }
        Text(
            level.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

/**
 * Every achievement as a shaped badge; locked ones are greyed out with a lock.
 */
@Composable
fun AchievementGrid(unlocked: Set<Achievement>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Achievements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${unlocked.size}/${Achievement.entries.size}", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            }
            // Three across leaves room for the longer titles without breaking words
            Achievement.entries.chunked(PER_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    row.forEach { achievement ->
                        AchievementBadge(achievement, achievement in unlocked, Modifier.weight(1f))
                    }
                    repeat(PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private const val PER_ROW = 3

@Composable
fun AchievementBadge(achievement: Achievement, unlocked: Boolean, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 56.dp) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "${achievement.title}: ${achievement.description}" + if (unlocked) ", unlocked" else ", locked"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            Modifier
                .size(size)
                .clip(achievement.shape.toShape())
                .background(if (unlocked) scheme.tertiary else scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (unlocked) achievement.icon else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (unlocked) scheme.onTertiary else scheme.outline
            )
        }
        Text(
            achievement.title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (unlocked) scheme.onSurface else scheme.onSurfaceVariant
        )
    }
}
