package com.decisionexecution.app.presentation.progress

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.decisionexecution.app.domain.repository.ProgressRepository
import com.decisionexecution.app.domain.service.Achievement
import com.decisionexecution.app.domain.service.Reward
import com.decisionexecution.app.domain.service.rewardsBetween
import androidx.compose.foundation.shape.RoundedCornerShape
import com.decisionexecution.app.domain.usecase.GetProgressUseCase
import com.decisionexecution.app.presentation.components.ConfettiBurst
import com.decisionexecution.app.presentation.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Something earned that hasn't been shown yet */
data class Celebration(
    val newLevel: Int?,
    val achievements: List<Achievement>,
    /** Palettes and plants this level-up unlocked */
    val rewards: List<Reward> = emptyList()
) {
    /** Different for every distinct celebration, so the confetti fires once per reward */
    val key: Int get() = (newLevel ?: 0) * 1000 + achievements.sumOf { it.ordinal + 1 }
}

fun pendingCelebration(level: Int, unlocked: Set<Achievement>, celebratedLevel: Int, celebrated: Set<String>): Celebration? {
    val newLevel = level.takeIf { it > celebratedLevel }
    val newAchievements = unlocked.filter { it.name !in celebrated }.sortedBy { it.ordinal }
    return if (newLevel == null && newAchievements.isEmpty()) null
    else Celebration(newLevel, newAchievements, newLevel?.let { rewardsBetween(celebratedLevel, it) }.orEmpty())
}

/**
 * Watches progress from anywhere in the app and pops a celebration for each new level or achievement.
 */
@HiltViewModel
class CelebrationViewModel @Inject constructor(
    getProgressUseCase: GetProgressUseCase,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val pending: StateFlow<Celebration?> = combine(
        getProgressUseCase(),
        progressRepository.getCelebratedLevel(),
        progressRepository.getCelebratedAchievements()
    ) { progress, level, seen ->
        pendingCelebration(progress.level.level, progress.unlocked, level, seen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onDismiss(celebration: Celebration) {
        viewModelScope.launch {
            progressRepository.markCelebrated(
                level = celebration.newLevel ?: 1,
                achievementIds = celebration.achievements.map { it.name }.toSet()
            )
        }
    }
}

@Composable
fun CelebrationHost(viewModel: CelebrationViewModel) {
    val celebration by viewModel.pending.collectAsStateWithLifecycle()
    val current = celebration ?: return
    ConfettiBurst(current.key)
    AlertDialog(
        onDismissRequest = { viewModel.onDismiss(current) },
        icon = { current.newLevel?.let { LevelBadge(it, Modifier.size(80.dp)) } },
        title = {
            Text(
                when {
                    current.newLevel != null -> "Level ${current.newLevel}!"
                    current.achievements.size == 1 -> "Achievement unlocked"
                    else -> "${current.achievements.size} achievements unlocked"
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                current.newLevel?.let {
                    Text(
                        "You're now a ${com.decisionexecution.app.domain.service.levelTitle(it)}. Keep going!",
                        textAlign = TextAlign.Center
                    )
                }
                if (current.rewards.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Column(Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unlocked", style = MaterialTheme.typography.labelLarge)
                            Text(
                                current.rewards.joinToString(" and ") { it.description },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Try it in Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
                current.achievements.take(MAX_SHOWN).forEach { achievement ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        AchievementBadge(achievement, unlocked = true, size = 44.dp, modifier = Modifier.width(64.dp))
                        Text(achievement.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
                if (current.achievements.size > MAX_SHOWN) {
                    Text("+${current.achievements.size - MAX_SHOWN} more on the Progress tab", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.onDismiss(current) }, shapes = ButtonDefaults.shapes()) { Text("Nice!") }
        }
    )
}

private const val MAX_SHOWN = 3
