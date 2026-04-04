# Implementation Plan: Decision-Driven Execution App

## Overview

This implementation plan breaks down the Decision-Driven Execution App into incremental coding tasks following Clean Architecture with MVVM pattern. The app is built with Kotlin, Jetpack Compose, Room Database, DataStore, and WorkManager. Tasks are organized to build foundational layers first (data models, database, repositories), then domain logic (use cases, scoring engine), and finally presentation layer (ViewModels, UI, widgets). Each task builds on previous work, with property-based tests integrated close to implementation to catch errors early.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Android project with Kotlin and Jetpack Compose
  - Add dependencies: Room, DataStore, WorkManager, Hilt, Kotest, MockK, Glance
  - Configure Hilt for dependency injection
  - Set up package structure: data/, domain/, presentation/, di/
  - _Requirements: 10.1, 10.2_

- [x] 2. Implement domain models and enums
  - [x] 2.1 Create Task domain model with validation
    - Define Task data class with id, title, description, category, urgency, importance, effort, energyTag, createdAt
    - Add init block with require() checks for urgency (1-5) and importance (1-5)
    - Create TaskCategory enum (SCHOOL, PERSONAL, WORK)
    - Create TaskEffort enum (LOW, MEDIUM, HIGH)
    - Create EnergyTag enum (LOW, DEEP_WORK)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 2.2 Write property test for Task validation
    - **Property 1: Task Attribute Validation**
    - **Validates: Requirements 1.2, 1.3**

  - [x] 2.3 Create FocusSession domain models
    - Define FocusSessionState sealed class (Idle, Focusing, Paused, Breaking)
    - Define FocusSessionData data class for persistence
    - Define JustStartResult sealed class (Success, NoTasksAvailable)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 2.4 Create UserPreferences data class
    - Define UserPreferences with focusDurationMinutes (default 25), breakDurationMinutes (default 5), themeMode
    - Create ThemeMode enum (LIGHT, DARK, SYSTEM)
    - _Requirements: 11.1, 11.2, 11.5_

  - [x] 2.5 Create EisenhowerQuadrant enum
    - Define URGENT_IMPORTANT, NOT_URGENT_IMPORTANT, URGENT_NOT_IMPORTANT, NEITHER
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 3. Implement data layer - Room database
  - [ ] 3.1 Create Room entities
    - Define TaskEntity with @Entity annotation and all task fields
    - Define TaskCompletionEntity with taskId, taskTitle, completedAt, focusDuration
    - Add proper @PrimaryKey, @ColumnInfo annotations
    - _Requirements: 10.1, 10.6_

  - [ ] 3.2 Create TaskDao interface
    - Define getAllActiveTasks() returning Flow<List<TaskEntity>>
    - Define getTaskById(id) returning Flow<TaskEntity?>
    - Define getTasksByCategory(category) returning Flow<List<TaskEntity>>
    - Define insertTask(task), updateTask(task), deleteTask(taskId), completeTask(taskId, timestamp)
    - _Requirements: 1.1, 1.7, 1.8, 1.9, 9.6_

  - [ ] 3.3 Create TaskCompletionDao interface
    - Define insertCompletion(completion)
    - Define getCompletionsInPeriod(startTimestamp, endTimestamp) returning Flow<List<TaskCompletionEntity>>
    - Define getTotalFocusTimeInPeriod(startTimestamp, endTimestamp) returning Flow<Int?>
    - Define getDaysWithCompletions(startTimestamp) returning Flow<Int>
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ] 3.4 Create AppDatabase class
    - Define @Database annotation with entities and version
    - Create abstract methods for taskDao() and taskCompletionDao()
    - _Requirements: 10.1_

- [ ] 4. Implement data layer - DataStore
  - [ ] 4.1 Create PreferencesRepository interface and implementation
    - Define interface with getPreferences() returning Flow<UserPreferences>
    - Define savePreferences(preferences), saveFocusSession(sessionData), getFocusSession()
    - Implement PreferencesRepositoryImpl using Preferences DataStore
    - _Requirements: 10.2, 10.4, 11.4_

  - [ ] 4.2 Write property test for preferences persistence
    - **Property 30: Preferences Persistence Round Trip**
    - **Validates: Requirements 10.2, 10.4, 11.4**

