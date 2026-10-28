package com.decisionexecution.app.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.service.FocusSessionManager
import com.decisionexecution.app.testutil.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs the real worker against a session manager with a fake clock.
 * Requirements: 13.1, 13.2, 13.3
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusTimerWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val task = Task("t-1", "Write essay", null, TaskCategory.SCHOOL, 4, 5, TaskEffort.HIGH, null, 1L)

    private fun workerFor(manager: FocusSessionManager): FocusTimerWorker =
        TestListenableWorkerBuilder<FocusTimerWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters): ListenableWorker =
                    FocusTimerWorker(appContext, params, manager)
            })
            .build()

    @Test
    fun `worker moves an expired focus phase to break and notifies`() = runTest {
        val clock = FakeTimeSource()
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(clock = clock, notifier = notifier)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        clock.advanceSeconds(25 * 60)

        workerFor(manager).doWork() shouldBe ListenableWorker.Result.success()

        manager.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
        notifier.focusCompleted shouldBe listOf("Write essay")
    }

    @Test
    fun `worker finishes an expired break and notifies`() = runTest {
        val clock = FakeTimeSource()
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(clock = clock, notifier = notifier)
        manager.startSession(task, focusDuration = 25, breakDuration = 5)
        clock.advanceSeconds(25 * 60)
        manager.sync()
        clock.advanceSeconds(5 * 60)

        workerFor(manager).doWork() shouldBe ListenableWorker.Result.success()

        manager.sessionState.value shouldBe FocusSessionState.Idle
        notifier.breakCompletedCount shouldBe 1
    }

    @Test
    fun `worker restores the session after process death`() = runTest {
        val clock = FakeTimeSource()
        val prefs = InMemoryPreferencesRepository()
        val tasks = taskRepositoryOf(task)
        focusSessionManager(prefs, tasks, clock).startSession(task, focusDuration = 25, breakDuration = 5)
        clock.advanceSeconds(25 * 60)

        // Only the worker runs in the new process
        val notifier = RecordingNotifier()
        val revived = focusSessionManager(prefs, tasks, clock, notifier = notifier)
        workerFor(revived).doWork() shouldBe ListenableWorker.Result.success()

        revived.sessionState.value.shouldBeInstanceOf<FocusSessionState.Breaking>()
        notifier.focusCompleted.size shouldBe 1
    }

    @Test
    fun `worker does nothing when no session exists`() = runTest {
        val notifier = RecordingNotifier()
        val manager = focusSessionManager(notifier = notifier)

        workerFor(manager).doWork() shouldBe ListenableWorker.Result.success()

        manager.sessionState.value shouldBe FocusSessionState.Idle
        notifier.focusCompleted.size shouldBe 0
        notifier.breakCompletedCount shouldBe 0
    }
}
