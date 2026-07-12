package com.example.data

import kotlinx.coroutines.flow.Flow

class EmpireRepository(private val empireDao: EmpireDao) {
    val empireState: Flow<EmpireState?> = empireDao.getEmpireState()
    val logs: Flow<List<LogEntry>> = empireDao.getLogs()

    suspend fun getEmpireStateSnapshot(): EmpireState? {
        return empireDao.getEmpireStateSnapshot()
    }

    suspend fun saveEmpireState(state: EmpireState) {
        empireDao.saveEmpireState(state)
    }

    suspend fun addLog(day: Int, category: String, message: String) {
        empireDao.insertLog(
            LogEntry(
                day = day,
                category = category,
                message = message
            )
        )
    }

    suspend fun clearLogs() {
        empireDao.clearLogs()
    }

    suspend fun initializeNewEmpire(rulerName: String) {
        empireDao.clearLogs()
        val defaultState = EmpireState(
            rulerName = rulerName,
            empireName = "${rulerName}\'s Empire"
        )
        empireDao.saveEmpireState(defaultState)
        
        empireDao.insertLog(
            LogEntry(
                day = 1,
                category = "Turn",
                message = "The grand chronicles of ${rulerName}\'s Empire begin! Long live the Emperor!"
            )
        )
    }
}
