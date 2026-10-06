package com.example.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine3d.MeshFactory
import com.example.game.engine3d.Renderer3D
import com.example.game.engine3d.Vector3
import com.example.game.model.CharacterProfile
import com.example.game.model.CharacterRole

@Composable
fun CharacterCreationScreen(
    initialProfile: CharacterProfile,
    onSaveAndLaunch: (CharacterProfile) -> Unit,
    onBack: () -> Unit,
    onSaveAndCinematic: ((CharacterProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var profile by remember { mutableStateOf(initialProfile) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Identity, 1 = Role, 2 = Suit & Gear
    var previewYaw by remember { mutableFloatStateOf(0f) }
    var previewPitch by remember { mutableFloatStateOf(0.1f) }
    var previewPose by remember { mutableStateOf(MeshFactory.AstronautPose.IDLE) }

    val renderer = remember { Renderer3D() }

    val astronautMesh = remember(profile.suitColor, profile.visorStyle, previewPose) {
        MeshFactory.createAstronautMesh(
            suitColor = profile.suitColor,
            visorColor = profile.visorStyle,
            pose = previewPose,
            animTime = 0f
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF03070E))
            .displayCutoutPadding()
            .systemBarsPadding()
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Interactive 3D Astronaut Rig Viewer
                Box(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .background(Color(0xFF010408))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                previewYaw += dragAmount.x * 0.015f
                                previewPitch = (previewPitch - dragAmount.y * 0.01f).coerceIn(-0.3f, 0.5f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val mesh = astronautMesh
                        mesh.position = Vector3(0f, 0f, 0f)
                        mesh.rotation = Vector3(0f, previewYaw, 0f)

                        val camDist = 3.6f
                        val camY = 1.2f + previewPitch * 2f
                        val camPos = Vector3(
                            kotlin.math.sin(previewYaw * 0.2f) * 0.4f,
                            camY,
                            camDist
                        )
                        val camTarget = Vector3(0f, 1.1f, 0f)

                        renderer.renderScene(
                            drawScope = this,
                            cameraPos = camPos,
                            cameraTarget = camTarget,
                            meshes = listOf(mesh),
                            flashlightActive = true,
                            isMars = false,
                            fieldOfView = 50f
                        )
                    }

                    // Top Left Info Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD9061320))
                            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Column {
                            Text(
                                text = "ASTRONAUT RIG PREVIEW",
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${profile.role.title.uppercase()} • ${profile.suitColorName.uppercase()}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 3D Preview Overlay Controls (Rotate hint + Animation Pose selector)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DRAG TO ORBIT 360° • SELECT ANIMATION CYCLE",
                            color = Color(0x9900E5FF),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Pose Selector Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PoseChip("IDLE", previewPose == MeshFactory.AstronautPose.IDLE) {
                                previewPose = MeshFactory.AstronautPose.IDLE
                            }
                            PoseChip("WALK", previewPose == MeshFactory.AstronautPose.WALK) {
                                previewPose = MeshFactory.AstronautPose.WALK
                            }
                            PoseChip("SCAN", previewPose == MeshFactory.AstronautPose.SCAN) {
                                previewPose = MeshFactory.AstronautPose.SCAN
                            }
                            PoseChip("REPAIR", previewPose == MeshFactory.AstronautPose.REPAIR) {
                                previewPose = MeshFactory.AstronautPose.REPAIR
                            }
                        }
                    }
                }

                // Right Column: Customization Controls Panel with Clearly Separated Sections
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .background(Color(0xFF08121E))
                        .padding(14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ASTRONAUT REGISTRATION & LOADOUT",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Planetary Expedition Personnel Dossier",
                                color = Color(0xAAFFFFFF),
                                fontSize = 9.sp
                            )
                        }

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Navigation Tabs with Clear Icons
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF0C1B2A),
                        contentColor = Color(0xFF00E5FF),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        CreationTabItem(0, selectedTab, "1. IDENTITY", Icons.Default.Badge) { selectedTab = 0 }
                        CreationTabItem(1, selectedTab, "2. ROLE & SKILLS", Icons.Default.Psychology) { selectedTab = 1 }
                        CreationTabItem(2, selectedTab, "3. SUIT & GEAR", Icons.Default.Shield) { selectedTab = 2 }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Content with Separated, Clear Options and Animated Crossfade
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = selectedTab,
                            label = "tabContentAnim"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> IdentityTabContent(profile = profile, onUpdate = { profile = it })
                                1 -> RoleTabContent(profile = profile, onUpdate = { profile = it })
                                2 -> SuitTabContent(profile = profile, onUpdate = { profile = it })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Launch Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveAndLaunch(profile) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF001A26)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("char_create_confirm_button")
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEPLOY TO SURFACE (PLAY)",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp
                            )
                        }

                        if (onSaveAndCinematic != null) {
                            OutlinedButton(
                                onClick = { onSaveAndCinematic(profile) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                                    .testTag("char_create_cinematic_button")
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WATCH INTRO",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Adaptive Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ASTRONAUT DOSSIER",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${profile.role.title} • ${profile.suitColorName}",
                            color = Color(0xAAFFFFFF),
                            fontSize = 9.sp
                        )
                    }

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0C1B2A),
                    contentColor = Color(0xFF00E5FF),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    CreationTabItem(0, selectedTab, "IDENTITY", Icons.Default.Badge) { selectedTab = 0 }
                    CreationTabItem(1, selectedTab, "ROLE", Icons.Default.Psychology) { selectedTab = 1 }
                    CreationTabItem(2, selectedTab, "SUIT", Icons.Default.Shield) { selectedTab = 2 }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Customization Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = selectedTab,
                        label = "tabContentAnimPortrait"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> IdentityTabContent(profile = profile, onUpdate = { profile = it })
                            1 -> RoleTabContent(profile = profile, onUpdate = { profile = it })
                            2 -> SuitTabContent(profile = profile, onUpdate = { profile = it })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Launch Action Buttons
                Button(
                    onClick = { onSaveAndLaunch(profile) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF001A26)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("char_create_confirm_button_portrait")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPLOY TO SURFACE (PLAY NOW)",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CreationTabItem(
    index: Int,
    activeIndex: Int,
    title: String,
    icon: ImageVector,
    onSelect: () -> Unit
) {
    Tab(
        selected = activeIndex == index,
        onClick = onSelect,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(title, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PoseChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF00E5FF) else Color(0x44000000))
            .border(1.dp, if (isSelected) Color.White else Color(0x3300E5FF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun IdentityTabContent(profile: CharacterProfile, onUpdate: (CharacterProfile) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Section 1: Callsign & Name
        item {
            OptionSectionCard(
                title = "ASTRONAUT CALLSIGN & SURNAME",
                badge = "REQUIRED",
                description = "Identification broadcasted across Outpost Alpha radio comms"
            ) {
                OutlinedTextField(
                    value = profile.name,
                    onValueChange = { onUpdate(profile.copy(name = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("char_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0x4400E5FF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x2200E5FF),
                        unfocusedContainerColor = Color(0x1100E5FF)
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                )
            }
        }

        // Section 2: Gender Presentation
        item {
            OptionSectionCard(
                title = "GENDER PRESENTATION",
                badge = profile.genderPresentation,
                description = "Physical profile calibration and biographical records"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Female", "Male", "Non-Binary / Neutral").forEach { g ->
                        SeparatedChoiceCard(
                            label = g,
                            isSelected = profile.genderPresentation == g,
                            onClick = { onUpdate(profile.copy(genderPresentation = g)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 3: Face Shape
        item {
            OptionSectionCard(
                title = "CRANIAL & JAW STRUCTURE",
                badge = profile.faceShape,
                description = "Pressurized helmet neck ring seal adaptation"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Chiseled", "Oval", "Square", "Round").forEach { f ->
                        SeparatedChoiceCard(
                            label = f,
                            isSelected = profile.faceShape == f,
                            onClick = { onUpdate(profile.copy(faceShape = f)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 4: Skin Pigmentation
        item {
            OptionSectionCard(
                title = "SKIN PIGMENTATION",
                badge = "MELANIN TONE",
                description = "Cosmic radiation exposure baseline calibration"
            ) {
                val tones = listOf(
                    Pair("Fair Porcelain", 0xFFFFDFC4),
                    Pair("Golden Warm", 0xFFE0AC69),
                    Pair("Olive Amber", 0xFFC68642),
                    Pair("Rich Chestnut", 0xFF8D5524),
                    Pair("Deep Espresso", 0xFF4A2A18)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tones.forEach { (name, tone) ->
                        val isSelected = profile.skinTone == tone
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdate(profile.copy(skinTone = tone)) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(tone))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0x44FFFFFF),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = name.split(" ").first(),
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x88FFFFFF),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Section 5: Voice Profile
        item {
            OptionSectionCard(
                title = "RADIO COMMS SYNTHESIS VOICE",
                badge = profile.voiceProfile.split(" ").first(),
                description = "Helmet transceiver audio frequency processing"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Alpha (Neutral)", "Bravo (Deep)", "Charlie (Crisp)").forEach { v ->
                        SeparatedChoiceCard(
                            label = v,
                            isSelected = profile.voiceProfile.startsWith(v.split(" ").first()),
                            onClick = { onUpdate(profile.copy(voiceProfile = v)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleTabContent(profile: CharacterProfile, onUpdate: (CharacterProfile) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(CharacterRole.values()) { role ->
            val isSelected = profile.role == role
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0x3300E5FF) else Color(0xD906121E)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0x2200E5FF),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onUpdate(profile.copy(role = role)) }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF00E5FF) else Color(0x2200E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (role) {
                                        CharacterRole.ENGINEER -> Icons.Default.Build
                                        CharacterRole.SCIENTIST -> Icons.Default.Biotech
                                        CharacterRole.MEDICAL_OFFICER -> Icons.Default.MedicalServices
                                        CharacterRole.EXPLORER -> Icons.Default.Explore
                                        CharacterRole.SYSTEMS_SPECIALIST -> Icons.Default.Memory
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF001726) else Color(0xFF80DEEA),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = role.title.uppercase(),
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00E676))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SELECTED SPECIALIZATION",
                                    color = Color(0xFF001B0B),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Special Ability: ${role.abilityName}",
                        color = Color(0xFFFFB300),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = role.abilityDescription,
                        color = Color(0xCCFFFFFF),
                        fontSize = 8.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Expedition Perks: " + role.perks.joinToString(" • "),
                        color = Color(0xFF81D4FA),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SuitTabContent(profile: CharacterProfile, onUpdate: (CharacterProfile) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Section 1: EVA Suit Color
        item {
            OptionSectionCard(
                title = "EXTRAVEHICULAR PRESSURE SUIT COLOR",
                badge = profile.suitColorName.uppercase(),
                description = "Thermal control outer layer and micrometeoroid garment"
            ) {
                val suitColors = listOf(
                    Pair("Horizon White", 0xFFECEFF1),
                    Pair("Mars Hazard Orange", 0xFFFF5722),
                    Pair("Titanium Grey", 0xFF607D8B),
                    Pair("Stealth Carbon", 0xFF212121),
                    Pair("Solar Gold Layer", 0xFFFFD54F)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    suitColors.forEach { (name, colorVal) ->
                        val isSelected = profile.suitColor == colorVal
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdate(profile.copy(suitColor = colorVal, suitColorName = name)) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0x44FFFFFF),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = name.split(" ").first(),
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x88FFFFFF),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Helmet Visor Reflection
        item {
            OptionSectionCard(
                title = "HELMET VISOR REFLECTION STYLE",
                badge = profile.visorName.uppercase(),
                description = "Vapor-deposited gold / dielectric solar glare filter"
            ) {
                val visors = listOf(
                    Pair("Solar Gold", 0xFFFFD700),
                    Pair("Polar Cyan", 0xFF00E5FF),
                    Pair("Mirror Chrome", 0xFFE0E0E0),
                    Pair("Ruby Red", 0xFFFF1744)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    visors.forEach { (name, colorVal) ->
                        val isSelected = profile.visorStyle == colorVal
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdate(profile.copy(visorStyle = colorVal, visorName = name)) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = name,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x88FFFFFF),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Mission Patch
        item {
            OptionSectionCard(
                title = "MISSION INSIGNIA CHEST PATCH",
                badge = profile.missionPatch,
                description = "Embroidered shoulder and chest expedition emblem"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair("Outpost One Expedition", "Inaugural deep-space planetary settlement initiative"),
                        Pair("Lunar South Pole Base", "Permanent human presence at Shackleton Crater"),
                        Pair("Ares Mars Pioneer", "First manned reconnaissance of Valles Marineris"),
                        Pair("Frontier Horizon", "Deep-space reconnaissance honor guard")
                    ).forEach { (patchName, desc) ->
                        val isSelected = profile.missionPatch == patchName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0x3300E5FF) else Color(0x1A000000))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0x2200E5FF),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onUpdate(profile.copy(missionPatch = patchName)) }
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = patchName,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(text = desc, color = Color(0x88FFFFFF), fontSize = 8.5.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSectionCard(
    title: String,
    badge: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xD906121E)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2A00E5FF), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x3300E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color(0xFF80DEEA),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(text = description, color = Color(0x88FFFFFF), fontSize = 8.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SeparatedChoiceCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00E5FF) else Color(0x1A00E5FF),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "choiceBg"
    )
    val animatedBorder by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0x3300E5FF),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "choiceBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBg)
            .border(
                1.dp,
                animatedBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF001A26) else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
