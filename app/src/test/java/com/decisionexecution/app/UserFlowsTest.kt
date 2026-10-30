package com.decisionexecution.app

import io.mockk.mockk
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.data.repository.PerformanceRepositoryImpl
import com.decisionexecution.app.data.repository.TaskRepositoryImpl
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.service.ScoringEngine
import com.decisionexecution.app.domain.usecase.*
import com.decisionexecution.app.presentation.focus.FocusUiState
import com.decisionexecution.app.presentation.focus.toUiState
import com.decisionexecution.app.presentation.home.HomeViewModel
import com.decisionexecution.app.presentation.settings.SettingsViewModel
import com.decisionexecution.app.presentation.tasks.TasksViewModel
import com.decisionexecution.app.presentation.tasks.UndoAction
import com.decisionexecution.app.domain.service.EisenhowerClassifier
import com.decisionexecution.app.testutil.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.UUID

/**
 * End-to-end flows through real use cases, repositories and an in-memory Room database.
 * Only the clock, scheduler and notifier are fakes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserFlowsTest {

    private lateinit var db: AppDatabase
    private val clock = FakeTimeSource(System.currentTimeMillis())
    private val prefs = InMemoryPreferencesRepository()
    private val notifier = RecordingNotifier()
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var performanceRepository: PerformanceRepositoryImpl
    private val scoring = ScoringEngine()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskRepository = TaskRepositoryImpl(db.taskDao(), db.taskCompletionDao(), db.subtaskDao())
        performanceRepository = PerformanceRepositoryImpl(db.taskCompletionDao(), db.focusSessionDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private val manager by lazy {
        focusSessionManager(prefs, taskRepository, clock, notifier = notifier, performance = performanceRepository)
    }
    private val getBestTask by lazy { GetBestTaskUseCase(taskRepository, scoring) }
    private val justStart by lazy { JustStartUseCase(getBestTask, StartFocusSessionUseCase(manager), prefs) }
    private val metrics by lazy { GetProductivityMetricsUseCase(performanceRepository) }

    private fun task(title: String, category: TaskCategory, urgency: Int, importance: Int) = Task(
        UUID.randomUUID().toString(), title, null, category, urgency, importance, TaskEffort.MEDIUM, null, clock.now
    )

    private fun todaysMetrics() = metrics(clock.now - 24 * 3600_000L, clock.now + 3600_000L, 7)

    @Test
    fun `just start picks the best task and runs focus then break to completion`() = runTest {
        val create = CreateTaskUseCase(taskRepository)
        create(task("Reply to email", TaskCategory.WORK, 2, 2))
        val important = task("Finish report", TaskCategory.WORK, 5, 5)
        create(important)

        val home = HomeViewModel(getBestTask, justStart, metrics, GetInsightsUseCase(performanceRepository), InMemoryPreferencesRepository())
        settle()
        home.uiState.value.bestTask shouldBe important

        home.onJustStartClicked()
        settle()
        home.uiState.value.focusSessionStarted shouldBe true
        (manager.sessionState.value as FocusSessionState.Focusing).task shouldBe important

        // What the Focus screen shows. Not FocusViewModel itself: its 1s tick loop never ends,
        // so runTest would keep advancing virtual time forever
        val focusUi = { manager.sessionState.value.toUiState() }
        clock.advanceSeconds(10 * 60)
        manager.sync()
        (focusUi() as FocusUiState.Focus).remainingSeconds shouldBe 15 * 60

        clock.advanceSeconds(15 * 60)
        manager.sync()
        focusUi().shouldBeInstanceOf<FocusUiState.Break>()
        notifier.focusCompleted shouldBe listOf("Finish report")

        clock.advanceSeconds(5 * 60)
        manager.sync()
        focusUi() shouldBe FocusUiState.Idle
        notifier.breakCompletedCount shouldBe 1

        // Finished focus time shows up in today's stats without counting as a completed task
        val today = todaysMetrics().first()
        today.totalFocusTime shouldBe 25
        today.completedTasks shouldBe 0
    }

    @Test
    fun `created task appears in its category, swipe-complete removes it and counts in metrics`() = runTest {
        val tasks = TasksViewModel(
            GetAllTasksUseCase(taskRepository),
            CompleteTaskUseCase(taskRepository, performanceRepository),
            DeleteTaskUseCase(taskRepository),
            scoring,
            EisenhowerClassifier(),
            UndoCompleteTaskUseCase(taskRepository, performanceRepository),
            CreateTaskUseCase(taskRepository)
        )
        val created = task("Read chapter 4", TaskCategory.SCHOOL, 3, 4)
        CreateTaskUseCase(taskRepository)(created)
        settle()

        tasks.uiState.value.tasksByCategory[TaskCategory.SCHOOL] shouldBe listOf(created)
        tasks.uiState.value.activeTaskCount shouldBe 1

        tasks.onTaskComplete(created.id)
        settle()

        tasks.uiState.value.activeTaskCount shouldBe 0
        tasks.uiState.value.tasksByCategory.values.flatten() shouldBe emptyList()
        todaysMetrics().first().completedTasks shouldBe 1

        // Undo brings it back and takes it out of the metrics again
        tasks.onUndo()
        settle()
        tasks.uiState.value.tasksByCategory[TaskCategory.SCHOOL] shouldBe listOf(created)
        todaysMetrics().first().completedTasks shouldBe 0
    }

    @Test
    fun `deleting a task can be undone`() = runTest {
        val tasks = TasksViewModel(
            GetAllTasksUseCase(taskRepository),
            CompleteTaskUseCase(taskRepository, performanceRepository),
            DeleteTaskUseCase(taskRepository),
            scoring,
            EisenhowerClassifier(),
            UndoCompleteTaskUseCase(taskRepository, performanceRepository),
            CreateTaskUseCase(taskRepository)
        )
        val created = task("Call the bank", TaskCategory.PERSONAL, 4, 3)
        CreateTaskUseCase(taskRepository)(created)
        settle()

        tasks.onTaskDelete(created.id)
        settle()
        tasks.uiState.value.activeTaskCount shouldBe 0
        tasks.uiState.value.undo shouldBe UndoAction.Deleted(created)

        tasks.onUndo()
        settle()
        tasks.uiState.value.tasksByCategory[TaskCategory.PERSONAL] shouldBe listOf(created)
    }

    @Test
    fun `finished focus sessions and completions build the streak and week`() = runTest {
        CreateTaskUseCase(taskRepository)(task("Write", TaskCategory.WORK, 5, 5))
        justStart()
        clock.advanceSeconds(25 * 60)
        manager.sync()
        settle()

        val insights = GetInsightsUseCase(performanceRepository)().first()
        insights.streakDays shouldBe 1
        insights.week.last().focusMinutes shouldBe 25
        insights.weekFocusMinutes shouldBe 25
    }

    @Test
    fun `changed preferences apply to the next focus session`() = runTest {
        CreateTaskUseCase(taskRepository)(task("Practice piano", TaskCategory.PERSONAL, 3, 3))
        val settings = SettingsViewModel(prefs, mockk(relaxed = true))

        settings.onFocusDurationChanged(50)
        settings.onBreakDurationChanged(10)
        justStart()

        (manager.sessionState.value as FocusSessionState.Focusing).totalSeconds shouldBe 50 * 60
        clock.advanceSeconds(50 * 60)
        manager.sync()
        (manager.sessionState.value as FocusSessionState.Breaking).totalSeconds shouldBe 10 * 60
    }

    @Test
    fun `just start with no tasks tells the user to add one`() = runTest {
        val home = HomeViewModel(getBestTask, justStart, metrics, GetInsightsUseCase(performanceRepository), InMemoryPreferencesRepository())
        settle()
        home.onJustStartClicked()
        settle()
        home.uiState.value.error shouldBe HomeViewModel.NO_TASKS_ERROR
        manager.sessionState.value shouldBe FocusSessionState.Idle
    }

    // Room delivers invalidations on its own executor; let them land
    private fun settle() {
        repeat(5) {
            Thread.sleep(20)
            ShadowLooper.idleMainLooper()
        }
    }
}
