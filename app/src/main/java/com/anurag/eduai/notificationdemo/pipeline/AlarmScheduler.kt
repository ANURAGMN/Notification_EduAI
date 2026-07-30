package com.anurag.eduai.notificationdemo.pipeline

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the two "tier" alarms (5 min, 10 min) from the moment the app is
 * opened. Equivalent role to the PDF's daily 8 PM AlarmManager registration,
 * just re-armed on every app open instead of being a single recurring alarm.
 */
object AlarmScheduler {

    private const val REQUEST_CODE_5MIN = 1005
    private const val REQUEST_CODE_10MIN = 1010

    private const val TIER_5 = 5
    private const val TIER_10 = 10

    fun scheduleInactivityAlarms(context: Context) {
        // Always clear old ones first so reopening the app doesn't leave
        // stale alarms scheduled from the previous session.
        cancelInactivityAlarms(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        scheduleOne(context, alarmManager, now + TIER_5 * 60_000L, TIER_5, REQUEST_CODE_5MIN)
        scheduleOne(context, alarmManager, now + TIER_10 * 60_000L, TIER_10, REQUEST_CODE_10MIN)
    }

    fun cancelInactivityAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(TIER_5 to REQUEST_CODE_5MIN, TIER_10 to REQUEST_CODE_10MIN).forEach { (tier, requestCode) ->
            val pi = buildPendingIntent(context, tier, requestCode)
            alarmManager.cancel(pi)
        }
    }

    private fun scheduleOne(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        tier: Int,
        requestCode: Int
    ) {
        val pi = buildPendingIntent(context, tier, requestCode)

        val canUseExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()

        if (canUseExact) {
            // setAlarmClock (not setExactAndAllowWhileIdle) is used here on
            // purpose: even "exact" alarms can still get deferred by
            // Android's App Standby buckets if it thinks this app is
            // rarely used — which is exactly what caused notifications to
            // arrive a day late. setAlarmClock is treated like a real
            // alarm-clock alert and is NEVER deferred, at the cost of a
            // small persistent alarm icon in the status bar until it fires.
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pi)
            alarmManager.setAlarmClock(alarmClockInfo, pi)
        } else {
            // Fallback for devices where the user hasn't granted the exact
            // alarm permission — still works for demo purposes, just less
            // precisely timed.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun buildPendingIntent(context: Context, tier: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, InactivityReceiver::class.java).apply {
            putExtra(InactivityReceiver.EXTRA_TIER, tier)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}