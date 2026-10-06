package com.example.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine3d.Renderer3D
import com.example.game.engine3d.Vector3
import com.example.game.ui.components.*
import com.example.game.viewmodel.GameScreenState
import com.example.game.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val renderer = remember { Renderer3D() }

    var joystickX by remember { mutableFloatStateOf(0f) }
    var joystickY by remember { mutableFloatStateOf(0f) }

    var keyW by remember { mutableStateOf(false) }
    var keyA by remember { mutableStateOf(false) }
    var keyS by remember { mutableStateOf(false) }
    var keyD by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    class CameraSmoother {
        var pos: Vector3? = null
        var target: Vector3? = null
        fun smooth(rawPos: Vector3, rawTarget: Vector3): Pair<Vector3, Vector3> {
            val p = if (pos == null) rawPos else pos!! + (rawPos - pos!!) * 0.32f
            val t = if (target == null) rawTarget else target!! + (rawTarget - target!!) * 0.32f
            pos = p
            target = t
            return Pair(p, t)
        }
    }
    val cameraSmoother = remember { CameraSmoother() }

    // 60fps continuous game loop
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastTime = now

            val effectiveX = if (joystickX != 0f) joystickX else ((if (keyD) 1f else 0f) - (if (keyA) 1f else 0f))
            val effectiveY = if (joystickY != 0f) joystickY else ((if (keyS) 1f else 0f) - (if (keyW) 1f else 0f))

            viewModel.updateSimulation(dt, effectiveX, effectiveY)
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val isDown = event.type == KeyEventType.KeyDown
                when (event.key) {
                    Key.W, Key.DirectionUp -> { keyW = isDown; true }
                    Key.S, Key.DirectionDown -> { keyS = isDown; true }
                    Key.A, Key.DirectionLeft -> { keyA = isDown; true }
                    Key.D, Key.DirectionRight -> { keyD = isDown; true }
                    Key.Spacebar -> {
                        if (isDown) viewModel.onJump()
                        true
                    }
                    Key.ShiftLeft, Key.ShiftRight -> {
                        if (isDown) viewModel.toggleSprint()
                        true
                    }
                    Key.C -> {
                        if (isDown) viewModel.toggleCrouch()
                        true
                    }
                    Key.E, Key.Enter -> {
                        if (isDown) viewModel.onInteract()
                        true
                    }
                    Key.F -> {
                        if (isDown) viewModel.toggleFlashlight()
                        true
                    }
                    Key.Q, Key.Tab -> {
                        if (isDown) viewModel.toggleScanner()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // 1. Fullscreen Widescreen 3D Viewport with Touch Camera Drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.onCameraRotate(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pPos = uiState.playerPos
                val yaw = uiState.cameraYaw
                val pitch = uiState.cameraPitch
                val dist = uiState.cameraDistance

                // Third-person camera position
                val camX = pPos.x + sin(yaw) * cos(pitch) * dist
                val camY = pPos.y + sin(pitch) * dist + 1.6f
                val camZ = pPos.z + cos(yaw) * cos(pitch) * dist

                val rawCamPos = Vector3(camX, camY, camZ)
                val rawCamTarget = pPos + Vector3(0f, 1.25f, 0f)

                // Smooth cinematic camera follow damping
                val (camPos, camTarget) = cameraSmoother.smooth(rawCamPos, rawCamTarget)

                renderer.renderScene(
                    drawScope = this,
                    cameraPos = camPos,
                    cameraTarget = camTarget,
                    meshes = viewModel.getSceneMeshes(),
                    particles = viewModel.getParticles(),
                    flashlightActive = uiState.flashlightActive,
                    emergencyAlertActive = uiState.environmentalEvent.isEmergencyAlert,
                    emergencyPulse = sin(System.currentTimeMillis() * 0.008f) * 0.5f + 0.5f,
                    isMars = uiState.isMars,
                    scannerActive = uiState.scannerActive,
                    showGroundShadows = uiState.settings.showGroundShadows,
                    showLensFlares = uiState.settings.showLensFlares,
                    depthFogEnabled = uiState.settings.depthFogEnabled,
                    fieldOfView = uiState.settings.fieldOfView,
                    wireframeHighlight = uiState.settings.wireframePanelHighlight,
                    cinematicPostProcessing = uiState.settings.cinematicPostProcessing
                )
            }
        }

        // 2. Landscape Top HUD (Mission briefing & Outpost Vitals)
        LandscapeTopHud(
            mission = uiState.currentMission,
            outpostStats = uiState.outpostStats,
            onOpenMap = { viewModel.navigateToScreen(GameScreenState.MAP_SCREEN) },
            onOpenInventory = { viewModel.navigateToScreen(GameScreenState.LOADOUT_INVENTORY) },
            onOpenPhotoMode = { viewModel.navigateToScreen(GameScreenState.PHOTO_MODE) },
            onOpenSettings = { viewModel.navigateToScreen(GameScreenState.SETTINGS_SCREEN) },
            onLearnMore = { viewModel.openEducationalCard("sabatier_oxygen") },
            onOpenMenu = { viewModel.navigateToScreen(GameScreenState.MAIN_MENU) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .displayCutoutPadding()
        )

        // Center Storm Alert Banner
        EnvironmentalHazardBanner(
            event = uiState.environmentalEvent,
            onClear = { viewModel.clearEnvironmentalHazard() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp)
        )

        // Radio Toast Notifications
        ToastNotificationBanner(
            message = uiState.notificationToast,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp)
        )

        // 3. Landscape Bottom-Left: Left Thumb Joystick + Vitals Cluster
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 12.dp)
                .navigationBarsPadding()
                .displayCutoutPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VirtualJoystick(
                size = uiState.settings.joystickSize.dp,
                onMove = { x, y ->
                    joystickX = x
                    joystickY = y
                }
            )

            if (!uiState.settings.minimalHudMode) {
                LandscapeVitalGauges(stats = uiState.playerStats)
            }
        }

        // 4. Landscape Bottom-Right: Right Thumb Action Controls
        val prompt = uiState.activeNearbyObject?.promptText ?: uiState.activeNpc?.let { "Talk with ${it.name}" }

        LandscapeActionControls(
            isGrounded = uiState.isGrounded,
            isSprinting = uiState.isSprinting,
            isCrouched = uiState.isCrouched,
            scannerActive = uiState.scannerActive,
            flashlightActive = uiState.flashlightActive,
            interactionPrompt = prompt,
            onJump = { viewModel.onJump() },
            onToggleSprint = { viewModel.toggleSprint() },
            onToggleCrouch = { viewModel.toggleCrouch() },
            onToggleScanner = { viewModel.toggleScanner() },
            onToggleFlashlight = { viewModel.toggleFlashlight() },
            onInteract = { viewModel.onInteract() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 12.dp)
                .navigationBarsPadding()
                .displayCutoutPadding()
        )

        // Vehicle Dismount Button
        if (uiState.currentScreen == GameScreenState.ROVER_DRIVING) {
            Button(
                onClick = { viewModel.exitVehicle() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 20.dp)
                    .testTag("exit_vehicle_button")
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DISMOUNT ROVER", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Environmental Hazard Simulator Trigger (Top Left)
        Button(
            onClick = {
                if (uiState.environmentalEvent.isEmergencyAlert) {
                    viewModel.clearEnvironmentalHazard()
                } else {
                    viewModel.triggerEnvironmentalStorm()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x66B71C1C), contentColor = Color.White),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 20.dp)
                .testTag("hazard_trigger_button")
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (uiState.environmentalEvent.isEmergencyAlert) "CLEAR HAZARD" else "TEST HAZARD",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Modals & Overlays
        when (uiState.currentScreen) {
            GameScreenState.DIALOGUE_ACTIVE -> {
                DialogueOverlay(
                    npc = uiState.activeNpc,
                    onSelectChoice = { viewModel.selectDialogueChoice(it) },
                    onClose = { viewModel.navigateToScreen(GameScreenState.PLAYING) }
                )
            }
            GameScreenState.LOADOUT_INVENTORY -> {
                InventoryLoadoutScreen(
                    items = uiState.inventoryItems,
                    scannerLevel = uiState.scannerLevel,
                    onUpgradeScanner = { viewModel.upgradeScanner() },
                    onClose = { viewModel.navigateToScreen(GameScreenState.PLAYING) }
                )
            }
            GameScreenState.MAP_SCREEN -> {
                MapOverlay(
                    isMars = uiState.isMars,
                    playerPos = uiState.playerPos,
                    onClose = { viewModel.navigateToScreen(GameScreenState.PLAYING) }
                )
            }
            GameScreenState.ENGINEERING_DECISION -> {
                EngineeringDecisionModal(
                    crisis = uiState.activeCrisis,
                    onSelectOption = { viewModel.selectEngineeringOption(it) },
                    onClose = { viewModel.navigateToScreen(GameScreenState.PLAYING) }
                )
            }
            GameScreenState.EDUCATIONAL_MODAL -> {
                EducationalCardDialog(
                    fact = uiState.activeEducationalFact,
                    onClose = { viewModel.closeEducationalCard() }
                )
            }
            GameScreenState.PHOTO_MODE -> {
                PhotoModeOverlay(
                    activeFilter = uiState.photoModeFilter,
                    onFilterChange = { /* Filter state */ },
                    onCapturePhoto = { viewModel.showToast("PHOTO CAPTURED: Saved to Mission Expedition Logbook!") },
                    onClose = { viewModel.navigateToScreen(GameScreenState.PLAYING) }
                )
            }
            GameScreenState.SETTINGS_SCREEN -> {
                SettingsScreen(
                    currentSettings = uiState.settings,
                    onSaveSettings = { viewModel.updateSettings(it) },
                    onClose = { viewModel.closeSettings() },
                    onReplenishResources = { viewModel.replenishOutpostResources() }
                )
            }
            GameScreenState.ENDING_SCREEN -> {
                EndingScreen(
                    endingTitle = uiState.finalEndingTitle,
                    endingText = uiState.finalEndingText,
                    playerProfile = uiState.playerProfile,
                    outpostStats = uiState.outpostStats,
                    decisions = uiState.decisionHistory,
                    onRestart = { viewModel.navigateToScreen(GameScreenState.MAIN_MENU) }
                )
            }
            else -> {}
        }
    }
}
