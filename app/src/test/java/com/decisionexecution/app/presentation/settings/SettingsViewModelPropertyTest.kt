package com.decisionexecution.app.presentation.settings

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import com.decisionexecution.app.domain.model.ThemeMode
import com.decisionexecution.app.domain.model.UserPreferences
import com.decisionexecution.app.domain.repository.PreferencesRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.Exhaustive
import io.kotest.property.exhaustive.ints
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Property-based tests for preferences validation in SettingsViewModel.
 * Feature: decision-driven-execution-app
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelPropertyTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "Property 31: Focus Duration Validation" {
        // Feature: decision-driven-execution-app, Property 31: For any focus duration value,
        // values between 5 and 90 minutes inclusive should be accepted, and values outside
        // this range should be rejected.
        // Validates: Requirements 11.1

        checkAll(Exhaustive.ints(-10..120)) { minutes ->
            val repository = createRepository()
            val viewModel = SettingsViewModel(repository)

            viewModel.onFocusDurationChanged(minutes)

            if (minutes in 5..90) {
                coVerify(exactly = 1) { repository.updateFocusDuration(minutes) }
                viewModel.uiState.value.error shouldBe null
            } else {
                coVerify(exactly = 0) { repository.updateFocusDuration(any()) }
                viewModel.uiState.value.error shouldNotBe null
            }
        }
    }

    "Property 32: Break Duration Validation" {
        // Feature: decision-driven-execution-app, Property 32: For any break duration value,
        // values between 1 and 30 minutes inclusive should be accepted, and values outside
        // this range should be rejected.
        // Validates: Requirements 11.2

        checkAll(Exhaustive.ints(-10..60)) { minutes ->
            val repository = createRepository()
            val viewModel = SettingsViewModel(repository)

            viewModel.onBreakDurationChanged(minutes)

            if (minutes in 1..30) {
                coVerify(exactly = 1) { repository.updateBreakDuration(minutes) }
                viewModel.uiState.value.error shouldBe null
            } else {
                coVerify(exactly = 0) { repository.updateBreakDuration(any()) }
                viewModel.uiState.value.error shouldNotBe null
            }
        }
    }

    "Property 31/32: range boundaries are inclusive" {
        // Validates: Requirements 11.1, 11.2
        isValidFocusDuration(4) shouldBe false
        isValidFocusDuration(5) shouldBe true
        isValidFocusDuration(90) shouldBe true
        isValidFocusDuration(91) shouldBe false
        isValidBreakDuration(0) shouldBe false
        isValidBreakDuration(1) shouldBe true
        isValidBreakDuration(30) shouldBe true
        isValidBreakDuration(31) shouldBe false
    }

    "loads stored preferences and saves theme changes" {
        // Requirements: 11.3, 11.4
        checkAll(PROPERTY_ITERATIONS, Arb.int(5..90), Arb.int(1..30), Arb.enum<ThemeMode>()) { focus, brk, theme ->
            val repository = createRepository(UserPreferences(focus, brk, ThemeMode.SYSTEM))
            val viewModel = SettingsViewModel(repository)

            viewModel.uiState.value.focusDuration shouldBe focus
            viewModel.uiState.value.breakDuration shouldBe brk

            viewModel.onThemeModeChanged(theme)
            coVerify { repository.savePreferences(UserPreferences(focus, brk, theme)) }
        }
    }
})

private fun createRepository(prefs: UserPreferences = UserPreferences()): PreferencesRepository {
    val repository = mockk<PreferencesRepository>(relaxed = true)
    every { repository.getPreferences() } returns flowOf(prefs)
    return repository
}
