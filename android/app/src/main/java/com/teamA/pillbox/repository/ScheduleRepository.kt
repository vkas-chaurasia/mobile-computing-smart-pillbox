package com.teamA.pillbox.repository

import android.content.Context
import com.teamA.pillbox.database.PillboxDatabase
import com.teamA.pillbox.database.mappers.ScheduleMapper
import com.teamA.pillbox.domain.MedicationSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing medication schedules.
 * Provides compartment-aware CRUD operations using Room database.
 */
class ScheduleRepository(context: Context) {
    
    private val database = PillboxDatabase.getDatabase(context)
    private val scheduleDao = database.medicationScheduleDao()

    /**
     * Get all active schedules from all compartments.
     * @return Flow of all active schedules
     */
    fun getAllSchedules(): Flow<List<MedicationSchedule>> {
        return scheduleDao.getAllSchedules()
            .map { entities -> ScheduleMapper.toDomainList(entities) }
    }

    /**
     * Get schedules for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2)
     * @return Flow of schedules for the specified compartment
     */
    fun getSchedulesByCompartment(compartmentNumber: Int): Flow<List<MedicationSchedule>> {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        return scheduleDao.getSchedulesByCompartment(compartmentNumber)
            .map { entities -> ScheduleMapper.toDomainList(entities) }
    }

    /**
     * Get a specific schedule by ID.
     * @param id Schedule ID
     * @return Flow of the schedule, or null if not found
     */
    suspend fun getSchedule(id: String): MedicationSchedule? {
        val entity = scheduleDao.getScheduleById(id)
        return entity?.let { ScheduleMapper.toDomain(it) }
    }

    /**
     * Save a schedule (insert or update).
     * @param schedule Schedule to save
     */
    suspend fun saveSchedule(schedule: MedicationSchedule) {
        val entity = ScheduleMapper.toEntity(schedule)
        scheduleDao.insertSchedule(entity)
    }

    /**
     * Delete a schedule by ID.
     * @param id Schedule ID to delete
     */
    suspend fun deleteSchedule(id: String) {
        scheduleDao.deleteSchedule(id)
    }

    /**
     * Delete all schedules for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2)
     */
    suspend fun deleteSchedulesByCompartment(compartmentNumber: Int) {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        scheduleDao.deleteSchedulesByCompartment(compartmentNumber)
    }

    /**
     * Delete all schedules (reset).
     */
    suspend fun resetAllSchedules() {
        scheduleDao.deleteAllSchedules()
    }
}
