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

/** Estimate calibration with no history yet */
fun noCalibration(): com.decisionexecution.app.domain.usecase.GetEstimateCalibrationUseCase =
    mockk<com.decisionexecution.app.domain.usecase.GetEstimateCalibrationUseCase>().also { every { it() } returns flowOf(null) }

/** Makes a mocked best-task use case answer [GetBestTaskUseCase.pick] from whatever its plain invoke returns */
fun stubPick(useCase: com.decisionexecution.app.domain.usecase.GetBestTaskUseCase) {
    every { useCase.pick(any()) } answers {
        kotlinx.coroutines.flow.flow {
            useCase().collect { task -> emit(task?.let { com.decisionexecution.app.domain.usecase.NextPick(it, null) }) }
        }
    }
}
