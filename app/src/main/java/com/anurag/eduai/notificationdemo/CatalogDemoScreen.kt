package com.anurag.eduai.notificationdemo

import android.content.Intent
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@Composable
fun CatalogDemoScreen(
    activity: ComponentActivity,
    initialDeepLinkRoute: String?,
    initialDeepLinkParams: String?,
    onRequireNotifications: (onGranted: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var deepLinkText by remember {
        mutableStateOf(formatDeepLink(initialDeepLinkRoute, initialDeepLinkParams))
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                val pad = dp(ctx, 20)
                setPadding(pad, pad, pad, pad)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            root.addView(header(ctx, "Notification catalog"))
            root.addView(subtext(ctx, "Tap a case to fire the real notification. Matches NOTIFICATION_USE_CASES.md §1–§7."))

            val statusText = subtext(ctx, NotificationBudget.formatStatus(ctx)).apply {
                setPadding(0, dp(ctx, 8), 0, dp(ctx, 4))
            }
            root.addView(statusText)

            val deepLinkView = subtext(ctx, deepLinkText).apply {
                setPadding(0, 0, 0, dp(ctx, 8))
            }
            root.addView(deepLinkView)

            root.addView(Button(ctx).apply {
                text = "Reset budget (debug)"
                setOnClickListener {
                    NotificationBudget.reset(ctx)
                    statusText.text = NotificationBudget.formatStatus(ctx)
                    Toast.makeText(ctx, "Budget reset.", Toast.LENGTH_SHORT).show()
                }
            })

            NotificationCategory.entries.forEach { category ->
                val casesInCategory = NotificationCatalog.cases.filter { it.category == category }
                if (casesInCategory.isEmpty()) return@forEach

                root.addView(sectionHeader(ctx, category.channelLabel, category))

                casesInCategory.forEach { case ->
                    val container = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(ctx, 4), 0, dp(ctx, 4))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    val button = Button(ctx).apply {
                        text = case.title
                        setAllCaps(false)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setOnClickListener {
                            val notifId = case.type.ordinal
                            if (!NotificationPermissionHelper.areNotificationsEnabled(ctx)) {
                                onRequireNotifications {
                                    fireCase(activity, case, notifId) { msg ->
                                        deepLinkView.text = msg
                                        deepLinkText = msg
                                    }
                                    statusText.text = NotificationBudget.formatStatus(ctx)
                                }
                                return@setOnClickListener
                            }
                            fireCase(activity, case, notifId) { msg ->
                                deepLinkView.text = msg
                                deepLinkText = msg
                            }
                            statusText.text = NotificationBudget.formatStatus(ctx)
                        }
                    }
                    val tag = TextView(ctx).apply {
                        text = if (case.serverTriggeredInProd) "sim" else "local"
                        setPadding(dp(ctx, 8), 0, 0, 0)
                        setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                        textSize = 11f
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    container.addView(button)
                    container.addView(tag)
                    root.addView(container)
                }
            }

            ScrollView(ctx).apply { addView(root) }
        },
        update = { scroll ->
            val deepLinkView = (scroll.getChildAt(0) as LinearLayout).getChildAt(3) as TextView
            deepLinkView.text = deepLinkText
        }
    )
}

private fun fireCase(
    activity: ComponentActivity,
    case: NotificationCase,
    notifId: Int,
    onDeepLinkUpdate: (String) -> Unit
) {
    val blockedReason = NotificationBudget.canSend(activity)
    if (blockedReason != null) {
        Toast.makeText(activity, "Blocked: $blockedReason", Toast.LENGTH_LONG).show()
        return
    }
    NotificationHelper.fire(activity, case, notifId)
    NotificationBudget.recordSend(activity)
    val note = if (case.serverTriggeredInProd) " (simulated - real app needs Phase 2 FCM)" else ""
    Toast.makeText(activity, "Fired: ${case.title}$note", Toast.LENGTH_SHORT).show()
}

fun handleCatalogDeepLink(intent: Intent?): String {
    val route = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE) ?: return "Last deep link: (none yet)"
    val params = intent.getStringExtra(NotificationHelper.EXTRA_PARAMS).orEmpty()
    return formatDeepLink(route, params)
}

private fun formatDeepLink(route: String?, params: String?): String {
    if (route.isNullOrBlank()) return "Last deep link: (none yet)"
    val paramsDisplay = if (params.isNullOrBlank()) "" else " params={$params}"
    return "Last deep link: route=\"$route\"$paramsDisplay"
}

private fun header(ctx: android.content.Context, text: String) = TextView(ctx).apply {
    this.text = text
    textSize = 20f
    setTypeface(typeface, Typeface.BOLD)
    setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
}

private fun subtext(ctx: android.content.Context, text: String) = TextView(ctx).apply {
    this.text = text
    textSize = 13f
    setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
}

private fun sectionHeader(ctx: android.content.Context, label: String, category: NotificationCategory) =
    TextView(ctx).apply {
        text = label
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(ContextCompat.getColor(ctx, category.textColorRes))
        setPadding(0, dp(ctx, 20), 0, dp(ctx, 6))
    }

private fun dp(ctx: android.content.Context, value: Int): Int =
    (value * ctx.resources.displayMetrics.density).toInt()
