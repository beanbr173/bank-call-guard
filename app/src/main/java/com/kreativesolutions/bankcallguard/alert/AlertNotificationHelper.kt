package com.kreativesolutions.bankcallguard.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kreativesolutions.bankcallguard.OverlayLauncher
import com.kreativesolutions.bankcallguard.R
import com.kreativesolutions.bankcallguard.domain.Assessment
import com.kreativesolutions.bankcallguard.domain.Risk

object AlertNotificationHelper {
    private const val CHANNEL_ID = "scam_alerts"
    private const val NOTIFICATION_ID_BASE = 4100

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyAlert(context: Context, assessment: Assessment, playAlarm: Boolean) {
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        val overlayIntent = OverlayLauncher.createIntent(context, assessment, playAlarm)
        val pendingIntent = PendingIntent.getActivity(
            context,
            assessment.hashCode(),
            overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (assessment.risk) {
            Risk.HIGH -> context.getString(R.string.notification_high_title)
            Risk.CAUTION -> context.getString(R.string.notification_caution_title)
            Risk.NONE -> return
        }
        val body = assessment.userMessage
            ?: assessment.bankName
            ?: context.getString(R.string.incoming_call)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = NOTIFICATION_ID_BASE + (assessment.callerNumber?.hashCode()?.and(0xFF) ?: 0)
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
