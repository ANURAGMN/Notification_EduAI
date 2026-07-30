package com.example.inactivitydemo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules exactly 4 streak-praise checkpoints (6h, 12h, 18h, 24h) from the
 * moment a new "qualifying use" is logged. Nothing is ever scheduled past
 * 24h for a given qualifying use — only these 4 alarms.
 *
 * Uses setAlarmClock() for the same reason as AlarmScheduler: Doze/App
 * Standby can defer even "exact" alarms, and setAlarmClock is the one API
 * that's never deferred.
 */
object StreakScheduler {

    private val TIERS_TO_REQUEST_CODES = listOf(6 to 6006, 12 to 6012, 18 to 6018, 24 to 6024)

    fun scheduleStreakAlarms(context: Context, fromTime: Long) {
        cancelStreakAlarms(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        TIERS_TO_REQUEST_CODES.forEach { (hours, requestCode) ->
            scheduleOne(context, alarmManager, fromTime + hours * 60L * 60_000L, hours, requestCode)
        }
    }

    fun cancelStreakAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        TIERS_TO_REQUEST_CODES.forEach { (hours, requestCode) ->
            alarmManager.cancel(buildPendingIntent(context, hours, requestCode))
        }
    }

    private fun scheduleOne(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        hours: Int,
        requestCode: Int
    ) {
        val pi = buildPendingIntent(context, hours, requestCode)
        val canUseExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (canUseExact) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, pi), pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun buildPendingIntent(context: Context, hours: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, StreakReceiver::class.java).apply {
            putExtra(StreakReceiver.EXTRA_HOURS, hours)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
