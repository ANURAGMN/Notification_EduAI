package com.anurag.eduai.notificationdemo.pipeline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreakReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_HOURS = "hours"
        private val VALID_TIERS = setOf(6, 12, 18, 24)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val hours = intent.getIntExtra(EXTRA_HOURS, 0)
        if (hours !in VALID_TIERS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val streak = db.streakDao().getStreak() ?: return@launch

                val alreadyShown = when (hours) {
                    6 -> streak.milestone6hShown
                    12 -> streak.milestone12hShown
                    18 -> streak.milestone18hShown
                    else -> streak.milestone24hShown
                }
                if (alreadyShown) return@launch

                val notifType = "Streak_${hours}h"

                // 1. Sync & Log Trigger immediately
                val syncedAnalytics = AnalyticsTracker.logEvent(
                    context,
                    notifType,
                    AnalyticsTracker.EventType.TRIGGERED
                )

                // 2. Use custom message if it exists (replaces "X" with streak count)
                val rawText = if (syncedAnalytics.customMsg.isNotEmpty()) {
                    syncedAnalytics.customMsg
                } else {
                    Messages.streakMessages.random()
                }
                val text = rawText.replace("X", streak.streakCount.toString())

                PipelineNotificationHelper.show(
                    context = context,
                    notifId = 5000 + hours,
                    title = "Streak check-in",
                    text = text,
                    goToAppRequestCode = 5100 + hours,
                    cancelRequestCode = 5200 + hours,
                    notifType = notifType
                )

                val updated = when (hours) {
                    6 -> streak.copy(milestone6hShown = true)
                    12 -> streak.copy(milestone12hShown = true)
                    18 -> streak.copy(milestone18hShown = true)
                    else -> streak.copy(milestone24hShown = true)
                }
                db.streakDao().upsertStreak(updated)
            } finally {
                pendingResult.finish()
            }
        }
    }
}