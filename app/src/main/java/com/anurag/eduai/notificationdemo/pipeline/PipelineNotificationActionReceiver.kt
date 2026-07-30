package com.anurag.eduai.notificationdemo.pipeline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PipelineNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL = "com.anurag.eduai.notificationdemo.pipeline.ACTION_CANCEL"
        const val EXTRA_NOTIF_ID = "notifId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL) return

        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val notifType = intent.getStringExtra("analytics_type")

        if (notifId != -1) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }

        if (notifType != null) {
            CoroutineScope(Dispatchers.IO).launch {
                AnalyticsTracker.logEvent(context, notifType, AnalyticsTracker.EventType.CANCELED)
            }
        }
    }
}
