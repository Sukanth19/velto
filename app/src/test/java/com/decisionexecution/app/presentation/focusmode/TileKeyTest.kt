package com.decisionexecution.app.presentation.focusmode

import com.decisionexecution.app.domain.model.FocusSessionState
import com.decisionexecution.app.domain.model.Task
import com.decisionexecution.app.domain.model.TaskCategory
import com.decisionexecution.app.domain.model.TaskEffort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TileKeyTest : StringSpec({
    val task = Task("t", "Write", null, TaskCategory.WORK, 3, 3, TaskEffort.LOW, null, 1)

    "the tile refreshes once a minute while focusing, not every second" {
        tileKey(FocusSessionState.Focusing(task, 0, 1500)) shouldBe tileKey(FocusSessionState.Focusing(task, 59, 1500))
        tileKey(FocusSessionState.Focusing(task, 0, 1500)) shouldNotBe tileKey(FocusSessionState.Focusing(task, 61, 1500))
    }

    "phase changes always refresh the tile" {
        tileKey(FocusSessionState.Focusing(task, 0, 1500)) shouldNotBe tileKey(FocusSessionState.Paused(task, 0, 1500))
        tileKey(FocusSessionState.Idle) shouldNotBe tileKey(FocusSessionState.Breaking(0, 300))
    }
})
