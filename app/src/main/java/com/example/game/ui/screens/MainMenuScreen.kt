package com.example.game.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.CharacterProfile
import com.example.game.ui.components.CelestialPlanet
import com.example.game.ui.components.SpaceCentricBackground

@Composable
fun MainMenuScreen(
    isMars: Boolean,
    playerProfile: CharacterProfile,
    onStartNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchPlanet: (Boolean) -> Unit,
    onOpenEducational: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current selected celestial body
    var currentPlanet by remember {
        mutableStateOf(if (isMars) CelestialPlanet.MARS else CelestialPlanet.MOON)
    }

    // Keep in sync with incoming isMars
    LaunchedEffect(isMars) {
        if (isMars && currentPlanet != CelestialPlanet.MARS && currentPlanet != CelestialPlanet.SATURN) {
            currentPlanet = CelestialPlanet.MARS
        } else if (!isMars && currentPlanet != CelestialPlanet.MOON && currentPlanet != CelestialPlanet.EARTH && currentPlanet != CelestialPlanet.JUPITER) {
            currentPlanet = CelestialPlanet.MOON
        }
    }

    // Breathing glow animation for aerospace borders
    val infiniteTransition = rememberInfiniteTransition(label = "menu_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Render with deep space background with stars, nebulae, meteors, and planets
    SpaceCentricBackground(
        selectedPlanet = currentPlanet,
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
                .systemBarsPadding()
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                // Wide Landscape Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Branding, Title & Planetary Theater Selector
                    Column(
                        modifier = Modifier
                            .weight(1.18f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeaderBranding(
                            planet = currentPlanet,
                            pulseGlow = pulseGlow
                        )

                        PlanetaryTheaterSelector(
                            currentPlanet = currentPlanet,
                            onSelectPlanet = { planet ->
                                currentPlanet = planet
                                onSwitchPlanet(planet.isMarsEngine)
                            }
                        )

                        FooterTelemetryStatus()
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right Column: Action Buttons & Commander Profile Card
                    Column(
                        modifier = Modifier
                            .weight(0.92f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        CommanderProfileCard(
                            profile = playerProfile,
                            planet = currentPlanet,
                            onCustomize = onStartNewGame
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        MenuActionButtons(
                            pulseGlow = pulseGlow,
                            onStartNewGame = onStartNewGame,
                            onContinueGame = onContinueGame,
                            onOpenSettings = onOpenSettings,
                            onOpenEducational = onOpenEducational
                        )
                    }
                }
            } else {
                // Adaptive Portrait Layout with smooth scrolling
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeaderBranding(
                        planet = currentPlanet,
                        pulseGlow = pulseGlow
                    )

                    PlanetaryTheaterSelector(
                        currentPlanet = currentPlanet,
                        onSelectPlanet = { planet ->
                            currentPlanet = planet
                            onSwitchPlanet(planet.isMarsEngine)
                        }
                    )

                    CommanderProfileCard(
                        profile = playerProfile,
                        planet = currentPlanet,
                        onCustomize = onStartNewGame
                    )

                    MenuActionButtons(
                        pulseGlow = pulseGlow,
                        onStartNewGame = onStartNewGame,
                        onContinueGame = onContinueGame,
                        onOpenSettings = onOpenSettings,
                        onOpenEducational = onOpenEducational
                    )

                    FooterTelemetryStatus()
                }
            }
        }
    }
}

@Composable
private fun HeaderBranding(
    planet: CelestialPlanet,
    pulseGlow: Float
) {
    Column {
        // Aerospace Program Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x2200E5FF))
                .border(1.dp, planet.primaryColor.copy(alpha = pulseGlow), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(planet.primaryColor)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "INTERPLANETARY EXPEDITION PROGRAM",
                color = Color(0xFFE0F7FA),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title with Sci-Fi Typography
        Text(
            text = "OUTPOST",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 7.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MISSION SURVIVAL",
                color = planet.primaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.5.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "\"Every Decision Keeps Someone Alive.\"",
            color = Color(0xFFFFB703),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PlanetaryTheaterSelector(
    currentPlanet: CelestialPlanet,
    onSelectPlanet: (CelestialPlanet) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xD905101C)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = currentPlanet.primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "CELESTIAL EXPEDITION DESTINATIONS",
                        color = Color(0x9900E5FF),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = currentPlanet.outpostName,
                    color = currentPlanet.accentColor,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Planet destination pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CelestialPlanet.values().forEach { planet ->
                    val isSelected = (planet == currentPlanet)
                    PlanetTabButton(
                        planet = planet,
                        isSelected = isSelected,
                        onClick = { onSelectPlanet(planet) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Planetary Telemetry Display Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33010408))
                    .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricItem("SURFACE GRAVITY", currentPlanet.gravity)
                TelemetryMetricItem("TEMP", currentPlanet.temperature)
                TelemetryMetricItem("ATMOSPHERE", currentPlanet.atmosphere)
                TelemetryMetricItem("SOLAR DIST", currentPlanet.distanceAu)
            }
        }
    }
}

@Composable
private fun PlanetTabButton(
    planet: CelestialPlanet,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) planet.primaryColor.copy(alpha = 0.28f) else Color(0x22000000),
        animationSpec = tween(durationMillis = 300),
        label = "planetTabBg"
    )
    val border by animateColorAsState(
        targetValue = if (isSelected) planet.primaryColor else Color(0x22FFFFFF),
        animationSpec = tween(durationMillis = 300),
        label = "planetTabBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (planet) {
                    CelestialPlanet.MOON -> "MOON"
                    CelestialPlanet.MARS -> "MARS"
                    CelestialPlanet.EARTH -> "EARTH"
                    CelestialPlanet.JUPITER -> "JUPITER"
                    CelestialPlanet.SATURN -> "SATURN"
                },
                color = if (isSelected) planet.primaryColor else Color(0xCCFFFFFF),
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TelemetryMetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color(0x77FFFFFF),
            fontSize = 6.5.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color(0xFFE0F7FA),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CommanderProfileCard(
    profile: CharacterProfile,
    planet: CelestialPlanet,
    onCustomize: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC05101A)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2A00E5FF), RoundedCornerShape(10.dp))
            .clickable(onClick = onCustomize)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(profile.suitColor))
                        .border(1.5.dp, planet.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = profile.name.ifEmpty { "COMMANDER" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${profile.role.title} • ${profile.role.abilityName}",
                        color = planet.primaryColor,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x2200E5FF))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EDIT SUIT",
                    color = Color(0xFF80DEEA),
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MenuActionButtons(
    pulseGlow: Float,
    onStartNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEducational: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedMenuButton(
            title = "ENTER EXPEDITION (PLAY NOW)",
            subtitle = "Deploy Directly to Surface Outpost Simulation",
            icon = Icons.Default.RocketLaunch,
            testTag = "menu_continue_button",
            isPrimary = true,
            pulseAlpha = pulseGlow,
            onClick = onContinueGame
        )

        AnimatedMenuButton(
            title = "CUSTOMIZE ASTRONAUT & DOSSIER",
            subtitle = "Role Specialization, Suit Color & Equipment",
            icon = Icons.Default.Person,
            testTag = "menu_new_game_button",
            isPrimary = false,
            onClick = onStartNewGame
        )

        AnimatedMenuButton(
            title = "FLIGHT SYSTEMS & SETTINGS",
            subtitle = "3D Graphics, Field of View, Sensors & Audio",
            icon = Icons.Default.Settings,
            testTag = "menu_settings_button",
            isPrimary = false,
            onClick = onOpenSettings
        )

        AnimatedMenuButton(
            title = "RESEARCH SCIENCE ARCHIVES",
            subtitle = "ISRU, Sabatier Reactors, CELSS Hydroponics",
            icon = Icons.Default.AutoStories,
            testTag = "menu_archives_button",
            isPrimary = false,
            onClick = onOpenEducational
        )
    }
}

@Composable
private fun FooterTelemetryStatus() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E676))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "3D Low-Gravity Physical Engine • Jetpack Compose 60 FPS • Telemetry Nominal",
            color = Color(0x77FFFFFF),
            fontSize = 7.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AnimatedMenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    isPrimary: Boolean,
    pulseAlpha: Float = 0f,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) Color(0xFF00E5FF) else Color(0xD9061626),
            contentColor = if (isPrimary) Color(0xFF001926) else Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(scale)
            .testTag(testTag)
            .border(
                width = if (isPrimary) 1.5.dp else 1.dp,
                color = if (isPrimary) Color.White.copy(alpha = 0.5f + pulseAlpha * 0.5f) else Color(0x3300E5FF),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPrimary) Color(0x22001926) else Color(0x2200E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color(0xFF001926) else Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 7.5.sp,
                    color = if (isPrimary) Color(0xCC002B3D) else Color(0x88FFFFFF)
                )
            }
        }
    }
}
