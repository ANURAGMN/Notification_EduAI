package com.anurag.eduai.notificationdemo.pipeline

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    // Using a hardcoded demo user ID. In a real app, use Firebase Auth UID.
    private const val USER_ID = "demo_user_123"

    /**
     * Fetches any remote customMsg, merges it with local data,
     * pushes the updated counts, and returns the merged result.
     */
    suspend fun syncAnalytics(localData: AnalyticsEntity): AnalyticsEntity {
        val docRef = db.collection("users").document(USER_ID)
            .collection("analytics").document(localData.notificationType)

        // 1. Fetch remote document
        val snapshot = docRef.get().await()
        // If the field doesn't exist or is null, it defaults to "" (blank)
        val remoteMsg = snapshot.getString("customMsg") ?: ""

        // 2. ALWAYS trust the remote message (even if it is blank to reset it)
        val mergedData = localData.copy(customMsg = remoteMsg)

        // 3. Push the merged data back to Firestore
        val map = hashMapOf(
            "notificationType" to mergedData.notificationType,
            "triggeredCount" to mergedData.triggeredCount,
            "goToAppCount" to mergedData.goToAppCount,
            "cancelCount" to mergedData.cancelCount,
            "customMsg" to mergedData.customMsg
        )
        docRef.set(map, SetOptions.merge()).await()

        return mergedData
    }
}