package com.teamA.pillbox.database.mappers

import com.teamA.pillbox.database.entities.MedicationScheduleEntity
import com.teamA.pillbox.domain.MedicationSchedule
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Mapper functions to convert between MedicationScheduleEntity and MedicationSchedule.
 */
object ScheduleMapper {

    /**
     * Convert entity to domain model.
     */
    fun toDomain(entity: MedicationScheduleEntity): MedicationSchedule {
        return MedicationSchedule(
            id = entity.id,
            compartmentNumber = entity.compartmentNumber,
            medicationName = entity.medicationName,
            daysOfWeek = entity.daysOfWeekString.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet(),
            time = LocalTime.ofSecondOfDay(entity.timeMillis / 1000),
            pillCount = entity.pillCount,
            isActive = entity.isActive,
            createdAt = entity.createdAt
        )
    }

    /**
     * Convert domain model to entity.
     */
    fun toEntity(domain: MedicationSchedule): MedicationScheduleEntity {
        return MedicationScheduleEntity(
            id = domain.id,
            compartmentNumber = domain.compartmentNumber,
            medicationName = domain.medicationName,
            daysOfWeekString = domain.daysOfWeek.joinToString(",") { it.name },
            timeMillis = domain.time.toSecondOfDay().toLong() * 1000,
            pillCount = domain.pillCount,
            isActive = domain.isActive,
            createdAt = domain.createdAt
        )
    }

    /**
     * Convert list of entities to domain models.
     */
    fun toDomainList(entities: List<MedicationScheduleEntity>): List<MedicationSchedule> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities.
     */
    fun toEntityList(domains: List<MedicationSchedule>): List<MedicationScheduleEntity> {
        return domains.map { toEntity(it) }
    }
}
