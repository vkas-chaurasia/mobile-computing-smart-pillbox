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
            val pillboxRepository = remember { PillboxRepository(application) }
            val pillboxScanner = remember { PillboxScanner(application) }
            val viewModelFactory = remember { PillboxViewModel.Factory(application, pillboxRepository, pillboxScanner) }
            val viewModel: PillboxViewModel = viewModel(factory = viewModelFactory)
            val permissionHelper = remember { BlePermissionHelper(this) }

            PillboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PillboxApp(
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
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
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


@Composable
fun PillboxApp(viewModel: PillboxViewModel, permissionHelper: BlePermissionHelper) {
    val uiState by viewModel.uiState.collectAsState()
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
        viewModel.onPermissionsResult(permissions.all { it.value }, permissionHelper)
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
            BluetoothDisabledScreen(onRequestEnable = { permissionHelper.requestEnableBluetooth() })
        }
        is PillboxViewModel.UiState.Connected -> {
            PillboxControlScreen(
                viewModel = viewModel,
                deviceName = state.deviceName ?: "Pillbox"
            )
        }
        else -> {
            PillboxScannerScreen(
                uiState = state,
                onScanClicked = onScanClicked,
                onDeviceSelected = { device, name -> viewModel.onDeviceSelected(device, name) }
            )
        }
    }
}

@Composable
fun BluetoothDisabledScreen(onRequestEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bluetooth is Disabled", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("This app requires Bluetooth to be enabled to scan for the Pillbox.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestEnable) {
            Text("Enable Bluetooth")
        }
    }
}
