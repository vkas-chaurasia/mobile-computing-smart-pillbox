package com.teamA.pillbox.database.daos

import androidx.room.*
import com.teamA.pillbox.database.entities.PairedDeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for paired devices.
 * Provides CRUD operations for managing paired BLE devices.
 */
@Dao
interface PairedDeviceDao {

    /**
     * Get all paired devices, ordered by most recently connected first.
     */
    @Query("SELECT * FROM paired_devices ORDER BY lastConnectedAt DESC")
    fun getAllPairedDevices(): Flow<List<PairedDeviceEntity>>

    /**
     * Get all paired devices synchronously (for one-time reads).
     */
    @Query("SELECT * FROM paired_devices ORDER BY lastConnectedAt DESC")
    suspend fun getAllPairedDevicesSync(): List<PairedDeviceEntity>

    /**
     * Get a specific paired device by MAC address.
     */
    @Query("SELECT * FROM paired_devices WHERE macAddress = :macAddress")
    suspend fun getPairedDeviceByMac(macAddress: String): PairedDeviceEntity?

    /**
     * Get the most recently connected device.
     */
    @Query("SELECT * FROM paired_devices ORDER BY lastConnectedAt DESC LIMIT 1")
    suspend fun getMostRecentDevice(): PairedDeviceEntity?

    /**
     * Insert or replace a paired device.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairedDevice(device: PairedDeviceEntity)

    /**
     * Update an existing paired device.
     */
    @Update
    suspend fun updatePairedDevice(device: PairedDeviceEntity)

    /**
     * Delete a specific paired device by MAC address.
     */
    @Query("DELETE FROM paired_devices WHERE macAddress = :macAddress")
    suspend fun deletePairedDevice(macAddress: String)

    /**
     * Delete all paired devices.
     */
    @Query("DELETE FROM paired_devices")
    suspend fun deleteAllPairedDevices()

    /**
     * Update the last connected timestamp and increment connection count.
     */
    @Query("UPDATE paired_devices SET lastConnectedAt = :timestamp, connectionCount = connectionCount + 1 WHERE macAddress = :macAddress")
    suspend fun updateLastConnected(macAddress: String, timestamp: Long)
}
