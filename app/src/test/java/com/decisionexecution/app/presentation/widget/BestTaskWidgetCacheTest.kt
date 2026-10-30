package com.decisionexecution.app.presentation.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The best-task widget keeps showing the last known task when loading fails or hangs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BestTaskWidgetCacheTest {

    private val cache = BestTaskWidgetCache(ApplicationProvider.getApplicationContext<Context>())
    private val known = BestTaskWidgetState("t-1", "Write essay", "School · High effort")

    @Test
    fun `fresh data is shown and remembered`() = runTest {
        loadBestTaskWidgetState(cache, 1_000) { known } shouldBe known
        cache.load() shouldBe known
    }

    @Test
    fun `a failing load falls back to the last known task`() = runTest {
        cache.save(known)
        loadBestTaskWidgetState(cache, 1_000) { throw IllegalStateException("db closed") } shouldBe known
    }

    @Test
    fun `a hanging load times out to the last known task`() = runTest {
        cache.save(known)
        loadBestTaskWidgetState(cache, 1_000) { awaitCancellation() } shouldBe known
    }

    @Test
    fun `with nothing cached a failure shows the empty state`() = runTest {
        loadBestTaskWidgetState(cache, 1_000) { throw IllegalStateException() } shouldBe bestTaskWidgetState(null)
    }
}
