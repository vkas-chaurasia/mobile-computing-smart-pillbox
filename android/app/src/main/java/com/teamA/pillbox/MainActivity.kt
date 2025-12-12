package com.teamA.pillbox

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamA.pillbox.ble.PillboxScanner
import com.teamA.pillbox.database.PillboxDatabase
import com.teamA.pillbox.navigation.PillboxNavGraph
import com.teamA.pillbox.repository.PairedDeviceRepository
import com.teamA.pillbox.notification.NotificationChannels
import com.teamA.pillbox.repository.PillboxRepository
import com.teamA.pillbox.service.AlertSchedulerService
import com.teamA.pillbox.ui.PillboxTheme
import com.teamA.pillbox.viewmodel.PillboxViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize notification channels
        NotificationChannels.createChannels(this)

        // Start periodic medication checks
        val alertScheduler = AlertSchedulerService(this)
        alertScheduler.startPeriodicChecks()

        setContent {
            val application = this.application
            val pillboxRepository = remember { PillboxRepository(application) }
            val pillboxScanner = remember { PillboxScanner(application) }
            
            // Initialize database and paired device repository
            val database = remember { PillboxDatabase.getDatabase(application) }
            val pairedDeviceRepository = remember { PairedDeviceRepository(database.pairedDeviceDao()) }
            
            val viewModelFactory = remember { 
                PillboxViewModel.Factory(
                    application, 
                    pillboxRepository, 
                    pillboxScanner,
                    pairedDeviceRepository = pairedDeviceRepository
                ) 
            }
            val viewModel: PillboxViewModel = viewModel(factory = viewModelFactory)
            val permissionHelper = remember { BlePermissionHelper(this) }

            PillboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PillboxNavGraph(
                        viewModel = viewModel,
                        permissionHelper = permissionHelper
                    )
                }
            }
        }
    }
}

class BlePermissionHelper(private val context: Context) {
    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ needs BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            // Also need location permission for BLE scanning to work
            listOf(
                Manifest.permission.BLUETOOTH_SCAN, 
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Android 11 and below needs location permission
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        Log.d("BlePermissionHelper", "Checking permissions:")
        getRequiredPermissions().forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            Log.d("BlePermissionHelper", "  $permission: ${if (granted) "GRANTED" else "DENIED"}")
        }
        
        val allGranted = getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        return allGranted
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        Log.d("BlePermissionHelper", "Location services: GPS=$gpsEnabled, Network=$networkEnabled")
        return gpsEnabled || networkEnabled
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled ?: false
    }

    fun requestEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BlePermissionHelper", "Cannot request BT enable without BLUETOOTH_CONNECT permission.")
            return
        }

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        (context as? ComponentActivity)?.startActivity(enableBtIntent)
    }
}


// Note: PillboxApp logic has been moved to PillboxNavGraph in navigation package
