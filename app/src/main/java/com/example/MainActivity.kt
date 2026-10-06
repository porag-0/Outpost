package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.ui.screens.CharacterCreationScreen
import com.example.game.ui.screens.CinematicIntroScreen
import com.example.game.ui.screens.GameScreen
import com.example.game.ui.screens.MainMenuScreen
import com.example.game.ui.screens.SettingsScreen
import com.example.game.viewmodel.GameScreenState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                // BackHandler routing
                BackHandler(enabled = uiState.currentScreen != GameScreenState.MAIN_MENU) {
                    when (uiState.currentScreen) {
                        GameScreenState.CHARACTER_CREATOR -> viewModel.navigateToScreen(GameScreenState.MAIN_MENU)
                        GameScreenState.CINEMATIC_INTRO -> viewModel.navigateToScreen(GameScreenState.PLAYING)
                        GameScreenState.SETTINGS_SCREEN -> viewModel.closeSettings()
                        GameScreenState.DIALOGUE_ACTIVE,
                        GameScreenState.LOADOUT_INVENTORY,
                        GameScreenState.MAP_SCREEN,
                        GameScreenState.ENGINEERING_DECISION,
                        GameScreenState.EDUCATIONAL_MODAL,
                        GameScreenState.PHOTO_MODE -> viewModel.navigateToScreen(GameScreenState.PLAYING)
                        GameScreenState.ROVER_DRIVING,
                        GameScreenState.DRONE_FLYING -> viewModel.exitVehicle()
                        GameScreenState.ENDING_SCREEN -> viewModel.navigateToScreen(GameScreenState.MAIN_MENU)
                        GameScreenState.PLAYING -> viewModel.navigateToScreen(GameScreenState.MAIN_MENU)
                        else -> {}
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (uiState.currentScreen) {
                        GameScreenState.MAIN_MENU -> {
                            MainMenuScreen(
                                isMars = uiState.isMars,
                                playerProfile = uiState.playerProfile,
                                onStartNewGame = { viewModel.navigateToScreen(GameScreenState.CHARACTER_CREATOR) },
                                onContinueGame = { viewModel.navigateToScreen(GameScreenState.PLAYING) },
                                onOpenSettings = { viewModel.navigateToScreen(GameScreenState.SETTINGS_SCREEN) },
                                onSwitchPlanet = { viewModel.switchCelestialBody(it) },
                                onOpenEducational = { viewModel.openEducationalCard("sabatier_oxygen") }
                            )
                        }

                        GameScreenState.SETTINGS_SCREEN -> {
                            if (uiState.previousScreen == GameScreenState.MAIN_MENU) {
                                SettingsScreen(
                                    currentSettings = uiState.settings,
                                    onSaveSettings = { viewModel.updateSettings(it) },
                                    onClose = { viewModel.closeSettings() },
                                    onReplenishResources = { viewModel.replenishOutpostResources() }
                                )
                            } else {
                                GameScreen(viewModel = viewModel)
                            }
                        }

                        GameScreenState.CHARACTER_CREATOR -> {
                            CharacterCreationScreen(
                                initialProfile = uiState.playerProfile,
                                onSaveAndLaunch = { updatedProfile ->
                                    viewModel.updateCharacterProfile(updatedProfile)
                                    viewModel.navigateToScreen(GameScreenState.PLAYING)
                                },
                                onSaveAndCinematic = { updatedProfile ->
                                    viewModel.updateCharacterProfile(updatedProfile)
                                    viewModel.navigateToScreen(GameScreenState.CINEMATIC_INTRO)
                                },
                                onBack = { viewModel.navigateToScreen(GameScreenState.MAIN_MENU) }
                            )
                        }

                        GameScreenState.CINEMATIC_INTRO -> {
                            CinematicIntroScreen(
                                isMars = uiState.isMars,
                                onComplete = { viewModel.navigateToScreen(GameScreenState.PLAYING) }
                            )
                        }

                        // In-game states (PLAYING, ROVER_DRIVING, DIALOGUE, INVENTORY, MAP, etc.)
                        else -> {
                            GameScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
