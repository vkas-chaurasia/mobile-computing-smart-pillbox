# Pillbox Android App - Project Objective

## Project Goal

Build a Proof of Concept (PoC) Android application for a smart medicine pillbox that:
- Connects to a BLE-enabled pillbox device via Bluetooth Low Energy
- Allows users to set medication schedules (one pill per day)
- Monitors pill consumption using tilt and light sensors
- Sends alerts when medication is due or missed
- Tracks medication adherence history

## Core Features

### 1. Device Connection & Management
- Bluetooth Low Energy (BLE) scanning and connection
- Real-time connection status monitoring
- Battery level display
- Device information (manufacturer, model)

### 2. Medication Scheduling
- Single medication schedule (one pill per day)
- Multi-day selection (Mon, Tue, Wed, etc.)
- Time picker for scheduled dose time
- Optional medication name
- Schedule reset/update functionality

### 3. Sensor-Based Pill Detection
- **Tilt Sensor**: Detects when pillbox is opened (tilt = 1)
- **Light Sensor**: Detects when pill is removed (light > threshold)
- **Detection Logic**: Both tilt AND light conditions must be met for successful detection
  - Tilt = 1 AND Light > threshold → Pill taken 
  - Tilt = 1 AND Light ≤ threshold → Box opened but pill not taken 
  - Tilt = 0 AND Light > threshold → Possible pill fell off (ignored) 
- Configurable thresholds (default: Light > 40%, Tilt = 1)
  - TODO: Adjust thresholds after connecting with real device

### 4. Alert & Notification System
- **Reminder Notification**: Triggered at scheduled medication time
- **Missed Dose Alert**: Triggered 1 hour after scheduled time if pill not consumed
- Connection status alerts (device disconnected)

### 5. Manual Override
- "Mark as Taken" button for manual dose recording
- Allows users to record consumption if sensors miss detection

### 6. History & Tracking
- Daily consumption records (taken/missed)
- Detection method tracking (sensor/manual)
- Weekly compliance statistics
- Consumption streak tracking

### 7. Settings & Configuration
- Theme selection (Light/Dark/System default)
- Sensor threshold configuration
- Notification preferences
- Device management (connect/disconnect)

