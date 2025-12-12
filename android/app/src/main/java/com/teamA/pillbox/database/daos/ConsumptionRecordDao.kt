package com.teamA.pillbox.database.daos

import androidx.room.*
import com.teamA.pillbox.database.entities.ConsumptionRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for consumption records.
 * Provides CRUD operations with compartment support.
 */
@Dao
interface ConsumptionRecordDao {

    /**
     * Get all records.
     */
    @Query("SELECT * FROM consumption_records ORDER BY dateEpochDays DESC, scheduledTimeMillis DESC")
    fun getAllRecords(): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get records by compartment number.
     */
    @Query("SELECT * FROM consumption_records WHERE compartmentNumber = :compartmentNumber ORDER BY dateEpochDays DESC, scheduledTimeMillis DESC")
    fun getRecordsByCompartment(compartmentNumber: Int): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get records by date range.
     */
    @Query("SELECT * FROM consumption_records WHERE dateEpochDays >= :startEpochDays AND dateEpochDays <= :endEpochDays ORDER BY dateEpochDays DESC, scheduledTimeMillis DESC")
    fun getRecordsByDateRange(startEpochDays: Long, endEpochDays: Long): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get records by compartment and date range.
     */
    @Query("SELECT * FROM consumption_records WHERE compartmentNumber = :compartmentNumber AND dateEpochDays >= :startEpochDays AND dateEpochDays <= :endEpochDays ORDER BY dateEpochDays DESC, scheduledTimeMillis DESC")
    fun getRecordsByCompartmentAndDateRange(compartmentNumber: Int, startEpochDays: Long, endEpochDays: Long): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get records by status.
     */
    @Query("SELECT * FROM consumption_records WHERE statusString = :status ORDER BY dateEpochDays DESC, scheduledTimeMillis DESC")
    fun getRecordsByStatus(status: String): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get records by compartment and status.
     */
    @Query("SELECT * FROM consumption_records WHERE compartmentNumber = :compartmentNumber AND statusString = :status ORDER BY dateEpochDays DESC, scheduledTimeMillis DESC")
    fun getRecordsByCompartmentAndStatus(compartmentNumber: Int, status: String): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get today's record for a specific compartment.
     */
    @Query("SELECT * FROM consumption_records WHERE compartmentNumber = :compartmentNumber AND dateEpochDays = :todayEpochDays LIMIT 1")
    fun getTodayRecord(compartmentNumber: Int, todayEpochDays: Long): Flow<ConsumptionRecordEntity?>

    /**
     * Get today's record for any compartment.
     */
    @Query("SELECT * FROM consumption_records WHERE dateEpochDays = :todayEpochDays ORDER BY scheduledTimeMillis DESC")
    fun getTodayRecords(todayEpochDays: Long): Flow<List<ConsumptionRecordEntity>>

    /**
     * Get a specific record by ID.
     */
    @Query("SELECT * FROM consumption_records WHERE id = :id")
    suspend fun getRecordById(id: String): ConsumptionRecordEntity?

    /**
     * Insert a new record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ConsumptionRecordEntity)

    /**
     * Update an existing record.
     */
    @Update
    suspend fun updateRecord(record: ConsumptionRecordEntity)

    /**
     * Delete a record by ID.
     */
    @Query("DELETE FROM consumption_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    /**
     * Delete all records.
     */
    @Query("DELETE FROM consumption_records")
    suspend fun deleteAllRecords()
}
