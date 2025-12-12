package com.teamA.pillbox.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.teamA.pillbox.database.converters.Converters
import com.teamA.pillbox.database.daos.ConsumptionRecordDao
import com.teamA.pillbox.database.daos.MedicationScheduleDao
import com.teamA.pillbox.database.entities.ConsumptionRecordEntity
import com.teamA.pillbox.database.entities.MedicationScheduleEntity

/**
 * Room database for the Pillbox app.
 * Contains tables for medication schedules and consumption records.
 */
@Database(
    entities = [
        MedicationScheduleEntity::class,
        ConsumptionRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PillboxDatabase : RoomDatabase() {

    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun consumptionRecordDao(): ConsumptionRecordDao

    companion object {
        @Volatile
        private var INSTANCE: PillboxDatabase? = null

        fun getDatabase(context: Context): PillboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PillboxDatabase::class.java,
                    "pillbox_database"
                )
                    .fallbackToDestructiveMigration() // For development - will be updated later
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
