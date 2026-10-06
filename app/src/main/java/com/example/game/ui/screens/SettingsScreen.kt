package com.example.game.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameSettings
import com.example.game.ui.components.CelestialPlanet
import com.example.game.ui.components.SpaceCentricBackground

@Composable
fun SettingsScreen(
    currentSettings: GameSettings,
    onSaveSettings: (GameSettings) -> Unit,
    onClose: () -> Unit,
    onReplenishResources: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var settings by remember { mutableStateOf(currentSettings) }
    var activeCategory by remember { mutableIntStateOf(0) }

    val categories = listOf(
        Pair("GRAPHICS", Icons.Default.Tune),
        Pair("CONTROLS", Icons.Default.SportsEsports),
        Pair("AUDIO", Icons.Default.VolumeUp),
        Pair("SIMULATION", Icons.Default.Science),
        Pair("INTERFACE", Icons.Default.Dashboard),
        Pair("OVERRIDE", Icons.Default.Build)
    )

    SpaceCentricBackground(
        selectedPlanet = CelestialPlanet.EARTH,
        planetScale = 0.75f,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
                .systemBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xF207121E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3300E5FF))
                                .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "EXPEDITION HARDWARE & SIMULATOR SETTINGS",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Interplanetary Colony Simulator • System Configuration & Calibration",
                                color = Color(0xAAFFFFFF),
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val defaults = GameSettings()
                                settings = defaults
                                onSaveSettings(defaults)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB300)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFB300)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("reset_defaults_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DEFAULT PRESETS", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("settings_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Landscape Split Layout: Left Category Rail, Right Detailed Options Column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Navigation Rail
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xD9060E18)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(170.dp)
                            .fillMaxHeight()
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SYSTEM CATEGORIES",
                                color = Color(0x8800E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            categories.forEachIndexed { index, (label, icon) ->
                                val isSelected = activeCategory == index
                                val animatedBg by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF00E5FF) else Color(0x1A00E5FF),
                                    animationSpec = tween(250),
                                    label = "catBg"
                                )
                                val animatedContentColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF001726) else Color.White,
                                    animationSpec = tween(250),
                                    label = "catText"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(animatedBg)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.White else Color(0x2200E5FF),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { activeCategory = index }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF001726) else Color(0xFF80DEEA),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            color = animatedContentColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right Content Area with Smooth Animation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AnimatedContent(
                            targetState = activeCategory,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(200)) { it / 8 })
                                    .togetherWith(fadeOut(animationSpec = tween(150)))
                            },
                            label = "settingsCatContent"
                        ) { targetCat ->
                            when (targetCat) {
                                0 -> GraphicsSettingsTab(settings) {
                                    settings = it
                                    onSaveSettings(it)
                                }
                                1 -> ControlsSettingsTab(settings) {
                                    settings = it
                                    onSaveSettings(it)
                                }
                                2 -> AudioSettingsTab(settings) {
                                    settings = it
                                    onSaveSettings(it)
                                }
                                3 -> SimulationSettingsTab(settings) {
                                    settings = it
                                    onSaveSettings(it)
                                }
                                4 -> InterfaceSettingsTab(settings) {
                                    settings = it
                                    onSaveSettings(it)
                                }
                                5 -> MaintenanceTab(
                                    onReplenishResources = onReplenishResources,
                                    onResetSettings = {
                                        val defaults = GameSettings()
                                        settings = defaults
                                        onSaveSettings(defaults)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// -------------------------------------------------------------
// 1. GRAPHICS TAB
// -------------------------------------------------------------
@Composable
private fun GraphicsSettingsTab(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Preset
        item {
            SettingsCard(
                title = "GRAPHICS QUALITY PRESET",
                badge = settings.graphicsPreset.uppercase(),
                description = "Automatically scales texture detail, polygon resolution, and particle density."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Low", "Medium", "High", "Ultra").forEach { preset ->
                        SegmentedOption(
                            text = preset,
                            isSelected = settings.graphicsPreset == preset,
                            modifier = Modifier.weight(1f)
                        ) {
                            val newFov = when (preset) {
                                "Low" -> 50.0f
                                "Medium" -> 54.0f
                                "High" -> 56.0f
                                "Ultra" -> 60.0f
                                else -> settings.fieldOfView
                            }
                            onUpdate(
                                settings.copy(
                                    graphicsPreset = preset,
                                    particleDensity = if (preset == "Low") "Low" else if (preset == "Medium") "Medium" else "High",
                                    showLensFlares = preset != "Low",
                                    showGroundShadows = preset != "Low",
                                    fieldOfView = newFov
                                )
                            )
                        }
                    }
                }
            }
        }

        // Resolution & Framerate
        item {
            SettingsCard(
                title = "DISPLAY RESOLUTION & FRAMERATE CAP",
                badge = "${settings.renderResolution} • ${settings.targetFramerate}",
                description = "Tune rendering scale and target framerate for optimal smoothness and thermal efficiency."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("Render Scale Resolution", color = Color(0xFF80DEEA), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("75% Performance", "100% Balanced", "125% Crisp").forEach { res ->
                                SegmentedOption(
                                    text = res,
                                    isSelected = settings.renderResolution == res,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onUpdate(settings.copy(renderResolution = res))
                                }
                            }
                        }
                    }

                    Column {
                        Text("Target Framerate Cap", color = Color(0xFF80DEEA), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("30 FPS", "60 FPS", "Max Unlocked").forEach { fps ->
                                SegmentedOption(
                                    text = fps,
                                    isSelected = settings.targetFramerate == fps,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onUpdate(settings.copy(targetFramerate = fps))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Camera FOV
        item {
            SettingsCard(
                title = "FIELD OF VIEW (FOV)",
                badge = "${settings.fieldOfView.toInt()}°",
                description = "Adjust camera optical angle. Higher FOV widens peripheral awareness of alien landscapes."
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Narrow (45°)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                        Text("Current: ${settings.fieldOfView.toInt()}°", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Wide (75°)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                    }
                    Slider(
                        value = settings.fieldOfView,
                        onValueChange = { onUpdate(settings.copy(fieldOfView = it)) },
                        valueRange = 45f..75f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x3300E5FF)
                        )
                    )
                }
            }
        }

        // Visual Toggles
        item {
            SettingsCard(
                title = "OPTICAL & ATMOSPHERIC EFFECTS",
                badge = "SHADERS",
                description = "Enable or disable individual GPU-accelerated rendering effects."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggleRow(
                        title = "Solar Anamorphic Lens Flare & Glare",
                        description = "Horizontal glare streaks and bright specular flares when facing the Sun",
                        checked = settings.showLensFlares,
                        onCheckedChange = { onUpdate(settings.copy(showLensFlares = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Regolith Ground Contact Shadows",
                        description = "Projects soft directional contact shadows underneath the astronaut and rover",
                        checked = settings.showGroundShadows,
                        onCheckedChange = { onUpdate(settings.copy(showGroundShadows = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Atmospheric Depth Fog & Dust Haze",
                        description = "Graduated distance fog for horizon depth during Martian sandstorms",
                        checked = settings.depthFogEnabled,
                        onCheckedChange = { onUpdate(settings.copy(depthFogEnabled = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Holographic Mesh Edge Wireframe",
                        description = "Futuristic neon wireframe lines on habitat structural polygons",
                        checked = settings.wireframePanelHighlight,
                        onCheckedChange = { onUpdate(settings.copy(wireframePanelHighlight = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Visor Vignette & Cinematic Contrast",
                        description = "Spherical helmet glass vignette shading along screen borders",
                        checked = settings.cinematicPostProcessing,
                        onCheckedChange = { onUpdate(settings.copy(cinematicPostProcessing = it)) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. CONTROLS TAB
// -------------------------------------------------------------
@Composable
private fun ControlsSettingsTab(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Camera Sensitivity
        item {
            SettingsCard(
                title = "CAMERA LOOK SENSITIVITY",
                badge = "${String.format("%.1f", settings.cameraSensitivity)}x",
                description = "Drag sensitivity multiplier when rotating the third-person viewport."
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slow (0.5x)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                        Text("Current: ${String.format("%.1f", settings.cameraSensitivity)}x", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Fast (2.5x)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                    }
                    Slider(
                        value = settings.cameraSensitivity,
                        onValueChange = { onUpdate(settings.copy(cameraSensitivity = it)) },
                        valueRange = 0.5f..2.5f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x3300E5FF)
                        )
                    )
                }
            }
        }

        // Camera Distance
        item {
            SettingsCard(
                title = "CAMERA ORBIT DISTANCE",
                badge = "${String.format("%.1f", settings.cameraDistance)} METERS",
                description = "Adjust third-person chase camera distance from the astronaut."
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Close Over-the-Shoulder (3.5m)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                        Text("Wide Horizon (7.5m)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                    }
                    Slider(
                        value = settings.cameraDistance,
                        onValueChange = { onUpdate(settings.copy(cameraDistance = it)) },
                        valueRange = 3.5f..7.5f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x3300E5FF)
                        )
                    )
                }
            }
        }

        // Joystick Size
        item {
            SettingsCard(
                title = "VIRTUAL JOYSTICK PHYSICAL SIZE",
                badge = "${settings.joystickSize} DP",
                description = "Adjust screen area of touch joystick for ergonomic thumb comfort."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("Compact (95dp)", 95),
                        Pair("Standard (120dp)", 120),
                        Pair("Expanded (145dp)", 145)
                    ).forEach { (label, size) ->
                        SegmentedOption(
                            text = label,
                            isSelected = settings.joystickSize == size,
                            modifier = Modifier.weight(1f)
                        ) {
                            onUpdate(settings.copy(joystickSize = size))
                        }
                    }
                }
            }
        }

        // Input & Haptics Toggles
        item {
            SettingsCard(
                title = "INPUT MODIFIERS & HAPTICS",
                badge = "KINEMATICS",
                description = "Configure movement behavior, inversion, and haptic vibration feedback."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggleRow(
                        title = "Invert Camera Pitch (Y-Axis)",
                        description = "Pull down to look up, standard flight simulator stick configuration",
                        checked = settings.invertYAxis,
                        onCheckedChange = { onUpdate(settings.copy(invertYAxis = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Always Auto-Sprint",
                        description = "Automatically engages sprint thrusters without needing double-tap",
                        checked = settings.autoSprint,
                        onCheckedChange = { onUpdate(settings.copy(autoSprint = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Tactile Haptic Vibration Feedback",
                        description = "Subtle tactile clicks on terminal interaction, low oxygen pulses, and airlock seals",
                        checked = settings.vibrationHaptics,
                        onCheckedChange = { onUpdate(settings.copy(vibrationHaptics = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Gyroscope Precision Aim Assist",
                        description = "Tilt device slightly for fine camera adjustment while aiming tools",
                        checked = settings.gyroscopeAimAssist,
                        onCheckedChange = { onUpdate(settings.copy(gyroscopeAimAssist = it)) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. AUDIO TAB
// -------------------------------------------------------------
@Composable
private fun AudioSettingsTab(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Master Volume
        item {
            SettingsCard(
                title = "MASTER AUDIO GAIN",
                badge = "${(settings.masterVolume * 100).toInt()}%",
                description = "Primary volume attenuation across all synthesizers, thrusters, and comms."
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mute (0%)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                        Text("Current: ${(settings.masterVolume * 100).toInt()}%", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Max (100%)", color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                    }
                    Slider(
                        value = settings.masterVolume,
                        onValueChange = { onUpdate(settings.copy(masterVolume = it)) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x3300E5FF)
                        )
                    )
                }
            }
        }

        // Ambient Wind & Planetary Drone
        item {
            SettingsCard(
                title = "AMBIENT PLANETARY SOUNDSCAPE",
                badge = "${(settings.ambientVolume * 100).toInt()}%",
                description = "Volume level of alien wind howls, low-frequency habitat hum, and cosmic radiation static."
            ) {
                Slider(
                    value = settings.ambientVolume,
                    onValueChange = { onUpdate(settings.copy(ambientVolume = it)) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x3300E5FF)
                    )
                )
            }
        }

        // Subsystem Audio Channels
        item {
            SettingsCard(
                title = "TELEMETRY & SPACE ACOUSTIC CHANNELS",
                badge = "CHANNELS",
                description = "Toggle specific procedural acoustic layers for extravehicular immersion."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggleRow(
                        title = "Sound Effects & Mechanical Footsteps",
                        description = "Airlock decompression hisses, rover electrical drive motors, and surface regolith footsteps",
                        checked = settings.sfxEnabled,
                        onCheckedChange = { onUpdate(settings.copy(sfxEnabled = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "EVA Suit Respirator & Breathing",
                        description = "Simulates authentic extravehicular pressurized helmet breathing and airflow",
                        checked = settings.respiratorSoundEnabled,
                        onCheckedChange = { onUpdate(settings.copy(respiratorSoundEnabled = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Sub-Orbital Radio Telemetry & Comm Chirps",
                        description = "Surface relay telemetry beeps and radio squelch tones during communication",
                        checked = settings.radioChirpsEnabled,
                        onCheckedChange = { onUpdate(settings.copy(radioChirpsEnabled = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Hazard Emergency Alarm Horns",
                        description = "Klaxon warnings during solar radiation events and critical oxygen depletions",
                        checked = settings.hazardAlarmSirenEnabled,
                        onCheckedChange = { onUpdate(settings.copy(hazardAlarmSirenEnabled = it)) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SIMULATION TAB
// -------------------------------------------------------------
@Composable
private fun SimulationSettingsTab(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Survival Pacing
        item {
            SettingsCard(
                title = "SURVIVAL DIFFICULTY PACING",
                badge = settings.survivalPacing.uppercase(),
                description = "Governs oxygen consumption rate, suit radiation exposure, and life-support resource consumption."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("Explorer (Relaxed)", "Explorer (Relaxed)"),
                        Pair("Standard RPG", "Standard"),
                        Pair("Hardcore (Realistic)", "Hardcore (Realistic)")
                    ).forEach { (display, id) ->
                        SegmentedOption(
                            text = display,
                            isSelected = settings.survivalPacing == id,
                            modifier = Modifier.weight(1f)
                        ) {
                            onUpdate(settings.copy(survivalPacing = id))
                        }
                    }
                }
            }
        }

        // Gravity Physics
        item {
            SettingsCard(
                title = "LOW-GRAVITY KINEMATICS PHYSICS",
                badge = when (settings.lunarGravityMultiplier) {
                    1.0f -> "1.0x AUTHENTIC"
                    1.4f -> "1.4x CINEMATIC"
                    else -> "1.8x LOW-G HOP"
                },
                description = "Tuning astronaut leap velocity and airborne suspension time during vertical traversal."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("Authentic (1.0x)", 1.0f),
                        Pair("Cinematic Leap (1.4x)", 1.4f),
                        Pair("Low-G Float (1.8x)", 1.8f)
                    ).forEach { (label, mult) ->
                        SegmentedOption(
                            text = label,
                            isSelected = settings.lunarGravityMultiplier == mult,
                            modifier = Modifier.weight(1f)
                        ) {
                            onUpdate(settings.copy(lunarGravityMultiplier = mult))
                        }
                    }
                }
            }
        }

        // Oxygen & Environmental Hazard Toggles
        item {
            SettingsCard(
                title = "ENVIRONMENTAL HAZARDS & OXYGEN DEPLETION",
                badge = "BIOMETRICS",
                description = "Configure physiological stress and harsh celestial conditions."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text("Oxygen Depletion Rate", color = Color(0xFF80DEEA), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Gentle", "Standard", "Harsh").forEach { rate ->
                                SegmentedOption(
                                    text = rate,
                                    isSelected = settings.oxygenDepletionRate == rate,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onUpdate(settings.copy(oxygenDepletionRate = rate))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Solar Radiation & Cosmic Ray Exposure",
                        description = "Astronaut incurs radiation damage during solar flare events outside bunker",
                        checked = settings.radiationExposure,
                        onCheckedChange = { onUpdate(settings.copy(radiationExposure = it)) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. INTERFACE & HUD TAB
// -------------------------------------------------------------
@Composable
private fun InterfaceSettingsTab(settings: GameSettings, onUpdate: (GameSettings) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Color Palette Theme
        item {
            SettingsCard(
                title = "HUD COLOR PALETTE ACCENT",
                badge = settings.telemetryColorTheme.uppercase(),
                description = "Customize primary holographic color accents across telemetry gauges and HUD overlays."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("Cyber Cyan", Color(0xFF00E5FF)),
                        Pair("Solar Gold", Color(0xFFFFB300)),
                        Pair("Emerald Bio", Color(0xFF00E676)),
                        Pair("Neon Violet", Color(0xFFB388FF))
                    ).forEach { (themeName, colorSwatch) ->
                        val isSelected = settings.telemetryColorTheme == themeName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colorSwatch.copy(alpha = 0.25f) else Color(0x22000000))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colorSwatch else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdate(settings.copy(telemetryColorTheme = themeName)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colorSwatch)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = themeName,
                                    color = if (isSelected) Color.White else Color(0xBBFFFFFF),
                                    fontSize = 8.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Units System
        item {
            SettingsCard(
                title = "MEASUREMENT TELEMETRY SYSTEM",
                badge = settings.unitsSystem.uppercase(),
                description = "Choose display units for temperature, barometric pressure, velocity, and distance."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Metric", "Imperial").forEach { sys ->
                        SegmentedOption(
                            text = if (sys == "Metric") "Metric (meters, kPa, °C)" else "Imperial (feet, PSI, °F)",
                            isSelected = settings.unitsSystem == sys,
                            modifier = Modifier.weight(1f)
                        ) {
                            onUpdate(settings.copy(unitsSystem = sys))
                        }
                    }
                }
            }
        }

        // HUD Opacity Slider
        item {
            SettingsCard(
                title = "HUD HOLOGRAM OPACITY",
                badge = "${(settings.hudOpacity * 100).toInt()}%",
                description = "Adjust alpha transparency of on-screen gauges and status readouts."
            ) {
                Slider(
                    value = settings.hudOpacity,
                    onValueChange = { onUpdate(settings.copy(hudOpacity = it)) },
                    valueRange = 0.5f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x3300E5FF)
                    )
                )
            }
        }

        // Alerts & Minimal HUD
        item {
            SettingsCard(
                title = "HEADS-UP DISPLAY (HUD) & MISSION ALERTS",
                badge = "INTERFACE",
                description = "Configure on-screen telemetry presentation and educational prompt popups."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggleRow(
                        title = "Automatic Science Discovery Alerts",
                        description = "Displays educational aerospace cards when approaching Sabatier or Kilopower modules",
                        checked = settings.automaticScienceAlerts,
                        onCheckedChange = { onUpdate(settings.copy(automaticScienceAlerts = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Top Compass Navigation Bar",
                        description = "Shows real-time azimuth heading and outpost module beacon markers",
                        checked = settings.showCompassBar,
                        onCheckedChange = { onUpdate(settings.copy(showCompassBar = it)) }
                    )
                    HorizontalDivider(color = Color(0x2200E5FF))
                    SettingToggleRow(
                        title = "Minimalist Cinematic HUD Mode",
                        description = "Hides secondary vitals meters during walking for an uncluttered cinematic visual experience",
                        checked = settings.minimalHudMode,
                        onCheckedChange = { onUpdate(settings.copy(minimalHudMode = it)) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. MAINTENANCE TAB
// -------------------------------------------------------------
@Composable
private fun MaintenanceTab(
    onReplenishResources: (() -> Unit)?,
    onResetSettings: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsCard(
                title = "EMERGENCY OUTPOST RESOURCE OVERRIDE",
                badge = "EMERGENCY PROTOCOL",
                description = "Instantly replenishes Outpost Alpha power grid, oxygen reserves, water storage, and astronaut vitals to 100% capacity."
            ) {
                Button(
                    onClick = { onReplenishResources?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF001A09)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("replenish_resources_button")
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REPLENISH OUTPOST VITALS & OXYGEN (100%)", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
                }
            }
        }

        item {
            SettingsCard(
                title = "FACTORY DEFAULTS RESTORATION",
                badge = "CALIBRATION",
                description = "Restores all graphics presets, field of view, audio balance, and control sensitivities to recommended expedition defaults."
            ) {
                Button(
                    onClick = onResetSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color(0xFF211500)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("factory_reset_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTORE RECOMMENDED BASELINE SETTINGS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE SETTINGS COMPONENTS (Clean, Separate, Polished)
// -------------------------------------------------------------
@Composable
private fun SettingsCard(
    title: String,
    badge: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xD9061322)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x3300E5FF))
                        .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color(0xFF80DEEA),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = description,
                color = Color(0x99FFFFFF),
                fontSize = 8.5.sp,
                lineHeight = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            content()
        }
    }
}

@Composable
private fun SegmentedOption(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00E5FF) else Color(0x22000000),
        animationSpec = tween(200),
        label = "segmentBg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF001A26) else Color.White,
        animationSpec = tween(200),
        label = "segmentText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBg)
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF00E5FF) else Color(0x2AFFFFFF),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = animatedTextColor,
            fontSize = 8.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color(0x88FFFFFF),
                fontSize = 8.sp,
                lineHeight = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0x5500E5FF),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0x33000000)
            ),
            modifier = Modifier.scale(0.80f)
        )
    }
}
