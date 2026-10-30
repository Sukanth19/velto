package com.decisionexecution.app.presentation.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.model.MoodEntry
import com.decisionexecution.app.domain.usecase.MoodInsights
import com.decisionexecution.app.presentation.components.ConfettiBurst
import com.decisionexecution.app.presentation.components.energyLabel
import com.decisionexecution.app.presentation.components.moodIcon
import com.decisionexecution.app.presentation.components.moodLabel
import com.decisionexecution.app.presentation.home.formatMinutes
import com.decisionexecution.app.presentation.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Today's check-in, the last two weeks of mood, the energy-vs-focus takeaway and past entries.
 */
@Composable
fun JournalScreen(onBack: () -> Unit, viewModel: JournalViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text("Journal") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                    }
                )
            }
        ) { padding ->
            val insights = uiState.insights
            if (insights == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
                return@Scaffold
            }
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item { MoodCheckInCard(uiState.today, viewModel::onSave) }
                item { InsightCard(insights) }
                item { MoodChart(insights.entries) }
                item { YearInPixels(uiState.year) }
                if (insights.entries.isNotEmpty()) {
                    item { Text("Entries", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.sm)) }
                }
                items(insights.entries, key = { it.day.toEpochDay() }) { EntryRow(it) }
            }
        }
        ConfettiBurst(uiState.savedKey, origin = androidx.compose.ui.geometry.Offset(0.5f, 0.3f))
    }
}

@Composable
private fun InsightCard(insights: MoodInsights) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer) {
        Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(MaterialShapes.Cookie7Sided.toShape()).background(scheme.tertiary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Insights, contentDescription = null, tint = scheme.onTertiary) }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    insights.headline ?: "Log a few days of high and low energy to see what fuels your focus",
                    style = MaterialTheme.typography.titleSmall
                )
                val high = insights.highEnergyFocus
                val low = insights.lowEnergyFocus
                if (high != null && low != null) {
                    Text(
                        "High energy: ${formatMinutes(high)} a day · Low energy: ${formatMinutes(low)} a day",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/** Two weeks of faces on a little stepped line, so ups and downs are visible at a glance. */
@Composable
private fun MoodChart(entries: List<MoodEntry>) {
    val today = LocalDate.now()
    val byDay = entries.associateBy { it.day }
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Last 2 weeks", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "Mood over the last two weeks" },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                (13 downTo 0).forEach { back ->
                    val entry = byDay[today.minusDays(back.toLong())]
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (entry != null) {
                            Icon(moodIcon(entry.mood), contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(entry.mood / 5f * 0.75f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(scheme.primary.copy(alpha = 0.3f + entry.energy * 0.14f))
                            )
                        } else {
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(scheme.outlineVariant))
                        }
                    }
                }
            }
        }
    }
}

/**
 * The year as pixels: a column per month, a square per day, coloured by that day's mood.
 */
@Composable
private fun YearInPixels(year: Map<LocalDate, MoodEntry>) {
    val today = LocalDate.now()
    val scheme = MaterialTheme.colorScheme
    // Sad to happy, from the error colour through to the primary
    val moodColors = listOf(
        scheme.error,
        lerp(scheme.error, scheme.tertiary, 0.5f),
        scheme.tertiary,
        lerp(scheme.tertiary, scheme.primary, 0.5f),
        scheme.primary
    )
    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${today.year} in pixels", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${year.size} days logged", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().semantics { contentDescription = "Mood for each day of ${today.year}, ${year.size} days logged" },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..12).forEach { month ->
                    val first = LocalDate.of(today.year, month, 1)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            first.month.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        (1..first.lengthOfMonth()).forEach { day ->
                            val date = first.withDayOfMonth(day)
                            val entry = year[date]
                            Box(
                                Modifier
                                    .size(width = 18.dp, height = 7.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            entry != null -> moodColors[entry.mood - 1]
                                            date.isAfter(today) -> scheme.surfaceContainer
                                            else -> scheme.surfaceContainerHighest
                                        }
                                    )
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text("Awful", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                moodColors.forEach { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(it)) }
                Text("Great", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EntryRow(entry: MoodEntry) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(moodIcon(entry.mood), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    "${moodLabel(entry.mood)} · ${energyLabel(entry.energy)}",
                    style = MaterialTheme.typography.titleSmall
                )
                entry.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(
                entry.day.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
