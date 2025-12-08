package com.teamA.pillbox.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.teamA.pillbox.PillboxSpec
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend

class PillboxManager(context: Context) : BleManager(context), Pillbox {

    // ***FIXED***: Renamed 'TAG' to 'tag' to follow Kotlin conventions.
    private val tag = "PillboxManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // --- Characteristics ---
    private var lightCharacteristic: BluetoothGattCharacteristic? = null
    private var light2Characteristic: BluetoothGattCharacteristic? = null // ***NEW***
    private var tiltCharacteristic: BluetoothGattCharacteristic? = null
    private var basCharacteristic: BluetoothGattCharacteristic? = null
    private var disModelCharacteristic: BluetoothGattCharacteristic? = null
    private var disManufCharacteristic: BluetoothGattCharacteristic? = null

    // --- State Flows ---
    private val _lightLevel = MutableStateFlow(0)
    override val lightLevel: StateFlow<Int> = _lightLevel

    private val _lightLevel2 = MutableStateFlow(0) // ***NEW***
    override val lightLevel2: StateFlow<Int> = _lightLevel2 // ***NEW***

    private val _tiltState = MutableStateFlow(0)
    override val tiltState: StateFlow<Int> = _tiltState

    private val _batteryLevel = MutableStateFlow(0)
    override val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _modelNumber = MutableStateFlow("N/A")
    override val modelNumber: StateFlow<String> = _modelNumber

    private val _manufacturerName = MutableStateFlow("N/A")
    override val manufacturerName: StateFlow<String> = _manufacturerName

    override val state: StateFlow<Pillbox.State> =
        stateAsFlow()
            .map { c ->
                when (c) {
                    is ConnectionState.Connecting,
                    is ConnectionState.Initializing -> Pillbox.State.LOADING
                    is ConnectionState.Ready -> Pillbox.State.READY
                    else -> Pillbox.State.NOT_AVAILABLE
                }
            }
            .stateIn(scope, SharingStarted.Lazily, Pillbox.State.NOT_AVAILABLE)


    override fun getMinLogPriority() = Log.VERBOSE

    override fun log(priority: Int, message: String) {
        // ***FIXED***: Updated to use the new 'tag' variable.
        Log.println(priority, tag, message)
    }

