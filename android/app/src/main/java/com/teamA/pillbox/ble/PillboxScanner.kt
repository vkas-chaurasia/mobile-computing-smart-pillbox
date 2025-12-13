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
        private const val DEBUG_MODE = true // Enable extra debugging
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            Log.d(TAG, "⚡ onScanResult called! callbackType=$callbackType")

            val deviceName = result.device.name ?: result.scanRecord?.deviceName ?: "Unknown"
            val deviceAddress = result.device.address
            val rssi = result.rssi
            
            // Log all scan results for debugging
            Log.d(TAG, "📱 Device found: name='$deviceName', address=$deviceAddress, RSSI=$rssi")
            
            // Check if this is the expected Arduino
            if (deviceAddress == "C1:7B:28:BA:4C:C9") {
                Log.d(TAG, "🎯 FOUND ARDUINO DEVICE! Address matches!")
            }
            
            // Check if device advertises the pillbox service UUID
            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids
            if (serviceUuids != null && serviceUuids.isNotEmpty()) {
                Log.d(TAG, "  📡 Service UUIDs: ${serviceUuids.joinToString()}")
            } else {
                Log.d(TAG, "  ⚠️ No service UUIDs advertised")
            }
            
            // Log advertised data
            val advData = scanRecord?.bytes
            if (advData != null) {
                Log.d(TAG, "  📊 Advertisement data size: ${advData.size} bytes")
            }
            
            // Add device if not already in list
            if (_scannedDevices.value.none { it.device.address == deviceAddress }) {
                _scannedDevices.value = _scannedDevices.value + result
                Log.d(TAG, "✅ SUCCESS: Added device to list: $deviceName ($deviceAddress)")
                Log.d(TAG, "   Total devices found: ${_scannedDevices.value.size}")
            } else {
                Log.d(TAG, "  ℹ️ Device already in list (updated)")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error: $errorCode"
            }
            Log.e(TAG, "Scan failed: $errorMessage (code: $errorCode)")
            _isScanning.value = false
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                if (_scannedDevices.value.none { it.device.address == result.device.address }) {
                    _scannedDevices.value = _scannedDevices.value + result
                    val deviceName = result.device.name ?: result.scanRecord?.deviceName ?: "Unknown BLE Device"
                    Log.d(TAG, "Batch scan result: $deviceName (${result.device.address})")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(useFilter: Boolean = true) {
        if (!hasRequiredScanPermission()) {
            Log.e(TAG, "startScan called without required permissions.")
            return
        }

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is null. Bluetooth may not be supported on this device.")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled.")
            return
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Cannot start scan, Bluetooth LE scanner is null.")
            return
        }

        if (_isScanning.value) {
            Log.w(TAG, "Scan already in progress, ignoring startScan request.")
            return
        }

        // SIMPLIFIED SCAN SETTINGS - Basic approach that works on all devices
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _scannedDevices.value = emptyList()
        
        Log.d(TAG, "🔧 Using SIMPLIFIED scan settings: mode=LOW_LATENCY only")
        Log.d(TAG, "BluetoothLeScanner instance: ${bluetoothLeScanner.hashCode()}")
        Log.d(TAG, "Callback instance: ${leScanCallback.hashCode()}")

        handler.postDelayed({
            if (_isScanning.value) {
                Log.d(TAG, "Scan timeout reached (${SCAN_TIMEOUT_MS}ms). Stopping scan.")
                stopScan()
            }
        }, SCAN_TIMEOUT_MS)

        // NO FILTER - Scan ALL BLE devices
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "🚀 Starting scan with NULL filter list...")
            
            // Try the most basic scan possible - null filters, basic settings
            bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            
            Log.d(TAG, "✅ startScan() called successfully")
            Log.d(TAG, "Expected Arduino MAC: C1:7B:28:BA:4C:C9")
            Log.d(TAG, "Waiting for onScanResult callbacks...")
            Log.d(TAG, "========================================")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start scan: ${e.message}", e)
            e.printStackTrace()
            _isScanning.value = false
            return
        }
        
        _isScanning.value = true
        
        // Verify scan started
        Log.d(TAG, "✓ Scan flag set to: ${_isScanning.value}")
        Log.d(TAG, "✓ Bluetooth adapter enabled: ${bluetoothAdapter.isEnabled}")
        
        // Schedule checks
        if (DEBUG_MODE) {
            handler.postDelayed({
                if (_isScanning.value) {
                    Log.d(TAG, "🔍 2s check: Devices=${_scannedDevices.value.size}, Scanning=${_isScanning.value}")
                    if (_scannedDevices.value.isEmpty()) {
                        Log.w(TAG, "   ⚠️ NO CALLBACK FIRED YET!")
                    }
                }
            }, 2000)
            
            handler.postDelayed({
                if (_isScanning.value) {
                    Log.d(TAG, "🔍 5s check: Devices=${_scannedDevices.value.size}, Scanning=${_isScanning.value}")
                }
            }, 5000)
            
            handler.postDelayed({
                if (_isScanning.value) {
                    Log.d(TAG, "🔍 10s check: Devices=${_scannedDevices.value.size}, Scanning=${_isScanning.value}")
                }
            }, 10000)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasRequiredScanPermission()) return

        if (_isScanning.value) {
            try {
                bluetoothLeScanner?.stopScan(leScanCallback)
                _isScanning.value = false
                Log.d(TAG, "Scan stopped. Total devices found: ${_scannedDevices.value.size}")
                if (_scannedDevices.value.isEmpty()) {
                    Log.w(TAG, "⚠️ WARNING: No devices found during scan!")
                    Log.w(TAG, "   Possible causes:")
                    Log.w(TAG, "   1. Arduino is not powered on or not advertising")
                    Log.w(TAG, "   2. Arduino is out of range")
                    Log.w(TAG, "   3. Other BLE interference")
                    Log.w(TAG, "   4. Phone's BLE stack issue (try restarting phone)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}", e)
            }
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
