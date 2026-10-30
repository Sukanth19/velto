package com.decisionexecution.app.presentation.review

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionexecution.app.domain.usecase.FocusWindow
import com.decisionexecution.app.domain.usecase.WeeklyReview
import com.decisionexecution.app.domain.usecase.formatHour
import com.decisionexecution.app.presentation.components.moodIcon
import com.decisionexecution.app.presentation.components.moodLabel
import com.decisionexecution.app.presentation.home.formatMinutes
import com.decisionexecution.app.presentation.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The last seven days at a glance. The summary card at the top is also what gets shared.
 */
@Composable
fun ReviewScreen(onBack: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardLayer = rememberGraphicsLayer()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Week in review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState.review?.isEmpty == false) {
                        IconButton(onClick = {
                            scope.launch { shareImage(context, cardLayer.toImageBitmap().asAndroidBitmap()) }
                        }) { Icon(Icons.Rounded.Share, contentDescription = "Share your week") }
                    }
                }
            )
        }
    ) { padding ->
        val review = uiState.review
        if (review == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.error != null) Text(uiState.error!!) else LoadingIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SummaryCard(
                review,
                uiState.streakDays,
                // Records what's drawn so the share button can turn it into an image
                Modifier.drawWithContent {
                    cardLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(cardLayer)
                }
            )
            if (review.isEmpty) {
                SuggestionCard(review.suggestion)
                return@Column
            }
            CompareRow(review)
            SuggestionCard(review.suggestion)
            if (review.wins.isNotEmpty()) WinsCard(review.wins)
            FocusHoursCard(review.focusByHour, review.focusWindow)
            review.thisWeek.averageMood?.let { MoodCard(it, review.lastWeek.averageMood) }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SummaryCard(review: WeeklyReview, streakDays: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dates = DateTimeFormatter.ofPattern("d MMM")
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer)))
            .padding(Spacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MY WEEK", style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                    Text(
                        "${review.start.format(dates)} – ${review.end.format(dates)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onPrimaryContainer
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) { Text("V", style = MaterialTheme.typography.titleLarge, color = scheme.onPrimary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BigStat(review.thisWeek.tasksDone.toString(), "tasks done", Modifier.weight(1f))
                BigStat(formatMinutes(review.thisWeek.focusMinutes), "focused", Modifier.weight(1f))
                BigStat(
                    review.thisWeek.habitRate?.let { "${(it * 100).roundToInt()}%" } ?: "–",
                    "habits hit",
                    Modifier.weight(1f)
                )
            }
            val extras = listOfNotNull(
                review.bestDay?.let { "Best day: ${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}" },
                streakDays.takeIf { it > 0 }?.let { "$it-day streak" }
            )
            if (extras.isNotEmpty()) {
                Text(extras.joinToString("  ·  "), style = MaterialTheme.typography.labelLarge, color = scheme.onPrimaryContainer)
            }
            Text("Made with Velto", style = MaterialTheme.typography.labelSmall, color = scheme.onPrimaryContainer.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
    }
}

/** How this week compares, for the numbers last week gives something to compare with */
@Composable
private fun CompareRow(review: WeeklyReview) {
    val changes = listOfNotNull(
        review.tasksChange?.let { "Tasks" to it },
        review.focusChange?.let { "Focus" to it }
    )
    if (changes.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        changes.forEach { (label, change) -> ChangeChip(label, change, Modifier.weight(1f)) }
    }
}

@Composable
private fun ChangeChip(label: String, change: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val up = change >= 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (up) scheme.tertiaryContainer else scheme.surfaceContainerHigh,
        contentColor = if (up) scheme.onTertiaryContainer else scheme.onSurfaceVariant
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, contentDescription = null)
            Column {
                Text("${if (up) "+" else ""}$change%", style = MaterialTheme.typography.titleMedium)
                Text("$label vs last week", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SuggestionCard(text: String) {
    InfoCard(Icons.Rounded.Lightbulb, "Try this week", MaterialTheme.colorScheme.secondaryContainer) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WinsCard(wins: List<String>) {
    InfoCard(Icons.Rounded.EmojiEvents, "Wins", MaterialTheme.colorScheme.surfaceContainerLow) {
        wins.forEach { win ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(win, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A day's 24 hours as bars, with the best window picked out */
@Composable
fun FocusHoursCard(byHour: List<Int>, window: FocusWindow?) {
    val scheme = MaterialTheme.colorScheme
    val max = (byHour.maxOrNull() ?: 0).coerceAtLeast(1)
    InfoCard(Icons.Rounded.Schedule, "When you focus best", scheme.surfaceContainerLow) {
        Text(
            window?.let { "${formatHour(it.startHour)} – ${formatHour(it.endHour)} is your sweet spot" }
                ?: "Finish an hour or so of focus sessions and your best time of day shows up here",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .semantics { contentDescription = "Focus minutes by hour of the day" },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            byHour.forEachIndexed { hour, minutes ->
                val inWindow = window != null && (hour - window.startHour + 24) % 24 < window.hours
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight((minutes.toFloat() / max).coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (inWindow) scheme.primary else scheme.primary.copy(alpha = 0.3f))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18).forEach {
                Text(formatHour(it), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoodCard(average: Double, lastWeek: Double?) {
    val rounded = average.roundToInt().coerceIn(1, 5)
    InfoCard(moodIcon(rounded), "Mood", MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            "Mostly ${moodLabel(rounded).lowercase()} (${"%.1f".format(average)} / 5)" + when {
                lastWeek == null -> ""
                average > lastWeek + 0.3 -> ", up from last week"
                average < lastWeek - 0.3 -> ", down from last week"
                else -> ", about the same as last week"
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(shape = RoundedCornerShape(24.dp), color = color) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            content()
        }
    }
}

/** Saves the card to the cache and opens the share sheet */
private suspend fun shareImage(context: Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "velto-week.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Share your week"))
}