- [ ] 5. Implement data layer - Repository implementations
  - [ ] 5.1 Create TaskRepository interface
    - Define getAllTasks(), getTaskById(id), insertTask(task), updateTask(task), deleteTask(taskId), completeTask(taskId, timestamp), getTasksByCategory(category)
    - All methods return Flow or suspend functions
    - _Requirements: 1.1, 1.7, 1.8, 1.9_

  - [ ] 5.2 Implement TaskRepositoryImpl
    - Inject TaskDao and TaskCompletionDao
    - Implement all methods with entity-to-domain model mapping
    - Map TaskEntity to Task and vice versa
    - _Requirements: 1.1, 1.7, 1.8, 1.9, 10.6_

  - [ ] 5.3 Write property tests for TaskRepository
    - **Property 2: Task CRUD Operations**
    - **Property 3: Task Update Persistence**
    - **Property 4: Task Deletion**
    - **Property 5: Task Completion Recording**
    - **Property 29: Task Persistence Round Trip**
    - **Validates: Requirements 1.1, 1.7, 1.8, 1.9, 10.1, 10.3, 10.6**

  - [ ] 5.3 Create PerformanceRepository interface and implementation
    - Define recordCompletion(taskId, timestamp, focusDuration)
    - Define getCompletionsInPeriod(startTimestamp, endTimestamp), getTotalFocusTimeInPeriod(), getConsistencyScore(days)
    - Implement PerformanceRepositoryImpl using TaskCompletionDao
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.6_

  - [ ] 5.4 Write property tests for PerformanceRepository
    - **Property 21: Completion Timestamp Recording**
    - **Property 22: Completed Tasks Count**
    - **Property 23: Total Focus Time Calculation**
    - **Property 24: Consistency Score Calculation**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [ ] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement domain layer - Scoring Engine
  - [ ] 7.1 Create ScoringEngine class
    - Implement calculateScore(task) using weighted formula: (2.0 * urgency) + (3.0 * importance) + (-0.5 * effortValue) + energyBonus
    - Map effort to values: LOW=1, MEDIUM=2, HIGH=3
    - Map energy to bonus: DEEP_WORK=1.0, LOW=0.5, null=0
    - Implement getBestTask(tasks) returning highest-scoring task with timestamp tiebreaker
    - Implement getTopThreeTasks(tasks) returning top 3 tasks in descending score order
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ] 7.2 Write property tests for ScoringEngine
    - **Property 6: Score Calculation and Best Task Selection**
    - **Property 7: Top Three Task Ordering**
    - **Property 8: Score Tiebreaker**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**

  - [ ] 7.3 Write unit tests for ScoringEngine edge cases
    - Test empty task list returns null
    - Test single task returns that task
    - Test identical scores use timestamp tiebreaker
    - _Requirements: 2.4, 2.5_

- [ ] 8. Implement domain layer - Eisenhower Classifier
  - [ ] 8.1 Create EisenhowerClassifier class
    - Implement classify(task) returning EisenhowerQuadrant based on urgency/importance thresholds
    - URGENT_IMPORTANT: urgency >= 4 AND importance >= 4
    - NOT_URGENT_IMPORTANT: urgency <= 3 AND importance >= 4
    - URGENT_NOT_IMPORTANT: urgency >= 4 AND importance <= 3
    - NEITHER: urgency <= 3 AND importance <= 3
    - Implement groupByQuadrant(tasks) returning Map<EisenhowerQuadrant, List<Task>>
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [ ] 8.2 Write property tests for EisenhowerClassifier
    - **Property 16: Eisenhower Urgent and Important Classification**
    - **Property 17: Eisenhower Not Urgent but Important Classification**
    - **Property 18: Eisenhower Urgent but Not Important Classification**
    - **Property 19: Eisenhower Neither Classification**
    - **Property 20: Eisenhower Quadrant Counts**
    - **Validates: Requirements 6.2, 6.3, 6.4, 6.5, 6.7**

