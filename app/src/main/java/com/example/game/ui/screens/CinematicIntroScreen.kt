package com.example.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.foundation.clickable
import com.example.game.ui.components.CelestialPlanet
import com.example.game.ui.components.SpaceCentricBackground

@Composable
fun CinematicIntroScreen(
    isMars: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stepIndex by remember { mutableIntStateOf(0) }

    val celestialPlanet = if (isMars) CelestialPlanet.MARS else CelestialPlanet.MOON
    val celestialName = if (isMars) "Mars" else "the Moon"
    val outpostName = if (isMars) "Ares Outpost Alpha" else "Shackleton Lunar Base"

    val scriptLines = remember {
        listOf(
            Pair("ORBITAL FLIGHT CONTROL", "Outpost One, this is Surface Flight Director. You are on nominal approach trajectory to $celestialName. Descent burn initiated."),
            Pair("LANDER TELEMETRY", "Radar altimeter active... 1,000 meters... 400 meters... Terminal deceleration thrusters firing at 80% throttle."),
            Pair("PILOT LOG", "Regolith dust cloud billowing across exterior sensors. Contact light verified. Engine shutdown nominal. Welcome to $outpostName."),
            Pair("AIRLOCK HYDRAULICS", "Equalizing pressure chamber... 101.3 kPa nominal... Exterior hatch unsealed. Step out onto the surface, astronaut."),
            Pair("COMMANDER SARAH VANCE", "\"Welcome to your new home. Every decision we make out here keeps someone alive. Let's make history.\"")
        )
    }

    LaunchedEffect(stepIndex) {
        if (stepIndex < scriptLines.size) {
            delay(2000)
            stepIndex++
        } else {
            delay(800)
            onComplete()
        }
    }

    SpaceCentricBackground(
        selectedPlanet = celestialPlanet,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (stepIndex < scriptLines.size - 1) {
                        stepIndex++
                    } else {
                        onComplete()
                    }
                }
                .displayCutoutPadding()
                .systemBarsPadding()
                .padding(24.dp)
        ) {
            // Prominent Instant Launch Button top right
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF001926)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("cinematic_skip_button")
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ENTER SURFACE (SKIP)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

        // Center Radio Log & Dialogue
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (stepIndex < scriptLines.size) {
                val current = scriptLines[stepIndex]

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC05101A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (stepIndex < scriptLines.size - 1) {
                                stepIndex++
                            } else {
                                onComplete()
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = current.first,
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = current.second,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onComplete,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color(0xFF001926)
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("DEPLOY TO SURFACE NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }

                            Text(
                                text = "• Tap anywhere to advance (${stepIndex + 1}/${scriptLines.size}) •",
                                color = Color(0x9900E5FF),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                // Grand Title Reveal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onComplete() }
                ) {
                    Text(
                        text = "OUTPOST",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 8.sp
                    )
                    Text(
                        text = "MISSION SURVIVAL",
                        color = Color(0xFF00E5FF),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"Every Decision Keeps Someone Alive.\"",
                        color = Color(0xFFFFB300),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF001926)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("STEP ONTO REGOLITH", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Bottom Progress Tracker
        LinearProgressIndicator(
            progress = { (stepIndex.toFloat() / scriptLines.size).coerceIn(0f, 1f) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.5f)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = Color(0xFF00E5FF),
            trackColor = Color(0x3300E5FF)
        )
    }
}
}
