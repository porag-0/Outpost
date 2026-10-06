package com.example.game.model

enum class EquipmentSlot(val label: String) {
    PRIMARY_TOOL("Primary Tool"),
    SECONDARY_TOOL("Secondary Tool"),
    SUIT("Extravehicular Suit"),
    HELMET("Pressurized Helmet"),
    BACKPACK("PLSS Backpack"),
    UTILITY("Utility Pouch"),
    SCANNER("Sensor / Scanner"),
    OXYGEN("Oxygen Tank"),
    POWER("Power Unit"),
    SPECIAL_ITEM("Special Device")
}

data class EquipmentItem(
    val id: String,
    val name: String,
    val slot: EquipmentSlot,
    val weightKg: Float,
    val durabilityPercent: Float = 100f,
    val description: String,
    val level: Int = 1,
    val maxLevel: Int = 3,
    val statBoost: String,
    val isEquipped: Boolean = false
)

data class RoverConfig(
    val typeName: String = "Lunar Exploration Rover LER-4",
    val speedKmh: Float = 24f,
    val batteryPercent: Float = 96f,
    val cargoCapacityKg: Float = 350f,
    val currentCargoKg: Float = 45f,
    val healthPercent: Float = 100f,
    val headlightActive: Boolean = true,
    val scannerRangeMeters: Float = 120f
)

data class DroneConfig(
    val name: String = "AeroScout Recon Quad",
    val batteryPercent: Float = 100f,
    val maxFlightTimeSec: Float = 180f,
    val altitudeMeters: Float = 15f,
    val maxAltitudeMeters: Float = 40f,
    val signalStrengthPercent: Float = 98f,
    val activeSensors: List<String> = listOf("LiDAR Topography", "Thermal Camera", "Atmospheric Spectrometer")
)
