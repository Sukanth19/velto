package com.decisionexecution.app.presentation

import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.testutil.emptyInsights
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
        task("3", "Study for calculus exam", TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, EnergyTag.DEEP_WORK),
        task("4", "Buy groceries", TaskCategory.PERSONAL, 2, 3, TaskEffort.LOW, null)
    )

    @Test
    fun homeScreen() {
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(tasks[0])
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(3, 85, 71.4))
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, emptyInsights())

        render("home") { HomeScreen({}, {}, {}, {}, viewModel) }

        composeRule.onNodeWithText("Just Start").assertIsDisplayed()
        composeRule.onNodeWithText("Finish quarterly report").assertIsDisplayed()
        composeRule.onNodeWithText("1h 25m").assertIsDisplayed()
    }

    @Test
    fun homeScreenDark() {
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(null)
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(0, 0, 0.0))
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics, emptyInsights())

        render("home_empty_dark", ThemeMode.DARK) { HomeScreen({}, {}, {}, {}, viewModel) }

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
        // Requirements: 9.3, 12.1 (accessibility)
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
            EndFocusSessionUseCase(manager)
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
        val viewModel = AddEditTaskViewModel(SavedStateHandle(), getTask, mockk(relaxed = true), mockk(relaxed = true))

        render("add_task") { AddEditTaskScreen({}, viewModel) }
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Title is required").assertIsDisplayed()
        save("add_task_error")
    }

    @Test
    fun settingsScreen() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        every { prefs.getPreferences() } returns flowOf(UserPreferences(45, 10, ThemeMode.SYSTEM))

        render("settings") { SettingsScreen({}, SettingsViewModel(prefs)) }
        composeRule.onNodeWithText("45 min").assertIsDisplayed()
        composeRule.onNodeWithText("10 min").assertIsDisplayed()
    }

    @Test
    fun tappingTheCircleCompletesAndOffersUndo() {
        val complete = mockk<CompleteTaskUseCase>()
        coEvery { complete(any(), any()) } returns 123L
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
        coVerify { complete("3", any()) }
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
        save("tasks_undo_snackbar")

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()
        coVerify { undo("3", 123L) }
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

        render("insights") { InsightsScreen(InsightsViewModel(useCase)) }
        composeRule.onNodeWithText("3 days").assertIsDisplayed()
        composeRule.onNodeWithText("4h 35m").assertIsDisplayed()
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

    private fun render(name: String, themeMode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        composeRule.setContent { VeltoTheme(themeMode = themeMode, dynamicColor = false) { content() } }
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
