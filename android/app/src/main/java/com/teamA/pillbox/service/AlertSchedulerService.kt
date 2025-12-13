package com.teamA.pillbox.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.teamA.pillbox.alarm.AlarmScheduler
import com.teamA.pillbox.domain.MedicationSchedule
import com.teamA.pillbox.worker.MedicationCheckWorker
import java.util.concurrent.TimeUnit

/**
 * Service for scheduling medication alerts.
 * Uses dual strategy:
 * - AlarmManager for exact-time notifications (primary)
 * - WorkManager for backup checks and missed dose detection (secondary)
 */
class AlertSchedulerService(private val context: Context) {

    private val TAG = "AlertSchedulerService"
    private val workManager = WorkManager.getInstance(context)
    private val alarmScheduler = AlarmScheduler(context)

    companion object {
        private const val WORK_NAME = "medication_check_work"
        private const val REPEAT_INTERVAL_MINUTES = 15L
    }

    /**
     * Start periodic medication checks using WorkManager (backup system).
     * Schedules a WorkManager task to run every 15 minutes.
     */
    fun startPeriodicChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
            .setRequiresBatteryNotLow(false) // Can run even on low battery
            .setRequiresCharging(false) // Can run without charging
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MedicationCheckWorker>(
            REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            workRequest
        )

        Log.d(TAG, "Periodic medication checks started (every $REPEAT_INTERVAL_MINUTES minutes)")
    }

    /**
     * Stop periodic medication checks.
     */
    fun stopPeriodicChecks() {
        workManager.cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "Periodic medication checks stopped")
    }

    /**
     * Schedule exact-time alarms for a medication schedule.
     * Uses AlarmManager for precise timing.
     * 
     * @param schedule The medication schedule to set alarms for
     */
    fun scheduleAlarmsForSchedule(schedule: MedicationSchedule) {
        alarmScheduler.scheduleAlarmsForSchedule(schedule)
        Log.d(TAG, "Alarms scheduled for schedule ${schedule.id}")
    }

    /**
     * Cancel alarms for a specific schedule.
     * 
     * @param scheduleId The schedule ID
     */
    fun cancelAlarmsForSchedule(scheduleId: String) {
        alarmScheduler.cancelAlarmsForSchedule(scheduleId)
        Log.d(TAG, "Alarms cancelled for schedule $scheduleId")
    }

    /**
     * Cancel all alarms for multiple schedules.
     * 
     * @param scheduleIds List of schedule IDs
     */
    fun cancelAllAlarms(scheduleIds: List<String>) {
        alarmScheduler.cancelAllAlarms(scheduleIds)
        Log.d(TAG, "All alarms cancelled for ${scheduleIds.size} schedules")
    }

    /**
     * Check if periodic checks are currently scheduled.
     */
    fun isScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfos.any { it.state.isFinished.not() }
    }
}
