package com.example.inactivitydemo

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table (id is always 1). Mirrors PDF's NotificationStateEntity,
 * minus the Firestore backup fields. Two flags instead of one because we
 * have two independent tiers (5 min, 10 min) instead of one daily check.
 */
@Entity(tableName = "notification_state")
data class NotificationStateEntity(
    @PrimaryKey val id: Int = 1,
    val shown5min: Boolean = false,
    val shown10min: Boolean = false,
    val lastNotifiedAt: Long = 0L
)