- [ ] 9. Implement domain layer - Focus Session Manager
  - [ ] 9.1 Create FocusSessionManager class
    - Define sessionState as MutableStateFlow<FocusSessionState> initialized to Idle
    - Implement startSession(task, focusDuration, breakDuration) that saves state to DataStore and updates StateFlow
    - Implement pauseSession() that transitions Focusing to Paused, preserving elapsed time
    - Implement resumeSession() that transitions Paused back to Focusing
    - Implement endSession() that transitions to Idle and records completion if session was completed
    - Implement transitionToBreak() for automatic transition when focus completes
    - Implement completeSession() for automatic transition when break completes
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.6, 4.7, 4.8, 4.9_

  - [ ] 9.2 Write property tests for FocusSessionManager
    - **Property 9: Custom Focus Duration Application**
    - **Property 10: Focus Session State Transition**
    - **Property 11: Focus Session Pause Preservation**
    - **Property 12: Focus Session Early Termination**
    - **Property 13: Focus Completion Recording**
    - **Validates: Requirements 4.2, 4.4, 4.6, 4.7, 4.8, 4.9, 11.3**

  - [ ] 9.3 Write unit tests for FocusSessionManager
    - Test default durations (25 minutes focus, 5 minutes break)
    - Test pause preserves exact remaining time
    - Test state transitions through full cycle
    - _Requirements: 4.1, 4.6, 4.7, 11.5_

- [ ] 10. Implement domain layer - Use Cases
  - [ ] 10.1 Create task management use cases
    - Implement CreateTaskUseCase(taskRepository) with validation
    - Implement UpdateTaskUseCase(taskRepository) with validation
    - Implement DeleteTaskUseCase(taskRepository)
    - Implement CompleteTaskUseCase(taskRepository, performanceRepository) that marks complete and records completion
    - Implement GetAllTasksUseCase(taskRepository)
    - Implement GetTaskByIdUseCase(taskRepository)
    - _Requirements: 1.1, 1.7, 1.8, 1.9, 7.1_

  - [ ] 10.2 Write unit tests for task use cases
    - Test CreateTaskUseCase validates urgency/importance ranges
    - Test CompleteTaskUseCase records completion timestamp
    - Test DeleteTaskUseCase removes task from queries
    - _Requirements: 1.2, 1.3, 1.8, 1.9_

  - [ ] 10.3 Create scoring use cases
    - Implement GetBestTaskUseCase(taskRepository, scoringEngine) that gets all tasks and returns best
    - Implement GetTopThreeTasksUseCase(taskRepository, scoringEngine)
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 10.4 Create JustStartUseCase
    - Inject GetBestTaskUseCase, StartFocusSessionUseCase, PreferencesRepository
    - Implement invoke() that gets best task, loads preferences, starts session, returns JustStartResult
    - Handle NoTasksAvailable case when best task is null
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 10.5 Create focus session use cases
    - Implement StartFocusSessionUseCase(focusSessionManager)
    - Implement PauseFocusSessionUseCase(focusSessionManager)
    - Implement ResumeFocusSessionUseCase(focusSessionManager)
    - Implement EndFocusSessionUseCase(focusSessionManager)
    - _Requirements: 4.6, 4.7, 4.8_

  - [ ] 10.6 Create Eisenhower use case
    - Implement GetEisenhowerMatrixUseCase(taskRepository, eisenhowerClassifier) that groups tasks by quadrant
    - _Requirements: 6.1, 6.6_

  - [ ] 10.7 Create performance tracking use cases
    - Implement GetProductivityMetricsUseCase(performanceRepository) with time period parameter
    - Return metrics: completedTasks, totalFocusTime, consistencyScore
    - _Requirements: 7.2, 7.3, 7.4, 7.5_

