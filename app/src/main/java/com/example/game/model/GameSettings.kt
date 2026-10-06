package com.example.game.model

data class GameSettings(
    // 1. Graphics & Visual Fidelity
    val graphicsPreset: String = "High", // "Low", "Medium", "High", "Ultra"
    val fieldOfView: Float = 56.0f, // 45° to 75°
    val showLensFlares: Boolean = true,
    val showGroundShadows: Boolean = true,
    val depthFogEnabled: Boolean = true,
    val particleDensity: String = "High", // "Low", "Medium", "High", "Ultra"
    val wireframePanelHighlight: Boolean = true,
    val cinematicPostProcessing: Boolean = true,
    val targetFramerate: String = "60 FPS", // "30 FPS", "60 FPS", "Max Unlocked"
    val renderResolution: String = "100%", // "75% Performance", "100% Balanced", "125% Crisp"

    // 2. Controls & Camera Dynamics
    val cameraSensitivity: Float = 1.0f, // 0.5x to 2.5x
    val cameraDistance: Float = 5.2f, // 3.5m to 7.5m
    val invertYAxis: Boolean = false,
    val joystickSize: Int = 120, // 95 (Compact), 120 (Standard), 145 (Large)
    val autoSprint: Boolean = false,
    val vibrationHaptics: Boolean = true,
    val gyroscopeAimAssist: Boolean = false,

    // 3. Audio & Comms Telemetry
    val masterVolume: Float = 0.85f, // 0.0 to 1.0
    val ambientVolume: Float = 0.80f,
    val sfxEnabled: Boolean = true,
    val radioChirpsEnabled: Boolean = true,
    val respiratorSoundEnabled: Boolean = true,
    val hazardAlarmSirenEnabled: Boolean = true,

    // 4. Survival Simulation & Environmental Physics
    val survivalPacing: String = "Standard", // "Explorer (Relaxed)", "Standard", "Hardcore (Realistic)"
    val lunarGravityMultiplier: Float = 1.0f, // 1.0x (Authentic), 1.4x (Cinematic Leap), 1.8x (Low-G Hop)
    val unitsSystem: String = "Metric", // "Metric", "Imperial"
    val oxygenDepletionRate: String = "Standard", // "Gentle", "Standard", "Harsh"
    val radiationExposure: Boolean = true,

    // 5. Interface, HUD & Optics
    val automaticScienceAlerts: Boolean = true,
    val minimalHudMode: Boolean = false,
    val showCompassBar: Boolean = true,
    val telemetryColorTheme: String = "Cyber Cyan", // "Cyber Cyan", "Solar Gold", "Emerald Bio", "Neon Violet"
    val hudOpacity: Float = 0.90f
)
