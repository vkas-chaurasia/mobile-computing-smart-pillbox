package com.teamA.pillbox.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.notification.NotificationService
import com.teamA.pillbox.repository.HistoryRepository
import com.teamA.pillbox.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * BroadcastReceiver that handles medication alarm triggers.
 * Triggered by AlarmManager at the exact scheduled time.
 */
class MedicationAlarmReceiver : BroadcastReceiver() {

    private val TAG = "MedicationAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
        val compartmentNumber = intent.getIntExtra(AlarmScheduler.EXTRA_COMPARTMENT_NUMBER, 0)
        val medicationName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medication"
        val isMissedDose = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_MISSED_DOSE, false)

        Log.d(TAG, "========================================")
        Log.d(TAG, "⏰ Alarm triggered!")
        Log.d(TAG, "Schedule ID: $scheduleId")
        Log.d(TAG, "Compartment: $compartmentNumber")
        Log.d(TAG, "Medication: $medicationName")
        Log.d(TAG, "Is Missed Dose: $isMissedDose")
        Log.d(TAG, "========================================")

        val notificationService = NotificationService(context)
        val scheduleRepository = ScheduleRepository(context)
        val historyRepository = HistoryRepository(context)

        // Use goAsync() to allow async operations in BroadcastReceiver
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the schedule
                val schedule = scheduleRepository.getSchedule(scheduleId)
                
                if (schedule == null) {
                    Log.e(TAG, "❌ Schedule not found: $scheduleId")
                    pendingResult.finish()
                    return@launch
                }

                // Get today's record for this compartment
                val todayRecord = historyRepository.getTodayRecord(compartmentNumber).first()
                Log.d(TAG, "Today's record status: ${todayRecord?.status ?: "null"}")

                if (isMissedDose) {
                    // Handle missed dose alarm
                    handleMissedDoseAlarm(schedule, todayRecord, historyRepository, notificationService)
                } else {
                    // Handle reminder alarm
                    handleReminderAlarm(schedule, todayRecord, notificationService)
                }

                pendingResult.finish()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling alarm", e)
                pendingResult.finish()
            }
        }
    }

    /**
     * Handle reminder alarm - show notification if medication not yet taken.
     */
    private suspend fun handleReminderAlarm(
        schedule: com.teamA.pillbox.domain.MedicationSchedule,
        todayRecord: ConsumptionRecord?,
        notificationService: NotificationService
    ) {
        if (todayRecord == null || todayRecord.status == ConsumptionStatus.PENDING) {
            Log.d(TAG, "✅ Showing reminder notification")
            notificationService.showReminderNotification(schedule)
        } else {
            Log.d(TAG, "⏭️ Reminder skipped - already taken (status: ${todayRecord.status})")
        }
    }

    /**
     * Handle missed dose alarm - create MISSED record and show alert.
     */
    private suspend fun handleMissedDoseAlarm(
        schedule: com.teamA.pillbox.domain.MedicationSchedule,
        todayRecord: ConsumptionRecord?,
        historyRepository: HistoryRepository,
        notificationService: NotificationService
    ) {
        if (todayRecord == null || todayRecord.status != ConsumptionStatus.TAKEN) {
            // Mark as MISSED if not already taken
            if (todayRecord == null) {
                // Create MISSED record
                val missedRecord = ConsumptionRecord(
                    id = UUID.randomUUID().toString(),
                    compartmentNumber = schedule.compartmentNumber,
                    date = LocalDate.now(),
                    scheduledTime = schedule.time,
                    consumedTime = null,
                    status = ConsumptionStatus.MISSED,
                    detectionMethod = null
                )
                historyRepository.createRecord(missedRecord)
                Log.d(TAG, "✅ Created MISSED record")
            } else if (todayRecord.status == ConsumptionStatus.PENDING) {
                // Update existing PENDING record to MISSED
                val updatedRecord = todayRecord.copy(
                    status = ConsumptionStatus.MISSED
                )
                historyRepository.updateRecord(updatedRecord)
                Log.d(TAG, "✅ Updated record to MISSED")
            }

            // Show missed dose notification
            Log.d(TAG, "✅ Showing missed dose notification")
            notificationService.showMissedDoseNotification(schedule)
        } else {
            Log.d(TAG, "⏭️ Missed dose alarm skipped - already taken")
        }
    }
}
