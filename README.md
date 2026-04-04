# Decision-Driven Execution App

A native Android productivity application that eliminates decision paralysis by automatically selecting the best task for users to execute right now.

## Overview

Unlike traditional to-do apps that present overwhelming lists, this app acts as a decision engine that analyzes task attributes (urgency, importance, effort, energy) and recommends a single optimal task. The core innovation is "Just Start" mode—a one-tap flow that selects the best task, launches a Pomodoro timer, and enters distraction-free focus mode without requiring any user decisions.

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture principles
- **Database**: Room (SQLite wrapper)
- **Preferences**: DataStore (Preferences DataStore)
- **Background Work**: WorkManager
- **Dependency Injection**: Hilt
- **Coroutines**: Kotlin Coroutines with Flow
- **Widgets**: Glance (Jetpack Compose for widgets)
- **Testing**: Kotest, MockK

## Project Structure

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
│   └── service/          # Business logic services
├── presentation/
│   ├── home/             # Home screen UI + ViewModel
│   ├── tasks/            # Tasks screen UI + ViewModel
│   ├── focus/            # Focus screen UI + ViewModel
│   └── widget/           # Widget providers
└── di/                   # Hilt modules
```

## Requirements

- Android SDK 26+ (Android 8.0 Oreo)
- Target SDK 34 (Android 14)
- Kotlin 1.9.20
- Gradle 8.2.0

## Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or physical device

## Testing

The project uses a dual testing approach:

- **Unit Tests**: Verify specific examples, edge cases, and error conditions using Kotest
- **Property-Based Tests**: Verify universal properties across all inputs using Kotest Property Testing

Run tests with:
```bash
./gradlew test
```

## License

[Add your license here]