- [ ] 11. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Implement dependency injection with Hilt
  - [ ] 12.1 Create Hilt modules
    - Create DatabaseModule providing AppDatabase, TaskDao, TaskCompletionDao
    - Create RepositoryModule providing TaskRepository, PreferencesRepository, PerformanceRepository
    - Create DomainModule providing ScoringEngine, EisenhowerClassifier, FocusSessionManager
    - Annotate modules with @Module and @InstallIn(SingletonComponent::class)
    - _Requirements: All (enables dependency injection)_

  - [ ] 12.2 Create Application class
    - Create DecisionExecutionApp extending Application
    - Annotate with @HiltAndroidApp
    - Register in AndroidManifest.xml
    - _Requirements: All (enables Hilt)_

- [ ] 13. Implement presentation layer - ViewModels
  - [ ] 13.1 Create HomeViewModel
    - Define HomeUiState data class (bestTask, completedTasksToday, totalFocusTimeToday, consistencyScore, isLoading, error)
    - Inject GetBestTaskUseCase, JustStartUseCase, GetProductivityMetricsUseCase
    - Expose uiState as StateFlow
    - Implement loadHomeData() combining best task and metrics flows
    - Implement onJustStartClicked() triggering JustStartUseCase
    - _Requirements: 2.2, 3.1, 3.4, 7.2, 7.3, 7.4, 12.4_

  - [ ] 13.2 Write unit tests for HomeViewModel
    - Test initial state loading
    - Test Just Start with no tasks shows error
    - Test Just Start with tasks triggers session
    - _Requirements: 3.4, 12.4_

  - [ ] 13.3 Create TasksViewModel
    - Define TasksUiState data class (tasksByCategory, selectedCategory, isLoading, error)
    - Inject GetAllTasksUseCase, CompleteTaskUseCase, DeleteTaskUseCase, ScoringEngine
    - Implement grouping tasks by category and sorting by score within category
    - Implement onTaskComplete(taskId), onTaskDelete(taskId), onCategoryFilter(category)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.6, 9.7_

  - [ ] 13.4 Write property tests for task grouping and sorting
    - **Property 25: Task Grouping by Category**
    - **Property 26: Task Sorting by Score Within Category**
    - **Property 27: Category Filtering**
    - **Property 28: Active Task Count**
    - **Validates: Requirements 9.1, 9.2, 9.6, 9.7**

  - [ ] 13.5 Create FocusViewModel
    - Define FocusUiState based on FocusSessionState
    - Inject FocusSessionManager, PauseFocusSessionUseCase, ResumeFocusSessionUseCase, EndFocusSessionUseCase
    - Expose sessionState from FocusSessionManager
    - Implement onPause(), onResume(), onEnd()
    - _Requirements: 4.6, 4.7, 4.8, 5.1, 5.2, 5.3, 5.4, 5.6_

  - [ ] 13.6 Write property tests for FocusViewModel
    - **Property 14: Focus UI Task Display**
    - **Property 15: Focus vs Break State Distinction**
    - **Validates: Requirements 5.2, 5.6**

  - [ ] 13.7 Create SettingsViewModel
    - Define SettingsUiState data class (focusDuration, breakDuration, themeMode)
    - Inject PreferencesRepository
    - Implement onFocusDurationChanged(minutes) with validation (5-90)
    - Implement onBreakDurationChanged(minutes) with validation (1-30)
    - Implement onThemeModeChanged(mode)
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [ ] 13.8 Write property tests for preferences validation
    - **Property 31: Focus Duration Validation**
    - **Property 32: Break Duration Validation**
    - **Validates: Requirements 11.1, 11.2**

