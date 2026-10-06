package com.example.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine3d.Vector3

@Composable
fun MapOverlay(
    isMars: Boolean,
    playerPos: Vector3,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapTitle = if (isMars) "ARES COLONY SECTOR TOPOGRAPHY (VALLES MARINERIS)" else "ARTEMIS LUNAR SOUTH POLE TACTICAL MAP (SHACKLETON)"

    val outpostPoints = listOf(
        Pair("Command Dome Alpha", Offset(0f, 0f)),
        Pair("Solar Photovoltaic Grid", Offset(-14f, 12f)),
        Pair("CELSS Greenhouse", Offset(14f, 6f)),
        Pair("Sabatier Life Support", Offset(9f, -5f)),
        Pair("Deep Space Dish", Offset(-12f, -12f)),
        Pair("Artemis Descent Lander", Offset(-24f, 4f)),
        Pair("Shadowed Crater Ice Reserve", Offset(22f, 25f)),
        Pair("Basaltic Mineral Ridge", Offset(-26f, -18f)),
        Pair("Deep Lava Tube Skylight", Offset(35f, -30f))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xEE03080F))
            .displayCutoutPadding()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF07121F))
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mapTitle,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Real-Time Orbital GPS & Surface Transponder Beacon Grid",
                        color = Color(0xAAFFFFFF),
                        fontSize = 10.sp
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tactical 2D Map Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF02070D))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    val scale = this.size.width / 90f // 90 meters scale

                    // Grid lines
                    val gridSpacing = 10f * scale
                    var gx = 0f
                    while (gx < this.size.width) {
                        drawLine(Color(0x1500E5FF), Offset(gx, 0f), Offset(gx, this.size.height), strokeWidth = 1f)
                        gx += gridSpacing
                    }
                    var gy = 0f
                    while (gy < this.size.height) {
                        drawLine(Color(0x1500E5FF), Offset(0f, gy), Offset(this.size.width, gy), strokeWidth = 1f)
                        gy += gridSpacing
                    }

                    // Radar Range Rings
                    drawCircle(Color(0x2200E5FF), radius = 20f * scale, center = Offset(cx, cy), style = Stroke(width = 1f))
                    drawCircle(Color(0x2200E5FF), radius = 40f * scale, center = Offset(cx, cy), style = Stroke(width = 1f))

                    // Draw outpost landmarks
                    for ((name, pt) in outpostPoints) {
                        val px = cx + pt.x * scale
                        val py = cy + pt.y * scale

                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 4f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color(0x4400E5FF),
                            radius = 8f,
                            center = Offset(px, py),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Draw Player Position (Flashing Yellow Pulse)
                    val playerMapX = cx + playerPos.x * scale
                    val playerMapY = cy + playerPos.z * scale // Z maps to Y in top-down 2D
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 6f,
                        center = Offset(playerMapX, playerMapY)
                    )
                    drawCircle(
                        color = Color(0x88FFD54F),
                        radius = 12f,
                        center = Offset(playerMapX, playerMapY),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Legend
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color(0xCC05101A), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text("• Yellow Dot: Current Astronaut Position", color = Color(0xFFFFD54F), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("• Cyan Rings: Outpost Modules & Mining Sites", color = Color(0xFF00E5FF), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
