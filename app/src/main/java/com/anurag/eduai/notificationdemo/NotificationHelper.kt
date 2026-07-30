package com.anurag.eduai.notificationdemo

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Builds and fires notifications matching NOTIFICATION_USE_CASES.md §7:
 * personalized-avatar large icon (placeholder face rendered here - swap for
 * the real TutorConfig bitmap render once that pipeline exists), category
 * tint on the small icon, BigTextStyle body, two-button pattern
 * (primary action + Cancel).
 */
object NotificationHelper {

    const val EXTRA_ROUTE = "extra_route"
    const val EXTRA_PARAMS = "extra_params" // "k1=v1,k2=v2"
    const val EXTRA_NOTIF_ID = "extra_notif_id"
    const val ACTION_CANCEL = "com.anurag.eduai.notificationdemo.ACTION_CANCEL"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        NotificationCategory.entries.forEach { category ->
            val importance = if (category.highImportanceDefault) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel = NotificationChannel(category.channelId, category.channelLabel, importance)
            manager.createNotificationChannel(channel)
        }
    }

    /** One-time placeholder for the real TutorConfig render-to-bitmap path (§7). */
    private fun buildAvatarBitmap(context: Context, tintColorRes: Int): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val tint = ContextCompat.getColor(context, tintColorRes)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; style = Paint.Style.FILL }

        val cx = size / 2f
        val cy = size / 2f
        val r = size / 2f - 6f
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, ringPaint)

        val eyeOffsetX = r * 0.35f
        val eyeOffsetY = r * 0.15f
        val eyeR = r * 0.09f
        canvas.drawCircle(cx - eyeOffsetX, cy - eyeOffsetY, eyeR, eyePaint)
        canvas.drawCircle(cx + eyeOffsetX, cy - eyeOffsetY, eyeR, eyePaint)

        val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint; style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND
        }
        val path = android.graphics.Path()
        path.moveTo(cx - r * 0.4f, cy + r * 0.15f)
        path.quadTo(cx, cy + r * 0.55f, cx + r * 0.4f, cy + r * 0.15f)
        canvas.drawPath(path, smilePaint)

        return bitmap
    }

    /**
     * Caller (MainActivity) is responsible for checking/requesting POST_NOTIFICATIONS
     * contextually before calling this - see §9/§17 of the plan. Suppressed here since
     * that check happens one layer up, not inside this helper.
     */
    @SuppressLint("MissingPermission")
    fun fire(context: Context, case: NotificationCase, notifId: Int) {
        val paramsString = case.deepLinkParams.entries.joinToString(",") { "${it.key}=${it.value}" }

        val primaryIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, case.deepLinkRoute)
            putExtra(EXTRA_PARAMS, paramsString)
        }
        val primaryPendingIntent = PendingIntent.getActivity(
            context, notifId, primaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, notifId + 10_000, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val largeIcon = buildAvatarBitmap(context, case.category.textColorRes)
        val accentColor = ContextCompat.getColor(context, case.category.dotColorRes)

        val builder = NotificationCompat.Builder(context, case.category.channelId)
            .setSmallIcon(R.drawable.ic_stat_eduai)
            .setLargeIcon(largeIcon)
            .setColor(accentColor)
            .setContentTitle(case.title)
            .setContentText(case.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(case.body))
            .setPriority(
                if (case.highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(primaryPendingIntent)
            .addAction(0, case.primaryActionLabel, primaryPendingIntent)
            .addAction(0, "Cancel", cancelPendingIntent)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
}
