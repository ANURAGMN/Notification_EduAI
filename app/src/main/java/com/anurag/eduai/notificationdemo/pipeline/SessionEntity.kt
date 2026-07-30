package com.anurag.eduai.notificationdemo.pipeline

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the PDF's SessionEntity: a new row is inserted every time the app
 * is opened/foregrounded, with sessionStartTime = System.currentTimeMillis().
 * We never delete old rows here (kept simple like the PDF) — we just always
 * read the latest one.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionStartTime: Long
)
