# Arduino — Smart Pillbox Firmware

Firmware for the **Adafruit Feather nRF52840** that reads two light sensors and a tilt sensor, then exposes the data over Bluetooth Low Energy (BLE) via custom GATT characteristics.

## Hardware

| Component | Connection |
|---|---|
| Light Sensor #1 | Analog pin **A0** |
| Light Sensor #2 | Analog pin **A2** |
| Tilt Sensor | Digital pin **9** |
| Grove Adapter | Used to connect sensors to the Feather |

## BLE GATT Service

| Characteristic | UUID | Type |
|---|---|---|
| Pillbox Service | `0000AB00-B5A3-F393-E0A9-E50E24DCCA9E` | Primary Service |
| Light Sensor 1 | `0000AB01-...` | Notify / Read — `uint8` (0–100 %) |
| Light Sensor 2 | `0000AB02-...` | Notify / Read — `uint8` (0–100 %) |
| Tilt Sensor | `0000AB03-...` | Notify / Read — `uint8` (0 or 1) |

## Sketches

| Folder | Description |
|---|---|
| `UartBlePeripheral_Light_Tilt/` | **Main sketch** — BLE peripheral with all three sensors |
| `LightSensorTest/` | Standalone test sketch for a single light sensor |
| `TiltSensorTest/` | Standalone test sketch for the tilt sensor |

## Prerequisites

- [Arduino IDE 2.x](https://www.arduino.cc/en/software/)
- **Board**: Adafruit Feather nRF52840 (install via Board Manager → Adafruit nRF52)
- **Library**: `Adafruit TinyUSB Library` (bundled with the board package)

## Upload

1. Connect the Feather via USB.
2. Arduino IDE → Tools → Board: **Adafruit Feather nRF52840 Sense**.
3. Arduino IDE → Tools → Port: select the Feather.
4. Arduino IDE → **Upload**.
