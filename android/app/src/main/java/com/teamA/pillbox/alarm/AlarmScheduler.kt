package com.teamA.pillbox.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.teamA.pillbox.domain.MedicationSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Manages exact-time alarms for medication reminders using AlarmManager.
 * Provides reliable, precise scheduling for medication notifications.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "AlarmScheduler"

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_COMPARTMENT_NUMBER = "compartment_number"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_IS_MISSED_DOSE = "is_missed_dose"
    }

    /**
     * Schedule an alarm for a medication reminder.
     * Creates two alarms:
     * 1. Reminder at scheduled time
     * 2. Missed dose alert 1 hour later
     * 
     * @param schedule The medication schedule
     */
    fun scheduleAlarmsForSchedule(schedule: MedicationSchedule) {
        val today = LocalDate.now()
        
        // Only schedule if today is in the schedule's days
        if (!schedule.daysOfWeek.contains(today.dayOfWeek)) {
            Log.d(TAG, "Skipping alarm for ${schedule.id} - today (${today.dayOfWeek}) not in schedule")
            return
        }

        // Schedule reminder alarm
        scheduleReminderAlarm(schedule, today)
        
        // Schedule missed dose alarm (1 hour after reminder)
        scheduleMissedDoseAlarm(schedule, today)
    }

    /**
     * Schedule the reminder alarm at the scheduled time.
     */
    private fun scheduleReminderAlarm(schedule: MedicationSchedule, date: LocalDate) {
        val scheduledDateTime = LocalDateTime.of(date, schedule.time)
        val triggerTimeMillis = scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        // Only schedule if the time is in the future
        if (triggerTimeMillis <= now) {
            Log.d(TAG, "Skipping reminder alarm for ${schedule.id} - time has passed")
            return
        }

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_COMPARTMENT_NUMBER, schedule.compartmentNumber)
            putExtra(EXTRA_MEDICATION_NAME, schedule.medicationName)
            putExtra(EXTRA_IS_MISSED_DOSE, false)
        }

        val requestCode = schedule.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for precise timing even in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }

        Log.d(TAG, "✅ Reminder alarm scheduled for ${schedule.medicationName} at $scheduledDateTime (request code: $requestCode)")
    }

    /**
     * Schedule the missed dose alarm 1 hour after the scheduled time.
     */
    private fun scheduleMissedDoseAlarm(schedule: MedicationSchedule, date: LocalDate) {
        val scheduledDateTime = LocalDateTime.of(date, schedule.time).plusHours(1)
        val triggerTimeMillis = scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        // Only schedule if the time is in the future
        if (triggerTimeMillis <= now) {
            Log.d(TAG, "Skipping missed dose alarm for ${schedule.id} - time has passed")
            return
        }

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_COMPARTMENT_NUMBER, schedule.compartmentNumber)
            putExtra(EXTRA_MEDICATION_NAME, schedule.medicationName)
            putExtra(EXTRA_IS_MISSED_DOSE, true)
        }

        // Use different request code for missed dose alarm (offset by 10000)
        val requestCode = schedule.id.hashCode() + 10000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for precise timing even in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }

        Log.d(TAG, "✅ Missed dose alarm scheduled for ${schedule.medicationName} at $scheduledDateTime (request code: $requestCode)")
    }

    /**
     * Cancel alarms for a specific schedule.
     * 
     * @param scheduleId The schedule ID
     */
    fun cancelAlarmsForSchedule(scheduleId: String) {
        val requestCode = scheduleId.hashCode()
        
        // Cancel reminder alarm
        val reminderIntent = Intent(context, MedicationAlarmReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            reminderIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        reminderPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "✅ Cancelled reminder alarm for schedule $scheduleId")
        }

        // Cancel missed dose alarm
        val missedDoseIntent = Intent(context, MedicationAlarmReceiver::class.java)
        val missedDosePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode + 10000,
            missedDoseIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        missedDosePendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "✅ Cancelled missed dose alarm for schedule $scheduleId")
        }
    }

    /**
     * Cancel all alarms (useful when resetting schedules).
     */
    fun cancelAllAlarms(scheduleIds: List<String>) {
        scheduleIds.forEach { scheduleId ->
            cancelAlarmsForSchedule(scheduleId)
        }
        Log.d(TAG, "✅ Cancelled all alarms for ${scheduleIds.size} schedules")
    }
}