- [ ] 14. Implement presentation layer - Compose UI screens
  - [ ] 14.1 Create Material 3 theme
    - Define DecisionExecutionTheme with Material 3 color scheme
    - Support light and dark mode based on system settings
    - Define typography and spacing tokens
    - _Requirements: 12.1, 12.2, 12.8_

  - [ ] 14.2 Create HomeScreen composable
    - Display "Just Start" button prominently
    - Display BestTaskCard showing next task with title, category, effort
    - Display ProductivityStats showing completedToday, focusTimeToday, consistencyScore
    - Handle loading and error states
    - Wire up HomeViewModel
    - _Requirements: 12.4, 7.7_

  - [ ] 14.3 Create TasksScreen composable
    - Display tasks grouped by category with headers
    - Implement SwipeToDismiss for complete (swipe right) and delete/edit (swipe left)
    - Display task attributes: urgency, importance, effort, energy tag
    - Implement category filter chips
    - Display total active task count
    - Wire up TasksViewModel
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 12.5_

  - [ ] 14.4 Create FocusScreen composable
    - Implement full-screen layout with minimal UI
    - Display task title prominently
    - Display CircularProgressIndicator showing elapsed/total time
    - Display formatted time remaining (MM:SS)
    - Show Pause and End buttons
    - Distinguish focus mode from break mode visually (different colors/text)
    - Wire up FocusViewModel
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 12.6_

  - [ ] 14.5 Create TaskDetailScreen composable
    - Display all task attributes
    - Provide edit and delete actions
    - Navigate back on save or delete
    - _Requirements: 1.7, 1.8_

  - [ ] 14.6 Create AddEditTaskScreen composable
    - Create form with TextField for title and description
    - Create dropdowns for category, effort, energy tag
    - Create sliders for urgency (1-5) and importance (1-5)
    - Validate inputs and show error messages
    - Call CreateTaskUseCase or UpdateTaskUseCase on save
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [ ] 14.7 Create SettingsScreen composable
    - Create sliders for focus duration (5-90) and break duration (1-30)
    - Display current values
    - Create theme mode selector (Light, Dark, System)
    - Wire up SettingsViewModel
    - _Requirements: 11.1, 11.2, 11.3_

  - [ ] 14.8 Create bottom navigation bar
    - Define three tabs: Home, Tasks, Focus
    - Implement navigation between screens
    - Highlight active tab
    - _Requirements: 12.3_

- [ ] 15. Implement navigation
  - [ ] 15.1 Create navigation graph
    - Define routes for Home, Tasks, Focus, TaskDetail, AddEditTask, Settings
    - Set Home as start destination
    - Implement bottom navigation with NavController
    - _Requirements: 12.3_

  - [ ] 15.2 Implement deep linking
    - Handle "just_start" action from widgets/notifications
    - Handle "open_task/{taskId}" action
    - Handle "focus" action
    - Implement deep link handling in MainActivity
    - _Requirements: 8.6, 8.7, 13.4, 13.5_

- [ ] 16. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 17. Implement background work - WorkManager
  - [ ] 17.1 Create FocusTimerWorker
    - Extend CoroutineWorker
    - Inject FocusSessionManager and NotificationHelper via @HiltWorker
    - Implement doWork() to check session state and update elapsed time
    - Trigger transitionToBreak() when focus completes
    - Trigger completeSession() when break completes
    - Show notifications on completion
    - _Requirements: 4.3, 4.4, 4.5, 13.1, 13.2, 13.3, 13.6_

  - [ ] 17.2 Integrate WorkManager with FocusSessionManager
    - Schedule PeriodicWorkRequest (1-minute interval) when session starts
    - Cancel work when session ends
    - Use ExistingPeriodicWorkPolicy.REPLACE to avoid duplicates
    - _Requirements: 13.1, 13.6_

  - [ ] 17.3 Write unit tests for FocusTimerWorker
    - Test worker updates session state correctly
    - Test worker triggers notifications on completion
    - Test worker cancels when session ends
    - _Requirements: 13.1, 13.2, 13.3_

- [ ] 18. Implement notifications
  - [ ] 18.1 Create NotificationHelper class
    - Create notification channel for focus notifications
    - Implement showFocusCompleteNotification() with deep link to break screen
    - Implement showBreakCompleteNotification() with deep link to home screen
    - Handle notification permission for Android 13+
    - _Requirements: 13.2, 13.3, 13.4, 13.5_

  - [ ] 18.2 Request notification permission
    - Check and request POST_NOTIFICATIONS permission on Android 13+
    - Gracefully degrade if permission denied (continue timer without notifications)
    - _Requirements: 13.2_

