# Mermaid Diagram Source Code

## 1. Full System Architecture (C4 Style)
Use this structure to visualize the Hardware, ALE, and Android layers.
Copy and paste into [Mermaid Live](https://mermaid.live/).

```mermaid
graph TD
    subgraph Hardware [Hardware Layer: Adafruit Feather nRF52840]
        style Hardware fill:#e1f5fe,stroke:#01579b
        LS1["Light Sensor 1<br/>(Analog A0)"]
        LS2["Light Sensor 2<br/>(Analog A2)"]
        TS["Tilt Sensor<br/>(Digital Pin 9)"]
        MCU["Microcontroller<br/>(Arduino Sketch)"]
        
        LS1 -->|Voltage| MCU
        LS2 -->|Voltage| MCU
        TS -->|Signal| MCU
    end

    subgraph BLE [Communication Layer: Bluetooth Low Energy]
        style BLE fill:#fff3e0,stroke:#e65100
        GATT[GATT Server]
        SVC["Pillbox Service<br/>UUID: ...ab00..."]
        
        C1["Char: Light 1<br/>UUID: ...ab01..."]
        C2["Char: Light 2<br/>UUID: ...ab02..."]
        C3["Char: Tilt<br/>UUID: ...ab03..."]
        
        MCU --> GATT
        GATT --> SVC
        SVC --> C1
        SVC --> C2
        SVC --> C3
    end

    subgraph Android [Application Layer: Android Mobile App]
        style Android fill:#e8f5e9,stroke:#1b5e20
        
        subgraph Data [Data Layer]
            Scanner[PillboxScanner]
            Repo[PillboxRepository]
            DB[("Room Database<br/>SQLite")]
            HistRepo[HistoryRepository]
            
            Entity1["MedicationSchedule<br/>Table"]
            Entity2["ConsumptionRecord<br/>Table"]
        end
        
        subgraph Domain [Domain Layer]
            VM[PillboxViewModel]
            Logic[PillDetectionLogic]
        end
        
        subgraph UI [Presentation Layer]
            Compose[Jetpack Compose UI]
            Nav[NavigationGraph]
        end
    end

    C1 -.->|Notify| Scanner
    C2 -.->|Notify| Scanner
    C3 -.->|Notify| Scanner
    
    Scanner --> Repo
    Repo --> VM
    VM -->|StateFlow| Compose
    
    VM --> Logic
    Logic -->|Event| HistRepo
    HistRepo --> DB
    DB --- Entity1
    DB --- Entity2
```

## 2. Detailed Class Diagram
```mermaid
classDiagram
    class MainActivity {
        +onCreate()
        +onStart()
        +onDeviceSelected()
    }
    class NavGraph {
        +NavHost(startDestination)
    }
    class PillboxViewModel {
        +uiState: StateFlow
        +connectionState: StateFlow
        +boxState: StateFlow
        +connectToDevice()
        +startScan()
        +handlePillDetection()
    }
    class HistoryViewModel {
        +uiState: StateFlow
        +filteredRecords: StateFlow
        +getTodayRecord()
        +getStatistics()
    }
    class PillboxRepository {
        +connect()
        +readBatteryLevel()
        +disconnect()
    }
    class HistoryRepository {
        +getAllRecords()
        +getRecordsByDateRange()
        +createRecord()
        +updateRecord()
    }
    class PillboxScanner {
        +startScan()
        +stopScan()
    }

    MainActivity ..> NavGraph : hosts
    NavGraph ..> PillboxViewModel : utilizes
    NavGraph ..> HistoryViewModel : utilizes
    PillboxViewModel --> PillboxRepository : depends on
    PillboxViewModel --> PillboxScanner : depends on
    PillboxViewModel --> HistoryRepository : saves records
    HistoryViewModel --> HistoryRepository : reads records
```

## 3. Sequence Diagram: Pill Taken Event
```mermaid
sequenceDiagram
    participant HW as Hardware (Sensors)
    participant BLE as Android BLE Stack
    participant Repo as PillboxRepository
    participant VM as PillboxViewModel
    participant DB as HistoryRepository
    participant UI as Compose UI

    Note over HW, BLE: Lid Opened & Light Levels Change
    HW->>BLE: Notify Characteristic (Light/Tilt)
    BLE->>Repo: onCharacteristicChanged()
    Repo->>VM: emit(lightSensorValue)
    
    rect rgb(240, 248, 255)
    Note right of VM: PillDetectionLogic
    VM->>VM: detectPillRemoval()
    VM->>VM: Check Thresholds & Schedule
    end

    alt Pill Detected
        VM->>DB: createRecord(Status=TAKEN)
        DB-->>VM: Success (Record Saved)
        VM->>UI: Update BoxState (OPEN/TAKEN)
        UI->>UI: Recompose (Show Green Check)
        Note over UI: User sees "Taken" status
    end
```

## 4. Arduino Logic Flowchart
```mermaid
flowchart TD
    Start([Start Loop]) --> ReadSensors[Read A0, A2, D9]
    ReadSensors --> CheckDiff{Change > Threshold?}
    
    CheckDiff -- Yes --> UpdateBLE[Update GATT Characteristics]
    UpdateBLE --> Notify[Notify Connected Client]
    Notify --> Delay[Delay 100ms]
    
    CheckDiff -- No --> Delay
    
    Delay --> ReadSensors
```

## 5. BLE GATT Services & Characteristics Schema
```mermaid
classDiagram
    direction TB
    class PillboxService {
        UUID: ...ab00...
        Type: Primary Service
    }
    class LightSensor1 {
        UUID: ...ab01...
        Props: Notify, Read
        Data: FLOAT (0-100%)
    }
    class LightSensor2 {
        UUID: ...ab02...
        Props: Notify, Read
        Data: FLOAT (0-100%)
    }
    class TiltSensor {
        UUID: ...ab03...
        Props: Notify, Read
        Data: UINT8 (0 / 1)
    }

    PillboxService *-- LightSensor1
    PillboxService *-- LightSensor2
    PillboxService *-- TiltSensor
```
