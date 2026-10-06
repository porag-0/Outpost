package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.game.model.CharacterStats
import com.example.game.model.EnvironmentalEvent
import com.example.game.model.Mission
import com.example.game.model.OutpostStats

@Composable
fun LandscapeTopHud(
    mission: Mission?,
    outpostStats: OutpostStats,
    onOpenMap: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenPhotoMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onLearnMore: () -> Unit,
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 650.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Mission Objective Card
                if (mission != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC05101A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mission.title.uppercase(),
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "+${mission.xpReward} XP",
                                    color = Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = mission.subtitle,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            mission.objectives.firstOrNull { !it.isCompleted }?.let { obj ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = obj.description,
                                        color = Color(0xFFE0F7FA),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Center: Outpost Global Vitals Telemetry Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xD9050F18))
                        .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF00E676)))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = outpostStats.colonyName.uppercase(),
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    ResourcePill(Icons.Default.Bolt, "${outpostStats.powerKw.toInt()} kW", Color(0xFFFFD54F))
                    ResourcePill(Icons.Default.Air, "${outpostStats.oxygenLevelPercent.toInt()}% O₂", Color(0xFF4FC3F7))
                    ResourcePill(Icons.Default.WaterDrop, "${outpostStats.waterReservesLiters.toInt()}L H₂O", Color(0xFF29B6F6))
                    ResourcePill(Icons.Default.Grass, "${outpostStats.foodStockDays}D Food", Color(0xFF81C784))
                }

                // Right: Fast HUD Icons (Map, Loadout, Science, Photo, Settings, Menu)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xD9050F18))
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HudIconButton(Icons.Default.Map, "Tactical Map", "hud_map_button", onOpenMap)
                    HudIconButton(Icons.Default.Backpack, "Loadout", "hud_inventory_button", onOpenInventory)
                    HudIconButton(Icons.Default.MenuBook, "Science Archives", "hud_learn_button", onLearnMore)
                    HudIconButton(Icons.Default.PhotoCamera, "Photo Mode", "hud_photo_button", onOpenPhotoMode)
                    HudIconButton(Icons.Default.Settings, "Expedition Settings", "hud_settings_button", onOpenSettings)
                    HudIconButton(Icons.Default.Menu, "Mission HQ", "hud_menu_button", onOpenMenu)
                }
            }
        } else {
            // Compact / Portrait layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xD9050F18))
                            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = outpostStats.colonyName.uppercase(),
                            color = Color(0xFF00E5FF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        ResourcePill(Icons.Default.Bolt, "${outpostStats.powerKw.toInt()} kW", Color(0xFFFFD54F))
                        ResourcePill(Icons.Default.Air, "${outpostStats.oxygenLevelPercent.toInt()}%", Color(0xFF4FC3F7))
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xD9050F18))
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        HudIconButton(Icons.Default.Map, "Map", "hud_map_button", onOpenMap)
                        HudIconButton(Icons.Default.Backpack, "Inventory", "hud_inventory_button", onOpenInventory)
                        HudIconButton(Icons.Default.Settings, "Settings", "hud_settings_button", onOpenSettings)
                        HudIconButton(Icons.Default.Menu, "Menu", "hud_menu_button", onOpenMenu)
                    }
                }

                if (mission != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC05101A)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mission.title,
                                color = Color(0xFFFFB300),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            mission.objectives.firstOrNull { !it.isCompleted }?.let { obj ->
                                Text(
                                    text = obj.description,
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcePill(icon: ImageVector, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun HudIconButton(icon: ImageVector, contentDesc: String, testTag: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(),
        label = "hudBtnScale"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scaleFactor)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x3300E5FF))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDesc, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun LandscapeVitalGauges(
    stats: CharacterStats,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xD9050E17)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Health
            GaugeRow("VITALS", stats.health, stats.maxHealth, Color(0xFFE53935), Icons.Default.Favorite)
            // Stamina
            GaugeRow("STAMINA", stats.stamina, stats.maxStamina, Color(0xFFFFB300), Icons.Default.DirectionsRun)
            // Oxygen
            GaugeRow("OXYGEN", stats.oxygen, stats.maxOxygen, if (stats.oxygen < 20f) Color(0xFFFF1744) else Color(0xFF00E5FF), Icons.Default.Air)

            Row(
                modifier = Modifier.width(115.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "RAD: ${stats.radiationExposure.toInt()} mSv", color = Color(0xFFFF7043), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(text = "TEMP: ${stats.suitTemperature}°C", color = Color(0xFF81D4FA), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun GaugeRow(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color,
    icon: ImageVector
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
        label = "gauge"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(115.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, color = Color(0xAAFFFFFF), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                Text(text = "${value.toInt()}%", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = color,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

@Composable
fun LandscapeActionControls(
    isGrounded: Boolean,
    isSprinting: Boolean,
    isCrouched: Boolean,
    scannerActive: Boolean,
    flashlightActive: Boolean,
    interactionPrompt: String?,
    onJump: () -> Unit,
    onToggleSprint: () -> Unit,
    onToggleCrouch: () -> Unit,
    onToggleScanner: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Prominent INTERACT button with smooth slide-in and breathing pulse
        AnimatedVisibility(
            visible = interactionPrompt != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { h: Int -> h / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { h: Int -> h / 2 })
        ) {
            if (interactionPrompt != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "interactGlow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow"
                )

                Button(
                    onClick = onInteract,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF001A26)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .testTag("action_interact_button")
                        .border(1.5.dp, Color.White.copy(alpha = glowAlpha), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = interactionPrompt.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action cluster
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundActionButton(Icons.Default.Radar, "SCAN", scannerActive, Color(0xFF00E5FF), "action_scanner_button", onToggleScanner)
            RoundActionButton(Icons.Default.Highlight, "LIGHT", flashlightActive, Color(0xFFFFD54F), "action_flashlight_button", onToggleFlashlight)
            RoundActionButton(Icons.Default.AirlineSeatReclineNormal, "CROUCH", isCrouched, Color(0xFF90A4AE), "action_crouch_button", onToggleCrouch)
            RoundActionButton(Icons.Default.Speed, "SPRINT", isSprinting, Color(0xFFFF7043), "action_sprint_button", onToggleSprint)

            // Jump button (Floaty leap)
            var jumpPressed by remember { mutableStateOf(false) }
            val jumpScale by animateFloatAsState(
                targetValue = if (jumpPressed) 0.92f else 1.0f,
                animationSpec = spring(),
                label = "jumpScale"
            )

            Button(
                onClick = onJump,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC0288D1), contentColor = Color.White),
                shape = CircleShape,
                modifier = Modifier
                    .size(54.dp)
                    .scale(jumpScale)
                    .testTag("action_jump_button")
                    .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Jump", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun RoundActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val animatedBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) activeColor else Color(0xAA0B1F33),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
        label = "btnBg"
    )
    val animatedBorder by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) Color.White else Color(0x4400E5FF),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
        label = "btnBorder"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedBg,
            contentColor = if (isActive) Color(0xFF001524) else Color.White
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(44.dp)
            .testTag(testTag)
            .border(1.dp, animatedBorder, CircleShape)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun EnvironmentalHazardBanner(
    event: EnvironmentalEvent,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "hazardPulse")
    val hazardAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "hazard"
    )

    AnimatedVisibility(
        visible = event.isEmergencyAlert,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xEE8A0E0E)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(2.dp, Color(0xFFFF5252).copy(alpha = hazardAlpha), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EMERGENCY ALERT: ${event.name.uppercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = event.description,
                        color = Color(0xFFFFCDD2),
                        fontSize = 9.sp
                    )
                }
                TextButton(onClick = onClear) {
                    Text("SHELTERED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ToastNotificationBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (message != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE002233))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
