package com.teamA.pillbox.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.teamA.pillbox.MainActivity
import com.teamA.pillbox.domain.MedicationSchedule
import java.util.UUID

/**
 * Service for displaying medication-related notifications.
 * Handles reminder notifications and missed dose alerts.
 */
class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Show a medication reminder notification at scheduled time.
     * 
     * @param schedule The medication schedule for which to show the reminder
     */
    fun showReminderNotification(schedule: MedicationSchedule) {
        val notificationId = schedule.id.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "⏰ Time to take your medication"
        val text = "${schedule.medicationName} - Slot ${schedule.compartmentNumber}"

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDER_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's time to take your medication from Slot ${schedule.compartmentNumber}.\n\nMedication: ${schedule.medicationName}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Keep notification visible until manually dismissed
            .setOngoing(true) // Make it persistent - user must swipe to dismiss
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Custom vibration pattern
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Show a missed dose alert notification.
     * Triggered 1 hour after scheduled time if medication not consumed.
     * 
     * @param schedule The medication schedule for which the dose was missed
     */
    fun showMissedDoseNotification(schedule: MedicationSchedule) {
        val notificationId = schedule.id.hashCode() + 10000 // Different ID to avoid conflicts

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "⚠️ Missed Medication Dose"
        val text = "${schedule.medicationName} - Slot ${schedule.compartmentNumber}"

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MISSED_DOSE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You missed your scheduled medication dose from Slot ${schedule.compartmentNumber}.\n\nMedication: ${schedule.medicationName}\nScheduled time: ${schedule.time}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Keep notification visible until manually dismissed
            .setOngoing(true) // Make it persistent - user must swipe to dismiss
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500)) // Longer vibration for missed dose
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancel a notification by ID.
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications.
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
