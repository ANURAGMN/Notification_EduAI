package com.anurag.eduai.notificationdemo.pipeline

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Same role as the PDF's BroadcastReceiver: Room never "calculates" anything
 * on its own. All the day-gap (here: minute-gap) math and the decision to
 * notify happens right here, every time an alarm wakes this up — using one
 * simple DAO read, exactly like the PDF describes.
 */
class InactivityReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TIER = "tier"
        const val CHANNEL_ID = "inactivity_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val tier = intent.getIntExtra(EXTRA_TIER, 0)
        if (tier != 5 && tier != 10) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val lastSession = db.sessionDao().getLatestSession()

                if (lastSession != null) {
                    val inactivityMinutes =
                        (System.currentTimeMillis() - lastSession.sessionStartTime) / (1000 * 60)

                    val state = db.notificationStateDao().getState() ?: NotificationStateEntity()
                    val alreadyShown = if (tier == 5) state.shown5min else state.shown10min

                    if (inactivityMinutes >= tier && !alreadyShown) {

                        // 1. Sync & Log Trigger immediately (this handles the network fetch)
                        val notifType = "Inactivity_${tier}min"
                        val syncedAnalytics = AnalyticsTracker.logEvent(
                            context,
                            notifType,
                            AnalyticsTracker.EventType.TRIGGERED
                        )

                        // 2. Show notification using potentially synced custom text
                        showNotification(context, tier, notifType, syncedAnalytics.customMsg)

                        // 3. Update Room state
                        val updated = if (tier == 5) {
                            state.copy(shown5min = true, lastNotifiedAt = System.currentTimeMillis())
                        } else {
                            state.copy(shown10min = true, lastNotifiedAt = System.currentTimeMillis())
                        }
                        db.notificationStateDao().upsertState(updated)
                    }
                }
            } finally {
                pendingResult.finish() // Tells Android the background work is done
            }
        }
    }

    private fun showNotification(context: Context, tier: Int, notifType: String, customMsg: String) {
        createNotificationChannel(context)

        // <-- NEW: Applies customMsg to any tier if it exists
        val text = if (customMsg.isNotEmpty()) {
            customMsg
        } else if (tier == 5) {
            Messages.fiveMinMessages.random()
        } else {
            Messages.tenMinMessages.random()
        }

        val notifId = 2000 + tier

        val goToAppIntent = Intent(context, com.anurag.eduai.notificationdemo.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("analytics_type", notifType)
            putExtra("analytics_action", "go_to_app")
            putExtra("cancel_notif_id", notifId)
            putExtra("open_tab", "pipeline")
        }
        val goToAppPendingIntent = PendingIntent.getActivity(
            context, 3000 + tier, goToAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, PipelineNotificationActionReceiver::class.java).apply {
            action = PipelineNotificationActionReceiver.ACTION_CANCEL
            putExtra(PipelineNotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra("analytics_type", notifType)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 4000 + tier, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$tier minutes of inactivity")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Go to app", goToAppPendingIntent)
            .addAction(0, "Cancel", cancelPendingIntent)

        val canPost = android.os.Build.VERSION.SDK_INT < 33 ||
                androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        if (canPost) {
            androidx.core.app.NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Inactivity Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
