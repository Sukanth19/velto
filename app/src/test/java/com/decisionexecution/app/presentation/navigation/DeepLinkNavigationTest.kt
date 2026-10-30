package com.decisionexecution.app.presentation.navigation

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Deep links from notifications and widgets must behave like tapping the matching tab.
 * Regression: arriving via the focus notification left Focus inside the Home tab's stack,
 * so tapping Home showed Focus again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeepLinkNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext())
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            // Same routes as AppNavHost with empty screens
            NavHost(navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {}
                composable(Routes.TASKS) {}
                composable(Routes.FOCUS) {}
                composable(Routes.JUST_START) {}
                composable(Routes.TASK_DETAIL) {}
            }
        }
    }

    private fun open(link: String) = composeRule.runOnIdle { navController.openDeepLink(Uri.parse(link)) }

    private fun tab(route: String) = composeRule.runOnIdle { navController.navigateToTab(route) }

    private val current get() = composeRule.runOnIdle { navController.currentDestination?.route }

    @Test
    fun `focus link then Home tab shows Home`() {
        open(DeepLinks.FOCUS) shouldBe true
        current shouldBe Routes.FOCUS

        tab(Routes.HOME)
        current shouldBe Routes.HOME
    }

    @Test
    fun `focus link keeps other tabs clean`() {
        open(DeepLinks.FOCUS)
        tab(Routes.TASKS)
        current shouldBe Routes.TASKS
        tab(Routes.HOME)
        current shouldBe Routes.HOME
        tab(Routes.FOCUS)
        current shouldBe Routes.FOCUS
    }

    @Test
    fun `open_task link opens that task's details`() {
        open(DeepLinks.openTask("t-42")) shouldBe true
        current shouldBe Routes.TASK_DETAIL
        composeRule.runOnIdle { navController.currentBackStackEntry?.arguments?.getString("taskId") } shouldBe "t-42"
    }

    @Test
    fun `just_start link goes to the just start route`() {
        open(DeepLinks.JUST_START) shouldBe true
        current shouldBe Routes.JUST_START
    }

    @Test
    fun `unknown links are ignored`() {
        open("https://example.com/focus") shouldBe false
        open("decisionexecution://nope") shouldBe false
        open("decisionexecution://open_task") shouldBe false
        current shouldBe Routes.HOME
    }
}
