# Design Document: Decision-Driven Execution App

## Overview

The Decision-Driven Execution App is a native Android productivity application built with Jetpack Compose and Material Design 3 that eliminates decision paralysis through intelligent task selection and frictionless execution. The app implements a scoring algorithm that analyzes task attributes (urgency, importance, effort, energy) to automatically recommend the optimal task, then provides a one-tap "Just Start" flow that launches directly into a Pomodoro-based focus session.

### Core Innovation

The primary differentiator is the removal of decision-making friction. Traditional to-do apps present users with overwhelming lists and require constant prioritization decisions. This app acts as a decision engine: users input task attributes once, then the system continuously recommends the single best task to work on right now. The "Just Start" button eliminates all intermediate steps between intention and execution.

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles
- **Database**: Room (SQLite wrapper)
- **Preferences**: DataStore (Preferences DataStore)
- **Background Work**: WorkManager
- **Dependency Injection**: Hilt
- **Coroutines**: Kotlin Coroutines with Flow for reactive data
- **Widgets**: Glance (Jetpack Compose for widgets)

## Architecture

### High-Level Architecture

The application follows Clean Architecture with three primary layers:

```
┌─────────────────────────────────────────────────────────┐
│                     Presentation Layer                   │
│  (Jetpack Compose UI + ViewModels + Widget Providers)   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                      Domain Layer                        │
│     (Use Cases + Business Logic + Domain Models)        │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                       Data Layer                         │
│  (Room Database + DataStore + Repository Implementations)│
└─────────────────────────────────────────────────────────┘
```

### Architectural Patterns

1. **MVVM (Model-View-ViewModel)**: ViewModels expose UI state as StateFlow, Compose UI observes and renders
2. **Repository Pattern**: Abstracts data sources (Room, DataStore) behind interfaces
3. **Use Case Pattern**: Each business operation is encapsulated in a single-responsibility use case
4. **Unidirectional Data Flow**: UI events flow down, state flows up
5. **Dependency Injection**: Hilt provides all dependencies, enabling testability

### Module Structure

```
app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entity/       # Room entities
│   │   └── database/     # Database definition
│   ├── preferences/      # DataStore implementation
│   └── repository/       # Repository implementations
├── domain/
│   ├── model/            # Domain models (Task, FocusSession, etc.)
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Business logic use cases
├── presentation/
│   ├── home/             # Home screen UI + ViewModel
│   ├── tasks/            # Tasks screen UI + ViewModel
│   ├── focus/            # Focus screen UI + ViewModel
│   ├── widget/           # Widget providers
│   └── navigation/       # Navigation setup
└── di/                   # Hilt modules
```

## Components and Interfaces

### 1. Task Manager Component

**Responsibility**: CRUD operations for tasks, validation, and persistence.

**Key Classes**:
- `TaskRepository` (interface in domain, implementation in data)
- `CreateTaskUseCase`
- `UpdateTaskUseCase`
- `DeleteTaskUseCase`
- `GetAllTasksUseCase`
- `CompleteTaskUseCase`

**TaskRepository Interface**:
```kotlin
interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(id: String): Flow<Task?>
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun completeTask(taskId: String, completionTimestamp: Long)
    fun getTasksByCategory(category: TaskCategory): Flow<List<Task>>
}
```

**Validation Logic**:
- Urgency: 1-5 inclusive (enforced in domain model with require())
- Importance: 1-5 inclusive (enforced in domain model with require())
- Category: enum (School, Personal, Work)
- Effort: enum (Low, Medium, High)
- Energy Tag: enum (Low, DeepWork) or null

### 2. Scoring Engine Component

**Responsibility**: Calculate priority scores for tasks and identify the best task to work on.

**Key Classes**:
- `ScoringEngine` (domain service)
- `GetBestTaskUseCase`
- `GetTopThreeTasksUseCase`

**Scoring Algorithm**:

The scoring algorithm combines multiple factors with weighted importance:

```
score = (urgency_weight * urgency) + 
        (importance_weight * importance) + 
        (effort_modifier * effort_value) +
        (energy_bonus)

Where:
- urgency_weight = 2.0
- importance_weight = 3.0
- effort_modifier = -0.5 (penalizes high effort)
- effort_value: Low=1, Medium=2, High=3
- energy_bonus: DeepWork=1.0, Low=0.5, None=0
```

This weighting prioritizes importance over urgency (avoiding the "urgency trap"), slightly penalizes high-effort tasks (encouraging momentum), and provides a small bonus for energy-tagged tasks.

