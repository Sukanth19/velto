package com.decisionexecution.app.data.repository

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.domain.model.*
import com.decisionexecution.app.domain.repository.TaskRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Property-based tests for TaskRepository.
 */
// Room needs a real SQLite, so these run under Robolectric rather than as Kotest specs
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskRepositoryPropertyTest {
    
    @Test
    fun `Task CRUD Operations`() = runTest {
        // For any valid task,
        // creating it, then retrieving it by ID should return an equivalent task
        // with all attributes preserved.
        
        checkAll(PROPERTY_ITERATIONS, taskArb()) { task ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Insert task
                repository.insertTask(task)
                
                // Retrieve task by ID
                val retrievedTask = repository.getTaskById(task.id).first()
                
                // Verify all attributes are preserved
                retrievedTask shouldNotBe null
                retrievedTask!!.id shouldBe task.id
                retrievedTask.title shouldBe task.title
                retrievedTask.description shouldBe task.description
                retrievedTask.category shouldBe task.category
                retrievedTask.urgency shouldBe task.urgency
                retrievedTask.importance shouldBe task.importance
                retrievedTask.effort shouldBe task.effort
                retrievedTask.energyTag shouldBe task.energyTag
                retrievedTask.createdAt shouldBe task.createdAt
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Task Update Persistence`() = runTest {
        // For any existing task
        // and any valid attribute changes, updating the task should result in the
        // modified attributes being persisted and retrievable.
        
        checkAll(PROPERTY_ITERATIONS, taskArb(), taskArb()) { originalTask, updatedAttributes ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Insert original task
                repository.insertTask(originalTask)
                
                // Create updated task with same ID but different attributes
                val updatedTask = originalTask.copy(
                    title = updatedAttributes.title,
                    description = updatedAttributes.description,
                    urgency = updatedAttributes.urgency,
                    importance = updatedAttributes.importance,
                    effort = updatedAttributes.effort,
                    energyTag = updatedAttributes.energyTag
                )
                
                // Update task
                repository.updateTask(updatedTask)
                
                // Retrieve updated task
                val retrievedTask = repository.getTaskById(originalTask.id).first()
                
                // Verify updated attributes are persisted
                retrievedTask shouldNotBe null
                retrievedTask!!.title shouldBe updatedTask.title
                retrievedTask.description shouldBe updatedTask.description
                retrievedTask.urgency shouldBe updatedTask.urgency
                retrievedTask.importance shouldBe updatedTask.importance
                retrievedTask.effort shouldBe updatedTask.effort
                retrievedTask.energyTag shouldBe updatedTask.energyTag
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Task Deletion`() = runTest {
        // For any existing task,
        // deleting it should result in the task no longer appearing in any task queries.
        
        checkAll(PROPERTY_ITERATIONS, taskArb()) { task ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Insert task
                repository.insertTask(task)
                
                // Verify task exists
                val beforeDeletion = repository.getAllTasks().first()
                beforeDeletion shouldContain task
                
                // Delete task
                repository.deleteTask(task.id)
                
                // Verify task no longer appears in queries
                val afterDeletion = repository.getAllTasks().first()
                afterDeletion shouldNotContain task
                
                val byId = repository.getTaskById(task.id).first()
                byId shouldBe null
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Task Completion Recording`() = runTest {
        // For any active task,
        // marking it as complete should result in the task having a completion timestamp
        // and not appearing in active task lists.
        
        checkAll(PROPERTY_ITERATIONS, taskArb(), Arb.long(1000000000000L..2000000000000L)) { task, completionTimestamp ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Insert task
                repository.insertTask(task)
                
                // Verify task appears in active tasks
                val beforeCompletion = repository.getAllTasks().first()
                beforeCompletion shouldContain task
                
                // Complete task
                repository.completeTask(task.id, completionTimestamp)
                
                // Verify task no longer appears in active task lists
                val afterCompletion = repository.getAllTasks().first()
                afterCompletion shouldNotContain task
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Task Persistence Round Trip`() = runTest {
        // For any valid task,
        // persisting it to the database and then retrieving all tasks should result
        // in a list containing an equivalent task.
        
        checkAll(PROPERTY_ITERATIONS, taskArb()) { task ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Persist task
                repository.insertTask(task)
                
                // Retrieve all tasks
                val allTasks = repository.getAllTasks().first()
                
                // Verify list contains equivalent task
                allTasks shouldContain task
            } finally {
                database.close()
            }
        }
    }
}

// Test helper functions
private fun createInMemoryDatabase(): AppDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

private fun createRepository(database: AppDatabase): TaskRepository {
    return TaskRepositoryImpl(
        taskDao = database.taskDao(),
        taskCompletionDao = database.taskCompletionDao()
    )
}

// Arbitrary generators for Task domain model
private fun taskArb(): Arb<Task> = arbitrary {
    Task(
        id = UUID.randomUUID().toString(),
        title = Arb.string(5..100).bind(),
        description = Arb.string(10..200).orNull().bind(),
        category = Arb.enum<TaskCategory>().bind(),
        urgency = Arb.int(1..5).bind(),
        importance = Arb.int(1..5).bind(),
        effort = Arb.enum<TaskEffort>().bind(),
        energyTag = Arb.enum<EnergyTag>().orNull().bind(),
        createdAt = Arb.long(1000000000000L..2000000000000L).bind()
    )
}