- [ ] 19. Implement widgets with Glance
  - [ ] 19.1 Create BestTaskWidget
    - Extend GlanceAppWidget
    - Display "Next Task" label and best task title
    - Display task category and effort
    - Show "No tasks available" when no tasks exist
    - Add "Just Start" button with deep link action
    - Update widget when task list changes (via WorkManager periodic sync every 15 minutes)
    - _Requirements: 8.1, 8.3, 8.4, 8.7_

  - [ ] 19.2 Create FocusTimerWidget
    - Extend GlanceAppWidget
    - Display current task title and timer (MM:SS)
    - Update every minute while session is active
    - Show "No active session" when idle
    - Add tap action to open Focus screen
    - _Requirements: 8.2, 8.5, 8.6_

  - [ ] 19.3 Create widget update workers
    - Create BestTaskWidgetUpdateWorker to refresh best task widget every 15 minutes
    - Create FocusTimerWidgetUpdateWorker to refresh timer widget every minute during active session
    - Schedule workers appropriately
    - _Requirements: 8.4, 8.5_

- [ ] 20. Implement error handling and edge cases
  - [ ] 20.1 Add validation error handling
    - Catch IllegalArgumentException from domain models
    - Display user-friendly error messages in UI
    - Test empty title, out-of-range urgency/importance, invalid durations
    - _Requirements: 1.2, 1.3, 11.1, 11.2_

  - [ ] 20.2 Add database error handling
    - Wrap database operations in try-catch blocks
    - Handle SQLiteException, SQLiteConstraintException, SQLiteDiskIOException
    - Display error messages and retry options in UI
    - _Requirements: 10.1, 10.6_

  - [ ] 20.3 Add WorkManager error handling
    - Implement retry with exponential backoff for worker failures (max 3 retries)
    - Handle timer drift by recalculating from stored start timestamp
    - Restore session state from DataStore on app restart after process death
    - _Requirements: 13.6_

  - [ ] 20.4 Add widget error handling
    - Catch all exceptions in widget update code
    - Display placeholder content when data unavailable
    - Use cached data on update timeout
    - _Requirements: 8.4, 8.5_

- [ ] 21. Final integration and polish
  - [ ] 21.1 Wire all components together
    - Verify all ViewModels are properly injected
    - Verify all navigation flows work correctly
    - Verify deep links from widgets and notifications work
    - Verify background timer continues when app is backgrounded
    - _Requirements: All_

  - [ ] 21.2 Test complete user flows
    - Test Just Start flow: tap button → best task selected → focus session starts → timer runs → break starts → session completes
    - Test task creation → appears in list → swipe to complete → removed from list → appears in metrics
    - Test preferences change → applied to next focus session
    - Test widget tap → opens app to correct screen
    - _Requirements: 3.1, 3.2, 3.3, 9.3, 11.3, 8.6, 8.7_

  - [ ] 21.3 Run all property-based tests with increased iterations
    - Run all property tests with 500 iterations
    - Verify no failures
    - _Requirements: All_

  - [ ] 21.4 Verify Material 3 design consistency
    - Check all screens use consistent spacing, typography, colors
    - Verify light and dark mode work correctly
    - Verify accessibility (content descriptions, touch targets)
    - _Requirements: 12.1, 12.2, 12.7, 12.8_

- [ ] 22. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples, edge cases, and error conditions
- Checkpoints ensure incremental validation throughout implementation
- The implementation follows Clean Architecture: data layer → domain layer → presentation layer
- All code uses Kotlin with Jetpack Compose, Room, DataStore, WorkManager, and Hilt
- Background timer uses WorkManager to survive process death and handle battery optimization
- Widgets use Glance (Jetpack Compose for widgets) for consistent UI patterns
