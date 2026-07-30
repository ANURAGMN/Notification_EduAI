package com.anurag.eduai.notificationdemo.pipeline

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anurag.eduai.notificationdemo.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PipelineScreen(
    sessionStartTime: Long,
    reminderEnabled: Boolean,
    reminderTriggerAt: Long,
    analyticsList: List<AnalyticsEntity>,
    notificationsEnabled: Boolean,
    onAddReminderClick: () -> Unit,
    onRemoveReminderClick: () -> Unit,
    onEnableNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!notificationsEnabled) {
                NotificationDisabledCard(onEnableClick = onEnableNotificationsClick)
            }

            Text(
                text = "Live pipeline (Notification_EduAI)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Room + AlarmManager scheduling from github.com/ANURAGMN/Notification_EduAI",
                style = MaterialTheme.typography.bodySmall
            )

            val formatted = remember(sessionStartTime) {
                if (sessionStartTime == 0L) "—" else
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sessionStartTime))
            }
            Text(modifier = Modifier.padding(top = 16.dp), text = "Last opened at: $formatted")

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
                Button(onClick = onAddReminderClick) { Text("Add reminder") }
                Button(onClick = onRemoveReminderClick) { Text("Remove") }
            }
        }

        HorizontalDivider(thickness = 2.dp, color = Color.Gray)

        Box(modifier = Modifier.weight(1f)) {
            PipelineAnalyticsPanel(analyticsList)
        }
    }
}

@Composable
private fun NotificationDisabledCard(onEnableClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.notification_prompt_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = stringResource(R.string.notification_prompt_body_settings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            OutlinedButton(
                onClick = onEnableClick,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(stringResource(R.string.notification_prompt_open_settings))
            }
        }
    }
}

@Composable
private fun PipelineAnalyticsPanel(analyticsList: List<AnalyticsEntity>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Analytics (Room + optional Firestore sync)",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (analyticsList.isEmpty()) {
            Text("No data recorded yet. Trigger some scheduled notifications!")
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
