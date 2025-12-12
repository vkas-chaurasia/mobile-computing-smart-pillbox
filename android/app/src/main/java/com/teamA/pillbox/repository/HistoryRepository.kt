package com.teamA.pillbox.repository

import android.content.Context
import com.teamA.pillbox.database.PillboxDatabase
import com.teamA.pillbox.database.mappers.RecordMapper
import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.domain.Statistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for managing consumption records.
 * Provides compartment-aware CRUD operations and statistics using Room database.
 */
class HistoryRepository(context: Context) {
    
    private val database = PillboxDatabase.getDatabase(context)
    private val recordDao = database.consumptionRecordDao()

    /**
     * Get all consumption records from all compartments.
     * @return Flow of all records, sorted by date (descending) and time (descending)
     */
    fun getAllRecords(): Flow<List<ConsumptionRecord>> {
        return recordDao.getAllRecords()
            .map { entities -> RecordMapper.toDomainList(entities) }
    }

    /**
     * Get records for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2)
     * @return Flow of records for the specified compartment
     */
    fun getRecordsByCompartment(compartmentNumber: Int): Flow<List<ConsumptionRecord>> {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        return recordDao.getRecordsByCompartment(compartmentNumber)
            .map { entities -> RecordMapper.toDomainList(entities) }
    }

    /**
     * Get records within a date range.
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return Flow of records within the date range
     */
    fun getRecordsByDateRange(start: LocalDate, end: LocalDate): Flow<List<ConsumptionRecord>> {
        val startEpochDays = start.toEpochDay()
        val endEpochDays = end.toEpochDay()
        return recordDao.getRecordsByDateRange(startEpochDays, endEpochDays)
            .map { entities -> RecordMapper.toDomainList(entities) }
    }

    /**
     * Get records for a specific compartment within a date range.
     * @param compartmentNumber Compartment number (1 or 2)
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return Flow of records for the specified compartment within the date range
     */
    fun getRecordsByCompartmentAndDateRange(
        compartmentNumber: Int,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<ConsumptionRecord>> {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        val startEpochDays = start.toEpochDay()
        val endEpochDays = end.toEpochDay()
        return recordDao.getRecordsByCompartmentAndDateRange(compartmentNumber, startEpochDays, endEpochDays)
            .map { entities -> RecordMapper.toDomainList(entities) }
    }

    /**
     * Get records by status.
     * @param status Consumption status to filter by
     * @return Flow of records with the specified status
     */
    fun getRecordsByStatus(status: ConsumptionStatus): Flow<List<ConsumptionRecord>> {
        return recordDao.getRecordsByStatus(status.name)
            .map { entities -> RecordMapper.toDomainList(entities) }
    }

    /**
     * Get records for a specific compartment by status.
     * @param compartmentNumber Compartment number (1 or 2)
     * @param status Consumption status to filter by
     * @return Flow of records for the specified compartment with the specified status
     */
    fun getRecordsByCompartmentAndStatus(
        compartmentNumber: Int,
        status: ConsumptionStatus
    ): Flow<List<ConsumptionRecord>> {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        return recordDao.getRecordsByCompartmentAndStatus(compartmentNumber, status.name)
            .map { entities -> RecordMapper.toDomainList(entities) }
    }

    /**
     * Get today's record for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2), or null for any compartment
     * @return Flow of today's record, or null if not found
     */
    fun getTodayRecord(compartmentNumber: Int?): Flow<ConsumptionRecord?> {
        val todayEpochDays = LocalDate.now().toEpochDay()
        return if (compartmentNumber != null) {
            require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
            recordDao.getTodayRecord(compartmentNumber, todayEpochDays)
                .map { entity -> entity?.let { RecordMapper.toDomain(it) } }
        } else {
            recordDao.getTodayRecords(todayEpochDays)
                .map { entities -> 
                    if (entities.isEmpty()) null 
                    else RecordMapper.toDomain(entities.first()) 
                }
        }
    }

    /**
     * Create a new consumption record.
     * @param record Record to create
     */
    suspend fun createRecord(record: ConsumptionRecord) {
        val entity = RecordMapper.toEntity(record)
        recordDao.insertRecord(entity)
    }

    /**
     * Update an existing consumption record.
     * @param record Record to update
     */
    suspend fun updateRecord(record: ConsumptionRecord) {
        val entity = RecordMapper.toEntity(record)
        recordDao.updateRecord(entity)
    }

    /**
     * Calculate statistics for a date range.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param compartmentNumber Compartment number (1 or 2), or null for all compartments
     * @return Statistics for the specified period and compartment
     */
    suspend fun getStatistics(
        startDate: LocalDate,
        endDate: LocalDate,
        compartmentNumber: Int?
    ): Statistics {
        val recordsFlow = if (compartmentNumber != null) {
            getRecordsByCompartmentAndDateRange(compartmentNumber, startDate, endDate)
        } else {
            getRecordsByDateRange(startDate, endDate)
        }
        
        val records = recordsFlow.first()
        
        val totalScheduled = records.size
        val totalTaken = records.count { it.status == ConsumptionStatus.TAKEN }
        val totalMissed = records.count { it.status == ConsumptionStatus.MISSED }
        val totalPending = records.count { it.status == ConsumptionStatus.PENDING }
        
        val compliancePercentage = Statistics.calculateCompliance(totalTaken, totalMissed)
        val currentStreak = calculateStreak(records)
        
        return Statistics(
            startDate = startDate,
            endDate = endDate,
            totalScheduled = totalScheduled,
            totalTaken = totalTaken,
            totalMissed = totalMissed,
            totalPending = totalPending,
            compliancePercentage = compliancePercentage,
            currentStreak = currentStreak
        )
    }

    /**
     * Calculate current streak of consecutive days with medication taken.
     * Streak counts backwards from today.
     * @param records List of consumption records
     * @return Current streak (0 if no streak exists)
     */
    private fun calculateStreak(records: List<ConsumptionRecord>): Int {
        val today = LocalDate.now()
        var streak = 0
        var currentDate = today

        // Sort records by date (descending) and group by date
        val sortedRecords = records
            .filter { it.status == ConsumptionStatus.TAKEN }
            .sortedByDescending { it.date }
            .groupBy { it.date }

        // Count consecutive days from today backwards
        while (true) {
            val hasRecord = sortedRecords.containsKey(currentDate) && 
                           sortedRecords[currentDate]?.any { it.status == ConsumptionStatus.TAKEN } == true
            
            if (hasRecord) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Delete all records (reset).
     */
    suspend fun deleteAllRecords() {
        recordDao.deleteAllRecords()
    }
}
