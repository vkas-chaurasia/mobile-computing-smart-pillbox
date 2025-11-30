package com.teamA.pillbox

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamA.pillbox.ble.PillboxScanner
import com.teamA.pillbox.repository.PillboxRepository
import com.teamA.pillbox.ui.PillboxControlScreen
import com.teamA.pillbox.ui.PillboxScannerScreen
import com.teamA.pillbox.ui.PillboxTheme
import com.teamA.pillbox.viewmodel.PillboxViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val application = this.application
            val context = LocalContext.current

            val pillboxRepository = remember { PillboxRepository(context) }
            val pillboxScanner = remember { PillboxScanner(context) }
            val viewModelFactory = remember { PillboxViewModel.Factory(application, pillboxRepository, pillboxScanner) }
            val viewModel: PillboxViewModel = viewModel(factory = viewModelFactory)

            val permissionHelper = remember { BlePermissionHelper(context) }

            PillboxTheme {
                PillboxApp(
                    viewModel = viewModel,
                    permissionHelper = permissionHelper
                )
            }
        }
    }
}

class BlePermissionHelper(private val context: Context) {

    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and higher
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // For Android 11 and lower
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled ?: false
    }

    @SuppressLint("MissingPermission")
    fun requestEnableBluetooth(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BlePermissionHelper", "Cannot programmatically request BT enable without BLUETOOTH_CONNECT.")
                return
            }
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivity(enableBtIntent)
    }
}


@Composable
fun PillboxApp(viewModel: PillboxViewModel, permissionHelper: BlePermissionHelper) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel, permissionHelper) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkBluetoothAndPermissions(permissionHelper)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        viewModel.onPermissionsResult(allGranted, permissionHelper)
    }

    val onScanClicked = {
        if (permissionHelper.hasRequiredPermissions()) {
            viewModel.startScan(permissionHelper)
        } else {
            permissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())
        }
    }

    when (val state = uiState) {
        is PillboxViewModel.UiState.BluetoothDisabled -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Bluetooth is Disabled", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("This app requires Bluetooth to be enabled to scan for the Pillbox.", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    (context as? ComponentActivity)?.let {
                        permissionHelper.requestEnableBluetooth(it)
                    }
                }) {
                    Text("Enable Bluetooth")
                }
            }
        }
        is PillboxViewModel.UiState.Connected -> {
            PillboxControlScreen(
                viewModel = viewModel,
                deviceName = state.deviceName ?: "Pillbox"
            )
        }
        else -> { // Handles Idle and Scanning states
            PillboxScannerScreen(
                uiState = state,
                onScanClicked = onScanClicked,
                onDeviceSelected = { device, name -> viewModel.onDeviceSelected(device, name) }
            )
        }
    }
}
