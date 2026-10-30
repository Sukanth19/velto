package com.decisionexecution.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.model.Recurrence
import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.service.daysUntil
import com.decisionexecution.app.domain.service.dueText
import com.decisionexecution.app.presentation.theme.Spacing
import java.time.LocalDate

val Recurrence.label: String
    get() = when (this) {
        Recurrence.DAILY -> "Daily"
        Recurrence.WEEKDAYS -> "Weekdays"
        Recurrence.WEEKLY -> "Weekly"
        Recurrence.MONTHLY -> "Monthly"
    }

/** Due date as a small chip, red once it's overdue and highlighted when it's today */
@Composable
fun DueChip(due: LocalDate, modifier: Modifier = Modifier, recurrence: Recurrence? = null, today: LocalDate = LocalDate.now()) {
    val scheme = MaterialTheme.colorScheme
    val days = daysUntil(due, today)
    val (container, content) = when {
        days < 0 -> scheme.errorContainer to scheme.onErrorContainer
        days == 0L -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    }
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Row(
            Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                if (recurrence != null) Icons.Rounded.Repeat else Icons.Rounded.Event,
                contentDescription = recurrence?.let { "Repeats ${it.label.lowercase()}" },
                modifier = Modifier.size(14.dp)
            )
            Text(dueText(due, today), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A step with a checkbox; the whole row is the touch target */
@Composable
fun StepRow(step: Subtask, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val text by animateColorAsState(
        if (step.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        label = "stepText"
    )
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = step.isDone, role = Role.Checkbox, onValueChange = onToggle)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = step.isDone, onCheckedChange = null)
        Text(
            step.title,
            style = MaterialTheme.typography.bodyLarge,
            color = text,
            textDecoration = if (step.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.padding(start = Spacing.sm)
        )
    }
}
