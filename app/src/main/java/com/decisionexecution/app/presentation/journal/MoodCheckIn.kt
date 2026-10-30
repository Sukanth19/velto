package com.decisionexecution.app.presentation.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.presentation.components.energyLabel
import com.decisionexecution.app.presentation.components.moodIcon
import com.decisionexecution.app.presentation.components.moodLabel
import com.decisionexecution.app.presentation.theme.Spacing

/**
 * The daily check-in: pick a face, then energy appears, then save. Starts from [existing] when
 * today was already logged, so it doubles as the edit form.
 */
@Composable
fun MoodCheckInCard(
    existing: MoodEntry?,
    onSave: (mood: Int, energy: Int, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var mood by remember(existing) { mutableIntStateOf(existing?.mood ?: 0) }
    var energy by remember(existing) { mutableIntStateOf(existing?.energy ?: 3) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    val scheme = MaterialTheme.colorScheme

    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = scheme.secondaryContainer) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                if (existing == null) "How are you feeling?" else "Today's check-in",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSecondaryContainer
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MoodEntry.SCALE.forEach { value ->
                    FilledIconToggleButton(
                        checked = mood == value,
                        onCheckedChange = { mood = value },
                        shapes = IconButtonDefaults.toggleableShapes(),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = scheme.surfaceContainerLowest,
                            contentColor = scheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .size(IconButtonDefaults.mediumContainerSize())
                            .semantics { role = Role.RadioButton }
                    ) {
                        Icon(moodIcon(value), contentDescription = moodLabel(value), modifier = Modifier.size(28.dp))
                    }
                }
            }
            AnimatedVisibility(visible = mood != 0) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Energy", style = MaterialTheme.typography.labelLarge, color = scheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                        Text(energyLabel(energy), style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        MoodEntry.SCALE.forEach { value ->
                            ToggleButton(
                                checked = energy == value,
                                onCheckedChange = { energy = value },
                                shapes = when (value) {
                                    MoodEntry.SCALE.first -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    MoodEntry.SCALE.last -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text(value.toString()) }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Anything on your mind? (optional)") },
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSave(mood, energy, note.trim().ifEmpty { null }) },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text(if (existing == null) "Log it" else "Update") }
                }
            }
        }
    }
}
