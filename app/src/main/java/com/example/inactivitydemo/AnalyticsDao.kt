package com.example.inactivitydemo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics")
    fun getAllAnalyticsFlow(): Flow<List<AnalyticsEntity>>

    @Query("SELECT * FROM analytics WHERE notificationType = :type")
    suspend fun getAnalytics(type: String): AnalyticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analytics: AnalyticsEntity)
}