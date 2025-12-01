package com.teamA.pillbox.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.teamA.pillbox.PillboxSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PillboxScanner(private val context: Context) {

    private val TAG = "PillboxScanner"
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private val _scannedDevices = MutableStateFlow<List<ScanResult>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (_scannedDevices.value.none { it.device.address == result.device.address }) {
                _scannedDevices.value = _scannedDevices.value + result
                val deviceName = result.device.name ?: result.scanRecord?.deviceName ?: "Unknown BLE Device"
                Log.d(TAG, "SUCCESS: Found a potential Pillbox device: $deviceName (${result.device.address})")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasRequiredScanPermission()) {
            Log.e(TAG, "startScan called without required permissions.")
            return
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Cannot start scan, Bluetooth is likely off or not supported.")
            return
        }

        if (_isScanning.value) {
            return
        }

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(PillboxSpec.PILLBOX_SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _scannedDevices.value = emptyList()

        handler.postDelayed({
            if (_isScanning.value) {
                stopScan()
            }
        }, SCAN_TIMEOUT_MS)

        bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, leScanCallback)
        _isScanning.value = true
        Log.d(TAG, "Scan started. Searching for devices with service UUID: ${PillboxSpec.PILLBOX_SERVICE_UUID}")
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasRequiredScanPermission()) return

        if (_isScanning.value) {
            bluetoothLeScanner?.stopScan(leScanCallback)
            _isScanning.value = false
            Log.d(TAG, "Scan stopped.")
        }

        handler.removeCallbacksAndMessages(null)
    }

    private fun hasRequiredScanPermission(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
