package com.example.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhotoModeOverlay(
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hideUiControls by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        if (!hideUiControls) {
            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC050E17))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PHOTO LAB • EXPEDITION OPTICS & ARCHIVE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { hideUiControls = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xAA000000))
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Hide UI", tint = Color.White)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xAA000000))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
                    }
                }
            }

            // Bottom Filter Bar & Shutter Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD081524))
                    .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT OPTICAL SENSOR FILTER:",
                    color = Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Orbital True", "Mars Warm Ochre", "Lunar Monochrome", "Cyberpunk Cyan").forEach { filter ->
                        val isSelected = activeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0x33000000))
                                .border(1.dp, if (isSelected) Color.White else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                .clickable { onFilterChange(filter) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Shutter Button
                Button(
                    onClick = onCapturePhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("photo_mode_shutter_button")
                        .border(3.dp, Color(0xFF00E5FF), CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(26.dp))
                }
            }
        } else {
            // When UI is hidden, tap anywhere to reveal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { hideUiControls = false }
            ) {
                Text(
                    text = "TAP SCREEN TO SHOW CONTROLS",
                    color = Color(0x88FFFFFF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                )
            }
        }
    }
}
