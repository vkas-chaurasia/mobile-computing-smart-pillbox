package com.teamA.pillbox.database.mappers

import com.teamA.pillbox.database.entities.ConsumptionRecordEntity
import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.domain.DetectionMethod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Mapper functions to convert between ConsumptionRecordEntity and ConsumptionRecord.
 */
object RecordMapper {

    /**
     * Convert entity to domain model.
     */
    fun toDomain(entity: ConsumptionRecordEntity): ConsumptionRecord {
        return ConsumptionRecord(
            id = entity.id,
            compartmentNumber = entity.compartmentNumber,
            date = LocalDate.ofEpochDay(entity.dateEpochDays),
            scheduledTime = LocalTime.ofSecondOfDay(entity.scheduledTimeMillis / 1000),
            consumedTime = entity.consumedTimeMillis?.let {
                LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC)
            },
            status = ConsumptionStatus.valueOf(entity.statusString),
            detectionMethod = entity.detectionMethodString?.let { DetectionMethod.valueOf(it) }
        )
    }

    /**
     * Convert domain model to entity.
     */
    fun toEntity(domain: ConsumptionRecord): ConsumptionRecordEntity {
        return ConsumptionRecordEntity(
            id = domain.id,
            compartmentNumber = domain.compartmentNumber,
            dateEpochDays = domain.date.toEpochDay(),
            scheduledTimeMillis = domain.scheduledTime.toSecondOfDay().toLong() * 1000,
            consumedTimeMillis = domain.consumedTime?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000),
            statusString = domain.status.name,
            detectionMethodString = domain.detectionMethod?.name
        )
    }

    /**
     * Convert list of entities to domain models.
     */
    fun toDomainList(entities: List<ConsumptionRecordEntity>): List<ConsumptionRecord> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities.
     */
    fun toEntityList(domains: List<ConsumptionRecord>): List<ConsumptionRecordEntity> {
        return domains.map { toEntity(it) }
    }
}