## High-Level User Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    APP LAUNCH                                │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Screen 1: Welcome & Connection                              │
│  - Check Bluetooth status                                    │
│  - Scan for pillbox device                                   │
│  - Connect to selected device                                │
│  - Show connection status + battery                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼ (Connected)
┌─────────────────────────────────────────────────────────────┐
│  Screen 3: Dashboard (Home)                                  │
│  - Today's medication status                                 │
│  - Next dose countdown                                       │
│  - Sensor status indicators                                  │
│  - Quick actions (Mark as Taken, View History, Edit)        │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Screen 2:    │ │ Screen 4:    │ │ Screen 5:    │
│ Schedule     │ │ History      │ │ Settings     │
│ Setup        │ │ & Analytics  │ │ & Device     │
│              │ │              │ │ Management   │
│ - Add/Edit   │ │ - View       │ │ - Theme      │
│   schedule   │ │   history    │ │ - Thresholds │
│ - Select     │ │ - Statistics │ │ - Device     │
│   days/time  │ │ - Compliance │ │   info       │
│ - Reset      │ │              │ │              │
└──────────────┘ └──────────────┘ └──────────────┘
```

### Detailed Flow States

```
┌─────────────────────────────────────────────────────────────┐
│                    BACKGROUND PROCESSES                      │
│                                                               │
│  1. Sensor Monitoring Service                                │
│     - Continuously monitor tilt & light sensors              │
│     - Detect pill removal (tilt=1 AND light>threshold)      │
│     - Match detection to scheduled dose                      │
│     - Update consumption record                              │
│                                                               │
│  2. Alert Scheduler Service                                  │
│     - Check active schedules every minute                    │
│     - At scheduled time → Send reminder notification         │
│     - After scheduled time + 1 hour → Send missed alert      │
│     - Mark as MISSED if not consumed within grace period    │
│                                                               │
│  3. BLE Connection Service                                   │
│     - Maintain connection to pillbox                         │
│     - Handle reconnection on disconnect                      │
│     - Monitor battery level                                  │
└─────────────────────────────────────────────────────────────┘
```

## Architecture Overview

### Component Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI LAYER                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Screen 1 │  │ Screen 2 │  │ Screen 3 │  │ Screen 4 │       │
│  │ Welcome  │  │ Schedule │  │Dashboard │  │ History  │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       │             │              │             │              │
│  ┌────┴─────────────┴──────────────┴─────────────┴─────┐       │
│  │                    Screen 5: Settings                │       │
│  └──────────────────────────────────────────────────────┘       │
└────────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      VIEWMODEL LAYER                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │PillboxViewModel  │  │ScheduleViewModel │  │HistoryViewModel│ │
│  │  - Connection    │  │  - Schedule CRUD │  │  - Records   │  │
│  │  - Sensor data   │  │  - Validation    │  │  - Statistics│  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘  │
│           │                     │                    │          │
│  ┌────────┴─────────────────────┴────────────────────┴──────┐  │
│  │              SettingsViewModel                            │  │
│  │              - Theme preferences                          │  │
│  │              - Sensor thresholds                          │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │PillboxRepository │  │ScheduleRepository│  │HistoryRepository││
│  │  - BLE connect   │  │  - Room DB       │  │  - Room DB    │  │
│  │  - Sensor stream │  │  - Schedule CRUD │  │  - Records    │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘  │
│           │                     │                    │          │
│  ┌────────┴─────────────────────┴────────────────────┴──────┐  │
│  │              SettingsRepository                           │  │
│  │              - SharedPreferences                           │  │
│  │              - Theme, thresholds, preferences               │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────┬───────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  BLE Layer   │    │  Database    │    │  Services    │
│              │    │              │    │              │
│ - Scanner    │    │ - Room DB    │    │ - Notification│
│ - Manager    │    │ - Schedules  │    │ - Sensor      │
│ - Connection │    │ - Records    │    │   Monitor    │
│              │    │              │    │ - Alert       │
│              │    │              │    │   Scheduler   │
└──────────────┘    └──────────────┘    └──────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                             ▼
                    ┌──────────────┐
                    │  Arduino      │
                    │  Pillbox      │
                    │  Device       │
                    │  (BLE)        │
                    └──────────────┘
```

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    SENSOR DATA FLOW                             │
│                                                                 │
│  Arduino Device                                                 │
│    │                                                            │
│    │ BLE Notification: "light:75;tilt:1"                       │
│    ▼                                                            │
│  PillboxManager (BLE Layer)                                     │
│    │                                                            │
│    │ StateFlow<String> sensorData                               │
│    ▼                                                            │
│  PillboxRepository                                              │
│    │                                                            │
│    │ StateFlow<String> sensorData                               │
│    ▼                                                            │
│  PillboxViewModel                                               │
│    │                                                            │
│    │ parseSensorData() → lightValue, tiltValue                  │
│    ▼                                                            │
│  SensorMonitoringService                                        │
│    │                                                            │
│    │ detectPillRemoval(tilt, light) → Boolean                  │
│    ▼                                                            │
│  HistoryRepository                                              │
│    │                                                            │
│    │ createConsumptionRecord(status=TAKEN, method=SENSOR)      │
│    ▼                                                            │
│  Room Database                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    SCHEDULE & ALERT FLOW                         │
│                                                                 │
│  User Input (Screen 2)                                           │
│    │                                                            │
│    │ MedicationSchedule(days, time, pillCount)                 │
│    ▼                                                            │
│  ScheduleViewModel                                              │
│    │                                                            │
│    │ Validation & StateFlow update                              │
│    ▼                                                            │
│  ScheduleRepository                                              │
│    │                                                            │
│    │ saveSchedule() → Room DB                                   │
│    ▼                                                            │
│  AlertSchedulerService                                           │
│    │                                                            │
│    │ - Monitor schedules every minute                          │
│    │ - At scheduled time → NotificationService                 │
│    │ - After scheduled + 1hr → Check consumption               │
│    │ - If not consumed → Mark MISSED + Alert                   │
│    ▼                                                            │
│  NotificationService                                             │
│    │                                                            │
│    │ Show notification to user                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Screen Structure