    @Suppress("DEPRECATION")
    private inner class PillboxGattCallback : BleManagerGattCallback() {

        @Deprecated("Required by this version of the library.", ReplaceWith(""))
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val pillboxService = gatt.getService(PillboxSpec.PILLBOX_SERVICE_UUID)
            if (pillboxService == null) {
                log(Log.ERROR, "CRITICAL: Pillbox Service not found.")
                return false
            }

            lightCharacteristic = pillboxService.getCharacteristic(PillboxSpec.LIGHT_SENSOR_CHARACTERISTIC_UUID)
            light2Characteristic = pillboxService.getCharacteristic(PillboxSpec.LIGHT_SENSOR_2_CHARACTERISTIC_UUID)
            tiltCharacteristic = pillboxService.getCharacteristic(PillboxSpec.TILT_SENSOR_CHARACTERISTIC_UUID)

            if (lightCharacteristic == null || light2Characteristic == null || tiltCharacteristic == null) {
                log(Log.ERROR, "CRITICAL: A required Pillbox characteristic was not found.")
                return false
            }

            val light1IsValid = (lightCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
            val light2IsValid = (light2Characteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
            val tiltIsValid = (tiltCharacteristic!!.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0

            if (!light1IsValid || !light2IsValid || !tiltIsValid) {
                log(Log.ERROR, "CRITICAL: Pillbox characteristics have wrong properties.")
                return false
            }
            log(Log.INFO, "SUCCESS: Required Pillbox Service and Characteristics are valid.")
            return true
        }

        @Deprecated("Required by this version of the library.", ReplaceWith(""))
        override fun isOptionalServiceSupported(gatt: BluetoothGatt): Boolean {
            gatt.getService(PillboxSpec.BAS_SERVICE_UUID)?.let { service ->
                basCharacteristic = service.getCharacteristic(PillboxSpec.BAS_LEVEL_CHARACTERISTIC_UUID)
            }
            gatt.getService(PillboxSpec.DIS_SERVICE_UUID)?.let { service ->
                disModelCharacteristic = service.getCharacteristic(PillboxSpec.DIS_MODEL_NUMBER_UUID)
                disManufCharacteristic = service.getCharacteristic(PillboxSpec.DIS_MANUFACTURER_NAME_UUID)
            }
            return true
        }

        @Deprecated("Required by this version of the library.", ReplaceWith(""))
        override fun initialize() {
            lightCharacteristic?.let { char ->
                setNotificationCallback(char)
                    .with { _, data: Data ->
                        val light = data.getIntValue(Data.FORMAT_UINT8, 0) ?: 0
                        if (_lightLevel.value != light) { _lightLevel.value = light }
                    }
            }

            light2Characteristic?.let { char ->
                setNotificationCallback(char)
                    .with { _, data: Data ->
                        val light2 = data.getIntValue(Data.FORMAT_UINT8, 0) ?: 0
                        if (_lightLevel2.value != light2) { _lightLevel2.value = light2 }
                    }
            }

            tiltCharacteristic?.let { char ->
                setNotificationCallback(char)
                    .with { _, data: Data ->
                        val tilt = data.getIntValue(Data.FORMAT_UINT8, 0) ?: 0
                        if (_tiltState.value != tilt) { _tiltState.value = tilt }
                    }
            }

            beginAtomicRequestQueue()
                .add(requestMtu(247).fail { _, status -> log(Log.WARN, "MTU request failed: $status") })
                .add(enableNotifications(lightCharacteristic!!).fail { _, status -> log(Log.ERROR, "Failed to enable Light1 notifications: $status") })
                .add(enableNotifications(light2Characteristic!!).fail { _, status -> log(Log.ERROR, "Failed to enable Light2 notifications: $status") })
                .add(enableNotifications(tiltCharacteristic!!).fail { _, status -> log(Log.ERROR, "Failed to enable Tilt notifications: $status") })
                .done { log(Log.INFO, "Pillbox sensor notifications enabled.") }
                .enqueue()

            basCharacteristic?.let { batChar ->
                setNotificationCallback(batChar)
                    .with { _, data ->
                        val battery = data.getIntValue(Data.FORMAT_UINT8, 0) ?: 0
                        if (_batteryLevel.value != battery) { _batteryLevel.value = battery }
                    }
                enableNotifications(batChar).enqueue()
            }
        }

        @Deprecated("Required by this version of the library.", ReplaceWith(""))
        override fun onServicesInvalidated() {
            lightCharacteristic = null
            light2Characteristic = null
            tiltCharacteristic = null
            basCharacteristic = null
            disModelCharacteristic = null
            disManufCharacteristic = null
        }
    }

    @Deprecated("Required by this version of the library.", ReplaceWith(""))
    override fun getGattCallback(): BleManagerGattCallback = PillboxGattCallback()

    suspend fun connectToPillbox(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                log(Log.ERROR, "Cannot connect: BLUETOOTH_CONNECT permission not granted on Android 12+.")
                return
            }
        }

        try {
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                log(Log.INFO, "Device is bonded. Removing bond info before connecting...")
                removeBond().suspend()
            }
            connect(device)
                .retry(3, 100)
                .useAutoConnect(false)
                .timeout(20_000)
                .suspend()
        } catch (e: Exception) {
            // ***FIXED***: Updated to use the new 'tag' variable.
            Log.e(tag, "Connection failed for ${device.address}", e)
        }
    }

    override fun release() {
        scope.cancel()
        disconnect().enqueue()
    }

    override suspend fun readDeviceInfo(): Boolean {
        return try {
            coroutineScope {
                launch { disModelCharacteristic?.let { _modelNumber.value = readCharacteristic(it).suspend().getStringValue(0) ?: "N/A" } }
                launch { disManufCharacteristic?.let { _manufacturerName.value = readCharacteristic(it).suspend().getStringValue(0) ?: "N/A" } }
            }
            true
        } catch (_: Exception) {
            // Renamed 'e' to '_' to indicate it's intentionally unused.
            false
        }
    }

    override suspend fun readBattery() {
        try {
            basCharacteristic?.let { _batteryLevel.value = readCharacteristic(it).suspend().getIntValue(Data.FORMAT_UINT8, 0) ?: 0 }
        } catch (e: Exception) {
            // ***FIXED***: Updated to use the new 'tag' variable.
            Log.e(tag, "Failed to read battery", e)
        }
    }
}
