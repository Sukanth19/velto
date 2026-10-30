package com.decisionexecution.app.presentation.inbox

import com.decisionexecution.app.testutil.InMemoryHabitRepository
import com.decisionexecution.app.testutil.InMemoryInboxRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest : StringSpec({
    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    "capturing trims the text, clears the draft and ignores blanks" {
        val viewModel = InboxViewModel(InMemoryInboxRepository(), InMemoryHabitRepository())

        viewModel.onDraftChange("  Buy a plant ")
        viewModel.onCapture()
        viewModel.uiState.value.draft shouldBe ""
        viewModel.onDraftChange("   ")
        viewModel.onCapture()

        viewModel.uiState.value.items.map { it.text } shouldBe listOf("Buy a plant")
    }

    "turning an item into a habit moves it out of the inbox" {
        val habits = InMemoryHabitRepository()
        val viewModel = InboxViewModel(InMemoryInboxRepository(), habits)
        viewModel.onDraftChange("Meditate")
        viewModel.onCapture()

        viewModel.onMakeHabit(viewModel.uiState.value.items.single())

        viewModel.uiState.value.items shouldBe emptyList()
        habits.getHabits(LocalDate.now()).first().single().habit.name shouldBe "Meditate"
    }

    "emptying the inbox celebrates" {
        val viewModel = InboxViewModel(InMemoryInboxRepository(), InMemoryHabitRepository())
        viewModel.onDraftChange("One thing")
        viewModel.onCapture()

        viewModel.onDismiss(viewModel.uiState.value.items.single())

        viewModel.uiState.value.inboxZeroKey shouldBe 1
    }
})
