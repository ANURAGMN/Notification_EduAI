package com.anurag.eduai.notificationdemo.pipeline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    // Same idea as PDF's getLatestSessionForStudent() — just no studentId
    // filter since this demo is single-user.
    @Query("SELECT * FROM sessions ORDER BY sessionStartTime DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?
}
