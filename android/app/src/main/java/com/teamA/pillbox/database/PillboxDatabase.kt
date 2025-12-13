package com.teamA.pillbox.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.teamA.pillbox.database.converters.Converters
import com.teamA.pillbox.database.daos.ConsumptionRecordDao
import com.teamA.pillbox.database.daos.MedicationScheduleDao
import com.teamA.pillbox.database.daos.PairedDeviceDao
import com.teamA.pillbox.database.entities.ConsumptionRecordEntity
import com.teamA.pillbox.database.entities.MedicationScheduleEntity
import com.teamA.pillbox.database.entities.PairedDeviceEntity

/**
 * Room database for the Pillbox app.
 * Contains tables for medication schedules, consumption records, and paired devices.
 */
@Database(
    entities = [
        MedicationScheduleEntity::class,
        ConsumptionRecordEntity::class,
        PairedDeviceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PillboxDatabase : RoomDatabase() {

    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun consumptionRecordDao(): ConsumptionRecordDao
    abstract fun pairedDeviceDao(): PairedDeviceDao

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
