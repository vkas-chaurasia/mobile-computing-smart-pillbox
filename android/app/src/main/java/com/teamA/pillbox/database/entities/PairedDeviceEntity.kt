package com.teamA.pillbox.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for paired BLE devices.
 * Stores previously connected pillbox devices for quick reconnection.
 */
@Entity(tableName = "paired_devices")
data class PairedDeviceEntity(
    /**
     * MAC address of the device (unique identifier).
     */
    @PrimaryKey
    val macAddress: String,
    
    /**
     * Device name (e.g., "pillbox").
     */
    val deviceName: String,
    
    /**
     * Timestamp when this device was first paired.
     */
    val pairedAt: Long,
    
    /**
     * Timestamp when this device was last connected.
     */
    val lastConnectedAt: Long,
    
    /**
     * Number of times this device has been successfully connected.
     */
    val connectionCount: Int = 0
)
