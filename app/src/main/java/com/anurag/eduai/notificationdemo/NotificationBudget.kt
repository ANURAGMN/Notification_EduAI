package com.anurag.eduai.notificationdemo

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implements the frequency cap locked in for NOTIFICATION_USE_CASES.md §5:
 * max 3 notifications/day, minimum 2-hour gap between any two - enforced
 * app-wide across all categories, not per-category.
 *
 * Backed by SharedPreferences here (stand-in for the real app's
 * NotificationBudgetEntity(userId, date, sentCount, lastSentAt) in Room).
 */
object NotificationBudget {

    private const val PREFS = "notification_budget"
    private const val KEY_DATE = "date"
    private const val KEY_COUNT = "sentCount"
    private const val KEY_LAST_SENT_AT = "lastSentAt"

    const val DAILY_CAP = 3
    const val MIN_GAP_MILLIS = 2 * 60 * 60 * 1000L // 2 hours

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Resets sentCount if the stored date isn't today - the "daily reset at local midnight" rule. */
    private fun rolloverIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.getString(KEY_DATE, null) != todayKey()) {
            p.edit()
                .putString(KEY_DATE, todayKey())
                .putInt(KEY_COUNT, 0)
                .apply()
        }
    }

    data class Status(
        val sentToday: Int,
        val remainingToday: Int,
        val lastSentAt: Long,
        val nextAllowedAt: Long,
    )

    fun status(context: Context): Status {
        rolloverIfNeeded(context)
        val p = prefs(context)
        val sent = p.getInt(KEY_COUNT, 0)
        val lastSentAt = p.getLong(KEY_LAST_SENT_AT, 0L)
        val nextAllowedAt = if (lastSentAt == 0L) 0L else lastSentAt + MIN_GAP_MILLIS
        return Status(
            sentToday = sent,
            remainingToday = (DAILY_CAP - sent).coerceAtLeast(0),
            lastSentAt = lastSentAt,
            nextAllowedAt = nextAllowedAt,
        )
    }

    /** Returns null if sending is allowed right now, or a human-readable reason if it's blocked. */
    fun canSend(context: Context): String? {
        val s = status(context)
        if (s.sentToday >= DAILY_CAP) {
            return "Daily cap reached (3/3). Resets at local midnight."
        }
        val now = System.currentTimeMillis()
        if (s.lastSentAt != 0L && now < s.nextAllowedAt) {
            val waitMinutes = (s.nextAllowedAt - now) / 60000
            return "Too soon - wait ${waitMinutes}m for the 2h gap (or reset budget below)."
        }
        return null
    }

    fun recordSend(context: Context) {
        rolloverIfNeeded(context)
        val p = prefs(context)
        val sent = p.getInt(KEY_COUNT, 0)
        p.edit()
            .putInt(KEY_COUNT, sent + 1)
            .putLong(KEY_LAST_SENT_AT, System.currentTimeMillis())
            .apply()
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun formatStatus(context: Context): String {
        val s = status(context)
        val fmt = SimpleDateFormat("HH:mm", Locale.US)
        val nextAllowedText = when {
            s.sentToday >= DAILY_CAP -> "cap reached for today"
            s.nextAllowedAt <= System.currentTimeMillis() -> "now"
            else -> "after ${fmt.format(Date(s.nextAllowedAt))}"
        }
        return "Sent today: ${s.sentToday}/${DAILY_CAP}  ·  next allowed: $nextAllowedText"
    }
}
