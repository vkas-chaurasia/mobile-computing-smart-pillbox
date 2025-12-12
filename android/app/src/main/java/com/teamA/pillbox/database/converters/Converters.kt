package com.teamA.pillbox.database.converters

import androidx.room.TypeConverter
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.domain.DetectionMethod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Type converters for Room database.
 * Converts between domain types and database storage types.
 */
class Converters {

    // DayOfWeek Set ↔ String
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(daysString: String): Set<DayOfWeek> {
        return if (daysString.isEmpty()) {
            emptySet()
        } else {
            daysString.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet()
        }
    }

    // LocalTime ↔ Long (milliseconds since midnight)
    @TypeConverter
    fun fromLocalTime(time: LocalTime): Long {
        return time.toSecondOfDay().toLong() * 1000
    }

    @TypeConverter
    fun toLocalTime(timeMillis: Long): LocalTime {
        return LocalTime.ofSecondOfDay(timeMillis / 1000)
    }

    // LocalDate ↔ Long (epoch days)
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long {
        return date.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDays: Long): LocalDate {
        return LocalDate.ofEpochDay(epochDays)
    }

    // LocalDateTime ↔ Long (epoch milliseconds)
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? {
        return dateTime?.toEpochSecond(java.time.ZoneOffset.UTC)?.times(1000)
    }

    @TypeConverter
    fun toLocalDateTime(epochMillis: Long?): LocalDateTime? {
        return epochMillis?.let {
            LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC)
        }
    }

    // ConsumptionStatus ↔ String
    @TypeConverter
    fun fromConsumptionStatus(status: ConsumptionStatus): String {
        return status.name
    }

    @TypeConverter
    fun toConsumptionStatus(statusString: String): ConsumptionStatus {
        return ConsumptionStatus.valueOf(statusString)
    }

    // DetectionMethod ↔ String
    @TypeConverter
    fun fromDetectionMethod(method: DetectionMethod?): String? {
        return method?.name
    }

    @TypeConverter
    fun toDetectionMethod(methodString: String?): DetectionMethod? {
        return methodString?.let { DetectionMethod.valueOf(it) }
    }
}
