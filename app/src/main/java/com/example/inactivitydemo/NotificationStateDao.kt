package com.example.inactivitydemo

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NotificationStateDao {

    @Query("SELECT * FROM notification_state WHERE id = 1")
    suspend fun getState(): NotificationStateEntity?

    @Upsert
    suspend fun upsertState(state: NotificationStateEntity)
}
