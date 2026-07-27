package com.example.inactivitydemo

import android.content.Context
import android.util.Log

object AnalyticsTracker {
    enum class EventType { TRIGGERED, GO_TO_APP, CANCELED }

    suspend fun logEvent(context: Context, type: String, event: EventType): AnalyticsEntity {
        val db = AppDatabase.getInstance(context)
        val dao = db.analyticsDao()
        val current = dao.getAnalytics(type) ?: AnalyticsEntity(notificationType = type)

        val updatedLocal = when (event) {
            EventType.TRIGGERED -> current.copy(triggeredCount = current.triggeredCount + 1)
            EventType.GO_TO_APP -> current.copy(goToAppCount = current.goToAppCount + 1)
            EventType.CANCELED -> current.copy(cancelCount = current.cancelCount + 1)
        }

        // Save locally first
        dao.insert(updatedLocal)

        // Attempt two-way Firestore Sync
        return try {
            val syncedData = FirestoreRepository.syncAnalytics(updatedLocal)
            // If Firestore had a new customMsg, update Room with it
            if (syncedData.customMsg != updatedLocal.customMsg) {
                dao.insert(syncedData)
            }
            syncedData
        } catch (e: Exception) {
            // If offline/network fails, just return the local data
            Log.e("AnalyticsTracker", "Firestore sync failed", e)
            updatedLocal
        }
    }
}