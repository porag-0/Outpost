package com.example.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.game.model.EquipmentItem
import com.example.game.model.EquipmentSlot

@Composable
fun InventoryLoadoutScreen(
    items: List<EquipmentItem>,
    scannerLevel: Int,
    onUpgradeScanner: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalWeight = items.filter { it.isEquipped }.sumOf { it.weightKg.toDouble() }.toFloat()
    val maxWeight = 45.0f

    var selectedItem by remember { mutableStateOf(items.firstOrNull()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6050C14))
            .displayCutoutPadding()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF091624))
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EQUIPMENT & MISSION LOADOUT",
                        color = Color(0xFF00E5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Mass & Payload Capacity Management",
                        color = Color(0xAAFFFFFF),
                        fontSize = 10.sp
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payload Weight Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CARRIED PAYLOAD MASS:",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format("%.1f", totalWeight)} / ${maxWeight.toInt()} kg",
                        color = if (totalWeight > maxWeight) Color(0xFFFF1744) else Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (totalWeight / maxWeight).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (totalWeight > maxWeight) Color(0xFFFF1744) else Color(0xFF00E5FF),
                    trackColor = Color(0x3300E5FF)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scanner Upgrade Showcase Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2136)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GEOLOGICAL SCANNER (LEVEL $scannerLevel/3)",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        val scannerSpec = when (scannerLevel) {
                            1 -> "Range: 50m • Detects: Surface rock types & damaged power nodes"
                            2 -> "Range: 100m • Detects: Mineral deposits, Hematite ore & Radiation zones"
                            else -> "Range: 200m • Detects: Subsurface water-ice, Basalt lava tubes & Anomalies"
                        }
                        Text(text = scannerSpec, color = Color(0xDDFFFFFF), fontSize = 10.sp)
                    }

                    if (scannerLevel < 3) {
                        Button(
                            onClick = onUpgradeScanner,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upgrade_scanner_button")
                        ) {
                            Text("UPGRADE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Text(
                            text = "MAX LEVEL",
                            color = Color(0xFFFFD54F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-column layout: Left Equipment list, Right Item inspection
            Row(modifier = Modifier.weight(1f)) {
                // Equipment Slot List
                LazyColumn(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        val isSelected = selectedItem?.id == item.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0x3300E5FF) else Color(0x44081726)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0x2200E5FF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedItem = item }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.slot.label.uppercase(),
                                        color = Color(0xFF81D4FA),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${item.weightKg} kg",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Item Inspection Panel
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF05111D)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(10.dp))
                ) {
                    if (selectedItem != null) {
                        val itm = selectedItem!!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = itm.name.uppercase(),
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mass: ${itm.weightKg} kg • Condition: ${itm.durabilityPercent.toInt()}%",
                                    color = Color(0xAAFFFFFF),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = itm.description,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "TECHNICAL BENEFIT:",
                                    color = Color(0xFFFFB300),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = itm.statBoost,
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3300E676))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "EQUIPPED IN EXTRAVEHICULAR PACK",
                                    color = Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
