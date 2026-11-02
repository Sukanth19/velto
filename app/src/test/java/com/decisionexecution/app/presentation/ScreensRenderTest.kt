package com.decisionexecution.app.presentation

import com.decisionexecution.app.testutil.noCalibration
import com.decisionexecution.app.testutil.stubPick
import com.decisionexecution.app.domain.usecase.DayPhaseSource

import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.testutil.taskRepositoryWithSteps
import com.decisionexecution.app.testutil.InMemoryPreferencesRepository
import com.decisionexecution.app.testutil.emptyInsights
import com.decisionexecution.app.testutil.InMemoryHabitRepository
import com.decisionexecution.app.testutil.InMemoryInboxRepository
import com.decisionexecution.app.testutil.InMemoryMoodRepository
import com.decisionexecution.app.testutil.progressUseCase
import com.decisionexecution.app.testutil.plantUseCase
import com.decisionexecution.app.presentation.decide.DecideScreen
import com.decisionexecution.app.presentation.decide.DecideViewModel
import com.decisionexecution.app.domain.service.LifetimeStats
import com.decisionexecution.app.presentation.habits.HabitsScreen
import com.decisionexecution.app.presentation.habits.HabitsViewModel
import com.decisionexecution.app.presentation.home.HomeExtrasViewModel
import com.decisionexecution.app.presentation.inbox.InboxScreen
import com.decisionexecution.app.presentation.inbox.InboxViewModel
import com.decisionexecution.app.presentation.journal.JournalScreen
import com.decisionexecution.app.presentation.journal.JournalViewModel
import java.time.LocalDate
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.lifecycle.SavedStateHandle
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.repository.PreferencesRepository
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.usecase.*
import com.decisionexecution.app.presentation.addedit.AddEditTaskScreen
import com.decisionexecution.app.presentation.addedit.AddEditTaskViewModel
import com.decisionexecution.app.presentation.focus.FocusScreen
import com.decisionexecution.app.presentation.focus.FocusViewModel
import com.decisionexecution.app.presentation.home.HomeScreen
import com.decisionexecution.app.presentation.insights.InsightsScreen
import com.decisionexecution.app.presentation.insights.InsightsViewModel
import com.decisionexecution.app.presentation.home.HomeViewModel
import com.decisionexecution.app.presentation.settings.SettingsScreen
import com.decisionexecution.app.presentation.settings.SettingsViewModel
import com.decisionexecution.app.presentation.tasks.TasksScreen
import com.decisionexecution.app.presentation.tasks.TasksViewModel
import com.decisionexecution.app.presentation.theme.VeltoTheme
import com.decisionexecution.app.domain.model.ThemePalette
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import com.decisionexecution.app.testutil.FakeTimeSource
import com.decisionexecution.app.testutil.focusSessionManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders each screen with real ViewModels over mocked data, checks key content is shown,
 * and saves a PNG per screen to app/build/screenshots for eyeballing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xhdpi")
class ScreensRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val tasks = listOf(
        task("1", "Finish quarterly report", TaskCategory.WORK, 5, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK),
        task("2", "Reply to client email", TaskCategory.WORK, 4, 2, TaskEffort.LOW, EnergyTag.LOW),
        task("3", "Study for calculus exam", TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK)
            .copy(dueDate = LocalDate.now().plusDays(1)),
        task("4", "Buy groceries", TaskCategory.PERSONAL, 2, 3, TaskEffort.LOW, null)
            .copy(dueDate = LocalDate.now().plusDays(4), recurrence = Recurrence.WEEKLY)
    )

    @Test
    fun homeScreen() {
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(tasks[0])
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(3, 85, 71.4))
        stubPick(getBest)
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, emptyInsights(), InMemoryPreferencesRepository(), mockk(relaxed = true), mockk(relaxed = true), noCalibration())

        val habits = sampleHabits()
        val extras = HomeExtrasViewModel(progressUseCase(SAMPLE_STATS), habits, InMemoryInboxRepository(*sampleInbox()), InMemoryMoodRepository(), plantUseCase(SAMPLE_STATS, habits))

        render("home") { HomeScreen({}, {}, {}, {}, viewModel, extrasViewModel = extras) }

        composeRule.onNodeWithText("Just Start").assertIsDisplayed()
        composeRule.onNodeWithText("Finish quarterly report").assertIsDisplayed()
        composeRule.onNodeWithText("1h 25m").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("How are you feeling?").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mover").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun homeScreenDark() {
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(null)
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(0, 0, 0.0))
        // Level 1 with a 1-day streak: the level and streak chips should line up
        val insights = mockk<GetInsightsUseCase>()
        every { insights(any(), any()) } returns flowOf(Insights(week = emptyList(), streakDays = 1))
        stubPick(getBest)
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, insights, InMemoryPreferencesRepository(), mockk(relaxed = true), mockk(relaxed = true), noCalibration())

        val extras = HomeExtrasViewModel(progressUseCase(), InMemoryHabitRepository(), InMemoryInboxRepository(), InMemoryMoodRepository(), plantUseCase())

        render("home_empty_dark", ThemeMode.DARK) { HomeScreen({}, {}, {}, {}, viewModel, extrasViewModel = extras) }

        composeRule.onNodeWithText("Nothing to do yet").assertIsDisplayed()
    }

    @Test
    fun tasksScreenFiltersByCategory() {
        val viewModel = tasksViewModel()

        render("tasks") { TasksScreen({}, {}, {}, viewModel) }
        composeRule.onNodeWithText("4 active").assertIsDisplayed()
        composeRule.onNodeWithText("Buy groceries").assertIsDisplayed()

        composeRule.onAllNodesWithText("School").onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Study for calculus exam").assertIsDisplayed()
        assertNotVisible("Buy groceries")
        // Count still reflects every active task
        composeRule.onNodeWithText("4 active").assertIsDisplayed()
        save("tasks_filtered_school")
    }

    @Test
    fun taskRowsAreUsableWithScreenReaders() {
        val complete = mockk<CompleteTaskUseCase>(relaxed = true)
        val getAll = mockk<GetAllTasksUseCase>()
        every { getAll() } returns flowOf(tasks)
        val viewModel = TasksViewModel(getAll, complete, mockk(relaxed = true), ScoringEngine(), EisenhowerClassifier(), mockk(relaxed = true), mockk(relaxed = true))
        render("tasks_a11y") { TasksScreen({}, {}, {}, viewModel) }

        // Swipe-only actions are also offered as accessibility actions
        // The node TalkBack focuses is the nearest ancestor of the title that carries the actions
        val title = composeRule.onNodeWithText("Buy groceries", useUnmergedTree = true).fetchSemanticsNode()
        val row = generateSequence(title) { it.parent }.first { SemanticsActions.CustomActions in it.config }
        val actions = row.config[SemanticsActions.CustomActions]
        actions.map { it.label } shouldBe listOf("Complete task", "Edit or delete")
        composeRule.runOnIdle { actions.first().action() }
        coVerify { complete("4", any()) }

        // Compact tags are spelled out
        composeRule.onAllNodesWithContentDescription("Urgency 2 of 5").fetchSemanticsNodes().size shouldBe 1
    }

    @Test
    fun focusScreenFocusingAndBreak() {
        val clock = FakeTimeSource()
        val manager = focusSessionManager(clock = clock)
        runBlocking { manager.startSession(tasks[0], 25, 5) }
        clock.advanceSeconds(9 * 60 + 35)
        runBlocking { manager.sync() }
        val viewModel = FocusViewModel(
            manager,
            PauseFocusSessionUseCase(manager),
            ResumeFocusSessionUseCase(manager),
            EndFocusSessionUseCase(manager),
            ExtendFocusSessionUseCase(manager),
            taskRepositoryWithSteps(
                Subtask("s-1", tasks[0].id, "Pull the Q3 numbers", true, 0),
                Subtask("s-2", tasks[0].id, "Draft the summary", false, 1),
                Subtask("s-3", tasks[0].id, "Send it to the team", false, 2)
            ),
            InMemoryPreferencesRepository()
        )

        render("focus") { FocusScreen({}, viewModel) }
        composeRule.onNodeWithText("15:25").assertIsDisplayed()
        composeRule.onNodeWithText("FOCUS").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PAUSED").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Resume").assertIsDisplayed()
        save("focus_paused")

        composeRule.onNodeWithContentDescription("Resume").performClick()
        runBlocking { manager.transitionToBreak() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("BREAK").assertIsDisplayed()
        composeRule.onNodeWithText("05:00").assertIsDisplayed()
        save("focus_break")
    }

    @Test
    fun addTaskScreenShowsTitleError() {
        val getTask = mockk<GetTaskByIdUseCase>()
        val viewModel = AddEditTaskViewModel(SavedStateHandle(), getTask, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), taskRepositoryWithSteps(), noCalibration())

        render("add_task") { AddEditTaskScreen({}, viewModel) }
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Title is required").assertIsDisplayed()
        save("add_task_error")
    }

    @Test
    fun homeWithDodgedTask() {
        val dodged = tasks[0].copy(dodges = 4, estimateMinutes = 45)
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(dodged)
        every { getBest.pick(any()) } returns flowOf(com.decisionexecution.app.domain.usecase.NextPick(dodged, "Your peak focus hours. Hard stuff first"))
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(3, 85, 71.4))
        val calibration = mockk<com.decisionexecution.app.domain.usecase.GetEstimateCalibrationUseCase>()
        every { calibration() } returns flowOf(com.decisionexecution.app.domain.service.EstimateCalibration(1.6, 5))
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, emptyInsights(), InMemoryPreferencesRepository(), mockk(relaxed = true), mockk(relaxed = true), calibration)
        val habits = sampleHabits()
        val extras = HomeExtrasViewModel(progressUseCase(SAMPLE_STATS), habits, InMemoryInboxRepository(*sampleInbox()), InMemoryMoodRepository(), plantUseCase(SAMPLE_STATS, habits))

        render("home_dodged") { HomeScreen({}, {}, {}, {}, viewModel, extrasViewModel = extras) }
        composeRule.onNodeWithText("Shrink it").assertIsDisplayed()
        composeRule.onNodeWithText("Not now").assertIsDisplayed()
    }

    @Test
    fun focusAfterStarter() {
        val clock = FakeTimeSource()
        val manager = focusSessionManager(clock = clock)
        runBlocking {
            manager.startStarter(tasks[0])
            clock.advanceSeconds(130)
            manager.sync()
        }
        val viewModel = FocusViewModel(
            manager, PauseFocusSessionUseCase(manager), ResumeFocusSessionUseCase(manager), EndFocusSessionUseCase(manager),
            ExtendFocusSessionUseCase(manager), taskRepositoryWithSteps(), InMemoryPreferencesRepository()
        )
        render("focus_starter_done") { FocusScreen({}, viewModel) }
        composeRule.onNodeWithText("Keep going for 25 min").assertIsDisplayed()
    }

    @Test
    fun plantVarieties() {
        render("plants") {
            androidx.compose.foundation.layout.Column {
                listOf(
                    com.decisionexecution.app.domain.service.PlantStage.SAPLING,
                    com.decisionexecution.app.domain.service.PlantStage.BLOOM,
                    com.decisionexecution.app.domain.service.PlantStage.TREE
                ).forEach { stage ->
                    androidx.compose.foundation.layout.Row {
                        com.decisionexecution.app.domain.service.PlantVariety.entries.forEach { variety ->
                            com.decisionexecution.app.presentation.components.PlantCompanion(
                                com.decisionexecution.app.domain.service.PlantState(stage, com.decisionexecution.app.domain.service.PlantHealth.THRIVING, 0, variety),
                                plantSize = androidx.compose.ui.unit.Dp(90f),
                                talkative = false
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun homeInSunset() = homeInPalette("home_sunset", ThemeMode.LIGHT, ThemePalette.SUNSET)

    @Test
    fun homeInForestPureBlack() = homeInPalette("home_forest_black", ThemeMode.DARK, ThemePalette.FOREST, pureBlack = true)

    @Test
    fun homeInOceanDark() = homeInPalette("home_ocean_dark", ThemeMode.DARK, ThemePalette.OCEAN)

    @Test
    fun homeInRose() = homeInPalette("home_rose", ThemeMode.LIGHT, ThemePalette.ROSE)

    @Test
    fun homeInGraphiteDark() = homeInPalette("home_graphite_dark", ThemeMode.DARK, ThemePalette.MONO)

    private fun homeInPalette(name: String, mode: ThemeMode, palette: ThemePalette, pureBlack: Boolean = false) {
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(tasks[0])
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(3, 85, 71.4))
        stubPick(getBest)
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, emptyInsights(), InMemoryPreferencesRepository(), mockk(relaxed = true), mockk(relaxed = true), noCalibration())

        val habits = sampleHabits()
        val extras = HomeExtrasViewModel(progressUseCase(SAMPLE_STATS), habits, InMemoryInboxRepository(*sampleInbox()), InMemoryMoodRepository(), plantUseCase(SAMPLE_STATS, habits))
        render(name, mode, palette, pureBlack) { HomeScreen({}, {}, {}, {}, viewModel, extrasViewModel = extras) }
        composeRule.onNodeWithText("Just Start").assertIsDisplayed()
    }

    @Test
    fun settingsInOcean() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        every { prefs.getPreferences() } returns flowOf(UserPreferences(themePalette = ThemePalette.OCEAN))
        render("settings_ocean", palette = ThemePalette.OCEAN) { SettingsScreen({}, SettingsViewModel(prefs, mockk(relaxed = true), progressUseCase(SAMPLE_STATS))) }
        composeRule.onNodeWithText("Ocean").assertIsDisplayed()
    }

    @Test
    fun settingsScreen() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        every { prefs.getPreferences() } returns flowOf(UserPreferences(45, 10, ThemeMode.SYSTEM))

        render("settings") { SettingsScreen({}, SettingsViewModel(prefs, mockk(relaxed = true), progressUseCase(SAMPLE_STATS))) }
        // Level 7: Rose is unlocked, Graphite and the bonsai are still to come
        composeRule.onNodeWithText("Rose").assertExists()
        composeRule.onNodeWithText("Level 9").assertExists()
        composeRule.onNodeWithText("45 min").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("10 min").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingTheCircleCompletesAndOffersUndo() {
        val complete = mockk<CompleteTaskUseCase>()
        val receipt = CompletionReceipt("3", 123L)
        coEvery { complete(any(), any(), any()) } returns receipt
        val undo = mockk<UndoCompleteTaskUseCase>(relaxed = true)
        val getAll = mockk<GetAllTasksUseCase>()
        every { getAll() } returns flowOf(tasks)
        val viewModel = TasksViewModel(getAll, complete, mockk(relaxed = true), ScoringEngine(), EisenhowerClassifier(), undo, mockk(relaxed = true))
        render("tasks_undo_before") { TasksScreen({}, {}, {}, viewModel) }

        // The first "Complete task" circle belongs to the first task listed (School section)
        val circles = composeRule.onAllNodes(
            SemanticsMatcher("complete circle") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Complete task" }
        )
        circles.onFirst().performClick()
        composeRule.waitForIdle()
        coVerify { complete("3", any(), any()) }
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
        save("tasks_undo_snackbar")

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()
        coVerify { undo(receipt) }
    }

    @Test
    fun tasksMatrixView() {
        val viewModel = tasksViewModel()
        render("tasks_matrix") { TasksScreen({}, {}, {}, viewModel) }
        composeRule.onNodeWithContentDescription("Priority matrix").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Do first").assertIsDisplayed()
        composeRule.onNodeWithText("• Finish quarterly report").assertIsDisplayed()
        save("tasks_matrix")
    }

    @Test
    fun insightsScreen() {
        val today = java.time.LocalDate.now()
        val week = (6 downTo 0).map { ago ->
            DayStats(today.minusDays(ago.toLong()), completedTasks = listOf(1, 0, 2, 3, 0, 1, 2)[6 - ago], focusMinutes = listOf(25, 0, 50, 75, 0, 25, 100)[6 - ago])
        }
        val useCase = mockk<GetInsightsUseCase>()
        every { useCase(any(), any()) } returns flowOf(Insights(week, streakDays = 3))

        render("insights") { InsightsScreen(InsightsViewModel(useCase, progressUseCase(SAMPLE_STATS), plantUseCase(SAMPLE_STATS, sampleHabits()))) }
        composeRule.onNodeWithText("3 days").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("4h 35m").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun launcherIcon() {
        // Draws the real adaptive icon as a launcher would, plus the themed (monochrome) variant
        val context = composeRule.activity
        val icon = context.getDrawable(com.decisionexecution.app.R.mipmap.ic_launcher) as AdaptiveIconDrawable
        val size = 432
        val bitmap = Bitmap.createBitmap(size * 3, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val mask = icon.iconMask
        val scale = size / 108f * (108f / 100f)
        fun drawMasked(left: Int, layers: List<android.graphics.drawable.Drawable>, tint: Int?) {
            canvas.save()
            canvas.translate(left.toFloat(), 0f)
            val path = android.graphics.Path(mask).apply {
                transform(android.graphics.Matrix().apply { setScale(scale * 100 / 108f, scale * 100 / 108f) })
            }
            canvas.clipPath(path)
            layers.forEachIndexed { i, d ->
                d.setBounds(-size / 6, -size / 6, size + size / 6, size + size / 6)
                // Themed icons tint only the monochrome (top) layer
                if (tint != null && i == layers.lastIndex) d.setTint(tint)
                d.draw(canvas)
            }
            canvas.restore()
        }
        drawMasked(0, listOf(icon.background, icon.foreground), null)
        canvas.drawRect(size.toFloat(), 0f, size * 2f, size.toFloat(), android.graphics.Paint().apply { color = android.graphics.Color.rgb(28, 27, 32) })
        drawMasked(size, listOf(icon.background, icon.foreground), null)
        val themedBg = android.graphics.drawable.ColorDrawable(android.graphics.Color.rgb(230, 222, 255))
        drawMasked(size * 2, listOf(themedBg, icon.monochrome!!.mutate()), android.graphics.Color.rgb(53, 22, 143))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "launcher_icon.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tasksViewModel(): TasksViewModel {
        val getAll = mockk<GetAllTasksUseCase>()
        every { getAll() } returns flowOf(tasks)
        return TasksViewModel(getAll, mockk(relaxed = true), mockk(relaxed = true), ScoringEngine(), EisenhowerClassifier(), mockk(relaxed = true), mockk(relaxed = true))
    }

    // LazyColumn may keep removed items composed for reuse, so check visibility rather than existence
    private fun assertNotVisible(text: String) {
        val nodes = composeRule.onAllNodesWithText(text)
        nodes.fetchSemanticsNodes().indices.forEach { nodes[it].assertIsNotDisplayed() }
    }

    @Test
    fun habitsScreen() {
        val viewModel = HabitsViewModel(sampleHabits())

        render("habits") { HabitsScreen(viewModel) }
        composeRule.onNodeWithText("Drink water").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 3 done today").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Mark Read 20 pages done").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 of 3 done today").assertIsDisplayed()
    }

    @Test
    fun habitsScreenEmptyDark() {
        render("habits_empty_dark", ThemeMode.DARK) { HabitsScreen(HabitsViewModel(InMemoryHabitRepository())) }
        composeRule.onNodeWithText("Build a streak").assertIsDisplayed()
    }

    @Test
    fun inboxScreen() {
        val inbox = InMemoryInboxRepository(*sampleInbox())
        val viewModel = InboxViewModel(inbox, InMemoryHabitRepository())

        render("inbox") { InboxScreen({}, {}, viewModel = viewModel) }
        composeRule.onNodeWithText("Look into a standing desk").assertIsDisplayed()

        composeRule.onAllNodesWithContentDescription("Bin it").onFirst().performClick()
        composeRule.waitForIdle()
        inbox.items.value.size shouldBe 1
    }

    @Test
    fun journalScreen() {
        val today = LocalDate.now()
        val moods = InMemoryMoodRepository(
            *(1..12).map { back ->
                MoodEntry(today.minusDays(back.toLong()), mood = 1 + back % 5, energy = 1 + (back * 2) % 5, note = if (back == 1) "Slept badly" else null)
            }.toTypedArray()
        )
        val performance = mockk<com.decisionexecution.app.domain.repository.PerformanceRepository>()
        every { performance.getFocusSessionsInPeriod(any(), any()) } returns flowOf(
            (1..12).map { back ->
                com.decisionexecution.app.domain.repository.FocusSessionRecord(
                    "t", today.minusDays(back.toLong()).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    durationMinutes = 15 * (1 + (back * 2) % 5)
                )
            }
        )
        val viewModel = JournalViewModel(moods, GetMoodInsightsUseCase(moods, performance))

        render("journal") { JournalScreen({}, viewModel) }
        composeRule.onNodeWithText("How are you feeling?").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun decideScreen() {
        val taskRepository = mockk<com.decisionexecution.app.domain.repository.TaskRepository>()
        every { taskRepository.getAllTasks() } returns flowOf(tasks)
        val moods = InMemoryMoodRepository(MoodEntry(LocalDate.now(), mood = 4, energy = 2, note = null))
        val viewModel = DecideViewModel(
            SavedStateHandle(mapOf(DecideViewModel.MINUTES_ARG to 15)),
            PickTaskUseCase(taskRepository, moods, DayPhaseSource.NEUTRAL, noCalibration()),
            mockk(relaxed = true)
        )

        render("decide") { DecideScreen({}, {}, viewModel) }
        // Short on time and energy: the quick, low-energy email beats the big report
        composeRule.mainClock.advanceTimeBy(5_000)
        composeRule.onNodeWithText("Reply to client email").assertIsDisplayed()
        save("decide")
    }

    @Test
    fun navBar() {
        var selected by androidx.compose.runtime.mutableStateOf("Home")
        val tabs = listOf(
            Triple("Home", androidx.compose.material.icons.Icons.Rounded.Home, androidx.compose.material.icons.Icons.Outlined.Home),
            Triple("Tasks", androidx.compose.material.icons.Icons.Rounded.TaskAlt, androidx.compose.material.icons.Icons.Outlined.TaskAlt),
            Triple("Habits", androidx.compose.material.icons.Icons.Rounded.LocalFireDepartment, androidx.compose.material.icons.Icons.Outlined.LocalFireDepartment),
            Triple("Focus", androidx.compose.material.icons.Icons.Rounded.Timer, androidx.compose.material.icons.Icons.Outlined.Timer),
            Triple("Progress", androidx.compose.material.icons.Icons.Rounded.Insights, androidx.compose.material.icons.Icons.Outlined.Insights)
        )
        render("navbar", ThemeMode.DARK) {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surface),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                com.decisionexecution.app.presentation.navigation.VeltoNavBar(
                    tabs.map { (label, filled, outlined) ->
                        com.decisionexecution.app.presentation.navigation.NavBarItem(label, outlined, filled, selected == label) { selected = label }
                    }
                )
            }
        }
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Habits").performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithText("Habits").assertIsDisplayed()
        save("navbar_habits")
    }

    @Test
    fun reviewScreen() {
        val today = LocalDate.now()
        val zone = java.time.ZoneId.systemDefault()
        fun at(daysAgo: Long, hour: Int) = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        val performance = mockk<com.decisionexecution.app.domain.repository.PerformanceRepository>()
        every { performance.getCompletionsInPeriod(any(), any()) } returns flowOf(
            listOf("Finish quarterly report", "Study for calculus exam", "Reply to client email", "Buy groceries")
                .mapIndexed { i, title -> com.decisionexecution.app.domain.repository.TaskCompletion("c$i", "t$i", title, at(i.toLong(), 15), null) } +
                com.decisionexecution.app.domain.repository.TaskCompletion("old", "t9", "Old", at(9, 12), null)
        )
        every { performance.getFocusSessionsInPeriod(any(), any()) } returns flowOf(
            (0L..12L).map { com.decisionexecution.app.domain.repository.FocusSessionRecord("t", at(it, 10), if (it < 7) 50 else 30) } +
                com.decisionexecution.app.domain.repository.FocusSessionRecord("t", at(2, 21), 25)
        )
        val moods = InMemoryMoodRepository(*(0L..6L).map { MoodEntry(today.minusDays(it), 3 + (it % 3).toInt(), 3, null) }.toTypedArray())
        val insights = mockk<GetInsightsUseCase>()
        every { insights(any(), any()) } returns flowOf(Insights(emptyList(), streakDays = 6))
        val viewModel = com.decisionexecution.app.presentation.review.ReviewViewModel(
            GetWeeklyReviewUseCase(performance, sampleHabits(), moods),
            insights
        )

        render("review") { com.decisionexecution.app.presentation.review.ReviewScreen({}, viewModel) }
        composeRule.onNodeWithText("tasks done").assertIsDisplayed()
        composeRule.onNodeWithText("Finish quarterly report").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("When you focus best").performScrollTo().assertIsDisplayed()
        save("review_scrolled")
    }

    private fun sampleHabits(): InMemoryHabitRepository {
        val today = LocalDate.now()
        fun habit(id: String, name: String, icon: HabitIcon, days: List<Long>) =
            HabitWithHistory(Habit(id, name, icon, id.toLong()), days.map { today.minusDays(it) }.toSet())
        return InMemoryHabitRepository(
            habit("1", "Drink water", HabitIcon.WATER, (0L..20L).toList()),
            habit("2", "Read 20 pages", HabitIcon.READ, listOf(1L, 2L, 3L, 5L, 8L, 9L, 30L)),
            habit("3", "Stretch", HabitIcon.MOVE, (1L..60L step 2).toList())
        )
    }

    private fun sampleInbox(): Array<InboxItem> {
        val now = System.currentTimeMillis()
        return arrayOf(
            InboxItem("i-1", "Look into a standing desk", now - 26 * 60 * 60 * 1000L),
            InboxItem("i-2", "Call grandma this weekend", now - 12 * 60 * 1000L)
        )
    }

    private fun render(
        name: String,
        themeMode: ThemeMode = ThemeMode.LIGHT,
        palette: ThemePalette = ThemePalette.WALLPAPER,
        pureBlack: Boolean = false,
        content: @Composable () -> Unit
    ) {
        composeRule.setContent { VeltoTheme(themeMode = themeMode, palette = palette, pureBlack = pureBlack, dynamicColor = false) { content() } }
        composeRule.waitForIdle()
        save(name)
    }

    private fun save(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        // captureToImage waits on a frame Robolectric never delivers, so draw the view directly
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        val SAMPLE_STATS = LifetimeStats(tasksCompleted = 18, focusMinutes = 640, focusSessions = 20, habitCheckIns = 40, habitsCreated = 3, moodEntries = 8, bestHabitStreak = 9)
    }

    private fun task(
        id: String,
        title: String,
        category: TaskCategory,
        urgency: Int,
        importance: Int,
        effort: TaskEffort,
        energy: EnergyTag?
    ) = Task(id, title, null, category, urgency, importance, effort, energy, 1_700_000_000_000L + id.toLong())
}
