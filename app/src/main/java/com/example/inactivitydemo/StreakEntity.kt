package com.example.inactivitydemo

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table (id always 1). Tracks the app-usage streak.
 *
 * A "qualifying use" = app opened and stayed in the foreground for at
 * least 5 seconds, counted only once per 6-hour window (see MainActivity's
 * onStop() check). Every time a new qualifying use is logged, streakCount
 * goes up by 1 and the 4 milestone flags reset so the next 6h/12h/18h/24h
 * praise notifications can fire again from that new point in time.
 */
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 1,
    val streakCount: Int = 0,
    val lastQualifyingUseAt: Long = 0L,
    val milestone6hShown: Boolean = false,
    val milestone12hShown: Boolean = false,
    val milestone18hShown: Boolean = false,
    val milestone24hShown: Boolean = false
)