**Tiebreaker**: When scores are equal, older tasks (by creation timestamp) are prioritized.

**ScoringEngine Interface**:
```kotlin
class ScoringEngine {
    fun calculateScore(task: Task): Double
    fun getBestTask(tasks: List<Task>): Task?
    fun getTopThreeTasks(tasks: List<Task>): List<Task>
}
```

### 3. Focus Session Component

**Responsibility**: Manage Pomodoro timer state, track elapsed time, handle pause/resume, and persist completed sessions.

**Key Classes**:
- `FocusSessionManager` (domain service)
- `StartFocusSessionUseCase`
- `PauseFocusSessionUseCase`
- `ResumeFocusSessionUseCase`
- `EndFocusSessionUseCase`
- `FocusTimerWorker` (WorkManager worker for background timing)

**State Machine**:
```
┌─────────┐
│  Idle   │
└────┬────┘
     │ start()
     ▼
┌─────────┐  pause()   ┌─────────┐
│ Focusing│◄──────────►│ Paused  │
└────┬────┘            └─────────┘
     │ focus_complete()
     ▼
┌─────────┐
│ Breaking│
└────┬────┘
     │ break_complete()
     ▼
┌─────────┐
│  Idle   │
└─────────┘
```

**FocusSessionManager Interface**:
```kotlin
class FocusSessionManager {
    val sessionState: StateFlow<FocusSessionState>
    
    suspend fun startSession(task: Task, focusDuration: Int, breakDuration: Int)
    suspend fun pauseSession()
    suspend fun resumeSession()
    suspend fun endSession()
}

sealed class FocusSessionState {
    object Idle : FocusSessionState()
    data class Focusing(
        val task: Task,
        val elapsedSeconds: Int,
        val totalSeconds: Int
    ) : FocusSessionState()
    data class Paused(
        val task: Task,
        val elapsedSeconds: Int,
        val totalSeconds: Int
    ) : FocusSessionState()
    data class Breaking(
        val elapsedSeconds: Int,
        val totalSeconds: Int
    ) : FocusSessionState()
}
```

**Background Timer Implementation**:
- Use WorkManager with PeriodicWorkRequest (1-minute intervals)
- Store session state in DataStore (task ID, start time, duration, paused state)
- Worker reads state, calculates elapsed time, updates StateFlow
- On completion, worker triggers notification and updates state

### 4. Just Start Mode Component

**Responsibility**: Orchestrate the one-tap flow from idle to focused execution.

**Key Classes**:
- `JustStartUseCase` (orchestrates multiple use cases)

**Flow**:
```kotlin
class JustStartUseCase(
    private val getBestTaskUseCase: GetBestTaskUseCase,
    private val startFocusSessionUseCase: StartFocusSessionUseCase,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): JustStartResult {
        val bestTask = getBestTaskUseCase() ?: return JustStartResult.NoTasksAvailable
        val preferences = preferencesRepository.getPreferences().first()
        startFocusSessionUseCase(
            task = bestTask,
            focusDuration = preferences.focusDurationMinutes,
            breakDuration = preferences.breakDurationMinutes
        )
        return JustStartResult.Success(bestTask)
    }
}

sealed class JustStartResult {
    data class Success(val task: Task) : JustStartResult()
    object NoTasksAvailable : JustStartResult()
}
```

### 5. Eisenhower Matrix Component

**Responsibility**: Classify tasks into four quadrants based on urgency and importance.

**Key Classes**:
- `EisenhowerClassifier` (domain service)
- `GetEisenhowerMatrixUseCase`

**Classification Logic**:
```kotlin
enum class EisenhowerQuadrant {
    URGENT_IMPORTANT,      // urgency >= 4 AND importance >= 4
    NOT_URGENT_IMPORTANT,  // urgency <= 3 AND importance >= 4
    URGENT_NOT_IMPORTANT,  // urgency >= 4 AND importance <= 3
    NEITHER                // urgency <= 3 AND importance <= 3
}

class EisenhowerClassifier {
    fun classify(task: Task): EisenhowerQuadrant {
        return when {
            task.urgency >= 4 && task.importance >= 4 -> URGENT_IMPORTANT
            task.urgency <= 3 && task.importance >= 4 -> NOT_URGENT_IMPORTANT
            task.urgency >= 4 && task.importance <= 3 -> URGENT_NOT_IMPORTANT
            else -> NEITHER
        }
    }
    
    fun groupByQuadrant(tasks: List<Task>): Map<EisenhowerQuadrant, List<Task>>
}
```

