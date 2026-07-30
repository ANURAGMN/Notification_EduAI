package com.anurag.eduai.notificationdemo

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.anurag.eduai.notificationdemo.pipeline.AlarmScheduler
import com.anurag.eduai.notificationdemo.pipeline.AnalyticsTracker
import com.anurag.eduai.notificationdemo.pipeline.AppDatabase
import com.anurag.eduai.notificationdemo.pipeline.NotificationStateEntity
import com.anurag.eduai.notificationdemo.pipeline.PipelineNotificationHelper
import com.anurag.eduai.notificationdemo.pipeline.PipelineScreen
import com.anurag.eduai.notificationdemo.pipeline.ReminderPreferences
import com.anurag.eduai.notificationdemo.pipeline.ReminderScheduler
import com.anurag.eduai.notificationdemo.pipeline.SessionEntity
import com.anurag.eduai.notificationdemo.pipeline.StreakEntity
import com.anurag.eduai.notificationdemo.pipeline.StreakScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Unified launcher: tab 1 = manual notification catalog (§1–§7 test harness),
 * tab 2 = live Room + AlarmManager pipeline from Notification_EduAI.
 */
class MainActivity : ComponentActivity() {

    private var pendingAfterPermission: (() -> Unit)? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingAfterPermission?.invoke()
            }
            pendingAfterPermission = null
        }

    private var foregroundEnteredAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.ensureChannels(this)
        PipelineNotificationHelper.ensureChannel(this)

        handlePipelineIntent(intent)
        maybePromptExactAlarmSetting()

        val db = AppDatabase.getInstance(applicationContext)
        val initialTab = if (intent.getStringExtra("open_tab") == "pipeline") 1 else 0
        val catalogRoute = intent.getStringExtra(NotificationHelper.EXTRA_ROUTE)
        val catalogParams = intent.getStringExtra(NotificationHelper.EXTRA_PARAMS)

        setContent {
            var selectedTab by remember { mutableIntStateOf(initialTab) }
            var sessionStartTime by remember { mutableStateOf(0L) }
            var reminderEnabled by remember { mutableStateOf(false) }
            var reminderTriggerAt by remember { mutableStateOf(0L) }
            var notificationsEnabled by remember {
                mutableStateOf(NotificationPermissionHelper.areNotificationsEnabled(this@MainActivity))
            }
            var showNotificationPrompt by remember { mutableStateOf(false) }
            var notificationPromptMode by remember {
                mutableStateOf(NotificationPromptMode.REQUEST_PERMISSION)
            }

            fun refreshNotificationState() {
                notificationsEnabled = NotificationPermissionHelper.areNotificationsEnabled(this@MainActivity)
            }

            fun promptForNotifications(onGranted: (() -> Unit)? = null) {
                val mode = NotificationPermissionHelper.resolvePromptMode(this@MainActivity)
                if (mode == null) {
                    onGranted?.invoke()
                    return
                }
                pendingAfterPermission = onGranted
                notificationPromptMode = mode
                showNotificationPrompt = true
            }

            val analyticsData by db.analyticsDao().getAllAnalyticsFlow()
                .collectAsState(initial = emptyList())

            androidx.compose.runtime.LaunchedEffect(Unit) {
                resetSession { sessionStartTime = it }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                launch {
                    ReminderPreferences.isEnabledFlow(applicationContext)
                        .collectLatest { reminderEnabled = it }
                }
                launch {
                    ReminderPreferences.triggerAtFlow(applicationContext)
                        .collectLatest { reminderTriggerAt = it }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        refreshNotificationState()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Catalog") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = {
                                        selectedTab = 1
                                        if (!notificationsEnabled) {
                                            promptForNotifications()
                                        }
                                    },
                                    text = { Text("Live pipeline") }
                                )
                            }

                            when (selectedTab) {
                                0 -> CatalogDemoScreen(
                                    activity = this@MainActivity,
                                    initialDeepLinkRoute = catalogRoute,
                                    initialDeepLinkParams = catalogParams,
                                    onRequireNotifications = { onGranted ->
                                        promptForNotifications(onGranted)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> PipelineScreen(
                                    sessionStartTime = sessionStartTime,
                                    reminderEnabled = reminderEnabled,
                                    reminderTriggerAt = reminderTriggerAt,
                                    analyticsList = analyticsData,
                                    notificationsEnabled = notificationsEnabled,
                                    onAddReminderClick = {
                                        promptForNotifications {
                                            showTimePickerAndSaveReminder()
                                        }
                                    },
                                    onRemoveReminderClick = { removeReminder() },
                                    onEnableNotificationsClick = { promptForNotifications() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    NotificationPermissionDialog(
                        visible = showNotificationPrompt,
                        mode = notificationPromptMode,
                        onDismiss = {
                            showNotificationPrompt = false
                            pendingAfterPermission = null
                        },
                        onConfirm = {
                            showNotificationPrompt = false
                            when (notificationPromptMode) {
                                NotificationPromptMode.REQUEST_PERMISSION -> {
                                    NotificationPermissionHelper.markPermissionRequested(this@MainActivity)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }
                                NotificationPromptMode.OPEN_SETTINGS -> {
                                    NotificationPermissionHelper.openNotificationSettings(this@MainActivity)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePipelineIntent(intent)
        if (intent.getStringExtra(NotificationHelper.EXTRA_ROUTE) != null) {
            // Catalog deep link — Compose state picks this up on next recomposition via intent
        }
        resetSession { }
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

    private fun handlePipelineIntent(intent: Intent?) {
        intent ?: return
        val type = intent.getStringExtra("analytics_type")
        val action = intent.getStringExtra("analytics_action")
        val notifId = intent.getIntExtra("cancel_notif_id", -1)

        if (notifId != -1) {
            NotificationManagerCompat.from(this).cancel(notifId)
            intent.removeExtra("cancel_notif_id")
        }

        if (type != null && action == "go_to_app") {
            CoroutineScope(Dispatchers.IO).launch {
                AnalyticsTracker.logEvent(applicationContext, type, AnalyticsTracker.EventType.GO_TO_APP)
            }
            intent.removeExtra("analytics_action")
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
