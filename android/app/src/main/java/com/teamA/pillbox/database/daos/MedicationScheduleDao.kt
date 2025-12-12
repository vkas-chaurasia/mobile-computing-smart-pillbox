package com.teamA.pillbox.database.daos

import androidx.room.*
import com.teamA.pillbox.database.entities.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for medication schedules.
 * Provides CRUD operations with compartment support.
 */
@Dao
interface MedicationScheduleDao {

    /**
     * Get all schedules.
     */
    @Query("SELECT * FROM medication_schedules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllSchedules(): Flow<List<MedicationScheduleEntity>>

    /**
     * Get schedules by compartment number.
     */
    @Query("SELECT * FROM medication_schedules WHERE compartmentNumber = :compartmentNumber AND isActive = 1 ORDER BY createdAt DESC")
    fun getSchedulesByCompartment(compartmentNumber: Int): Flow<List<MedicationScheduleEntity>>

    /**
     * Get a specific schedule by ID.
     */
    @Query("SELECT * FROM medication_schedules WHERE id = :id")
    suspend fun getScheduleById(id: String): MedicationScheduleEntity?

    /**
     * Get all schedules (including inactive).
     */
    @Query("SELECT * FROM medication_schedules ORDER BY createdAt DESC")
    suspend fun getAllSchedulesSync(): List<MedicationScheduleEntity>

    /**
     * Insert a new schedule.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicationScheduleEntity)

    /**
     * Update an existing schedule.
     */
    @Update
    suspend fun updateSchedule(schedule: MedicationScheduleEntity)

    /**
     * Delete a schedule by ID.
     */
    @Query("DELETE FROM medication_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: String)

    /**
     * Delete all schedules for a specific compartment.
     */
    @Query("DELETE FROM medication_schedules WHERE compartmentNumber = :compartmentNumber")
    suspend fun deleteSchedulesByCompartment(compartmentNumber: Int)

    /**
     * Delete all schedules.
     */
    @Query("DELETE FROM medication_schedules")
    suspend fun deleteAllSchedules()
}
