package com.example.inactivitydemo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_prefs")

/**
 * Custom reminder time — DataStore only, no Room table involved at all, per
 * the requirement that this feature stays fully independent of the
 * Inactivity/Streak Room state.
 *
 * One-shot by design: setReminder() stores the exact next trigger time;
 * once ReminderReceiver fires, it calls clearReminder() so it does NOT
 * repeat the next day unless the user manually adds it again.
 */
object ReminderPreferences {

    private val KEY_ENABLED = booleanPreferencesKey("reminder_enabled")
    private val KEY_TRIGGER_AT = longPreferencesKey("reminder_trigger_at")

    fun isEnabledFlow(context: Context): Flow<Boolean> =
        context.reminderDataStore.data.map { it[KEY_ENABLED] ?: false }

    fun triggerAtFlow(context: Context): Flow<Long> =
        context.reminderDataStore.data.map { it[KEY_TRIGGER_AT] ?: 0L }

    suspend fun setReminder(context: Context, triggerAtMillis: Long) {
        context.reminderDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = true
            prefs[KEY_TRIGGER_AT] = triggerAtMillis
        }
    }

    suspend fun clearReminder(context: Context) {
        context.reminderDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = false
            prefs[KEY_TRIGGER_AT] = 0L
        }
    }
}
