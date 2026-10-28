package com.decisionexecution.app.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import com.decisionexecution.app.presentation.home.HomeViewModel
import com.decisionexecution.app.presentation.settings.SettingsScreen
import com.decisionexecution.app.presentation.settings.SettingsViewModel
import com.decisionexecution.app.presentation.tasks.TasksScreen
import com.decisionexecution.app.presentation.tasks.TasksViewModel
import com.decisionexecution.app.presentation.theme.DecisionExecutionTheme
import io.mockk.every
import io.mockk.mockk
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
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics)

        render("home") { HomeScreen({}, {}, {}, {}, viewModel) }

        composeRule.onNodeWithText("Just Start").assertIsDisplayed()
        composeRule.onNodeWithText("Finish quarterly report").assertIsDisplayed()
        composeRule.onNodeWithText("85m").assertIsDisplayed()
    }

    @Test
    fun homeScreenDark() {
        val getBest = mockk<GetBestTaskUseCase>()
        every { getBest() } returns flowOf(null)
        val metrics = mockk<GetProductivityMetricsUseCase>()
        every { metrics(any(), any(), any()) } returns flowOf(ProductivityMetrics(0, 0, 0.0))
        val viewModel = HomeViewModel(getBest, mockk(relaxed = true), metrics)

        render("home_empty_dark", ThemeMode.DARK) { HomeScreen({}, {}, {}, {}, viewModel) }

        composeRule.onNodeWithText("Nothing to do yet").assertIsDisplayed()
    }

    @Test
    fun tasksScreenFiltersByCategory() {
        val viewModel = tasksViewModel()

        render("tasks") { TasksScreen({}, {}, {}, viewModel) }
        composeRule.onNodeWithText("Tasks · 4 active").assertIsDisplayed()
        composeRule.onNodeWithText("Buy groceries").assertIsDisplayed()

        composeRule.onNodeWithText("School").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Study for calculus exam").assertIsDisplayed()
        assertNotVisible("Buy groceries")
        // Count still reflects every active task
        composeRule.onNodeWithText("Tasks · 4 active").assertIsDisplayed()
        save("tasks_filtered_school")
    }

    @Test
    fun focusScreenFocusingAndBreak() {
        val manager = FocusSessionManager(mockk<PreferencesRepository>(relaxed = true))
        runBlocking { manager.startSession(tasks[0], 25, 5) }
        manager.updateElapsedSeconds(9 * 60 + 35)
        val viewModel = FocusViewModel(
            manager,
            PauseFocusSessionUseCase(manager),
            ResumeFocusSessionUseCase(manager),
            EndFocusSessionUseCase(manager)
        )

        render("focus") { FocusScreen({}, viewModel) }
        composeRule.onNodeWithText("15:25").assertIsDisplayed()
        composeRule.onNodeWithText("FOCUS").assertIsDisplayed()

        composeRule.onNodeWithText("Pause").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PAUSED").assertIsDisplayed()
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
        save("focus_paused")

        composeRule.onNodeWithText("Resume").performClick()
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

    private fun tasksViewModel(): TasksViewModel {
        val getAll = mockk<GetAllTasksUseCase>()
        every { getAll() } returns flowOf(tasks)
        return TasksViewModel(getAll, mockk(relaxed = true), mockk(relaxed = true), ScoringEngine())
    }

    // LazyColumn may keep removed items composed for reuse, so check visibility rather than existence
    private fun assertNotVisible(text: String) {
        val nodes = composeRule.onAllNodesWithText(text)
        nodes.fetchSemanticsNodes().indices.forEach { nodes[it].assertIsNotDisplayed() }
    }

    private fun render(name: String, themeMode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        composeRule.setContent { DecisionExecutionTheme(themeMode = themeMode) { content() } }
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
