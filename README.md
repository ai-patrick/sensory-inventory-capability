# Sensor Inventory Capability Analysis

Android app for **Lab 5 (ICS 2300)** — Android Sensor Inventory and Capability Analysis.

## Overview

This app enumerates all sensors exposed by the Android `SensorManager` on a physical device or emulator, displays detailed metadata for each sensor, and provides a comparative analysis for the **MavunoLink** agricultural tracking use case.

## Features

### Device Information
- Device model and manufacturer
- Android version and API level
- Environment detection (physical device vs emulator)

### Sensor Inventory
- Complete list of all available sensors with total count
- Empty-state handling for devices/emulators with no sensors
- Expandable cards showing detailed metadata per sensor:
  - **Name** & **Type** (with human-readable type name)
  - **Vendor**
  - **Maximum Range** (with correct SI units)
  - **Resolution** (with correct SI units)
  - **Power consumption** (µA)
  - **Minimum delay** (µs) / **Maximum delay** (µs) — API 24+
  - **FIFO reserved/max event count** — API 24+
  - **String type** — API 24+
  - **Reporting mode** (Continuous, On Change, One Shot, Special) — API 24+
  - **Wake-up sensor** flag — API 24+

### Sensor Comparison for MavunoLink
- Compares **Accelerometer** vs **Gyroscope** for motion tracking
- Justifies sensor fusion approach for agricultural equipment monitoring
- Covers battery cost, measurement quality, device availability
- Falls back gracefully when sensors are unavailable (emulator limitations noted)

## Requirements

- Android Studio (latest)
- JDK 11+
- Android SDK API 34+ (compileSdk 36)
- Minimum SDK: API 24 (Android 7.0 Nougat)

## Building

```bash
# Clone the repository
git clone https://github.com/ai-patrick/sensory-inventory-capability.git
cd sensory-inventory-capability

# Build debug APK
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
sensorInventory/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/sensorinventory/
│   │   │   │   └── MainActivity.java      # Main logic: sensor enumeration, UI, comparison
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml  # Main screen with ScrollView + RecyclerView
│   │   │   │   │   └── item_sensor.xml    # Expandable sensor card layout
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml        # All UI strings
│   │   │   │   │   └── colors.xml         # Color palette (green theme)
│   │   │   │   └── drawable/
│   │   │   │       └── badge_background.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                          # Unit tests
│   │   └── androidTest/                   # Instrumented tests
│   ├── build.gradle.kts
│   └── .gitignore
├── gradle/
│   ├── libs.versions.toml                 # Version catalog
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── README.md
```

## Key Implementation Details

### Sensor Enumeration
```java
SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
```

### Environment Detection
Checks multiple `Build` properties to distinguish physical devices from emulators:
```java
boolean isEmulator = Build.FINGERPRINT.startsWith("generic")
    || Build.MODEL.contains("Emulator")
    || Build.HARDWARE.contains("goldfish")
    || "ranchu".equals(Build.HARDWARE);
// ... additional checks
```

### Units per Sensor Type
| Sensor Type | Unit |
|-------------|------|
| Accelerometer, Gravity, Linear Acceleration | m/s² |
| Gyroscope | rad/s |
| Magnetometer | µT |
| Light | lux |
| Pressure | Pa |
| Temperature | °C |
| Proximity | cm |
| Relative Humidity | % |
| Rotation Vector, Orientation | radians |
| Step Counter/Detector | steps |
| Heart Rate | bpm |

### Reporting Modes (API 24+)
- **Continuous**: Reports at regular intervals (e.g., accelerometer)
- **On Change**: Reports only when value changes significantly (e.g., step counter)
- **One Shot**: Reports once then disables (e.g., significant motion)
- **Special**: Custom reporting behavior (API 28+)

## Lab 5 Requirements Mapping

| Requirement | Implementation |
|-------------|----------------|
| List sensors with total count | `SensorManager.getSensorList(TYPE_ALL)` + RecyclerView badge |
| Device, OS, real/simulated | `Build` properties + environment detection |
| 3+ sensor metadata (name, type, vendor, range, resolution, power, delay) | Expandable cards with all fields |
| Units & meaning explained | Units per sensor type; comparison section explains each |
| Compare two sensors for MavunoLink | Accelerometer vs Gyroscope with justification |
| Handle unavailable sensor / empty inventory | Empty state view; comparison handles missing sensors |
| Emulator limitations noted | Comparison text explicitly calls out emulator limits |

## Screenshots

*(Add screenshots here after running on device/emulator)*

## Academic Context

- **Course**: ICS 2300 — Mobile Application Development
- **Institution**: JKUAT (Jomo Kenyatta University of Agriculture and Technology)
- **Assignment**: Lab_05_hm5 — Android Sensor Inventory and Capability Analysis
- **Instructor**: Dr. Lawrence Nderu
- **Due**: 18 November 2026, 23:59 EAT

## License

Educational use — JKUAT ICS 2300 coursework.

## Author

Patrick — ai-patrick