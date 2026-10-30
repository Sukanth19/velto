package com.decisionexecution.app.testutil

import com.decisionexecution.app.domain.model.Subtask
import com.decisionexecution.app.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

/** A relaxed task repository that answers step lookups, for view models that show steps */
fun taskRepositoryWithSteps(vararg steps: Subtask): TaskRepository {
    val repository = mockk<TaskRepository>(relaxed = true)
    every { repository.getSubtasks(any()) } answers { flowOf(steps.filter { it.taskId == firstArg<String>() }) }
    return repository
}
