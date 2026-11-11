package com.decisionexecution.app.presentation.settings

import com.decisionexecution.app.domain.model.CalendarReminder
import com.decisionexecution.app.domain.service.LifetimeStats
import com.decisionexecution.app.testutil.FakeCalendarRepository
import com.decisionexecution.app.testutil.GOOGLE_CALENDAR
import com.decisionexecution.app.testutil.InMemoryPreferencesRepository
import com.decisionexecution.app.testutil.progressUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** Linking Google Calendar from settings */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsCalendarTest : StringSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    fun viewModel(calendar: FakeCalendarRepository) =
        SettingsViewModel(InMemoryPreferencesRepository(), mockk(relaxed = true), progressUseCase(LifetimeStats()), calendar)

    "granting access shows the calendars to pick from" {
        val vm = viewModel(FakeCalendarRepository())

        vm.onCalendarAccessGranted()

        vm.uiState.value.calendarChoices shouldBe listOf(GOOGLE_CALENDAR)
        vm.uiState.value.calendarError shouldBe null
    }

    "picking a calendar links it and closes the picker" {
        val calendar = FakeCalendarRepository()
        val vm = viewModel(calendar)
        vm.onCalendarAccessGranted()

        vm.onCalendarPicked(GOOGLE_CALENDAR)

        vm.uiState.value.calendarChoices shouldBe null
        vm.uiState.value.calendar.calendar shouldBe GOOGLE_CALENDAR
    }

    "denying access explains why it's needed" {
        val vm = viewModel(FakeCalendarRepository())

        vm.onCalendarAccessDenied()

        vm.uiState.value.calendarError shouldBe "Velto needs calendar access to add your tasks to it"
        vm.uiState.value.calendarChoices shouldBe null
    }

    "a phone with no calendars says to sign in first" {
        val vm = viewModel(FakeCalendarRepository(calendars = emptyList()))

        vm.onCalendarAccessGranted()

        vm.uiState.value.calendarChoices shouldBe null
        vm.uiState.value.calendarError shouldBe "No calendars on this phone yet. Sign in to Google Calendar first, then try again."
    }

    "access taken away between asking and reading counts as denied" {
        val vm = viewModel(FakeCalendarRepository().apply { failWith = SecurityException("revoked") })

        vm.onCalendarAccessGranted()

        vm.uiState.value.calendarError shouldBe "Velto needs calendar access to add your tasks to it"
    }

    "unlinking and changing the reminder are saved" {
        val calendar = FakeCalendarRepository(linked = GOOGLE_CALENDAR)
        val vm = viewModel(calendar)
        vm.uiState.value.calendar.isLinked shouldBe true

        vm.onCalendarReminderChanged(CalendarReminder.WEEK_BEFORE)
        vm.onUnlinkCalendar()

        vm.uiState.value.calendar.reminder shouldBe CalendarReminder.WEEK_BEFORE
        vm.uiState.value.calendar.isLinked shouldBe false
    }
})
