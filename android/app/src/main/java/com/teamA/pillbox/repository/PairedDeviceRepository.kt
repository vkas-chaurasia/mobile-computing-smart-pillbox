package com.teamA.pillbox.repository

import android.util.Log
import com.teamA.pillbox.database.daos.PairedDeviceDao
import com.teamA.pillbox.database.entities.PairedDeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing paired BLE devices.
 * Handles CRUD operations and business logic for device pairing.
 */
class PairedDeviceRepository(
    private val pairedDeviceDao: PairedDeviceDao
) {
    companion object {
        private const val TAG = "PairedDeviceRepo"
    }

    /**
     * Get all paired devices as a Flow (auto-updates UI).
     */
    val allPairedDevices: Flow<List<PairedDeviceEntity>> = pairedDeviceDao.getAllPairedDevices()

    /**
     * Get all paired devices synchronously.
     */
    suspend fun getAllPairedDevicesSync(): List<PairedDeviceEntity> {
        return pairedDeviceDao.getAllPairedDevicesSync()
    }

    /**
     * Get a specific paired device by MAC address.
     */
    suspend fun getPairedDevice(macAddress: String): PairedDeviceEntity? {
        return pairedDeviceDao.getPairedDeviceByMac(macAddress)
    }

    /**
     * Get the most recently connected device.
     */
    suspend fun getMostRecentDevice(): PairedDeviceEntity? {
        return pairedDeviceDao.getMostRecentDevice()
    }

    /**
     * Add a new paired device or update if already exists.
     * 
     * @param macAddress Device MAC address
     * @param deviceName Device name
     * @return true if device was newly added, false if updated
     */
    suspend fun addOrUpdatePairedDevice(macAddress: String, deviceName: String): Boolean {
        val existingDevice = pairedDeviceDao.getPairedDeviceByMac(macAddress)
        val currentTime = System.currentTimeMillis()

        return if (existingDevice != null) {
            // Update existing device
            val updatedDevice = existingDevice.copy(
                deviceName = deviceName,
                lastConnectedAt = currentTime,
                connectionCount = existingDevice.connectionCount + 1
            )
            pairedDeviceDao.updatePairedDevice(updatedDevice)
            Log.d(TAG, "Updated paired device: $macAddress (${updatedDevice.connectionCount} connections)")
            false
        } else {
            // Add new device
            val newDevice = PairedDeviceEntity(
                macAddress = macAddress,
                deviceName = deviceName,
                pairedAt = currentTime,
                lastConnectedAt = currentTime,
                connectionCount = 1
            )
            pairedDeviceDao.insertPairedDevice(newDevice)
            Log.d(TAG, "Added new paired device: $macAddress")
            true
        }
    }

    /**
     * Update the last connected timestamp for a device.
     */
    suspend fun updateLastConnected(macAddress: String) {
        val currentTime = System.currentTimeMillis()
        pairedDeviceDao.updateLastConnected(macAddress, currentTime)
        Log.d(TAG, "Updated last connected time for: $macAddress")
    }

    /**
     * Remove a paired device.
     */
    suspend fun removePairedDevice(macAddress: String) {
        pairedDeviceDao.deletePairedDevice(macAddress)
        Log.d(TAG, "Removed paired device: $macAddress")
    }

    /**
     * Remove all paired devices.
     */
    suspend fun removeAllPairedDevices() {
        pairedDeviceDao.deleteAllPairedDevices()
        Log.d(TAG, "Removed all paired devices")
    }

    /**
     * Check if a device is already paired.
     */
    suspend fun isDevicePaired(macAddress: String): Boolean {
        return pairedDeviceDao.getPairedDeviceByMac(macAddress) != null
    }
}
