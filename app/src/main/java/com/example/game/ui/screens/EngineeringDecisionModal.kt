package com.example.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.EngineeringCrisis
import com.example.game.model.EngineeringDecisionOption

@Composable
fun EngineeringDecisionModal(
    crisis: EngineeringCrisis?,
    onSelectOption: (EngineeringDecisionOption) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (crisis == null) return

    val protocolLetters = listOf("A", "B", "C", "D")
    val protocolColors = listOf(Color(0xFF00E5FF), Color(0xFFFFB300), Color(0xFF00E676), Color(0xFFFF7043))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .displayCutoutPadding()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xF2081524)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = crisis.title,
                                color = Color(0xFFFFB300),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Colony Survival Directive • Critical Engineering Trade-Off",
                                color = Color(0xAAFFFFFF),
                                fontSize = 9.sp
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Crisis Context Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33000000))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = crisis.context,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "SELECT REMEDIATION PROTOCOL (EACH OPTION OFFERS DISTINCT BENEFITS AND RISKS):",
                    color = Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Separate, Distinct Options List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(crisis.options) { index, opt ->
                        val letter = protocolLetters.getOrElse(index) { "•" }
                        val accentColor = protocolColors.getOrElse(index) { Color(0xFF00E5FF) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x33082035)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .testTag("engineering_option_${opt.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Header: Letter Badge + Title + Role Tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = letter,
                                                color = Color(0xFF001524),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = opt.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    if (opt.requiredRole != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x33FFD54F))
                                                .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "BEST FOR: ${opt.requiredRole.title.uppercase()}",
                                                color = Color(0xFFFFD54F),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = opt.description,
                                    color = Color(0xFFE0F7FA),
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Clearly Separated Pros & Cons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Pros
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x2200E676))
                                            .border(1.dp, Color(0x4400E676), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("ADVANTAGES / GAINS:", color = Color(0xFF00E676), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            opt.pros.forEach { p ->
                                                Text("• $p", color = Color.White, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    // Cons
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x22FF5252))
                                            .border(1.dp, Color(0x44FF5252), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("RISKS / EXPOSURE:", color = Color(0xFFFF5252), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            opt.cons.forEach { c ->
                                                Text("• $c", color = Color.White, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    // Execute Button
                                    Button(
                                        onClick = { onSelectOption(opt) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color(0xFF001524)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("EXECUTE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
