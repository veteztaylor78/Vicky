package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "empire_state")
data class EmpireState(
    @PrimaryKey val id: Int = 1,
    val rulerName: String = "MAM",
    val empireName: String = "MAM\'s Empire",
    val day: Int = 1,
    
    // Resources
    val gold: Int = 1000,
    val food: Int = 1000,
    val wood: Int = 500,
    val stone: Int = 300,
    val population: Int = 50,
    val happiness: Int = 85, // Scale of 0 to 100
    
    // Buildings
    val farmCount: Int = 1,
    val lumberMillCount: Int = 1,
    val quarryCount: Int = 1,
    val goldMineCount: Int = 1,
    val barracksCount: Int = 0,
    val castleWallLevel: Int = 0,
    val templeCount: Int = 0,
    
    // Military
    val peasantCount: Int = 10,
    val knightCount: Int = 2,
    val archerCount: Int = 3,
    val mageCount: Int = 0,
    
    // Upgrades / Research
    val cropRotation: Boolean = false,
    val steelAxes: Boolean = false,
    val masonry: Boolean = false,
    val fortification: Boolean = false,
    val chivalry: Boolean = false,
    val arcaneWisdom: Boolean = false
)

@Entity(tableName = "empire_logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val category: String, // "Turn", "Decision", "Campaign", "Advisor", "Attack"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
