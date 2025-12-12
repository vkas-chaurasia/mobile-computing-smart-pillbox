package com.teamA.pillbox.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.teamA.pillbox.worker.MedicationCheckWorker
import java.util.concurrent.TimeUnit

/**
 * Service for scheduling periodic medication checks.
 * Uses WorkManager to check schedules every 15 minutes.
 */
class AlertSchedulerService(private val context: Context) {

    private val TAG = "AlertSchedulerService"
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val WORK_NAME = "medication_check_work"
        private const val REPEAT_INTERVAL_MINUTES = 15L
    }

    /**
     * Start periodic medication checks.
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
     * Check if periodic checks are currently scheduled.
     */
    fun isScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfos.any { it.state.isFinished.not() }
    }
}
