package com.decisionexecution.app.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.repository.FocusSessionRecord
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskCompletion
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.domain.service.LifetimeStats
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.usecase.*
import com.decisionexecution.app.presentation.focus.FocusScreen
import com.decisionexecution.app.presentation.focus.FocusViewModel
import com.decisionexecution.app.presentation.habits.HabitsScreen
import com.decisionexecution.app.presentation.habits.HabitsViewModel
import com.decisionexecution.app.presentation.home.HomeExtrasViewModel
import com.decisionexecution.app.presentation.home.HomeScreen
import com.decisionexecution.app.presentation.home.HomeViewModel
import com.decisionexecution.app.presentation.insights.InsightsScreen
import com.decisionexecution.app.presentation.insights.InsightsViewModel
import com.decisionexecution.app.presentation.review.ReviewScreen
import com.decisionexecution.app.presentation.review.ReviewViewModel
import com.decisionexecution.app.presentation.tasks.TasksScreen
import com.decisionexecution.app.presentation.tasks.TasksViewModel
import com.decisionexecution.app.presentation.theme.VeltoTheme
import com.decisionexecution.app.testutil.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders the Play Store images into fastlane/metadata. Skipped unless asked for:
 *
 *     ./gradlew testDebugUnitTest --tests '*StoreAssetsTest*' -PstoreAssets=true
 *
 * Phone screenshots are 1080x2160 (Play allows up to 2:1), the feature graphic 1024x500.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h720dp-xxhdpi")
class StoreAssetsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val imagesDir = File("../fastlane/metadata/android/en-US/images")
    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()

    @Before
    fun onlyWhenAsked() = assumeTrue(System.getProperty("storeAssets") == "true")

    private val tasks = listOf(
        task("1", "Finish the quarterly report", TaskCategory.WORK, 5, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK, due = today),
        task("2", "Study for calculus exam", TaskCategory.SCHOOL, 3, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK, due = today.plusDays(1)),
        task("3", "Reply to the client email", TaskCategory.WORK, 4, 2, TaskEffort.LOW, EnergyTag.LOW),
        task("4", "Plan the weekend trip", TaskCategory.PERSONAL, 2, 3, TaskEffort.MEDIUM, null, due = today.plusDays(5)),
        task("5", "Water the plants", TaskCategory.PERSONAL, 3, 2, TaskEffort.LOW, EnergyTag.LOW, due = today.plusDays(2), repeat = Recurrence.WEEKLY)
    )
    private val stats = LifetimeStats(
        tasksCompleted = 46, focusMinutes = 1_580, focusSessions = 52, habitCheckIns = 120,
        habitsCreated = 4, moodEntries = 21, bestHabitStreak = 12, inboxCaptured = 14
    )

    @Test
    fun phoneScreenshots() {
        val habits = habits()
        val moods = InMemoryMoodRepository()
        val getBest = mockk<GetBestTaskUseCase>().also { every { it() } returns flowOf(tasks[0]) }
        val metrics = mockk<GetProductivityMetricsUseCase>().also {
            every { it(any(), any(), any()) } returns flowOf(ProductivityMetrics(3, 95, 86.0))
        }
        val insights = insights()
        val home = HomeViewModel(getBest, mockk(relaxed = true), metrics, insights, InMemoryPreferencesRepository())
        val extras = HomeExtrasViewModel(
            progressUseCase(stats), habits, InMemoryInboxRepository(), moods,
            GetPlantUseCase(progressUseCase(stats), insights, habits, moods)
        )
        shoot("1_home") { HomeScreen({}, {}, {}, {}, home, extrasViewModel = extras) }

        val clock = FakeTimeSource()
        val manager = focusSessionManager(clock = clock)
        runBlocking { manager.startSession(tasks[0], 25, 5) }
        clock.advanceSeconds(9 * 60 + 35)
        runBlocking { manager.sync() }
        val focus = FocusViewModel(
            manager, PauseFocusSessionUseCase(manager), ResumeFocusSessionUseCase(manager),
            EndFocusSessionUseCase(manager), ExtendFocusSessionUseCase(manager),
            taskRepositoryWithSteps(
                Subtask("s1", "1", "Pull the Q3 numbers", true, 0),
                Subtask("s2", "1", "Draft the summary", false, 1),
                Subtask("s3", "1", "Send it to the team", false, 2)
            )
        )
        shoot("2_focus") { FocusScreen({}, focus) }

        val getAll = mockk<GetAllTasksUseCase>().also { every { it() } returns flowOf(tasks) }
        val tasksVm = TasksViewModel(getAll, mockk(relaxed = true), mockk(relaxed = true), ScoringEngine(), EisenhowerClassifier(), mockk(relaxed = true), mockk(relaxed = true))
        shoot("3_tasks") { TasksScreen({}, {}, {}, tasksVm) }

        shoot("4_habits") { HabitsScreen(HabitsViewModel(habits)) }

        val review = ReviewViewModel(GetWeeklyReviewUseCase(performance(), habits, moodsForWeek()), insights)
        shoot("5_review") { ReviewScreen({}, review) }

        val progress = InsightsViewModel(insights, progressUseCase(stats), GetPlantUseCase(progressUseCase(stats), insights, habits, moods))
        shoot("6_progress") { InsightsScreen(progress) }
    }

    @Test
    @Config(qualifiers = "w512dp-h250dp-xhdpi")
    fun featureGraphic() {
        composeRule.setContent { VeltoTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) { FeatureGraphic() } }
        composeRule.waitForIdle()
        save(File(imagesDir, "featureGraphic.png"))
    }

    @Test
    fun icon() {
        val icon = composeRule.activity.getDrawable(com.decisionexecution.app.R.mipmap.ic_launcher) as AdaptiveIconDrawable
        // Play shows the icon on its own mask, so the full square goes up
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        listOf(icon.background, icon.foreground).forEach {
            it.setBounds(-size / 4, -size / 4, size + size / 4, size + size / 4)
            it.draw(canvas)
        }
        write(bitmap, File(imagesDir, "icon.png"))
    }

    @Composable
    private fun FeatureGraphic() {
        val scheme = MaterialTheme.colorScheme
        val stroke = with(LocalDensity.current) { Stroke(10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer)))
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Velto", style = MaterialTheme.typography.displaySmall.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize), color = scheme.onPrimaryContainer)
                    Text("Stop deciding.\nStart doing.", style = MaterialTheme.typography.headlineSmall, color = scheme.onPrimaryContainer)
                    Text("Tasks · Focus · Habits · Journal", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                }
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(170.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(scheme.surface.copy(alpha = 0.5f)))
                    CircularWavyProgressIndicator(
                        progress = { 0.68f },
                        modifier = Modifier.size(140.dp),
                        color = scheme.primary,
                        trackColor = scheme.surfaceContainerHighest,
                        stroke = stroke,
                        trackStroke = stroke,
                        wavelength = 40.dp
                    )
                    Text("17:05", style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
                }
            }
        }
    }

    // Content can only be set once per test, so later screens swap in through this
    private var screen by mutableStateOf<(@Composable () -> Unit)?>(null)

    private fun shoot(name: String, content: @Composable () -> Unit) {
        if (screen == null) {
            screen = content
            composeRule.setContent {
                VeltoTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                    // Keyed so each screen starts fresh instead of reusing the last one's state
                    androidx.compose.runtime.key(screen) { screen?.invoke() }
                }
            }
        } else {
            composeRule.runOnIdle { screen = content }
        }
        composeRule.waitForIdle()
        save(File(imagesDir, "phoneScreenshots/$name.png"))
    }

    private fun save(file: File) {
        // captureToImage waits on a frame Robolectric never delivers, so draw the view directly
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        write(bitmap, file)
    }

    private fun write(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun habits(): InMemoryHabitRepository {
        fun habit(id: String, name: String, icon: HabitIcon, days: List<Long>) =
            HabitWithHistory(Habit(id, name, icon, 1L), days.map { today.minusDays(it) }.toSet())
        return InMemoryHabitRepository(
            habit("1", "Drink water", HabitIcon.WATER, (0L..40L).filter { it % 9 != 5L }),
            habit("2", "Read 20 pages", HabitIcon.READ, (1L..60L).filter { it % 3 != 0L }),
            habit("3", "Stretch", HabitIcon.MOVE, (0L..12L).toList()),
            habit("4", "Meditate", HabitIcon.MIND, (2L..80L step 2).toList())
        )
    }

    private fun insights(): GetInsightsUseCase {
        val week = (6 downTo 0).map { ago ->
            DayStats(today.minusDays(ago.toLong()), listOf(2, 1, 3, 4, 2, 3, 3)[6 - ago], listOf(50, 25, 75, 100, 50, 75, 95)[6 - ago])
        }
        return mockk<GetInsightsUseCase>().also { every { it(any(), any()) } returns flowOf(Insights(week, streakDays = 12)) }
    }

    private fun performance(): PerformanceRepository {
        fun at(daysAgo: Long, hour: Int) = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        val titles = listOf("Finish the quarterly report", "Calculus practice set", "Book flights", "Fix the bike", "Call the bank")
        return mockk<PerformanceRepository>().also {
            every { it.getCompletionsInPeriod(any(), any()) } returns flowOf(
                titles.mapIndexed { i, t -> TaskCompletion("c$i", "t$i", t, at(i.toLong(), 16), null) } +
                    (8L..12L).map { TaskCompletion("o$it", "o", "Older", at(it, 12), null) }
            )
            every { it.getFocusSessionsInPeriod(any(), any()) } returns flowOf(
                (0L..20L).map { FocusSessionRecord("t", at(it, 11), if (it < 7) 50 else 30) }
            )
        }
    }

    private fun moodsForWeek() = InMemoryMoodRepository(
        *(0L..6L).map { MoodEntry(today.minusDays(it), mood = 3 + (it % 3).toInt(), energy = 3, note = null) }.toTypedArray()
    )

    private fun task(
        id: String, title: String, category: TaskCategory, urgency: Int, importance: Int,
        effort: TaskEffort, energy: EnergyTag?, due: LocalDate? = null, repeat: Recurrence? = null
    ) = Task(id, title, null, category, urgency, importance, effort, energy, 1_700_000_000_000L + id.toLong(), due, repeat)
}
