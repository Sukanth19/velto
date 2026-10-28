package com.decisionexecution.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.model.EnergyTag
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
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
fun AttributeTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
        )
    }
}

/**
 * Urgency, importance, effort and energy tag for a task. Requirements: 9.5
 */
@Composable
fun TaskAttributeRow(task: Task, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AttributeTag("U${task.urgency}")
        AttributeTag("I${task.importance}")
        AttributeTag(task.effort.label)
        task.energyTag?.let { AttributeTag(it.label) }
    }
}