### Screen 1: Welcome & Connection
**Purpose**: Initial setup and device connection

**Features**:
- Bluetooth status check and enable prompt
- BLE device scanning
- Device list with RSSI and battery indicators
- Connection status display
- Navigation to Dashboard after successful connection

**UI Components**:
- Bluetooth enable button (if disabled)
- Scan button with loading indicator
- Device list (cards with name, address, RSSI, battery)
- Connection status card
- Continue button (enabled when connected)

---

### Screen 2: Medication Schedule Setup
**Purpose**: Configure medication schedule

**Features**:
- Add/Edit single medication schedule
- Days of week multi-select (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
- Time picker (HH:MM format)
- Optional medication name field
- Reset schedule button (clears all data)
- Display current schedule if exists

**UI Components**:
- Schedule form (days selector, time picker, name field)
- Current schedule card (if exists)
- Save/Update button
- Reset button (with confirmation dialog)
- Empty state message (if no schedule)

---

### Screen 3: Dashboard (Home)
**Purpose**: Main screen showing current status

**Features**:
- Today's medication status
  - Next dose time and countdown
  - Status badge (Pending/Taken/Missed)
- Connection status card (connected/disconnected, battery level)
- Sensor status indicators
  - Box status (closed/open from tilt sensor)
  - Light level display
  - Last detection timestamp
- Quick actions
  - "Mark as Taken" button (manual override)
  - "View History" button
  - "Edit Schedule" button
- Today's compliance indicator

**UI Components**:
- Status cards (connection, medication, sensors)
- Countdown timer
- Action buttons
- Sensor indicators

---

### Screen 4: History & Analytics
**Purpose**: Track medication adherence

**Features**:
- Consumption history list
  - Date, scheduled time, status (Taken/Missed)
  - Detection method (Sensor/Manual)
  - Actual consumption time (if taken)
- Filter options (All / Taken / Missed)
- Statistics section
  - Weekly compliance percentage
  - Current streak (consecutive days)
  - Total taken/missed counts

**UI Components**:
- History list (chronological)
- Filter tabs/chips
- Statistics cards
- Empty state (if no history)

---

### Screen 5: Settings & Device Management
**Purpose**: App configuration and device management

**Features**:
- Connected device section
  - Device name, battery level, MAC address
  - Disconnect button
- Theme settings
  - Light mode / Dark mode / System default
- Sensor threshold configuration
  - Light threshold slider (default: 40%)
  - Tilt threshold display (default: 1, read-only for now)
  - TODO comments for real device testing
- Notification settings
  - Enable/disable reminders
  - Sound and vibration preferences
- About section
- Reset all data option

**UI Components**:
- Settings list with sections
- Theme selector (radio buttons or dropdown)
- Threshold sliders
- Toggle switches for notifications
- Device info card

---

## Data Models

### MedicationSchedule
```kotlin
data class MedicationSchedule(
    val id: String,
    val medicationName: String = "Medication",
    val daysOfWeek: Set<DayOfWeek>, // MONDAY, TUESDAY, etc.
    val time: LocalTime, // 16:00
    val pillCount: Int = 1,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

### ConsumptionRecord
```kotlin
data class ConsumptionRecord(
    val id: String,
    val date: LocalDate,
    val scheduledTime: LocalTime,
    val consumedTime: LocalDateTime?,
    val status: ConsumptionStatus, // PENDING, TAKEN, MISSED
    val detectionMethod: DetectionMethod? // SENSOR, MANUAL, null
)
```

### SensorThresholds
```kotlin
data class SensorThresholds(
    val lightThreshold: Int = 40, // TODO: Adjust after connecting with real device
    val tiltThreshold: Int = 1    // TODO: Adjust after connecting with real device
)
```

## Technical Stack

### Existing Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (ViewModel + Repository)
- **BLE Library**: Nordic BLE Library (`no.nordicsemi.android:ble-ktx:2.11.0`)
- **Reactive**: StateFlow, Flow
- **Coroutines**: Kotlin Coroutines

### New Additions Required
- **Database**: Room Database (for schedules and history)
- **Preferences**: SharedPreferences / DataStore (for settings)
- **Notifications**: NotificationManager + NotificationChannel
- **Background Work**: WorkManager (for alert scheduling)
- **Date/Time**: java.time (LocalTime, LocalDate, LocalDateTime)

## Implementation Phases

### Phase 1: Core MVP (Foundation)
1. ✅ Screen 1: Connection
2. ⬜ Screen 2: Schedule Setup
   - Room Database setup
   - ScheduleRepository
   - ScheduleViewModel
   - UI components
3. ⬜ Screen 3: Dashboard
   - Today's status display
   - Manual "Mark as Taken" functionality
   - Sensor status indicators
4. ⬜ Basic sensor detection logic
   - PillDetectionLogic class
   - Threshold configuration
   - Match detection to scheduled dose

### Phase 2: Alerts & History
5. ⬜ Notification system
   - NotificationService
   - NotificationChannel setup
   - Reminder notifications
   - Missed dose alerts
6. ⬜ Alert scheduler
   - WorkManager setup
   - Schedule monitoring (every minute)
   - 1-hour grace period logic
   - Mark as MISSED functionality
7. ⬜ Screen 4: History (new)
   - HistoryRepository
   - HistoryViewModel
   - UI components
   - Statistics calculation

### Phase 3: Polish & Settings
8. ⬜ Screen 5: Settings (new)
   - SettingsRepository (SharedPreferences)
   - SettingsViewModel
   - Theme implementation
   - Threshold configuration UI
9. ⬜ Navigation
   - Bottom navigation bar
   - Navigation graph
   - Deep linking (optional)
10. ⬜ Testing & refinement
    - Threshold adjustment based on real device testing
    - Bug fixes
    - UI/UX improvements

## Key Implementation Notes

### Sensor Detection Algorithm
- **Success Condition**: `tilt == 1 AND light > threshold`
- **Failure Condition**: `tilt == 1 AND light <= threshold` (box opened, pill not taken)
- **Ignore Condition**: `tilt == 0 AND light > threshold` (possible pill fell off)
- Thresholds are configurable with TODO comments for real device testing

### Alert Timing
- **Reminder**: At scheduled time → "Time to take your medication!"
- **Missed Alert**: After scheduled time + 1 hour → "You missed your medication dose!"
- **Grace Period**: 1 hour after scheduled time

### Manual Override
- Available on Dashboard screen
- Only works for PENDING or MISSED doses
- Creates ConsumptionRecord with `detectionMethod = MANUAL`
- Updates status to TAKEN

### Schedule Reset
- Available in Screen 2 (Schedule Setup)
- Clears all schedule data
- Confirmation dialog before reset

## Success Criteria

1. User can connect to pillbox device via BLE
2. User can set medication schedule (days + time)
3. App detects pill consumption via sensors (tilt + light)
4. App sends reminder notification at scheduled time
5. App sends missed alert after 1-hour grace period
6. User can manually mark dose as taken
7. App tracks and displays consumption history
8. User can view statistics and compliance data
9. User can configure theme and sensor thresholds
10. App maintains connection and handles disconnections gracefully

---

## Notes for Development

- All sensor thresholds should include TODO comments: "TODO: Adjust after connecting with real device"
- Default thresholds: Light > 40%, Tilt = 1
- Maintain existing architecture patterns (MVVM, Repository, StateFlow)
- Use Room Database for persistent data (schedules, history)
- Use SharedPreferences for user preferences (theme, thresholds)
- Background services should be lightweight and battery-efficient
- Handle edge cases: device disconnection, app backgrounding, timezone changes

