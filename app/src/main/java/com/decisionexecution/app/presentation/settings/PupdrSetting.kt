package com.decisionexecution.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.ecosystem.Ecosystem
import com.decisionexecution.app.ecosystem.HabitLinks
import com.decisionexecution.app.ecosystem.VeltoBridge

/**
 * Sync with pupdr, the gym tracker: creatine, water and workout habits tick off in both apps.
 * Self-contained (reads its own small preferences file), so it doesn't touch the settings ViewModel.
 */
@Composable
internal fun PupdrSetting() {
    val context = LocalContext.current
    val links = remember { HabitLinks(context) }
    val installed = remember { VeltoBridge.pupdrInstalled(context) }
    var enabled by remember { mutableStateOf(links.enabled) }
    val linked = remember(enabled) { links.linkedKeys().map(Ecosystem.Habits::label) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (installed) "Sync habits with pupdr" else "pupdr isn't installed", style = MaterialTheme.typography.labelLarge)
                Text(
                    when {
                        !installed -> "Install pupdr, the gym tracker, and creatine, water and workout habits tick off in both apps"
                        linked.isEmpty() -> "Creatine, water and workouts logged in pupdr show up here as habits, and ticking them here updates pupdr"
                        else -> "Linked: ${linked.joinToString(", ")}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = installed && enabled,
                enabled = installed,
                onCheckedChange = { links.enabled = it; enabled = it },
            )
        }
    }
}
