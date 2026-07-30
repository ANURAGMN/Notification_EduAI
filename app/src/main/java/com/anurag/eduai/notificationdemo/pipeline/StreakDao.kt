package com.anurag.eduai.notificationdemo.pipeline

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StreakDao {

    @Query("SELECT * FROM streak WHERE id = 1")
    suspend fun getStreak(): StreakEntity?

    @Upsert
    suspend fun upsertStreak(streak: StreakEntity)
}
