package com.example.game.model

data class CharacterStats(
    val health: Float = 100f,
    val maxHealth: Float = 100f,
    val stamina: Float = 100f,
    val maxStamina: Float = 100f,
    val oxygen: Float = 100f,
    val maxOxygen: Float = 100f,
    val radiationExposure: Float = 12f, // mSv (millisieverts)
    val maxRadiation: Float = 100f,
    val suitTemperature: Float = 21.5f, // Celsius inside suit
    val suitIntegrity: Float = 100f,
    val powerCell: Float = 100f,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNextLevel: Int = 500,
    val skillPoints: Int = 2
)

data class OutpostStats(
    val colonyName: String = "Artemis Outpost Alpha",
    val powerKw: Float = 85f,
    val maxPowerKw: Float = 100f,
    val oxygenLevelPercent: Float = 94f,
    val waterReservesLiters: Float = 1450f,
    val foodStockDays: Int = 68,
    val habitabilityScore: Float = 92f,
    val activeCrewCount: Int = 5,
    val solarEfficiency: Float = 1.0f // drops during dust storms
)

data class SkillTree(
    val engineeringLevel: Int = 1,
    val scienceLevel: Int = 1,
    val survivalLevel: Int = 1,
    val explorationLevel: Int = 1
)