### 6. Performance Tracker Component

**Responsibility**: Record completed tasks, calculate productivity metrics, and compute consistency scores.

**Key Classes**:
- `PerformanceRepository` (interface in domain, implementation in data)
- `RecordCompletionUseCase`
- `GetProductivityMetricsUseCase`

**Metrics Calculation**:
- **Total Completed Tasks**: Count of completed tasks in time period
- **Total Focus Time**: Sum of all completed focus session durations in time period
- **Consistency Score**: Measures regularity of completions over time

**Consistency Score Algorithm**:
```
consistency_score = (days_with_completions / total_days) * 100

Where:
- days_with_completions: number of unique days with at least one completion
- total_days: number of days in the analysis period (e.g., 7 for weekly, 30 for monthly)
- Result: percentage from 0-100
```

**PerformanceRepository Interface**:
```kotlin
interface PerformanceRepository {
    suspend fun recordCompletion(taskId: String, timestamp: Long, focusDuration: Int?)
    fun getCompletionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskCompletion>>
    fun getTotalFocusTimeInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int>
    fun getConsistencyScore(days: Int): Flow<Double>
}
```

### 7. Widget Provider Component

**Responsibility**: Render home screen widgets and handle widget interactions.

**Key Classes**:
- `BestTaskWidget` (Glance widget showing next task)
- `FocusTimerWidget` (Glance widget showing active timer)
- `QuickStartWidget` (Glance widget with start button)

**Widget Update Strategy**:
- Best Task Widget: Updates when task list changes (via WorkManager periodic sync every 15 minutes)
- Focus Timer Widget: Updates every minute while session is active
- Quick Start Widget: Static, launches app with Just Start intent

**Widget Implementation Pattern** (using Glance):
```kotlin
class BestTaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val bestTask = getBestTaskUseCase().collectAsState(initial = null)
            BestTaskWidgetContent(bestTask.value)
        }
    }
}
```

### 8. Navigation Component

**Responsibility**: Handle app navigation and deep linking from widgets/notifications.

**Navigation Graph**:
```
Home Screen (default)
├── Tasks Screen (bottom nav)
├── Focus Screen (bottom nav)
└── Task Detail Screen (from task tap)

Deep Links:
- app://just-start (launches Just Start flow)
- app://focus (opens Focus screen)
- app://task/{taskId} (opens Task Detail)
```

## Data Models

### Task Entity (Room)

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val category: String, // "SCHOOL", "PERSONAL", "WORK"
    val urgency: Int,      // 1-5
    val importance: Int,   // 1-5
    val effort: String,    // "LOW", "MEDIUM", "HIGH"
    val energyTag: String?, // "LOW", "DEEP_WORK", or null
    val createdAt: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
```

### Task Domain Model

```kotlin
data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val category: TaskCategory,
    val urgency: Int,
    val importance: Int,
    val effort: TaskEffort,
    val energyTag: EnergyTag?,
    val createdAt: Long
) {
    init {
        require(urgency in 1..5) { "Urgency must be between 1 and 5" }
        require(importance in 1..5) { "Importance must be between 1 and 5" }
    }
}

