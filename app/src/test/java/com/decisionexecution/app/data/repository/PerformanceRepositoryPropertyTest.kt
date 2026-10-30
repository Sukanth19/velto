package com.decisionexecution.app.data.repository

import com.decisionexecution.app.testutil.PROPERTY_ITERATIONS
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.decisionexecution.app.data.local.database.AppDatabase
import com.decisionexecution.app.domain.repository.PerformanceRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
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
import java.util.concurrent.TimeUnit

/**
 * Property-based tests for PerformanceRepository.
 */
// Room needs a real SQLite, so these run under Robolectric rather than as Kotest specs
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerformanceRepositoryPropertyTest {
    
    @Test
    fun `Completion Timestamp Recording`() = runTest {
        // For any completed task,
        // the performance tracker should store a completion record with a timestamp
        // matching the completion time.
        
        checkAll(PROPERTY_ITERATIONS,
            Arb.string(10..50),
            Arb.long(1000000000000L..2000000000000L),
            Arb.int(5..90).orNull()
        ) { taskId, timestamp, focusDuration ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Record completion
                repository.recordCompletion(taskId, timestamp, focusDuration)
                
                // Retrieve completions in a wide time period
                val startTimestamp = timestamp - TimeUnit.DAYS.toMillis(1)
                val endTimestamp = timestamp + TimeUnit.DAYS.toMillis(1)
                val completions = repository.getCompletionsInPeriod(startTimestamp, endTimestamp).first()
                
                // Verify completion record exists with matching timestamp
                val completion = completions.find { it.taskId == taskId }
                completion shouldNotBe null
                completion!!.completedAt shouldBe timestamp
                completion.focusDuration shouldBe focusDuration
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Completed Tasks Count`() = runTest {
        // For any time period
        // and set of completions, the count of completed tasks in that period should
        // equal the number of completions with timestamps within the period bounds.
        
        checkAll(PROPERTY_ITERATIONS,
            Arb.list(completionDataArb(), 0..20),
            Arb.long(1000000000000L..1500000000000L),
            Arb.long(1500000000001L..2000000000000L)
        ) { completions, startTimestamp, endTimestamp ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Record all completions
                completions.forEach { (taskId, timestamp, focusDuration) ->
                    repository.recordCompletion(taskId, timestamp, focusDuration)
                }
                
                // Count completions in period
                val retrievedCompletions = repository.getCompletionsInPeriod(startTimestamp, endTimestamp).first()
                
                // Calculate expected count (completions within period)
                val expectedCount = completions.count { (_, timestamp, _) ->
                    timestamp in startTimestamp..endTimestamp
                }
                
                // Verify count matches
                retrievedCompletions.size shouldBe expectedCount
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Total Focus Time Calculation`() = runTest {
        // For any time period
        // and set of completions with focus durations, the total focus time should
        // equal the sum of all focus durations within the period.
        
        checkAll(PROPERTY_ITERATIONS,
            Arb.list(completionDataArb(), 0..20),
            Arb.long(1000000000000L..1500000000000L),
            Arb.long(1500000000001L..2000000000000L)
        ) { completions, startTimestamp, endTimestamp ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                // Record all completions
                completions.forEach { (taskId, timestamp, focusDuration) ->
                    repository.recordCompletion(taskId, timestamp, focusDuration)
                }
                
                // Get total focus time in period
                val totalFocusTime = repository.getTotalFocusTimeInPeriod(startTimestamp, endTimestamp).first()
                
                // Calculate expected total (sum of focus durations within period)
                val expectedTotal = completions
                    .filter { (_, timestamp, _) -> timestamp in startTimestamp..endTimestamp }
                    .sumOf { (_, _, focusDuration) -> focusDuration ?: 0 }
                
                // Verify total matches
                totalFocusTime shouldBe expectedTotal
            } finally {
                database.close()
            }
        }
    }
    
    @Test
    fun `Total Focus Time includes finished focus sessions`() = runTest {
        // focus minutes from finished focus
        // phases count toward total focus time, and never toward completed tasks.

        checkAll(PROPERTY_ITERATIONS,
            Arb.list(Arb.pair(Arb.long(1000000000000L..2000000000000L), Arb.int(5..90)), 0..20),
            Arb.long(1000000000000L..1500000000000L),
            Arb.long(1500000000001L..2000000000000L)
        ) { sessions, startTimestamp, endTimestamp ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)

            try {
                sessions.forEach { (timestamp, minutes) ->
                    repository.recordFocusSession(UUID.randomUUID().toString(), timestamp, minutes)
                }

                val expected = sessions.filter { it.first in startTimestamp..endTimestamp }.sumOf { it.second }
                repository.getTotalFocusTimeInPeriod(startTimestamp, endTimestamp).first() shouldBe expected
                repository.getCompletionsInPeriod(startTimestamp, endTimestamp).first().size shouldBe 0
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun `Consistency Score Calculation`() = runTest {
        // For any set of completions
        // over a time period, the consistency score should equal
        // (days with completions / total days) * 100.
        
        checkAll(PROPERTY_ITERATIONS,
            Arb.int(1..30),
            Arb.list(Arb.int(0..29), 0..30)
        ) { totalDays, daysWithCompletionsIndices ->
            val database = createInMemoryDatabase()
            val repository = createRepository(database)
            
            try {
                val now = System.currentTimeMillis()
                val uniqueDays = daysWithCompletionsIndices.toSet()
                
                // Record completions on specific days
                uniqueDays.forEach { dayOffset ->
                    if (dayOffset < totalDays) {
                        val timestamp = now - TimeUnit.DAYS.toMillis(dayOffset.toLong())
                        repository.recordCompletion(UUID.randomUUID().toString(), timestamp, 25)
                    }
                }
                
                // Get consistency score
                val consistencyScore = repository.getConsistencyScore(totalDays).first()
                
                // Calculate expected score
                val validDays = uniqueDays.count { it < totalDays }
                val expectedScore = (validDays.toDouble() / totalDays.toDouble()) * 100.0
                
                // Verify score matches (with small tolerance for floating point)
                consistencyScore shouldBe expectedScore
                consistencyScore shouldBeGreaterThanOrEqual 0.0
                consistencyScore shouldBeLessThanOrEqual 100.0
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

private fun createRepository(database: AppDatabase): PerformanceRepository {
    return PerformanceRepositoryImpl(
        taskCompletionDao = database.taskCompletionDao(),
        focusSessionDao = database.focusSessionDao()
    )
}

// Arbitrary generator for completion data
private data class CompletionData(
    val taskId: String,
    val timestamp: Long,
    val focusDuration: Int?
)

private fun completionDataArb(): Arb<CompletionData> = arbitrary {
    CompletionData(
        taskId = UUID.randomUUID().toString(),
        timestamp = Arb.long(1000000000000L..2000000000000L).bind(),
        focusDuration = Arb.int(5..90).orNull().bind()
    )
}
