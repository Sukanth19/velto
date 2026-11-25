package com.decisionexecution.app.ecosystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitLinksTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `links map keys to habits both ways`() {
        val links = HabitLinks(context)
        assertTrue(links.enabled)
        assertTrue(links.linkedKeys().isEmpty())
        links.link(Ecosystem.Habits.CREATINE, "h1")
        links.link(Ecosystem.Habits.WATER, "h2")
        assertEquals("h1", links.habitFor(Ecosystem.Habits.CREATINE))
        assertEquals(Ecosystem.Habits.WATER, links.keyFor("h2"))
        assertNull(links.keyFor("unlinked"))
        assertEquals(listOf("Creatine", "Water"), links.linkedKeys().map(Ecosystem.Habits::label))
        links.enabled = false
        assertFalse(HabitLinks(context).enabled)
    }

    @Test
    fun `no other app is trusted without a pinned signature`() {
        assertFalse(Ecosystem.isTrusted(context, "com.example.fake"))
        assertFalse(Ecosystem.isTrusted(context, Ecosystem.PUPDR)) // not installed in the test sandbox
        assertFalse(Ecosystem.isTrusted(context, null))
    }
}
