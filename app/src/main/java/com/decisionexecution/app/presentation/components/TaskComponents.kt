package com.decisionexecution.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.model.EisenhowerQuadrant
import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.presentation.theme.Spacing

val TaskCategory.label: String
    get() = when (this) {
        TaskCategory.SCHOOL -> "School"
        TaskCategory.PERSONAL -> "Personal"
        TaskCategory.WORK -> "Work"
    }

val TaskEffort.label: String
    get() = when (this) {
        TaskEffort.LOW -> "Low effort"
        TaskEffort.MEDIUM -> "Medium effort"
        TaskEffort.HIGH -> "High effort"
    }

val EnergyTag.label: String
    get() = when (this) {
        EnergyTag.LOW -> "Low energy"
        EnergyTag.DEEP_WORK -> "Deep work"
    }

@Composable
fun AttributeTag(text: String, modifier: Modifier = Modifier, spokenText: String = text) {
    Surface(
        modifier = modifier.semantics { contentDescription = spokenText },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(horizontal = Spacing.sm, vertical = 2.dp)
                .clearAndSetSemantics { }
        )
    }
}

/**
 * Urgency, importance, effort and energy tag for a task.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskAttributeRow(task: Task, modifier: Modifier = Modifier) {
    // Wraps onto a second line on narrow cards instead of squeezing the last tag
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        // The deadline matters most, so it leads
        task.dueDate?.let { DueChip(it, recurrence = task.recurrence) }
        if (task.dueDate == null) task.recurrence?.let { AttributeTag("Repeats ${it.label.lowercase()}") }
        // Compact on screen, spelled out for screen readers
        AttributeTag("U${task.urgency}", spokenText = "Urgency ${task.urgency} of 5")
        AttributeTag("I${task.importance}", spokenText = "Importance ${task.importance} of 5")
        AttributeTag(task.effort.label)
        task.energyTag?.let { AttributeTag(it.label) }
    }
}

private val classifier = EisenhowerClassifier()

val Task.quadrant: EisenhowerQuadrant get() = classifier.classify(this)

val EisenhowerQuadrant.label: String
    get() = when (this) {
        EisenhowerQuadrant.URGENT_IMPORTANT -> "Do first"
        EisenhowerQuadrant.NOT_URGENT_IMPORTANT -> "Schedule"
        EisenhowerQuadrant.URGENT_NOT_IMPORTANT -> "Quick win"
        EisenhowerQuadrant.NEITHER -> "Later"
    }

val EisenhowerQuadrant.hint: String
    get() = when (this) {
        EisenhowerQuadrant.URGENT_IMPORTANT -> "Urgent and important"
        EisenhowerQuadrant.NOT_URGENT_IMPORTANT -> "Important, not urgent"
        EisenhowerQuadrant.URGENT_NOT_IMPORTANT -> "Urgent, less important"
        EisenhowerQuadrant.NEITHER -> "Neither, park it"
    }

/** Container/content colour pair for a quadrant, taken from the theme so Material You applies. */
@Composable
fun EisenhowerQuadrant.colors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        EisenhowerQuadrant.URGENT_IMPORTANT -> scheme.errorContainer to scheme.onErrorContainer
        EisenhowerQuadrant.NOT_URGENT_IMPORTANT -> scheme.primaryContainer to scheme.onPrimaryContainer
        EisenhowerQuadrant.URGENT_NOT_IMPORTANT -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        EisenhowerQuadrant.NEITHER -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
}

@Composable
fun TaskCategory.color(): Color = when (this) {
    TaskCategory.WORK -> MaterialTheme.colorScheme.primary
    TaskCategory.SCHOOL -> MaterialTheme.colorScheme.tertiary
    TaskCategory.PERSONAL -> MaterialTheme.colorScheme.secondary
}

@Composable
fun QuadrantBadge(quadrant: EisenhowerQuadrant, modifier: Modifier = Modifier) {
    val (container, content) = quadrant.colors()
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Text(
            quadrant.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        )
    }
}

/** Section title with a count pill, e.g. "Work  3". */
@Composable
fun SectionHeader(title: String, count: Int?, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        count?.let {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), contentColor = accent) {
                Text(
                    it.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                )
            }
        }
    }
}