enum class TaskCategory { SCHOOL, PERSONAL, WORK }
enum class TaskEffort { LOW, MEDIUM, HIGH }
enum class EnergyTag { LOW, DEEP_WORK }
```

### Task Completion Entity (Room)

```kotlin
@Entity(tableName = "task_completions")
data class TaskCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val taskTitle: String,
    val completedAt: Long,
    val focusDuration: Int? // in minutes, null if completed without focus session
)
```

### User Preferences (DataStore)

```kotlin
data class UserPreferences(
    val focusDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

### Focus Session State (DataStore for persistence)

```kotlin
data class FocusSessionData(
    val taskId: String?,
    val taskTitle: String?,
    val startTimestamp: Long?,
    val focusDurationSeconds: Int?,
    val breakDurationSeconds: Int?,
    val isPaused: Boolean = false,
    val pausedAtSeconds: Int? = null
)
```

## Database Schema

### Room Database Definition

```kotlin
@Database(
    entities = [TaskEntity::class, TaskCompletionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
}
```

### TaskDao

```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<TaskEntity?>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND category = :category")
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
    
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :timestamp WHERE id = :taskId")
    suspend fun completeTask(taskId: String, timestamp: Long)
}
```

### TaskCompletionDao

```kotlin
@Dao
interface TaskCompletionDao {
    @Insert
    suspend fun insertCompletion(completion: TaskCompletionEntity)
    
    @Query("SELECT * FROM task_completions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp ORDER BY completedAt DESC")
    fun getCompletionsInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<List<TaskCompletionEntity>>
    
    @Query("SELECT SUM(focusDuration) FROM task_completions WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp AND focusDuration IS NOT NULL")
    fun getTotalFocusTimeInPeriod(startTimestamp: Long, endTimestamp: Long): Flow<Int?>
    
    @Query("SELECT COUNT(DISTINCT DATE(completedAt / 1000, 'unixepoch')) FROM task_completions WHERE completedAt >= :startTimestamp")
    fun getDaysWithCompletions(startTimestamp: Long): Flow<Int>
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Task Attribute Validation

*For any* task creation attempt, urgency and importance values must be between 1 and 5 inclusive, and values outside this range must be rejected with an error.

**Validates: Requirements 1.2, 1.3**

### Property 2: Task CRUD Operations

*For any* valid task, creating it, then retrieving it by ID should return an equivalent task with all attributes preserved.

**Validates: Requirements 1.1**

### Property 3: Task Update Persistence

*For any* existing task and any valid attribute changes, updating the task should result in the modified attributes being persisted and retrievable.

**Validates: Requirements 1.7**

### Property 4: Task Deletion

*For any* existing task, deleting it should result in the task no longer appearing in any task queries.

**Validates: Requirements 1.8**

### Property 5: Task Completion Recording

*For any* active task, marking it as complete should result in the task having a completion timestamp and not appearing in active task lists.

**Validates: Requirements 1.9**

### Property 6: Score Calculation and Best Task Selection

*For any* non-empty list of tasks, the best task returned by the scoring engine should have a score greater than or equal to all other tasks in the list.

**Validates: Requirements 2.1, 2.2**

### Property 7: Top Three Task Ordering

*For any* list of tasks with at least three tasks, the top three tasks returned should be in descending order by score.

**Validates: Requirements 2.3**

### Property 8: Score Tiebreaker

*For any* set of tasks with identical scores, the task with the earliest creation timestamp should be ranked higher.

**Validates: Requirements 2.4**

### Property 9: Custom Focus Duration Application

*For any* valid focus duration (5-90 minutes) and break duration (1-30 minutes), setting these preferences and starting a focus session should result in the session using those durations.

**Validates: Requirements 4.2, 11.3**

### Property 10: Focus Session State Transition

*For any* active focus session, when the elapsed time reaches the focus duration, the session state should automatically transition to the break state.

**Validates: Requirements 4.4**

### Property 11: Focus Session Pause Preservation

*For any* active focus session paused at any point in time, the remaining time should be preserved and resuming should continue from the same point.

**Validates: Requirements 4.6, 4.7**

### Property 12: Focus Session Early Termination

*For any* active focus session, ending it early should transition the session state back to idle.

**Validates: Requirements 4.8**

### Property 13: Focus Completion Recording

*For any* focus session that completes successfully, the performance tracker should record a completion entry with the correct focus duration.

**Validates: Requirements 4.9**

### Property 14: Focus UI Task Display

*For any* active focus session, the focus UI state should contain the task title of the task being worked on.

**Validates: Requirements 5.2**

### Property 15: Focus vs Break State Distinction

*For any* focus session, the UI state during focus time should be distinguishable from the UI state during break time.

**Validates: Requirements 5.6**

### Property 16: Eisenhower Urgent and Important Classification

*For any* task with urgency >= 4 and importance >= 4, the Eisenhower classifier should classify it as Urgent and Important.

**Validates: Requirements 6.2**

### Property 17: Eisenhower Not Urgent but Important Classification

*For any* task with urgency <= 3 and importance >= 4, the Eisenhower classifier should classify it as Not Urgent but Important.

**Validates: Requirements 6.3**

### Property 18: Eisenhower Urgent but Not Important Classification

*For any* task with urgency >= 4 and importance <= 3, the Eisenhower classifier should classify it as Urgent but Not Important.

**Validates: Requirements 6.4**

### Property 19: Eisenhower Neither Classification

*For any* task with urgency <= 3 and importance <= 3, the Eisenhower classifier should classify it as Neither Urgent nor Important.

**Validates: Requirements 6.5**

### Property 20: Eisenhower Quadrant Counts

*For any* list of tasks, the sum of task counts across all four Eisenhower quadrants should equal the total number of tasks.

**Validates: Requirements 6.7**

### Property 21: Completion Timestamp Recording

*For any* completed task, the performance tracker should store a completion record with a timestamp matching the completion time.

**Validates: Requirements 7.1**

### Property 22: Completed Tasks Count

*For any* time period and set of completions, the count of completed tasks in that period should equal the number of completions with timestamps within the period bounds.

**Validates: Requirements 7.2**

### Property 23: Total Focus Time Calculation

*For any* time period and set of completions with focus durations, the total focus time should equal the sum of all focus durations within the period.

**Validates: Requirements 7.3**

### Property 24: Consistency Score Calculation

*For any* set of completions over a time period, the consistency score should equal (days with completions / total days) * 100.

**Validates: Requirements 7.4**

### Property 25: Task Grouping by Category

*For any* list of tasks, grouping by category should result in each task appearing in exactly one category group matching its category attribute.

**Validates: Requirements 9.1**

### Property 26: Task Sorting by Score Within Category

*For any* list of tasks within a single category, the tasks should be ordered in descending order by their priority score.

**Validates: Requirements 9.2**

### Property 27: Category Filtering

*For any* list of tasks and any category, filtering by that category should return only tasks with that category attribute.

**Validates: Requirements 9.6**

### Property 28: Active Task Count

*For any* list of active tasks, the displayed count should equal the number of tasks in the list.

**Validates: Requirements 9.7**

### Property 29: Task Persistence Round Trip

*For any* valid task, persisting it to the database and then retrieving all tasks should result in a list containing an equivalent task.

**Validates: Requirements 10.1, 10.3, 10.6**

### Property 30: Preferences Persistence Round Trip

*For any* valid user preferences, persisting them to DataStore and then retrieving preferences should return equivalent preference values.

**Validates: Requirements 10.2, 10.4, 11.4**

### Property 31: Focus Duration Validation

*For any* focus duration value, values between 5 and 90 minutes inclusive should be accepted, and values outside this range should be rejected.

**Validates: Requirements 11.1**

### Property 32: Break Duration Validation

*For any* break duration value, values between 1 and 30 minutes inclusive should be accepted, and values outside this range should be rejected.

**Validates: Requirements 11.2**

## Error Handling

### Validation Errors

**Task Attribute Validation**:
- Urgency/importance out of range: Throw `IllegalArgumentException` with descriptive message
- Invalid duration values: Throw `IllegalArgumentException` with descriptive message
- Empty task title: Throw `IllegalArgumentException` with message "Task title cannot be empty"

**Error Handling Strategy**:
- Domain models use `require()` checks in init blocks for immediate validation
- Use cases catch domain exceptions and map to sealed result types
- ViewModels expose error states via StateFlow for UI display

### Database Errors

**Room Database Failures**:
- Database corruption: Catch `SQLiteException`, log error, attempt database rebuild
- Constraint violations: Catch `SQLiteConstraintException`, map to domain error
- Disk full: Catch `SQLiteDiskIOException`, display user-facing error message

**Error Recovery**:
- All database operations wrapped in try-catch blocks
- Failed operations return Result.Error with error details
- UI displays error messages and retry options

### Background Work Errors

**WorkManager Failures**:
- Worker failures: Implement retry with exponential backoff (max 3 retries)
- Timer drift: Recalculate elapsed time from stored start timestamp on each worker run
- Process death: Restore session state from DataStore on app restart

**Notification Errors**:
- Notification permission denied: Gracefully degrade, continue timer without notifications
- Notification channel errors: Log error, attempt channel recreation

### Widget Errors

**Widget Update Failures**:
- Widget provider crashes: Catch all exceptions in widget update code, log errors
- Data unavailable: Display placeholder content in widget
- Update timeout: Use cached data from previous update

## Testing Strategy

### Dual Testing Approach

This project requires both unit tests and property-based tests for comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, error conditions, and integration points
- **Property-based tests**: Verify universal properties across all inputs using randomized test data

### Property-Based Testing Configuration

**Library**: Kotest Property Testing (for Kotlin)

**Configuration**:
- Minimum 100 iterations per property test
- Each property test must include a comment tag referencing the design document property
- Tag format: `// Feature: decision-driven-execution-app, Property {number}: {property_text}`

**Example Property Test**:
```kotlin
class ScoringEnginePropertyTest : StringSpec({
    "Property 6: Score Calculation and Best Task Selection" {
        // Feature: decision-driven-execution-app, Property 6: For any non-empty list of tasks, 
        // the best task returned should have a score >= all other tasks
        
        checkAll(Arb.list(Arb.task(), range = 1..100)) { tasks ->
            val scoringEngine = ScoringEngine()
            val bestTask = scoringEngine.getBestTask(tasks)
            
            bestTask shouldNotBe null
            val bestScore = scoringEngine.calculateScore(bestTask!!)
            
            tasks.forEach { task ->
                val taskScore = scoringEngine.calculateScore(task)
                bestScore shouldBeGreaterThanOrEqual taskScore
            }
        }
    }
})
```

### Unit Testing Strategy

**Focus Areas**:
1. **Edge Cases**: Empty lists, null values, boundary values (min/max durations)
2. **Specific Examples**: Default preferences (25/5), empty task list handling
3. **Error Conditions**: Invalid input validation, database errors, worker failures
4. **Integration Points**: Repository implementations, ViewModel state updates, navigation flows

**Example Unit Test**:
```kotlin
class ScoringEngineTest : StringSpec({
    "should return null when task list is empty" {
        val scoringEngine = ScoringEngine()
        val bestTask = scoringEngine.getBestTask(emptyList())
        bestTask shouldBe null
    }
    
    "should use default preferences of 25 minutes focus and 5 minutes break" {
        val preferences = UserPreferences()
        preferences.focusDurationMinutes shouldBe 25
        preferences.breakDurationMinutes shouldBe 5
    }
})
```

### Test Coverage Goals

- **Domain Layer**: 100% coverage (pure business logic, highly testable)
- **Data Layer**: 90% coverage (repository implementations, DAOs)
- **Presentation Layer**: 70% coverage (ViewModels, UI state logic)
- **UI Components**: 50% coverage (Compose UI, focus on critical user flows)

### Testing Tools

- **Unit Testing**: JUnit 5, Kotest
- **Property Testing**: Kotest Property Testing
- **Mocking**: MockK
- **Database Testing**: Room in-memory database
- **Coroutine Testing**: kotlinx-coroutines-test
- **UI Testing**: Compose UI Testing

### Continuous Integration

- Run all tests on every pull request
- Fail build if property tests fail
- Generate coverage reports and enforce minimum thresholds
- Run property tests with increased iterations (500+) on main branch merges


## Implementation Notes

### Dependency Injection Setup

**Hilt Modules**:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "decision_execution_db"
        ).build()
    }
    
    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
    
    @Provides
    fun provideTaskCompletionDao(database: AppDatabase): TaskCompletionDao = 
        database.taskCompletionDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        taskCompletionDao: TaskCompletionDao
    ): TaskRepository = TaskRepositoryImpl(taskDao, taskCompletionDao)
    
    @Provides
    @Singleton
    fun providePreferencesRepository(
        @ApplicationContext context: Context
    ): PreferencesRepository = PreferencesRepositoryImpl(context)
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun provideScoringEngine(): ScoringEngine = ScoringEngine()
    
    @Provides
    @Singleton
    fun provideFocusSessionManager(
        preferencesRepository: PreferencesRepository
    ): FocusSessionManager = FocusSessionManager(preferencesRepository)
}
```

### State Management Pattern

**ViewModel State Pattern**:

```kotlin
data class HomeUiState(
    val bestTask: Task? = null,
    val completedTasksToday: Int = 0,
    val totalFocusTimeToday: Int = 0,
    val consistencyScore: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel @Inject constructor(
    private val getBestTaskUseCase: GetBestTaskUseCase,
    private val justStartUseCase: JustStartUseCase,
    private val getProductivityMetricsUseCase: GetProductivityMetricsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                getBestTaskUseCase(),
                getProductivityMetricsUseCase(period = Period.TODAY)
            ) { bestTask, metrics ->
                HomeUiState(
                    bestTask = bestTask,
                    completedTasksToday = metrics.completedTasks,
                    totalFocusTimeToday = metrics.totalFocusTime,
                    consistencyScore = metrics.consistencyScore,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun onJustStartClicked() {
        viewModelScope.launch {
            when (val result = justStartUseCase()) {
                is JustStartResult.Success -> {
                    // Navigation handled by UI observing focus session state
                }
                is JustStartResult.NoTasksAvailable -> {
                    _uiState.update { it.copy(error = "No tasks available. Create a task first.") }
                }
            }
        }
    }
}
```

### WorkManager Timer Implementation

**FocusTimerWorker**:

```kotlin
@HiltWorker
class FocusTimerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val focusSessionManager: FocusSessionManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val sessionState = focusSessionManager.sessionState.value
        
        when (sessionState) {
            is FocusSessionState.Focusing -> {
                if (sessionState.elapsedSeconds >= sessionState.totalSeconds) {
                    focusSessionManager.transitionToBreak()
                    notificationHelper.showFocusCompleteNotification()
                }
            }
            is FocusSessionState.Breaking -> {
                if (sessionState.elapsedSeconds >= sessionState.totalSeconds) {
                    focusSessionManager.completeSession()
                    notificationHelper.showBreakCompleteNotification()
                }
            }
            else -> {
                // No active session, cancel worker
                return Result.success()
            }
        }
        
        return Result.success()
    }
}
```

**Worker Scheduling**:

```kotlin
class FocusSessionManager @Inject constructor(
    private val workManager: WorkManager,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun startSession(task: Task, focusDuration: Int, breakDuration: Int) {
        // Save session state to DataStore
        preferencesRepository.saveFocusSession(
            FocusSessionData(
                taskId = task.id,
                taskTitle = task.title,
                startTimestamp = System.currentTimeMillis(),
                focusDurationSeconds = focusDuration * 60,
                breakDurationSeconds = breakDuration * 60
            )
        )
        
        // Schedule periodic worker
        val workRequest = PeriodicWorkRequestBuilder<FocusTimerWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "focus_timer",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        _sessionState.value = FocusSessionState.Focusing(
            task = task,
            elapsedSeconds = 0,
            totalSeconds = focusDuration * 60
        )
    }
}
```

### Widget Implementation with Glance

**BestTaskWidget**:

```kotlin
class BestTaskWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val bestTask = getBestTaskFromRepository()
            
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next Task",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    if (bestTask != null) {
                        Text(
                            text = bestTask.title,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 2
                        )
                        
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        
                        Text(
                            text = "${bestTask.category.name} • ${bestTask.effort.name} effort",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        Text(
                            text = "No tasks available",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    Button(
                        text = "Just Start",
                        onClick = actionStartActivity<MainActivity>(
                            parameters = actionParametersOf(
                                "action" to "just_start"
                            )
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    private suspend fun getBestTaskFromRepository(): Task? {
        // Access repository via dependency injection
        // Return best task or null
    }
}
```

### Navigation Deep Linking

**MainActivity Deep Link Handling**:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            DecisionExecutionTheme {
                val navController = rememberNavController()
                
                // Handle deep link from intent
                LaunchedEffect(intent) {
                    handleDeepLink(intent, navController)
                }
                
                AppNavigation(navController = navController)
            }
        }
    }
    
    private fun handleDeepLink(intent: Intent, navController: NavController) {
        when (intent.getStringExtra("action")) {
            "just_start" -> {
                // Trigger Just Start flow
                navController.navigate("focus")
            }
            "open_task" -> {
                val taskId = intent.getStringExtra("task_id")
                navController.navigate("task/$taskId")
            }
        }
    }
}
```

### UI Component Examples

**Home Screen Composable**:

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFocus: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        bottomBar = { BottomNavigationBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Primary action button
            Button(
                onClick = {
                    viewModel.onJustStartClicked()
                    onNavigateToFocus()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Just Start",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Best task card
            if (uiState.bestTask != null) {
                BestTaskCard(task = uiState.bestTask!!)
            } else {
                EmptyTaskCard()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Minimal stats
            ProductivityStats(
                completedToday = uiState.completedTasksToday,
                focusTimeToday = uiState.totalFocusTimeToday,
                consistencyScore = uiState.consistencyScore
            )
            
            // Error display
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = uiState.error!!)
                }
            }
        }
    }
}
```

**Focus Screen Composable**:

```kotlin
@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        when (val state = sessionState) {
            is FocusSessionState.Focusing -> {
                FocusingContent(
                    task = state.task,
                    elapsedSeconds = state.elapsedSeconds,
                    totalSeconds = state.totalSeconds,
                    onPause = { viewModel.pauseSession() },
                    onEnd = { viewModel.endSession() }
                )
            }
            is FocusSessionState.Paused -> {
                PausedContent(
                    task = state.task,
                    elapsedSeconds = state.elapsedSeconds,
                    totalSeconds = state.totalSeconds,
                    onResume = { viewModel.resumeSession() },
                    onEnd = { viewModel.endSession() }
                )
            }
            is FocusSessionState.Breaking -> {
                BreakingContent(
                    elapsedSeconds = state.elapsedSeconds,
                    totalSeconds = state.totalSeconds
                )
            }
            is FocusSessionState.Idle -> {
                Text(
                    text = "No active session",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun FocusingContent(
    task: Task,
    elapsedSeconds: Int,
    totalSeconds: Int,
    onPause: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        CircularProgressIndicator(
            progress = elapsedSeconds.toFloat() / totalSeconds.toFloat(),
            modifier = Modifier.size(200.dp),
            strokeWidth = 8.dp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = formatTime(totalSeconds - elapsedSeconds),
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onPause) {
                Text("Pause")
            }
            
            OutlinedButton(onClick = onEnd) {
                Text("End")
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
```

## Design Decisions and Rationales

### 1. MVVM with Clean Architecture

**Decision**: Use MVVM pattern with Clean Architecture layers (Presentation, Domain, Data).

**Rationale**: 
- MVVM is the recommended pattern for Jetpack Compose applications
- Clean Architecture provides clear separation of concerns and testability
- Domain layer contains pure business logic, making it highly testable with property-based tests
- Repository pattern abstracts data sources, enabling easy mocking in tests

### 2. Room Database for Local Storage

**Decision**: Use Room (SQLite wrapper) for task and completion persistence.

**Rationale**:
- Room provides compile-time SQL verification
- Supports Flow for reactive data updates
- Efficient for structured data with relationships
- Well-integrated with Android architecture components
- Supports in-memory database for testing

### 3. DataStore for Preferences

**Decision**: Use DataStore (Preferences DataStore) for user preferences and session state.

**Rationale**:
- DataStore is the modern replacement for SharedPreferences
- Provides type-safe access with Kotlin coroutines and Flow
- Handles data consistency and thread safety automatically
- Better performance than SharedPreferences for frequent updates

### 4. WorkManager for Background Timers

**Decision**: Use WorkManager with periodic work requests for focus session timing.

**Rationale**:
- WorkManager is guaranteed to execute even if app is killed
- Handles battery optimization and doze mode automatically
- Survives process death and device reboots
- Integrates with Android system constraints
- Alternative (foreground service) would drain battery and require persistent notification

### 5. Glance for Widgets

**Decision**: Use Glance (Jetpack Compose for widgets) for home screen widgets.

**Rationale**:
- Glance uses Compose-like declarative syntax, consistent with app UI
- Simplifies widget development compared to RemoteViews
- Supports Material 3 theming
- Easier to maintain with shared UI logic

### 6. Scoring Algorithm Weights

**Decision**: Weight importance (3.0) higher than urgency (2.0), penalize effort (-0.5).

**Rationale**:
- Prioritizing importance over urgency avoids the "urgency trap" (constantly fighting fires)
- Slight effort penalty encourages momentum by suggesting easier tasks when scores are close
- Energy bonus rewards users for tagging tasks, improving data quality
- Weights are tunable based on user feedback

### 7. Consistency Score Formula

**Decision**: Calculate consistency as (days with completions / total days) * 100.

**Rationale**:
- Simple, intuitive metric that users can understand
- Encourages daily engagement without requiring specific task counts
- Percentage format (0-100) is familiar and easy to interpret
- Focuses on habit formation rather than raw productivity

### 8. Eisenhower Matrix Thresholds

**Decision**: Use 4+ for "high" urgency/importance, 3- for "low".

**Rationale**:
- Creates clear separation between quadrants
- 1-3 represents "low to medium", 4-5 represents "high to critical"
- Avoids ambiguity of middle values (3 is clearly "not high")
- Aligns with common mental models of 5-point scales

## Summary

This design document specifies a native Android productivity application that eliminates decision paralysis through intelligent task scoring and frictionless execution. The architecture follows Clean Architecture principles with MVVM, using Jetpack Compose for UI, Room for persistence, and WorkManager for background timing. The scoring algorithm prioritizes importance over urgency and slightly penalizes high-effort tasks to encourage momentum.

The design includes 32 correctness properties that will be validated through property-based testing using Kotest, ensuring the system behaves correctly across all valid inputs. The combination of property-based tests (for universal correctness) and unit tests (for specific examples and edge cases) provides comprehensive test coverage.

Key technical decisions include using DataStore for preferences, Glance for widgets, and WorkManager for reliable background timer execution. The focus session state machine ensures accurate timing across app lifecycle events, and the Eisenhower Matrix provides automatic task classification for workload visualization.

