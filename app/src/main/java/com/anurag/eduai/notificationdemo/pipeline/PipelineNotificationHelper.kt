package com.anurag.eduai.notificationdemo.pipeline

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anurag.eduai.notificationdemo.MainActivity

object PipelineNotificationHelper {

    const val CHANNEL_ID = "pipeline_alerts_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun show(
        context: Context,
        notifId: Int,
        title: String,
        text: String,
        goToAppRequestCode: Int,
        cancelRequestCode: Int,
        notifType: String
    ) {
        ensureChannel(context)

        val goToAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("analytics_type", notifType)
            putExtra("analytics_action", "go_to_app")
            putExtra("cancel_notif_id", notifId)
            putExtra("open_tab", "pipeline")
        }
        val goToAppPendingIntent = PendingIntent.getActivity(
            context, goToAppRequestCode, goToAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, PipelineNotificationActionReceiver::class.java).apply {
            action = PipelineNotificationActionReceiver.ACTION_CANCEL
            putExtra(PipelineNotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra("analytics_type", notifType)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, cancelRequestCode, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Go to app", goToAppPendingIntent)
            .addAction(0, "Cancel", cancelPendingIntent)

        val canPost = Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (canPost) {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }
}
