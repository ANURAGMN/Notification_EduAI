package com.anurag.eduai.notificationdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Handles the explicit "Cancel" button on a notification (§7's two-button
 * pattern) - dismisses just that notification, distinct from swipe-to-dismiss.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_CANCEL) {
            val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIF_ID, -1)
            if (notifId != -1) {
                NotificationManagerCompat.from(context).cancel(notifId)
            }
        }
    }
}
