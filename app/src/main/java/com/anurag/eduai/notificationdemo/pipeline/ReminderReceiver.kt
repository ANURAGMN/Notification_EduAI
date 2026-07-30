package com.anurag.eduai.notificationdemo.pipeline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notifType = "Reminder"

                // 1. Sync & Log Trigger immediately
                val syncedAnalytics = AnalyticsTracker.logEvent(
                    context,
                    notifType,
                    AnalyticsTracker.EventType.TRIGGERED
                )

                // 2. Use custom message if it exists
                val text = if (syncedAnalytics.customMsg.isNotEmpty()) {
                    syncedAnalytics.customMsg
                } else {
                    Messages.reminderMessages.random()
                }

                PipelineNotificationHelper.show(
                    context = context,
                    notifId = 9001,
                    title = "Study reminder",
                    text = text,
                    goToAppRequestCode = 9101,
                    cancelRequestCode = 9201,
                    notifType = notifType
                )
                ReminderPreferences.clearReminder(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}