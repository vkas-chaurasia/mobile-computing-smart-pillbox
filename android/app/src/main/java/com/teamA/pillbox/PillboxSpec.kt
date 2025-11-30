package com.teamA.pillbox

import java.util.UUID

object PillboxSpec {

    // NORDIC UART SERVICE
    val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

     // The TX (Transmit) Characteristic.
     // The phone receives notifications from this to get sensor data.
    val NUS_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

     // The RX (Receive) Characteristic.
     // The phone writes to this to send commands to the Pillbox.
    val NUS_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

    // BATTERY SERVICE
    // Used for reading battery status.
    val BAS_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    val BAS_LEVEL_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

    // DEVICE INFORMATION SERVICE
    // Used for reading static device metadata.
    val DIS_SERVICE_UUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    val DIS_MODEL_NUMBER_UUID: UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")
    val DIS_MANUFACTURER_NAME_UUID: UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")
}
