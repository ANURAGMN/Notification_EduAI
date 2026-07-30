package com.example.inactivitydemo

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private var foregroundEnteredAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntentForAnalytics(intent) // Catch cold starts from notifications

        requestNotificationPermissionIfNeeded()
        maybePromptExactAlarmSetting()

        val db = AppDatabase.getInstance(applicationContext)

        setContent {
            var sessionStartTime by remember { mutableStateOf(0L) }
            var reminderEnabled by remember { mutableStateOf(false) }
            var reminderTriggerAt by remember { mutableStateOf(0L) }

            // Collect analytics data for the test UI
            val analyticsData by db.analyticsDao().getAllAnalyticsFlow().collectAsState(initial = emptyList())

            LaunchedEffect(Unit) {
                resetSession { sessionStartTime = it }
            }

            LaunchedEffect(Unit) {
                launch {
                    ReminderPreferences.isEnabledFlow(applicationContext).collectLatest { reminderEnabled = it }
                }
                launch {
                    ReminderPreferences.triggerAtFlow(applicationContext).collectLatest { reminderTriggerAt = it }
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Original UI wrapped in a weight block
                        Box(modifier = Modifier.weight(1f)) {
                            ScreenContent(
                                sessionStartTime = sessionStartTime,
                                reminderEnabled = reminderEnabled,
                                reminderTriggerAt = reminderTriggerAt,
                                onAddReminderClick = { showTimePickerAndSaveReminder() },
                                onRemoveReminderClick = { removeReminder() }
                            )
                        }

                        Divider(thickness = 2.dp, color = Color.Gray)

                        // New Analytics Test UI
                        Box(modifier = Modifier.weight(1f)) {
                            AnalyticsTestUI(analyticsData)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentForAnalytics(intent) // Catch warm starts from notifications
        resetSession { }
    }

    private fun handleIntentForAnalytics(intent: Intent?) {
        intent?.let {
            val type = it.getStringExtra("analytics_type")
            val action = it.getStringExtra("analytics_action")
            val notifId = it.getIntExtra("cancel_notif_id", -1)

            // <-- NEW: Clear the notification from the tray immediately
            if (notifId != -1) {
                NotificationManagerCompat.from(this).cancel(notifId)
                it.removeExtra("cancel_notif_id")
            }

            if (type != null && action == "go_to_app") {
                CoroutineScope(Dispatchers.IO).launch {
                    AnalyticsTracker.logEvent(applicationContext, type, AnalyticsTracker.EventType.GO_TO_APP)
                }
                // Remove extra so it doesn't get double-counted on rotation/recreation
                it.removeExtra("analytics_action")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foregroundEnteredAt = System.currentTimeMillis()
    }

    override fun onStop() {
        super.onStop()
        val stayedMillis = System.currentTimeMillis() - foregroundEnteredAt
        if (stayedMillis >= 5_000L) {
            logQualifyingUseForStreak()
        }
    }

    private fun resetSession(onDone: (Long) -> Unit) {
        val now = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.sessionDao().insertSession(SessionEntity(sessionStartTime = now))
            db.notificationStateDao().upsertState(
                NotificationStateEntity(shown5min = false, shown10min = false, lastNotifiedAt = 0L)
            )

            withContext(Dispatchers.Main) {
                NotificationManagerCompat.from(applicationContext).cancel(2005)
                NotificationManagerCompat.from(applicationContext).cancel(2010)
                AlarmScheduler.scheduleInactivityAlarms(applicationContext)
                onDone(now)
            }
        }
    }

    private fun logQualifyingUseForStreak() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            val streak = db.streakDao().getStreak() ?: StreakEntity()
            val now = System.currentTimeMillis()
            val sixHoursMillis = 6 * 60 * 60 * 1000L

            val qualifies = streak.lastQualifyingUseAt == 0L ||
                    (now - streak.lastQualifyingUseAt) >= sixHoursMillis

            if (qualifies) {
                val updated = streak.copy(
                    streakCount = streak.streakCount + 1,
                    lastQualifyingUseAt = now,
                    milestone6hShown = false,
                    milestone12hShown = false,
                    milestone18hShown = false,
                    milestone24hShown = false
                )
                db.streakDao().upsertStreak(updated)

                withContext(Dispatchers.Main) {
                    StreakScheduler.scheduleStreakAlarms(applicationContext, now)
                }
            }
        }
    }

    private fun showTimePickerAndSaveReminder() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val triggerCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val triggerAt = triggerCalendar.timeInMillis

                CoroutineScope(Dispatchers.IO).launch {
                    ReminderPreferences.setReminder(applicationContext, triggerAt)
                    withContext(Dispatchers.Main) {
                        ReminderScheduler.scheduleReminder(applicationContext, triggerAt)
                    }
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun removeReminder() {
        CoroutineScope(Dispatchers.IO).launch {
            ReminderPreferences.clearReminder(applicationContext)
            withContext(Dispatchers.Main) {
                ReminderScheduler.cancelReminder(applicationContext)
                NotificationManagerCompat.from(applicationContext).cancel(9001)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun maybePromptExactAlarmSetting() {
        if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

// ==========================================
// COMPOSABLES
// ==========================================

@Composable
fun ScreenContent(
    sessionStartTime: Long,
    reminderEnabled: Boolean,
    reminderTriggerAt: Long,
    onAddReminderClick: () -> Unit,
    onRemoveReminderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Notification Demo", style = MaterialTheme.typography.titleLarge)

        val formatted = remember(sessionStartTime) {
            if (sessionStartTime == 0L) "—" else
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sessionStartTime))
        }
        Text(modifier = Modifier.padding(top = 16.dp), text = "Last opened at: $formatted")

        // Restored from your original code
        Text(
            modifier = Modifier.padding(top = 24.dp),
            text = "Inactivity: press Home and wait ~5 / ~10 min for those notifications.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Streak: open the app for 5+ sec, at least once every 6h, to build your streak.",
            style = MaterialTheme.typography.bodySmall
        )

        val reminderStatusText = if (reminderEnabled && reminderTriggerAt > 0L) {
            val formattedReminder = remember(reminderTriggerAt) {
                SimpleDateFormat("HH:mm 'on' MMM d", Locale.getDefault()).format(Date(reminderTriggerAt))
            }
            "Reminder set for $formattedReminder"
        } else {
            "No reminder set"
        }
        Text(modifier = Modifier.padding(top = 24.dp), text = reminderStatusText)

        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddReminderClick) { Text("Add") }
            Button(onClick = onRemoveReminderClick) { Text("Remove") }
        }
    }
}

@Composable
fun AnalyticsTestUI(analyticsList: List<AnalyticsEntity>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Analytics Test Panel (DB Sync Live)",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (analyticsList.isEmpty()) {
            Text("No data recorded yet. Trigger some notifications!")
        } else {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray).padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Type", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                        Text("Pop", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
                        Text("Go", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
                        Text("Can", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
                        Text("Ign", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
                    }
                }

                items(analyticsList) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.notificationType, modifier = Modifier.weight(1.5f))
                        Text("${item.triggeredCount}", modifier = Modifier.weight(0.7f))
                        Text("${item.goToAppCount}", modifier = Modifier.weight(0.7f))
                        Text("${item.cancelCount}", modifier = Modifier.weight(0.7f))
                        Text("${item.ignoredCount}", modifier = Modifier.weight(0.7f))
                    }
                }
            }
        }
    }
}