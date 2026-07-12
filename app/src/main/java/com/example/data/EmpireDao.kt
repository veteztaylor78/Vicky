package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpireDao {
    @Query("SELECT * FROM empire_state WHERE id = 1")
    fun getEmpireState(): Flow<EmpireState?>

    @Query("SELECT * FROM empire_state WHERE id = 1")
    suspend fun getEmpireStateSnapshot(): EmpireState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEmpireState(state: EmpireState)

    @Query("SELECT * FROM empire_logs ORDER BY id DESC")
    fun getLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM empire_logs")
    suspend fun clearLogs()
}
