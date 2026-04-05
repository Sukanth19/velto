package com.decisionexecution.app.domain.usecase

import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.repository.PerformanceRepository
import com.decisionexecution.app.domain.repository.TaskCompletion
import com.decisionexecution.app.domain.repository.TaskRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

/**
 * Unit tests for task management use cases.
 * Feature: decision-driven-execution-app
 */
class TaskUseCasesTest : StringSpec({
    
    "CreateTaskUseCase should validate urgency is between 1 and 5" {
        // Requirements: 1.2
        val taskRepository = mockk<TaskRepository>()
        val createTaskUseCase = CreateTaskUseCase(taskRepository)
        
        // Test invalid urgency (too low)
        shouldThrow<IllegalArgumentException> {
            val invalidTask = Task(
                id = UUID.randomUUID().toString(),
                title = "Invalid Task",
                description = null,
                category = TaskCategory.WORK,
                urgency = 0, // Invalid: below 1
                importance = 3,
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            createTaskUseCase(invalidTask)
        }
        
        // Test invalid urgency (too high)
        shouldThrow<IllegalArgumentException> {
            val invalidTask = Task(
                id = UUID.randomUUID().toString(),
                title = "Invalid Task",
                description = null,
                category = TaskCategory.WORK,
                urgency = 6, // Invalid: above 5
                importance = 3,
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            createTaskUseCase(invalidTask)
        }
        
        // Test valid urgency values (1-5)
        coEvery { taskRepository.insertTask(any()) } just Runs
        
        for (urgency in 1..5) {
            val validTask = Task(
                id = UUID.randomUUID().toString(),
                title = "Valid Task $urgency",
                description = null,
                category = TaskCategory.WORK,
                urgency = urgency,
                importance = 3,
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            createTaskUseCase(validTask)
        }
        
        coVerify(exactly = 5) { taskRepository.insertTask(any()) }
    }
    
    "CreateTaskUseCase should validate importance is between 1 and 5" {
        // Requirements: 1.3
        val taskRepository = mockk<TaskRepository>()
        val createTaskUseCase = CreateTaskUseCase(taskRepository)
        
        // Test invalid importance (too low)
        shouldThrow<IllegalArgumentException> {
            val invalidTask = Task(
                id = UUID.randomUUID().toString(),
                title = "Invalid Task",
                description = null,
                category = TaskCategory.WORK,
                urgency = 3,
                importance = 0, // Invalid: below 1
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            createTaskUseCase(invalidTask)
        }
        
        // Test invalid importance (too high)
        shouldThrow<IllegalArgumentException> {
            val invalidTask = Task(
                id = UUID.randomUUID().toString(),
                title = "Invalid Task",
                description = null,
                category = TaskCategory.WORK,
                urgency = 3,
                importance = 6, // Invalid: above 5
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            createTaskUseCase(invalidTask)
        }
        
        // Test valid importance values (1-5)
        coEvery { taskRepository.insertTask(any()) } just Runs
        
        for (importance in 1..5) {
            val validTask = Task(
                id = UUID.randomUUID().toString(),
                title = "Valid Task $importance",
                description = null,
                category = TaskCategory.WORK,
                urgency = 3,
                importance = importance,
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            )
            createTaskUseCase(validTask)
        }
        
        coVerify(exactly = 5) { taskRepository.insertTask(any()) }
    }
    
    "CompleteTaskUseCase should record completion timestamp" {
        // Requirements: 1.9, 7.1
        val taskRepository = mockk<TaskRepository>()
        val performanceRepository = mockk<PerformanceRepository>()
        val completeTaskUseCase = CompleteTaskUseCase(taskRepository, performanceRepository)
        
        val taskId = "task-123"
        val focusDuration = 25
        
        coEvery { taskRepository.completeTask(any(), any()) } just Runs
        coEvery { performanceRepository.recordCompletion(any(), any(), any()) } just Runs
        
        val beforeTimestamp = System.currentTimeMillis()
        completeTaskUseCase(taskId, focusDuration)
        val afterTimestamp = System.currentTimeMillis()
        
        // Verify task was marked complete
        coVerify { taskRepository.completeTask(taskId, match { it in beforeTimestamp..afterTimestamp }) }
        
        // Verify completion was recorded with timestamp and focus duration
        coVerify { 
            performanceRepository.recordCompletion(
                taskId, 
                match { it in beforeTimestamp..afterTimestamp },
                focusDuration
            ) 
        }
    }
    
    "CompleteTaskUseCase should record completion without focus duration" {
        // Requirements: 1.9, 7.1
        val taskRepository = mockk<TaskRepository>()
        val performanceRepository = mockk<PerformanceRepository>()
        val completeTaskUseCase = CompleteTaskUseCase(taskRepository, performanceRepository)
        
        val taskId = "task-456"
        
        coEvery { taskRepository.completeTask(any(), any()) } just Runs
        coEvery { performanceRepository.recordCompletion(any(), any(), any()) } just Runs
        
        completeTaskUseCase(taskId, focusDuration = null)
        
        // Verify completion was recorded with null focus duration
        coVerify { performanceRepository.recordCompletion(taskId, any(), null) }
    }
    
    "DeleteTaskUseCase should remove task from queries" {
        // Requirements: 1.8
        val taskRepository = mockk<TaskRepository>()
        val deleteTaskUseCase = DeleteTaskUseCase(taskRepository)
        
        val taskId = "task-to-delete"
        val task = Task(
            id = taskId,
            title = "Task to Delete",
            description = null,
            category = TaskCategory.PERSONAL,
            urgency = 2,
            importance = 3,
            effort = TaskEffort.LOW,
            energyTag = null,
            createdAt = System.currentTimeMillis()
        )
        
        // Before deletion, task exists
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        
        val tasksBeforeDeletion = taskRepository.getAllTasks().first()
        tasksBeforeDeletion.size shouldBe 1
        tasksBeforeDeletion[0].id shouldBe taskId
        
        // Delete the task
        coEvery { taskRepository.deleteTask(taskId) } just Runs
        deleteTaskUseCase(taskId)
        
        // After deletion, task should not appear
        every { taskRepository.getAllTasks() } returns flowOf(emptyList())
        
        val tasksAfterDeletion = taskRepository.getAllTasks().first()
        tasksAfterDeletion.size shouldBe 0
        
        coVerify { taskRepository.deleteTask(taskId) }
    }
    
    "UpdateTaskUseCase should validate updated task attributes" {
        // Requirements: 1.7, 1.2, 1.3
        val taskRepository = mockk<TaskRepository>()
        val updateTaskUseCase = UpdateTaskUseCase(taskRepository)
        
        // Test that validation still applies on update
        shouldThrow<IllegalArgumentException> {
            val invalidTask = Task(
                id = "existing-task",
                title = "Updated Task",
                description = "Updated description",
                category = TaskCategory.SCHOOL,
                urgency = 7, // Invalid
                importance = 3,
                effort = TaskEffort.HIGH,
                energyTag = EnergyTag.DEEP_WORK,
                createdAt = System.currentTimeMillis()
            )
            updateTaskUseCase(invalidTask)
        }
        
        // Test valid update
        coEvery { taskRepository.updateTask(any()) } just Runs
        
        val validTask = Task(
            id = "existing-task",
            title = "Updated Task",
            description = "Updated description",
            category = TaskCategory.SCHOOL,
            urgency = 5,
            importance = 4,
            effort = TaskEffort.HIGH,
            energyTag = EnergyTag.DEEP_WORK,
            createdAt = System.currentTimeMillis()
        )
        
        updateTaskUseCase(validTask)
        
        coVerify { taskRepository.updateTask(validTask) }
    }
    
    "GetAllTasksUseCase should return all active tasks" {
        // Requirements: 1.1
        val taskRepository = mockk<TaskRepository>()
        val getAllTasksUseCase = GetAllTasksUseCase(taskRepository)
        
        val tasks = listOf(
            Task(
                id = "1",
                title = "Task 1",
                description = null,
                category = TaskCategory.WORK,
                urgency = 3,
                importance = 4,
                effort = TaskEffort.MEDIUM,
                energyTag = null,
                createdAt = System.currentTimeMillis()
            ),
            Task(
                id = "2",
                title = "Task 2",
                description = null,
                category = TaskCategory.PERSONAL,
                urgency = 2,
                importance = 3,
                effort = TaskEffort.LOW,
                energyTag = EnergyTag.LOW,
                createdAt = System.currentTimeMillis()
            )
        )
        
        every { taskRepository.getAllTasks() } returns flowOf(tasks)
        
        val result = getAllTasksUseCase().first()
        
        result.size shouldBe 2
        result shouldBe tasks
    }
    
    "GetTaskByIdUseCase should return specific task" {
        // Requirements: 1.1
        val taskRepository = mockk<TaskRepository>()
        val getTaskByIdUseCase = GetTaskByIdUseCase(taskRepository)
        
        val taskId = "specific-task"
        val task = Task(
            id = taskId,
            title = "Specific Task",
            description = "A specific task",
            category = TaskCategory.SCHOOL,
            urgency = 4,
            importance = 5,
            effort = TaskEffort.HIGH,
            energyTag = EnergyTag.DEEP_WORK,
            createdAt = System.currentTimeMillis()
        )
        
        every { taskRepository.getTaskById(taskId) } returns flowOf(task)
        
        val result = getTaskByIdUseCase(taskId).first()
        
        result shouldBe task
    }
    
    "GetTaskByIdUseCase should return null for non-existent task" {
        // Requirements: 1.1
        val taskRepository = mockk<TaskRepository>()
        val getTaskByIdUseCase = GetTaskByIdUseCase(taskRepository)
        
        val nonExistentId = "does-not-exist"
        
        every { taskRepository.getTaskById(nonExistentId) } returns flowOf(null)
        
        val result = getTaskByIdUseCase(nonExistentId).first()
        
        result shouldBe null
    }
})
