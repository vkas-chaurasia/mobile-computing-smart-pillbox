# MSE TSM MobCom Project A: Smart Pillbox System

**Team A**: Vikas Chaurasia, Sai Kiran Sabavath, Jonathan Müller

## 1. Introduction (Use Case)

The **Smart Pillbox System** is designed to assist patients in adhering to their medication schedules. Non-adherence to medication is a global health issue, often due to simple forgetfulness.

**Use Case**:
A patient fills the smart pillbox with their daily medication. When they open the lid to take a pill, the system:
1. Detects the opening via light sensors.
2. Transmits this "Pill Taken" event immediately to the companion Android app.
3. Logs the event in a history database for review by the patient or caregiver.
4. Updates the dashboard to show the medication as "Taken" for the current slot.

This automated tracking removes the burden of manual logging and provides peace of mind.

## 2. System Architecture

### Reference Model

The system follows a classic **IoT Gateway Architecture**:
- **Perception Layer (Hardware)**: An Arduino-based device equipped with:
    - **Light Sensors**: Detect ambient light when compartments open.
    - **Tilt Sensor**: Detects physical movement/handling of the box.
- **Network Layer**: Bluetooth Low Energy (BLE) serves as the communication medium, chosen for its low power consumption suitable for battery-operated devices.
- **Application Layer (Android)**: The "Pillbox" app acts as the gateway and user interface, processing raw sensor data into meaningful health insights.

> See [diagrams.md](diagrams.md) for full architecture diagrams rendered in Mermaid.

### Interfaces

#### A. Bluetooth Low Energy (BLE) Interface

The core communication is defined by a custom GATT Service.
- **Service UUID**: `0000ab00-b5a3-f393-e0a9-e50e24dcca9e`
- **Characteristics**:
    - **Light Sensor #1**: `...ab01...` (Notify/Read) — Sends percentage (0–100%).
    - **Light Sensor #2**: `...ab02...` (Notify/Read) — Sends percentage (0–100%).
    - **Tilt Sensor**: `...ab03...` (Notify/Read) — Sends binary state (0/1).

#### B. HTTP/Cloud Interfaces

*Note: The current iteration operates in **Offline Mode**.*
Data is stored locally using **Room Database** (SQLite). No external REST APIs are currently implemented, ensuring privacy and functionality without internet access.

## 3. User Interface (UI)

The application features a modern, clean interface built with **Jetpack Compose**.

### 3.1 Welcome Screen

The entry point of the app, designed to be inviting and simple, guiding the user to connect their device.

### 3.2 Dashboard Screen

The main hub where users spend most of their time. It provides connection status, live sensor updates (Open/Closed), and schedule cards.

### 3.3 Navigation

The app uses a bottom navigation bar to switch between:
- **Home**: Live dashboard.
- **History**: A list of all past "Pill Taken" events.
- **Schedule**: Configuration of medication times.
- **Settings**: Device scanning and generic preferences.

## 4. Software Design

The application works on the **Clean Architecture** principle combined with **MVVM (Model-View-ViewModel)**.

### 4.1 Class Diagram

The following diagram illustrates the separation of concerns:
- **Main Activity**: Hosts the Navigation Graph.
- **ViewModel**: Holds UI state (StateFlow) and communicates with the Repository.
- **Repository**: Abstracts data sources (BLE Manager and Room Database).

> See [diagrams.md](diagrams.md#2-detailed-class-diagram) for the full class diagram.

### 4.2 Sequence Diagram

This diagram details the flow of data from the hardware sensor to the UI update:
1. **Hardware** notifies the Android BLE Manager.
2. **Repo** receives the byte array and emits a Flow.
3. **ViewModel** collects the Flow and updates `_uiState`.
4. **UI** (Compose) observes the state change and redraws the screen.

> See [diagrams.md](diagrams.md#3-sequence-diagram-pill-taken-event) for the full sequence diagram.

## 5. Discussion

### Achievements

- **Seamless Connectivity**: Successfully implemented robust BLE scanning and auto-reconnection logic.
- **Real-time Responsiveness**: Achieved near-instant (<200ms) UI updates when sensors are triggered.
- **Modern Tech Stack**: Fully utilized Kotlin, Coroutines, and Jetpack Compose, resulting in a maintainable codebase.

### Technical Issues

- **Background Execution**: Android 12+ has strict restrictions on background scanning. We had to implement a foreground service to maintain connection stability when the app is minimized.
- **Sensor Noise**: The light sensors fluctuated under indoor lighting. We implemented a software "deadband" filter in the Arduino code to prevent spamming notifications.

### Lessons Learned

- **State Management**: `StateFlow` is superior to `LiveData` for the domain layer but requires careful collection in the UI lifecycle.
- **BLE Complexity**: Handling the asynchronous nature of GATT operations (connect → discover services → enable notifications) requires a robust state machine, not just linear code.

### Outlook

- **Cloud Sync**: Future versions will sync data to a FHIR-compliant cloud server.
- **Notifications**: Push notifications will remind users if they *haven't* opened the box by a specific time.
