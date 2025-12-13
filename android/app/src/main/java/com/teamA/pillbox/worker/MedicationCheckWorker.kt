package com.teamA.pillbox.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.domain.DetectionMethod
import com.teamA.pillbox.domain.MedicationSchedule
import com.teamA.pillbox.notification.NotificationService
import com.teamA.pillbox.repository.HistoryRepository
import com.teamA.pillbox.repository.ScheduleRepository
import com.teamA.pillbox.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * WorkManager worker that periodically checks medication schedules.
 * Runs every 15 minutes to:
 * - Trigger reminder notifications at scheduled time
 * - Mark doses as MISSED after 1-hour grace period
 * - Trigger missed dose alerts
 */
class MedicationCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "MedicationCheckWorker"

    private val scheduleRepository = ScheduleRepository(context)
    private val historyRepository = HistoryRepository(context)
    private val settingsRepository = SettingsRepository(context)
    private val notificationService = NotificationService(context)

    override suspend fun doWork(): Result {
        return try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "========================================")
            Log.d(TAG, "Medication check started at ${LocalDateTime.now()}")
            Log.d(TAG, "========================================")

            // Get all active schedules
            val allSchedules = scheduleRepository.getAllSchedules().first()
            val today = LocalDate.now()
            val currentTime = LocalTime.now()
            
            Log.d(TAG, "Current date: $today (${today.dayOfWeek})")
            Log.d(TAG, "Current time: $currentTime")
            Log.d(TAG, "Total schedules found: ${allSchedules.size}")

            // Check each schedule
            var checkCount = 0
            allSchedules.forEach { schedule ->
                Log.d(TAG, "")
                Log.d(TAG, "--- Schedule ${schedule.id} ---")
                Log.d(TAG, "  Compartment: ${schedule.compartmentNumber}")
                Log.d(TAG, "  Medication: ${schedule.medicationName}")
                Log.d(TAG, "  Scheduled time: ${schedule.time}")
                Log.d(TAG, "  Days: ${schedule.daysOfWeek}")
                Log.d(TAG, "  Is active: ${schedule.isActive}")
                Log.d(TAG, "  Today is ${today.dayOfWeek}, in schedule: ${schedule.daysOfWeek.contains(today.dayOfWeek)}")
                
                if (schedule.isActive && schedule.daysOfWeek.contains(today.dayOfWeek)) {
                    checkCount++
                    checkSchedule(schedule, today, currentTime)
                } else {
                    Log.d(TAG, "  ⏭️ Skipped (not active or not today)")
                }
            }

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Medication check completed successfully")
            Log.d(TAG, "Checked $checkCount/${allSchedules.size} schedules")
            Log.d(TAG, "Duration: ${duration}ms")
            Log.d(TAG, "========================================")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in medication check", e)
            Result.retry() // Retry on failure
        }
    }

    /**
     * Check a single schedule and handle reminders/missed doses.
     */
    private suspend fun checkSchedule(
        schedule: MedicationSchedule,
        today: LocalDate,
        currentTime: LocalTime
    ) {
        val scheduledTime = schedule.time
        val oneHourAfter = scheduledTime.plusHours(1)
        
        Log.d(TAG, "  Checking time windows:")
        Log.d(TAG, "    Current time: $currentTime")
        Log.d(TAG, "    Scheduled: $scheduledTime - ${scheduledTime.plusMinutes(15)}")
        Log.d(TAG, "    Missed window: $oneHourAfter - ${oneHourAfter.plusMinutes(15)}")

        // Get today's record for this compartment
        val todayRecord = historyRepository.getTodayRecord(schedule.compartmentNumber).first()
        Log.d(TAG, "  Today's record: ${todayRecord?.status ?: "null"}")

        when {
            // Case 1: Current time is at scheduled time → Show reminder
            currentTime >= scheduledTime && currentTime < scheduledTime.plusMinutes(15) -> {
                Log.d(TAG, "  ⏰ In REMINDER window")
                if (todayRecord == null || todayRecord.status == ConsumptionStatus.PENDING) {
                    // Show reminder notification
                    notificationService.showReminderNotification(schedule)
                    Log.d(TAG, "  ✅ Reminder notification shown for schedule ${schedule.id}")
                } else {
                    Log.d(TAG, "  ⏭️ Reminder skipped - already taken (status: ${todayRecord.status})")
                }
            }

            // Case 2: Current time is 1 hour after scheduled time → Check if missed
            currentTime >= oneHourAfter && currentTime < oneHourAfter.plusMinutes(15) -> {
                Log.d(TAG, "  ⚠️ In MISSED DOSE window")
                if (todayRecord == null || todayRecord.status != ConsumptionStatus.TAKEN) {
                    // Mark as MISSED if not already taken
                    if (todayRecord == null) {
                        // Create MISSED record
                        val missedRecord = ConsumptionRecord(
                            id = UUID.randomUUID().toString(),
                            compartmentNumber = schedule.compartmentNumber,
                            date = today,
                            scheduledTime = scheduledTime,
                            consumedTime = null,
                            status = ConsumptionStatus.MISSED,
                            detectionMethod = null
                        )
                        historyRepository.createRecord(missedRecord)
                        Log.d(TAG, "  ✅ Created MISSED record for schedule ${schedule.id}")
                    } else if (todayRecord.status == ConsumptionStatus.PENDING) {
                        // Update existing PENDING record to MISSED
                        val updatedRecord = todayRecord.copy(
                            status = ConsumptionStatus.MISSED
                        )
                        historyRepository.updateRecord(updatedRecord)
                        Log.d(TAG, "  ✅ Updated record to MISSED for schedule ${schedule.id}")
                    }

                    // Show missed dose notification
                    notificationService.showMissedDoseNotification(schedule)
                    Log.d(TAG, "  ✅ Missed dose notification shown for schedule ${schedule.id}")
                } else {
                    Log.d(TAG, "  ⏭️ Missed dose check skipped - already taken")
                }
            }
            
            else -> {
                Log.d(TAG, "  ⏭️ Outside notification windows")
            }
        }
    }
}
