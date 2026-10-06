package com.example.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import com.example.game.model.DialogueChoice
import com.example.game.model.NpcCharacter

@Composable
fun DialogueOverlay(
    npc: NpcCharacter?,
    onSelectChoice: (DialogueChoice) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (npc == null) return

    val node = npc.currentDialogueNode

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x77000000))
            .displayCutoutPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xF5061422)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Speaker Dossier Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(npc.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = npc.name.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x3300E5FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = npc.callsign,
                                        color = Color(0xFF00E5FF),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${npc.roleTitle} • ${npc.routineDescription}",
                                color = Color(0xAAFFFFFF),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Relationship Metrics
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoreTag("CREW TRUST", npc.trustScore, Color(0xFF00E676))
                        ScoreTag("RESPECT", npc.respectScore, Color(0xFF00E5FF))
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0x3300E5FF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Spoken Dialogue Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33000000))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "\"${node.dialogueText}\"",
                        color = Color(0xFFE0F7FA),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SELECT YOUR RESPONSE (INFLUENCES CREW DYNAMICS & MISSION PATHS):",
                    color = Color(0xFFFFB300),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Clear, Distinct & Separated Option Cards (2x2 Grid with Letter Badges)
                val badgeColors = listOf(Color(0xFF00E5FF), Color(0xFFFFB300), Color(0xFF00E676), Color(0xFFFF7043))
                val letters = listOf("A", "B", "C", "D")

                val chunked = node.choices.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunked.forEachIndexed { rowIdx, rowChoices ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowChoices.forEachIndexed { colIdx, choice ->
                                val overallIdx = rowIdx * 2 + colIdx
                                val badgeColor = badgeColors.getOrElse(overallIdx) { Color(0xFF00E5FF) }
                                val letter = letters.getOrElse(overallIdx) { "•" }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x400C2238)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.5.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .clickable { onSelectChoice(choice) }
                                        .testTag("dialogue_choice_${choice.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Distinct Letter Pill
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = letter,
                                                color = Color(0xFF001726),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Dialogue Text & Impact
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = choice.text.replace(Regex("^[A-D]\\.\\s*"), ""),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 15.sp
                                            )
                                            if (choice.trustDelta > 0 || choice.respectDelta > 0) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (choice.trustDelta > 0) {
                                                        Text("+${choice.trustDelta} Trust", color = Color(0xFF00E676), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                    if (choice.respectDelta > 0) {
                                                        Text("+${choice.respectDelta} Respect", color = Color(0xFF00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowChoices.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreTag(label: String, value: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x44000000))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: $value%",
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
