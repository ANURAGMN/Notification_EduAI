package com.example.inactivitydemo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics")
data class AnalyticsEntity(
    @PrimaryKey val notificationType: String,
    val triggeredCount: Int = 0,
    val goToAppCount: Int = 0,
    val cancelCount: Int = 0,
    val customMsg: String = "" // NEW: Stores custom message from Firestore
) {
    val ignoredCount: Int
        get() = triggeredCount - (goToAppCount + cancelCount)
}