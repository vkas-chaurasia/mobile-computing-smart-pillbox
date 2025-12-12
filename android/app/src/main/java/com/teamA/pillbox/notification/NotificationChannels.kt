package com.teamA.pillbox.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Manages notification channels for the Pillbox app.
 * Creates channels for medication reminders and missed dose alerts.
 */
object NotificationChannels {

    const val CHANNEL_REMINDER_ID = "medication_reminder"
    const val CHANNEL_MISSED_DOSE_ID = "missed_dose"

    private const val CHANNEL_REMINDER_NAME = "Medication Reminders"
    private const val CHANNEL_REMINDER_DESCRIPTION = "Notifications for scheduled medication reminders"
    private const val CHANNEL_MISSED_DOSE_NAME = "Missed Dose Alerts"
    private const val CHANNEL_MISSED_DOSE_DESCRIPTION = "Notifications for missed medication doses"

    /**
     * Create all notification channels.
     * Should be called when the app starts (e.g., in Application class or MainActivity).
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Reminder channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                CHANNEL_REMINDER_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_REMINDER_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            // Missed dose channel
            val missedDoseChannel = NotificationChannel(
                CHANNEL_MISSED_DOSE_ID,
                CHANNEL_MISSED_DOSE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_MISSED_DOSE_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(missedDoseChannel)
        }
    }

    /**
     * Check if notifications are enabled for a channel.
     */
    fun areNotificationsEnabled(context: Context, channelId: String): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
