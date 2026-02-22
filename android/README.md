# Android — Pillbox Companion App

A **Jetpack Compose** Android application that connects to the Smart Pillbox hardware over BLE, tracks medication schedules, and records consumption history.

## Architecture

The app follows **Clean Architecture** with **MVVM**:

```
app/src/main/java/com/teamA/pillbox/
├── ble/                # BLE scanning & connection
├── database/           # Room entities, DAOs, database
├── domain/             # Domain models & business logic
├── repository/         # Data repositories
├── viewmodel/          # ViewModels (StateFlow-based)
├── ui/                 # Jetpack Compose screens & components
├── navigation/         # Navigation graph
├── notification/       # Notification channels & service
├── alarm/              # Alarm scheduling
├── sensor/             # Sensor monitoring service
├── service/            # Background services
├── worker/             # WorkManager workers
└── MainActivity.kt     # Entry point
```

## Key Features

- **BLE Connection** — Scan, connect, and auto-reconnect to the pillbox device
- **Real-time Sensor Data** — Live light and tilt sensor readings via GATT notifications
- **Medication Scheduling** — Set daily medication reminders with day-of-week selection
- **Pill Detection** — Automatic detection using combined tilt + light threshold logic
- **History & Analytics** — Consumption records, streaks, and weekly compliance stats
- **Notifications** — Reminder and missed-dose alerts via foreground service
- **Theming** — Light / Dark / System-default theme support

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Reactive | Kotlin Coroutines, StateFlow, Flow |
| BLE | Nordic BLE Library (`no.nordicsemi.android:ble-ktx`) |
| Database | Room (SQLite) |
| Background | WorkManager, Foreground Service |
| DI | Manual (constructor injection) |

## Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- Android device with BLE support, **API 23+**

## Build & Run

1. Open the `android/` folder in Android Studio.
2. Sync Gradle.
3. Connect a device via USB (or use an emulator without BLE).
4. **Run → Run 'app'**.
