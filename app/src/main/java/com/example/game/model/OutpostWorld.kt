package com.example.game.model

import com.example.game.engine3d.Vector3

enum class WorldObjectType {
    HABITAT_AIRLOCK,
    OXYGEN_GENERATOR,
    SOLAR_ARRAY,
    GREENHOUSE,
    COMMUNICATIONS_DISH,
    PLANETARY_ROVER,
    SCOUT_DRONE,
    WATER_ICE_DEPOSIT,
    MINERAL_OUTCROP,
    SPACE_LANDER,
    NPC_ASTRONAUT
}

data class InteractiveWorldObject(
    val id: String,
    val name: String,
    val type: WorldObjectType,
    val position: Vector3,
    val interactionRadius: Float = 3.5f,
    val promptText: String,
    val educationalFactId: String? = null,
    var isOperational: Boolean = true,
    var healthPercent: Float = 100f
)

enum class HazardType {
    NONE,
    SOLAR_RADIATION_FLARE,
    MARTIAN_DUST_STORM,
    METEOR_MICROMETEOROID_SWARM
}

data class EnvironmentalEvent(
    val type: HazardType = HazardType.NONE,
    val name: String = "Normal Surface Conditions",
    val description: String = "Cosmic background radiation within safe thresholds. Atmospheric visibility clear.",
    val radiationMultiplier: Float = 1.0f,
    val solarEfficiency: Float = 1.0f,
    val timeRemainingSeconds: Float = 0f,
    val isEmergencyAlert: Boolean = false
)
