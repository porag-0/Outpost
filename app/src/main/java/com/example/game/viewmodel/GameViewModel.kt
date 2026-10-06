package com.example.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.SoundSystem
import com.example.game.engine3d.CollisionSystem
import com.example.game.engine3d.Mesh3D
import com.example.game.engine3d.MeshFactory
import com.example.game.engine3d.Particle3D
import com.example.game.engine3d.Vector3
import com.example.game.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.*

enum class GameScreenState {
    MAIN_MENU,
    CHARACTER_CREATOR,
    CINEMATIC_INTRO,
    PLAYING,
    DIALOGUE_ACTIVE,
    LOADOUT_INVENTORY,
    MAP_SCREEN,
    ENGINEERING_DECISION,
    EDUCATIONAL_MODAL,
    PHOTO_MODE,
    ROVER_DRIVING,
    DRONE_FLYING,
    SETTINGS_SCREEN,
    ENDING_SCREEN
}

data class GameUiState(
    val currentScreen: GameScreenState = GameScreenState.MAIN_MENU,
    val previousScreen: GameScreenState = GameScreenState.MAIN_MENU,
    val playerProfile: CharacterProfile = CharacterProfile(),
    val playerStats: CharacterStats = CharacterStats(),
    val outpostStats: OutpostStats = OutpostStats(),
    val isMars: Boolean = false, // false = Moon (Apollo/Artemis 1.62m/s²), true = Mars (3.72m/s²)
    val playerPos: Vector3 = Vector3(0f, 0f, 18f), // In front of base
    val playerRotationY: Float = PI.toFloat(), // Facing towards base
    val playerPose: MeshFactory.AstronautPose = MeshFactory.AstronautPose.IDLE,
    val isSprinting: Boolean = false,
    val isCrouched: Boolean = false,
    val isGrounded: Boolean = true,
    val flashlightActive: Boolean = true,
    val scannerActive: Boolean = false,
    val cameraYaw: Float = 0f,
    val cameraPitch: Float = 0.22f,
    val cameraDistance: Float = 5.2f,
    val currentMission: Mission? = null,
    val allMissions: List<Mission> = emptyList(),
    val activeNearbyObject: InteractiveWorldObject? = null,
    val activeNpc: NpcCharacter? = null,
    val currentDialogueNode: DialogueNode? = null,
    val activeEducationalFact: EducationalFact? = null,
    val activeCrisis: EngineeringCrisis? = null,
    val environmentalEvent: EnvironmentalEvent = EnvironmentalEvent(),
    val inventoryItems: List<EquipmentItem> = emptyList(),
    val scannerLevel: Int = 1,
    val roverConfig: RoverConfig = RoverConfig(),
    val droneConfig: DroneConfig = DroneConfig(),
    val photoModeFilter: String = "Orbital True",
    val photoModeFov: Float = 55f,
    val notificationToast: String? = null,
    val finalEndingTitle: String = "",
    val finalEndingText: String = "",
    val decisionHistory: List<String> = emptyList(),
    val settings: GameSettings = GameSettings()
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val soundSystem = SoundSystem()

    // 3D Scene Meshes Cache
    private var baseMeshes: List<Mesh3D> = emptyList()
    private var astronautMesh: Mesh3D? = null
    private var roverMesh: Mesh3D? = null
    private var droneMesh: Mesh3D? = null

    // Particles
    private val activeParticles = mutableListOf<Particle3D>()

    // Movement physics & smooth kinematics
    private var velocityY = 0f
    private var currentVelX = 0f
    private var currentVelZ = 0f
    private var landingSquat = 0f
    private var playerBankAngle = 0f
    private var playerSlopePitch = 0f
    private var playerStrideSpeed = 1.0f
    private var roverWheelSpin = 0f
    private var roverSteer = 0f
    private var roverPos = Vector3(6f, 0f, 14f)
    private var roverRotY = 0.8f
    private var roverPitch = 0f
    private var roverRoll = 0f
    private var dronePos = Vector3(2f, 1.2f, 6f)
    private var droneTiltX = 0f
    private var droneTiltZ = 0f
    private var animClock = 0f

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var diff = (to - from) % (2f * Math.PI.toFloat())
        if (diff < -Math.PI.toFloat()) diff += 2f * Math.PI.toFloat()
        if (diff > Math.PI.toFloat()) diff -= 2f * Math.PI.toFloat()
        return from + diff * t.coerceIn(0f, 1f)
    }

    init {
        initInitialMissions()
        initDefaultInventory()
        initWorldMeshes()
    }

    private fun initDefaultInventory() {
        val items = listOf(
            EquipmentItem(
                id = "item_scanner",
                name = "Multi-Frequency Geological Scanner",
                slot = EquipmentSlot.SCANNER,
                weightKg = 3.5f,
                description = "Emits pulse resonance to detect regolith minerals, water-ice, and damaged circuitry.",
                level = 1,
                statBoost = "+50m Scan Radius, Mineral Resonance",
                isEquipped = true
            ),
            EquipmentItem(
                id = "item_repair_tool",
                name = "Pneumatic Plasma Welder & Multi-Tool",
                slot = EquipmentSlot.PRIMARY_TOOL,
                weightKg = 4.2f,
                description = "Standard-issue aerospace repair tool for hermetic sealing, electrical shunting, and hardware fabrication.",
                level = 1,
                statBoost = "+40% Outpost Repair Velocity",
                isEquipped = true
            ),
            EquipmentItem(
                id = "item_oxygen_cell",
                name = "Cryogenic Backup O2 Tank",
                slot = EquipmentSlot.OXYGEN,
                weightKg = 6.0f,
                description = "High-pressure composite tank providing emergency oxygen reserve when away from base.",
                level = 1,
                statBoost = "+25 Max Oxygen Capacity",
                isEquipped = true
            ),
            EquipmentItem(
                id = "item_power_pack",
                name = "Graphene Battery Cell",
                slot = EquipmentSlot.POWER,
                weightKg = 5.0f,
                description = "High-density solid-state power module supplying tool energy and thermal regulation.",
                level = 1,
                statBoost = "+30% Power Cell Longevity",
                isEquipped = true
            ),
            EquipmentItem(
                id = "item_rover_key",
                name = "Planetary Rover Encryption Key",
                slot = EquipmentSlot.SPECIAL_ITEM,
                weightKg = 0.5f,
                description = "Authorizes orbital uplink and remote ignition for the Lunar/Martian Exploration Rover.",
                level = 1,
                statBoost = "Unlocks Rover Operation",
                isEquipped = true
            )
        )
        _uiState.update { it.copy(inventoryItems = items) }
    }

    private fun initInitialMissions() {
        val missions = listOf(
            Mission(
                id = "m1_first_landing",
                chapter = 1,
                title = "Chapter 1: First Landing",
                subtitle = "Calibrate Equipment & Report to Commander",
                briefing = "You have safely touched down at Outpost Alpha. Before venturing into deep regolith, pressurize your suit, test your scanner, and report to Commander Sarah Vance at the Command Dome airlock.",
                objectives = listOf(
                    Objective("m1_obj1", "Equip suit & test geological scanner", isCompleted = false),
                    Objective("m1_obj2", "Approach the Command Dome airlock", isCompleted = false),
                    Objective("m1_obj3", "Report to Commander Sarah Vance", isCompleted = false)
                ),
                xpReward = 250,
                isCurrentActive = true
            ),
            Mission(
                id = "m2_restore_power",
                chapter = 2,
                title = "Chapter 2: The First Problem",
                subtitle = "Restore the Sabatier Life Support Generator",
                briefing = "A power surge tripped the Sabatier oxygen generator and photovoltaic array #2. Oxygen production is fluctuating. Inspect the life support module, diagnose the failure, and bring it back online.",
                objectives = listOf(
                    Objective("m2_obj1", "Inspect the Sabatier Oxygen Generator", isCompleted = false),
                    Objective("m2_obj2", "Diagnose electrical relay & initiate repair", isCompleted = false),
                    Objective("m2_obj3", "Clear regolith dust from Solar Array #2", isCompleted = false)
                ),
                xpReward = 400
            ),
            Mission(
                id = "m3_survey_crater",
                chapter = 3,
                title = "Chapter 3: Unknown Territory",
                subtitle = "Survey the South Pole Ice Crater",
                briefing = "Our orbital radar detected subterranean ice deposits in the permanently shadowed crater south of the outpost. Drive the Exploration Rover to the crater, deploy the drill, and extract a core water sample.",
                objectives = listOf(
                    Objective("m3_obj1", "Board the Planetary Exploration Rover", isCompleted = false),
                    Objective("m3_obj2", "Drive 150m south to the Shadowed Crater rim", isCompleted = false),
                    Objective("m3_obj3", "Extract pristine water-ice sample", isCompleted = false)
                ),
                xpReward = 600
            ),
            Mission(
                id = "m4_the_storm",
                chapter = 4,
                title = "Chapter 4: The Storm",
                subtitle = "Prepare Outpost for Radiation & Dust Event",
                briefing = "Deep Space Sensor Network detects an incoming Solar Energetic Particle (SEP) event. You must secure exposed outpost modules, redirect energy to the electromagnetic deflection coil, and take shelter inside the regolith bunker.",
                objectives = listOf(
                    Objective("m4_obj1", "Trigger Outpost Emergency Alert Siren", isCompleted = false),
                    Objective("m4_obj2", "Secure rover and drone in hangar", isCompleted = false),
                    Objective("m4_obj3", "Safely enter the pressurized habitat bunker", isCompleted = false)
                ),
                xpReward = 800
            ),
            Mission(
                id = "m5_resource_crisis",
                chapter = 5,
                title = "Chapter 5: Resource Crisis",
                subtitle = "Critical Engineering Allocation",
                briefing = "Grid capacity dropped to 45 kW. You face an engineering trade-off: route available kilowatts to the Hydroponic Greenhouse to prevent crop starvation, or to the Science Laboratory to decode critical water purification telemetry.",
                objectives = listOf(
                    Objective("m5_obj1", "Access the Master Power Grid Terminal", isCompleted = false),
                    Objective("m5_obj2", "Make the critical allocation decision", isCompleted = false)
                ),
                xpReward = 1000
            ),
            Mission(
                id = "m6_the_discovery",
                chapter = 6,
                title = "Chapter 6: The Discovery",
                subtitle = "Analyze Subterranean Lava Tube Anomaly",
                briefing = "Deploy the AeroScout Recon Quad drone into the collapsed skylight of the nearby basalt lava tube. Sensors show anomalous thermal vents that could shelter a secondary permanent human settlement.",
                objectives = listOf(
                    Objective("m6_obj1", "Deploy Recon Drone into the lava tube", isCompleted = false),
                    Objective("m6_obj2", "Map structural integrity of the tube ceiling", isCompleted = false)
                ),
                xpReward = 1200
            ),
            Mission(
                id = "m7_survival",
                chapter = 7,
                title = "Chapter 7: Emergency Rescue",
                subtitle = "Save Stranded Crew Member Before O2 Depletion",
                briefing = "Marcus Rivera's rover suffered a wheel hub fracture 180 meters east during an unexpected meteor shower. His suit PLSS has 5 minutes of oxygen remaining. Mount the rover and execute an emergency rescue.",
                objectives = listOf(
                    Objective("m7_obj1", "Reach Marcus's beacon coordinates", isCompleted = false),
                    Objective("m7_obj2", "Tether emergency oxygen feed", isCompleted = false),
                    Objective("m7_obj3", "Transport him safely back to the Medical Bay", isCompleted = false)
                ),
                xpReward = 1500
            ),
            Mission(
                id = "m8_the_future",
                chapter = 8,
                title = "Chapter 8: The Future",
                subtitle = "The Destiny of Outpost One",
                briefing = "All foundational milestones have been evaluated. Review your engineering choices, crew trust records, and resource reserves to determine whether Outpost One achieves self-sustainability or requires emergency evacuation.",
                objectives = listOf(
                    Objective("m8_obj1", "Convene the Outpost Council at Command", isCompleted = false),
                    Objective("m8_obj2", "Receive Surface Command Evaluation and Colony Verdict", isCompleted = false)
                ),
                xpReward = 2000
            )
        )

        _uiState.update {
            it.copy(
                allMissions = missions,
                currentMission = missions.first()
            )
        }
    }

    private fun initWorldMeshes() {
        val meshes = mutableListOf<Mesh3D>()
        val isMars = _uiState.value.isMars

        // 1. Smooth High-Density Terrain
        val terrain = MeshFactory.createTerrain(isMars = isMars)
        meshes.add(terrain)

        // 2. Geodesic Habitat Command Dome
        val dome = MeshFactory.createOutpostDome()
        dome.position = Vector3(0f, MeshFactory.getTerrainHeight(0f, 0f), 0f)
        meshes.add(dome)

        // 3. Pressurized Utility Corridor Conduits connecting modules
        val corridorToGreenhouse = MeshFactory.createUtilityConduitTunnel(length = 7.5f)
        corridorToGreenhouse.position = Vector3(7.2f, MeshFactory.getTerrainHeight(7.2f, 3.0f), 3.0f)
        corridorToGreenhouse.rotation = Vector3(0f, -0.42f, 0f)
        meshes.add(corridorToGreenhouse)

        val corridorToOxygen = MeshFactory.createUtilityConduitTunnel(length = 5.0f)
        corridorToOxygen.position = Vector3(4.5f, MeshFactory.getTerrainHeight(4.5f, -2.5f), -2.5f)
        corridorToOxygen.rotation = Vector3(0f, 0.52f, 0f)
        meshes.add(corridorToOxygen)

        // 4. Solar Array 1 & 2
        val solar1 = MeshFactory.createSolarArray()
        solar1.position = Vector3(-14f, MeshFactory.getTerrainHeight(-14f, 10f), 10f)
        solar1.rotation = Vector3(0f, 0.4f, 0f)
        meshes.add(solar1)

        val solar2 = MeshFactory.createSolarArray()
        solar2.position = Vector3(-14f, MeshFactory.getTerrainHeight(-14f, 18f), 18f)
        solar2.rotation = Vector3(0f, 0.4f, 0f)
        meshes.add(solar2)

        // 5. CELSS Aeroponic Greenhouse
        val greenhouse = MeshFactory.createGreenhouse()
        greenhouse.position = Vector3(14f, MeshFactory.getTerrainHeight(14f, 6f), 6f)
        greenhouse.rotation = Vector3(0f, -0.3f, 0f)
        meshes.add(greenhouse)

        // 6. Sabatier Oxygen & Water Reactor
        val oxyGen = MeshFactory.createOxygenGenerator(isDamaged = false)
        oxyGen.position = Vector3(9f, MeshFactory.getTerrainHeight(9f, -5f), -5f)
        meshes.add(oxyGen)

        // 7. Communications Deep Space Network Dish
        val commDish = MeshFactory.createCommDish()
        commDish.position = Vector3(-12f, MeshFactory.getTerrainHeight(-12f, -12f), -12f)
        commDish.rotation = Vector3(0f, 0.6f, 0f)
        meshes.add(commDish)

        // 8. Spacecraft Lander (on landing pad)
        val lander = MeshFactory.createSpaceLander()
        lander.position = Vector3(-24f, MeshFactory.getTerrainHeight(-24f, 4f), 4f)
        meshes.add(lander)

        // 9. Cryogenic Propellant & Life Support Storage Cluster
        val cryoStorage = MeshFactory.createCryoStorageUnit()
        cryoStorage.position = Vector3(-17f, MeshFactory.getTerrainHeight(-17f, -2.5f), -2.5f)
        cryoStorage.rotation = Vector3(0f, 0.35f, 0f)
        meshes.add(cryoStorage)

        // 10. Science Field Survey Station Rack
        val scienceRack = MeshFactory.createScienceFieldRack()
        scienceRack.position = Vector3(18.5f, MeshFactory.getTerrainHeight(18.5f, 19.5f), 19.5f)
        scienceRack.rotation = Vector3(0f, -0.6f, 0f)
        meshes.add(scienceRack)

        // 11. Perimeter Navigation Beacon Pylons lining exploration paths
        val beaconWaypoints = listOf(
            Vector3(-18f, 0f, 5f),   // Lander approach path
            Vector3(-6f, 0f, 21f),   // Solar farm perimeter
            Vector3(12f, 0f, 17f),   // Greenhouse pathway
            Vector3(18f, 0f, 22f),   // South Crater descent rim
            Vector3(25f, 0f, 25f),   // Water-ice rim beacon
            Vector3(-14f, 0f, -14f), // Comm relay perimeter
            Vector3(13f, 0f, -8f)    // Reactor perimeter
        )
        val beaconLightColor = if (isMars) 0xFFFFB300 else 0xFF00E5FF
        for (bPos in beaconWaypoints) {
            val pylon = MeshFactory.createPerimeterBeaconPylon(beaconLightColor)
            pylon.position = Vector3(bPos.x, MeshFactory.getTerrainHeight(bPos.x, bPos.z), bPos.z)
            meshes.add(pylon)
        }

        // 12. Water-Ice Deposit in South Crater
        val iceDeposit = MeshFactory.createMineralDeposit("ice")
        iceDeposit.position = Vector3(22f, MeshFactory.getTerrainHeight(22f, 25f), 25f)
        meshes.add(iceDeposit)

        // 13. Hematite / Mineral Outcrop
        val minOutcrop = MeshFactory.createMineralDeposit("hematite")
        minOutcrop.position = Vector3(-26f, MeshFactory.getTerrainHeight(-26f, -18f), -18f)
        meshes.add(minOutcrop)

        // 14. Natural Planetary Boulders on crater rims and ridgelines
        val boulderConfigs = listOf(
            Pair(Vector3(18f, 0f, 19f), Vector3(1.6f, 1.1f, 1.4f)),
            Pair(Vector3(27f, 0f, 21f), Vector3(2.2f, 1.4f, 1.9f)),
            Pair(Vector3(-21f, 0f, -14f), Vector3(1.7f, 1.2f, 1.5f)),
            Pair(Vector3(-32f, 0f, -22f), Vector3(2.6f, 1.8f, 2.3f)),
            Pair(Vector3(29f, 0f, -8f), Vector3(1.9f, 1.3f, 1.6f)),
            Pair(Vector3(8f, 0f, -17f), Vector3(1.3f, 0.9f, 1.2f)),
            Pair(Vector3(-8f, 0f, 23f), Vector3(1.5f, 1.0f, 1.4f)),
            Pair(Vector3(-19f, 0f, 27f), Vector3(2.0f, 1.4f, 1.8f)),
            Pair(Vector3(24f, 0f, 15f), Vector3(1.8f, 1.2f, 1.5f)),
            Pair(Vector3(-15f, 0f, -9f), Vector3(1.4f, 1.0f, 1.3f)),
            Pair(Vector3(14f, 0f, 28f), Vector3(2.1f, 1.5f, 1.7f)),
            Pair(Vector3(-28f, 0f, 12f), Vector3(1.9f, 1.3f, 1.6f))
        )
        for ((pos, size) in boulderConfigs) {
            val b = MeshFactory.createBoulder(size, isMars = isMars)
            b.position = Vector3(pos.x, MeshFactory.getTerrainHeight(pos.x, pos.z), pos.z)
            meshes.add(b)
        }

        // Initialize rover and drone world positions
        roverPos = Vector3(6f, MeshFactory.getTerrainHeight(6f, 14f), 14f)
        dronePos = Vector3(2f, MeshFactory.getTerrainHeight(2f, 6f) + 1.2f, 6f)

        baseMeshes = meshes
    }

    // World Interactive Objects Definition
    fun getInteractiveObjects(): List<InteractiveWorldObject> {
        return listOf(
            InteractiveWorldObject(
                id = "obj_airlock",
                name = "Habitat Main Airlock",
                type = WorldObjectType.HABITAT_AIRLOCK,
                position = Vector3(0f, 0f, 7.2f),
                promptText = "Pressurize Airlock & Enter Habitat",
                educationalFactId = "regolith_sintering"
            ),
            InteractiveWorldObject(
                id = "obj_oxygen_gen",
                name = "Sabatier Oxygen & Water Generator",
                type = WorldObjectType.OXYGEN_GENERATOR,
                position = Vector3(9f, 0f, -5f),
                promptText = "Inspect Sabatier Life Support",
                educationalFactId = "sabatier_oxygen"
            ),
            InteractiveWorldObject(
                id = "obj_solar_array",
                name = "Solar Photovoltaic Grid Array #2",
                type = WorldObjectType.SOLAR_ARRAY,
                position = Vector3(-14f, 0f, 10f),
                promptText = "Clear Regolith Dust & Calibrate Sun Tracker",
                educationalFactId = "kilopower_nuclear"
            ),
            InteractiveWorldObject(
                id = "obj_greenhouse",
                name = "CELSS Aeroponic Greenhouse",
                type = WorldObjectType.GREENHOUSE,
                position = Vector3(14f, 0f, 6f),
                promptText = "Check Nutrient Film & Plant Biomass",
                educationalFactId = "closed_loop_greenhouse"
            ),
            InteractiveWorldObject(
                id = "obj_comm_dish",
                name = "Deep Space Network Relay Dish",
                type = WorldObjectType.COMMUNICATIONS_DISH,
                position = Vector3(-12f, 0f, -12f),
                promptText = "Uplink Telemetry to Surface Command"
            ),
            InteractiveWorldObject(
                id = "obj_rover",
                name = "Lunar Exploration Rover (LER-4)",
                type = WorldObjectType.PLANETARY_ROVER,
                position = Vector3(6f, 0f, 14f),
                promptText = "Board Exploration Rover",
                educationalFactId = "rocker_bogie"
            ),
            InteractiveWorldObject(
                id = "obj_drone",
                name = "AeroScout Recon Quad Drone",
                type = WorldObjectType.SCOUT_DRONE,
                position = Vector3(2f, 0.5f, 6f),
                promptText = "Deploy Reconnaissance Drone"
            ),
            InteractiveWorldObject(
                id = "obj_ice_deposit",
                name = "Permanently Shadowed Water-Ice Deposit",
                type = WorldObjectType.WATER_ICE_DEPOSIT,
                position = Vector3(22f, -2.5f, 25f),
                promptText = "Core Drill & Collect Pure Water-Ice Sample"
            ),
            InteractiveWorldObject(
                id = "obj_mineral",
                name = "Basaltic Iron Oxide Outcrop",
                type = WorldObjectType.MINERAL_OUTCROP,
                position = Vector3(-26f, 0.5f, -18f),
                promptText = "Perform Spectrometric Analysis"
            ),
            InteractiveWorldObject(
                id = "obj_lander",
                name = "Artemis Descent Stage Lander",
                type = WorldObjectType.SPACE_LANDER,
                position = Vector3(-24f, 0f, 4f),
                promptText = "Inspect Propulsion Telemetry & Cargo Bay"
            )
        )
    }

    fun getNpcCharacters(): List<NpcCharacter> {
        return listOf(
            NpcCharacter(
                id = "npc_commander",
                name = "Commander Sarah Vance",
                callsign = "Vanguard Lead",
                roleTitle = "Mission Commander",
                avatarColor = 0xFF0288D1,
                position = Vector3(1.6f, 0f, 6.2f),
                trustScore = 65,
                respectScore = 70,
                friendshipScore = 55,
                routineDescription = "Inspecting airlock telemetry and evaluating surface safety protocols.",
                currentDialogueNode = DialogueNode(
                    nodeId = "cmd_start",
                    speakerName = "Commander Sarah Vance",
                    speakerRole = "Mission Commander",
                    dialogueText = "Welcome to Outpost Alpha, astronaut. The environment is unforgiving, but with your skills we can make this settlement thrive. We're monitoring fluctuating power from the western habitat. What's your immediate assessment?",
                    choices = listOf(
                        DialogueChoice(
                            id = "c1",
                            text = "A. 'I'll head directly to the generator and repair it myself.'",
                            responseText = "Commander Vance nods: 'That's the initiative I like to see. Keep your comms open and monitor your suit integrity.'",
                            trustDelta = 8,
                            respectDelta = 10
                        ),
                        DialogueChoice(
                            id = "c2",
                            text = "B. 'Let's consult Marcus the engineer before touching high-voltage conduits.'",
                            responseText = "Commander Vance smiles: 'Cautious and methodical. Teamwork is how we avoid tragedies out here.'",
                            respectDelta = 8,
                            friendshipDelta = 6
                        ),
                        DialogueChoice(
                            id = "c3",
                            text = "C. 'What is the risk to life support if the generator fails completely?'",
                            responseText = "Commander Vance frowns seriously: 'We have 4 hours of reserve oxygen in the secondary tanks. We can't afford hesitation.'",
                            trustDelta = 4
                        ),
                        DialogueChoice(
                            id = "c4",
                            text = "D. 'We should shut down non-essential research systems to conserve battery.'",
                            responseText = "Commander Vance considers: 'A prudent engineering move. Dr. Chen won't be happy about his lab, but safety comes first.'",
                            trustDelta = 6,
                            respectDelta = 6
                        )
                    )
                )
            ),
            NpcCharacter(
                id = "npc_engineer",
                name = "Marcus Rivera",
                callsign = "Wrench-1",
                roleTitle = "Chief Life Support Engineer",
                avatarColor = 0xFFEF6C00,
                position = Vector3(8.0f, 0f, -3.8f),
                trustScore = 58,
                respectScore = 62,
                friendshipScore = 60,
                routineDescription = "Tuning catalytic valves on the Sabatier oxygen reactor.",
                currentDialogueNode = DialogueNode(
                    nodeId = "eng_start",
                    speakerName = "Marcus Rivera",
                    speakerRole = "Chief Life Support Engineer",
                    dialogueText = "Hey there! Mind giving me a hand? The Sabatier reactor's nickel catalyst bed is overheating, and fine regolith dust jammed the heat exchanger radiator.",
                    choices = listOf(
                        DialogueChoice(
                            id = "e1",
                            text = "A. 'I have plasma welder tools ready. Let's flush the cooling manifold.'",
                            responseText = "Marcus grins: 'Music to my ears! Hand me the wrench and let's bleed the line.'",
                            trustDelta = 10,
                            friendshipDelta = 10
                        ),
                        DialogueChoice(
                            id = "e2",
                            text = "B. 'Could we cycle liquid nitrogen from the reserve tanks to cool it faster?'",
                            responseText = "Marcus raises an eyebrow: 'Clever thinking! That could prevent thermal runaway before we replace the filter.'",
                            respectDelta = 12,
                            trustDelta = 8
                        ),
                        DialogueChoice(
                            id = "e3",
                            text = "C. 'How much regolith dust has accumulated on the solar panels?'",
                            responseText = "Marcus sighs: 'About a 15% efficiency drop already. The electrostatic sweepers are struggling.'",
                            respectDelta = 4
                        )
                    )
                )
            ),
            NpcCharacter(
                id = "npc_scientist",
                name = "Dr. Alan Chen",
                callsign = "Spectrum",
                roleTitle = "Chief Astrobiologist & Geologist",
                avatarColor = 0xFF7B1FA2,
                position = Vector3(11.5f, 0f, 5.8f),
                trustScore = 50,
                respectScore = 65,
                friendshipScore = 52,
                routineDescription = "Sequencing DNA from hydroponic microgreens and examining basalt thin sections.",
                currentDialogueNode = DialogueNode(
                    nodeId = "sci_start",
                    speakerName = "Dr. Alan Chen",
                    speakerRole = "Chief Astrobiologist & Geologist",
                    dialogueText = "Look at this spectral readout! The South Pole crater contains high-purity water ice trapped under 30 centimeters of regolith. If we verify that deposit, Outpost Alpha can produce its own rocket propellant!",
                    choices = listOf(
                        DialogueChoice(
                            id = "s1",
                            text = "A. 'I will take the rover and extract a core drill sample immediately.'",
                            responseText = "Dr. Chen's eyes light up: 'Splendid! Seal the sample container hermetically to avoid sublimation in vacuum!'",
                            trustDelta = 10,
                            friendshipDelta = 10
                        ),
                        DialogueChoice(
                            id = "s2",
                            text = "B. 'What volatile compounds should we watch out for during drilling?'",
                            responseText = "Dr. Chen nods approvingly: 'Watch for trapped ammonia and carbon monoxide clathrates. Ensure your visor seal is 100% tight.'",
                            respectDelta = 12
                        ),
                        DialogueChoice(
                            id = "s3",
                            text = "C. 'Is the crater slope safe for the rover wheels?'",
                            responseText = "Dr. Chen cautions: 'Stick to the northern rim slope—the rocker-bogie suspension handles up to 35-degree inclines safely.'",
                            trustDelta = 5
                        )
                    )
                )
            ),
            NpcCharacter(
                id = "npc_doctor",
                name = "Dr. Elena Rostova",
                callsign = "Medic Alpha",
                roleTitle = "Flight Surgeon & Medical Officer",
                avatarColor = 0xFFD81B60,
                position = Vector3(-4.0f, 0f, 8.5f),
                trustScore = 60,
                respectScore = 60,
                friendshipScore = 65,
                routineDescription = "Calibrating radiation dosimeters and preparing hyperbaric oxygen chambers.",
                currentDialogueNode = DialogueNode(
                    nodeId = "doc_start",
                    speakerName = "Dr. Elena Rostova",
                    speakerRole = "Medical Officer",
                    dialogueText = "Astronaut, hold still a moment while I sync your suit vitals. Heart rate is steady, but your radiation dosimeter registered a slight bump from cosmic rays. How are you feeling in the low gravity?",
                    choices = listOf(
                        DialogueChoice(
                            id = "d1",
                            text = "A. 'Feeling sharp and focused. Ready for whatever the outpost needs.'",
                            responseText = "Dr. Elena smiles: 'Good. Just remember that low gravity reduces bone density over time. Do your mandatory treadmill resistance workout!'",
                            friendshipDelta = 8,
                            trustDelta = 6
                        ),
                        DialogueChoice(
                            id = "d2",
                            text = "B. 'Suit heating is a bit sluggish on the shaded side of the habitat.'",
                            responseText = "Dr. Elena notes: 'I'll have Marcus check your liquid cooling garment circulation before your next long EVA.'",
                            respectDelta = 8
                        )
                    )
                )
            ),
            NpcCharacter(
                id = "npc_botanist",
                name = "Maya Lin",
                callsign = "Bio-Lead",
                roleTitle = "Hydroponics Specialist & Rover Pilot",
                avatarColor = 0xFF43A047,
                position = Vector3(13.2f, 0f, 4.2f),
                trustScore = 55,
                respectScore = 55,
                friendshipScore = 70,
                routineDescription = "Pruning aeroponic dwarf wheat and sweet potato leaves.",
                currentDialogueNode = DialogueNode(
                    nodeId = "bio_start",
                    speakerName = "Maya Lin",
                    speakerRole = "Hydroponics Specialist",
                    dialogueText = "Our CELSS crops are recycling 96% of the crew's carbon dioxide into crisp, sweet oxygen right now! Fresh greens also boost crew morale exponentially compared to freeze-dried paste.",
                    choices = listOf(
                        DialogueChoice(
                            id = "b1",
                            text = "A. 'You've built something truly miraculous out here in the void, Maya.'",
                            responseText = "Maya smiles brightly: 'Thank you! Nature finds a way, even in a pressurized tent millions of kilometers from Earth.'",
                            friendshipDelta = 12,
                            trustDelta = 8
                        ),
                        DialogueChoice(
                            id = "b2",
                            text = "B. 'How much water does the greenhouse demand daily?'",
                            responseText = "Maya explains: 'About 45 liters, but 92% of that is recaptured via dehumidifiers and purified back to potable standard.'",
                            respectDelta = 10
                        )
                    )
                )
            )
        )
    }

    // Dynamic Game Loop & Physics Update (Called per frame or timer)
    fun updateSimulation(dt: Float, moveX: Float, moveY: Float) {
        animClock += dt

        val state = _uiState.value
        if (state.currentScreen != GameScreenState.PLAYING &&
            state.currentScreen != GameScreenState.ROVER_DRIVING &&
            state.currentScreen != GameScreenState.DRONE_FLYING
        ) {
            return
        }

        // Gravity factor: Moon = 1.62, Mars = 3.72
        val gravity = if (state.isMars) 3.72f else 1.62f
        val jumpImpulse = if (state.isMars) 3.2f else 4.6f

        var newPos = state.playerPos
        var newPose = MeshFactory.AstronautPose.IDLE

        // Handle movement when walking/running on foot
        if (state.currentScreen == GameScreenState.PLAYING) {
            val speed = when {
                state.isSprinting -> 7.2f
                state.isCrouched -> 1.8f
                else -> 3.6f
            }

            val hasInput = abs(moveX) > 0.05f || abs(moveY) > 0.05f
            var targetVelX = 0f
            var targetVelZ = 0f
            var targetAngle = state.playerRotationY

            if (hasInput) {
                val inputMag = kotlin.math.hypot(moveX, moveY).coerceIn(0f, 1f)
                val moveAngle = atan2(moveX, -moveY) + state.cameraYaw
                targetAngle = moveAngle
                targetVelX = sin(moveAngle) * speed * inputMag
                targetVelZ = -cos(moveAngle) * speed * inputMag
            }

            // Smooth linear acceleration and deceleration with low-G inertia
            val accel = if (hasInput) 9.0f else 7.5f
            currentVelX += (targetVelX - currentVelX) * (dt * accel).coerceIn(0f, 1f)
            currentVelZ += (targetVelZ - currentVelZ) * (dt * accel).coerceIn(0f, 1f)

            val horizontalSpeed = kotlin.math.hypot(currentVelX, currentVelZ)

            // Smooth shortest-path angular slerp/lerp
            val newRot = if (hasInput) {
                lerpAngle(state.playerRotationY, targetAngle, (dt * 10.5f).coerceIn(0f, 1f))
            } else {
                state.playerRotationY
            }

            // Centripetal dynamic banking: astronaut body leans smoothly into turns
            val angularVelocity = (newRot - state.playerRotationY) / dt.coerceAtLeast(0.001f)
            val targetBank = (-angularVelocity * 0.07f).coerceIn(-0.35f, 0.35f)
            playerBankAngle += (targetBank - playerBankAngle) * (dt * 8.0f).coerceIn(0f, 1f)

            // Dynamic stride velocity tracking: gait matches foot travel speed across regolith
            playerStrideSpeed = if (horizontalSpeed > 0.15f) {
                (horizontalSpeed / (if (state.isSprinting) 6.4f else 3.2f)).coerceIn(0.5f, 1.8f)
            } else 1.0f

            // Position integration with 3D obstacle collision detection and smooth surface sliding
            val rawNextX = newPos.x + currentVelX * dt
            val rawNextZ = newPos.z + currentVelZ * dt

            // Check collision against all structures, dome, boulders, machines, and parked rover
            val extraObs = listOf(CollisionSystem.Obstacle.Circle(roverPos.x, roverPos.z, 1.45f, "ParkedRover"))
            val (nextX, nextZ) = CollisionSystem.resolvePosition(
                startX = newPos.x,
                startZ = newPos.z,
                desiredX = rawNextX,
                desiredZ = rawNextZ,
                entityRadius = 0.45f,
                extraObstacles = extraObs
            )

            val actualDx = nextX - newPos.x
            val actualDz = nextZ - newPos.z
            // If movement was stopped by an obstacle, damp velocity in that direction smoothly without stalling sliding motion
            if (rawNextX != newPos.x && abs(actualDx) < abs(rawNextX - newPos.x) * 0.5f) {
                currentVelX *= 0.35f
            }
            if (rawNextZ != newPos.z && abs(actualDz) < abs(rawNextZ - newPos.z) * 0.5f) {
                currentVelZ *= 0.35f
            }

            val groundY = MeshFactory.getTerrainHeight(nextX, nextZ)
            val terrainNorm = MeshFactory.getTerrainNormal(nextX, nextZ)

            // Posture slope adaptation: forward lean uphill, backward lean downhill
            val forwardDir = Vector3(sin(newRot), 0f, -cos(newRot))
            val targetSlopePitch = (-forwardDir.x * terrainNorm.x - forwardDir.z * terrainNorm.z) * 0.60f
            playerSlopePitch += (targetSlopePitch - playerSlopePitch) * (dt * 6.5f).coerceIn(0f, 1f)

            // Footstep sounds & dust particles
            if (state.isGrounded && horizontalSpeed > 0.3f && (animClock % (if (state.isSprinting) 0.28f else 0.42f) < dt)) {
                soundSystem.playFootstep()
                spawnDustParticles(Vector3(nextX, groundY, nextZ), count = 2, isMars = state.isMars)
            }

            // Stamina & Oxygen consumption
            if (horizontalSpeed > 0.2f) {
                val staminaDrain = if (state.isSprinting) 12f * dt else 2f * dt
                val o2Drain = if (state.isSprinting) 0.6f * dt else 0.2f * dt
                _uiState.update { s ->
                    val newStamina = (s.playerStats.stamina - staminaDrain).coerceAtLeast(0f)
                    val newO2 = (s.playerStats.oxygen - o2Drain).coerceAtLeast(0f)
                    s.copy(
                        playerStats = s.playerStats.copy(stamina = newStamina, oxygen = newO2),
                        playerRotationY = newRot
                    )
                }
            } else {
                _uiState.update { s ->
                    val newStamina = (s.playerStats.stamina + 15f * dt).coerceAtMost(s.playerStats.maxStamina)
                    s.copy(
                        playerStats = s.playerStats.copy(stamina = newStamina),
                        playerRotationY = newRot
                    )
                }
            }

            // Vertical gravity & jumping with ground elevation tracking
            if (!state.isGrounded) {
                velocityY -= gravity * dt
                var nextY = newPos.y + velocityY * dt
                if (nextY <= groundY) {
                    nextY = groundY
                    velocityY = 0f
                    landingSquat = 1.0f // Soft-landing knee flexion
                    _uiState.update { it.copy(isGrounded = true) }
                    spawnDustParticles(Vector3(nextX, groundY, nextZ), count = 5, isMars = state.isMars)
                    soundSystem.playFootstep()
                }
                newPos = Vector3(nextX, nextY, nextZ)
                newPose = MeshFactory.AstronautPose.JUMP
            } else {
                newPos = Vector3(nextX, groundY, nextZ)
                newPose = when {
                    horizontalSpeed > 4.2f -> MeshFactory.AstronautPose.RUN
                    horizontalSpeed > 0.2f -> MeshFactory.AstronautPose.WALK
                    state.isCrouched -> MeshFactory.AstronautPose.CROUCH
                    else -> MeshFactory.AstronautPose.IDLE
                }
            }

            // Smooth decay for soft landing squat
            landingSquat = (landingSquat - dt * 4.5f).coerceAtLeast(0f)

            // Low oxygen warning sound
            if (state.playerStats.oxygen < 15f && (animClock % 4.0f < dt)) {
                soundSystem.playAlarmSiren()
                showToast("CRITICAL WARNING: OXYGEN RESERVES BELOW 15%!")
            }

            checkProximities(newPos)
        } else if (state.currentScreen == GameScreenState.ROVER_DRIVING) {
            val roverSpeed = 12.0f
            val steerInput = moveX.coerceIn(-1f, 1f)
            val throttleInput = -moveY.coerceIn(-1f, 1f)

            roverSteer += (steerInput * 0.48f - roverSteer) * (dt * 7.5f).coerceIn(0f, 1f)
            val newRot = state.playerRotationY + roverSteer * throttleInput * dt * 2.2f
            val driveDir = Vector3(sin(newRot), 0f, -cos(newRot))
            val forwardVel = throttleInput * roverSpeed * dt

            val rawNextX = newPos.x + driveDir.x * forwardVel
            val rawNextZ = newPos.z + driveDir.z * forwardVel

            // Rover collision resolution with sliding response (rover physical radius 1.35m)
            val (nextX, nextZ) = CollisionSystem.resolvePosition(
                startX = newPos.x,
                startZ = newPos.z,
                desiredX = rawNextX,
                desiredZ = rawNextZ,
                entityRadius = 1.35f
            )
            val groundY = MeshFactory.getTerrainHeight(nextX, nextZ)
            val terrainNorm = MeshFactory.getTerrainNormal(nextX, nextZ)

            newPos = Vector3(nextX, groundY, nextZ)
            roverPos = newPos
            roverRotY = newRot
            // Wheel spin strictly matched to linear distance moved (distance / radius)
            roverWheelSpin += (forwardVel / 0.36f)

            // Terrain slope pitch and roll with suspension damping and inertia
            val targetPitch = -terrainNorm.z * 0.42f - (throttleInput * 0.08f) // nose dip on throttle/brake
            val targetRoll = terrainNorm.x * 0.42f - (roverSteer * throttleInput * 0.15f) // centrifugal roll
            roverPitch += (targetPitch - roverPitch) * (dt * 8.5f).coerceIn(0f, 1f)
            roverRoll += (targetRoll - roverRoll) * (dt * 8.5f).coerceIn(0f, 1f)

            if (abs(forwardVel) > 0.02f) {
                spawnDustParticles(newPos, count = 3, isMars = state.isMars)
            }

            _uiState.update { s ->
                s.copy(
                    playerRotationY = newRot,
                    roverConfig = s.roverConfig.copy(
                        batteryPercent = (s.roverConfig.batteryPercent - 0.08f * dt).coerceAtLeast(0f)
                    )
                )
            }
        } else if (state.currentScreen == GameScreenState.DRONE_FLYING) {
            val droneSpeed = 9.0f
            val moveAngle = atan2(moveX, -moveY) + state.cameraYaw
            val targetVelX = sin(moveAngle) * droneSpeed * abs(moveX)
            val targetVelZ = -cos(moveAngle) * droneSpeed * abs(moveY)

            currentVelX += (targetVelX - currentVelX) * (dt * 6.5f).coerceIn(0f, 1f)
            currentVelZ += (targetVelZ - currentVelZ) * (dt * 6.5f).coerceIn(0f, 1f)

            val nextX = (newPos.x + currentVelX * dt).coerceIn(-48f, 48f)
            val nextZ = (newPos.z + currentVelZ * dt).coerceIn(-48f, 48f)
            val groundY = MeshFactory.getTerrainHeight(nextX, nextZ)
            val hoverY = groundY + 3.4f + sin(animClock * 2.2f) * 0.16f

            newPos = Vector3(nextX, hoverY, nextZ)
            dronePos = newPos

            droneTiltX += (-currentVelZ * 0.08f - droneTiltX) * (dt * 7f).coerceIn(0f, 1f)
            droneTiltZ += (currentVelX * 0.08f - droneTiltZ) * (dt * 7f).coerceIn(0f, 1f)
        }

        // Update particle lifetimes
        updateParticles(dt)

        _uiState.update {
            it.copy(
                playerPos = newPos,
                playerPose = if (it.scannerActive) MeshFactory.AstronautPose.SCAN else newPose
            )
        }
    }

    private fun checkProximities(playerPos: Vector3) {
        val objects = getInteractiveObjects()
        var nearestObj: InteractiveWorldObject? = null
        var minObjDist = Float.MAX_VALUE

        for (obj in objects) {
            val dist = playerPos.distanceTo(obj.position)
            if (dist < obj.interactionRadius && dist < minObjDist) {
                nearestObj = obj
                minObjDist = dist
            }
        }

        val npcs = getNpcCharacters()
        var nearestNpc: NpcCharacter? = null
        var minNpcDist = Float.MAX_VALUE

        for (npc in npcs) {
            val dist = playerPos.distanceTo(npc.position)
            if (dist < 3.2f && dist < minNpcDist) {
                nearestNpc = npc
                minNpcDist = dist
            }
        }

        _uiState.update {
            it.copy(
                activeNearbyObject = nearestObj,
                activeNpc = nearestNpc
            )
        }
    }

    private fun spawnDustParticles(pos: Vector3, count: Int, isMars: Boolean) {
        val color = if (isMars) 0x99BF360C else 0x999E9E9E
        for (i in 0 until count) {
            val vx = (Math.random().toFloat() - 0.5f) * 1.5f
            val vy = (Math.random().toFloat() * 1.2f) + 0.3f
            val vz = (Math.random().toFloat() - 0.5f) * 1.5f
            activeParticles.add(
                Particle3D(
                    position = pos + Vector3(0f, 0.1f, 0f),
                    velocity = Vector3(vx, vy, vz),
                    life = 1.0f,
                    maxLife = 1.0f,
                    color = color,
                    size = 0.15f
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val it = activeParticles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
            } else {
                p.position = p.position + p.velocity * dt
                p.velocity = p.velocity - Vector3(0f, 1.2f * dt, 0f) // low grav settling
            }
        }
    }

    fun getSceneMeshes(): List<Mesh3D> {
        val list = mutableListOf<Mesh3D>()
        list.addAll(baseMeshes)

        val state = _uiState.value
        // If on foot, draw animated astronaut mesh with soft landing squat and realistic kinematics
        if (state.currentScreen != GameScreenState.ROVER_DRIVING) {
            val pMesh = MeshFactory.createAstronautMesh(
                suitColor = state.playerProfile.suitColor,
                visorColor = state.playerProfile.visorStyle,
                pose = state.playerPose,
                animTime = animClock,
                landingSquat = landingSquat,
                bankAngle = playerBankAngle,
                strideSpeed = playerStrideSpeed,
                slopePitch = playerSlopePitch
            )
            pMesh.position = state.playerPos
            pMesh.rotation = Vector3(0f, state.playerRotationY, 0f)
            list.add(pMesh)
        }

        // Draw animated Rover with steering, wheel spin, and terrain slope compliance
        val roverActive = state.currentScreen == GameScreenState.ROVER_DRIVING
        val roverCurrentPos = if (roverActive) state.playerPos else roverPos
        val roverCurrentRotY = if (roverActive) state.playerRotationY else roverRotY
        val rMesh = MeshFactory.createRoverMesh(
            steerAngle = if (roverActive) roverSteer else 0f,
            wheelSpin = roverWheelSpin,
            pitchAngle = roverPitch,
            rollAngle = roverRoll
        )
        rMesh.position = roverCurrentPos
        rMesh.rotation = Vector3(roverPitch, roverCurrentRotY, roverRoll)
        list.add(rMesh)

        // Draw animated Drone with high-speed spinning rotors and aerodynamic banking
        val droneActive = state.currentScreen == GameScreenState.DRONE_FLYING
        val droneCurrentPos = if (droneActive) state.playerPos else dronePos
        val dMesh = MeshFactory.createDroneMesh(
            rotorAngle = animClock * 28.0f,
            tiltX = droneTiltX,
            tiltZ = droneTiltZ
        )
        dMesh.position = droneCurrentPos
        dMesh.rotation = Vector3(droneTiltX, if (droneActive) state.playerRotationY else 0.4f, droneTiltZ)
        list.add(dMesh)

        // Draw NPC meshes planted firmly on terrain
        for (npc in getNpcCharacters()) {
            val npcGroundY = MeshFactory.getTerrainHeight(npc.position.x, npc.position.z)
            val npcMesh = MeshFactory.createAstronautMesh(
                suitColor = 0xFF37474F,
                visorColor = 0xFF81D4FA,
                accentColor = npc.avatarColor,
                pose = MeshFactory.AstronautPose.IDLE,
                animTime = animClock + npc.position.x
            )
            npcMesh.position = Vector3(npc.position.x, npcGroundY, npc.position.z)
            npcMesh.rotation = Vector3(0f, 0.4f, 0f)
            list.add(npcMesh)
        }

        return list
    }

    fun getParticles(): List<Particle3D> = activeParticles

    // User Actions
    fun onJump() {
        val state = _uiState.value
        if (state.isGrounded && state.playerStats.stamina >= 10f) {
            velocityY = if (state.isMars) 3.4f else 4.8f
            _uiState.update {
                it.copy(
                    isGrounded = false,
                    playerStats = it.playerStats.copy(stamina = it.playerStats.stamina - 10f)
                )
            }
            soundSystem.playFootstep()
        }
    }

    fun toggleSprint() {
        _uiState.update { it.copy(isSprinting = !it.isSprinting) }
    }

    fun toggleCrouch() {
        _uiState.update { it.copy(isCrouched = !it.isCrouched) }
    }

    fun toggleFlashlight() {
        _uiState.update { it.copy(flashlightActive = !it.flashlightActive) }
        soundSystem.playRadioChirp()
    }

    fun toggleScanner() {
        val willBeActive = !_uiState.value.scannerActive
        _uiState.update { it.copy(scannerActive = willBeActive) }
        if (willBeActive) {
            soundSystem.playScannerPing()
            showToast("GEOLOGICAL SCANNER ENGAGED: Detecting Subsurface Spectral Signatures")

            // Check if Chapter 1 mission objective is met
            completeObjectiveIfMatches("m1_obj1")
        }
    }

    fun onCameraRotate(dragDeltaX: Float, dragDeltaY: Float) {
        _uiState.update { s ->
            val sens = s.settings.cameraSensitivity
            val invert = if (s.settings.invertYAxis) -1f else 1f
            val newYaw = s.cameraYaw - dragDeltaX * 0.006f * sens
            val newPitch = (s.cameraPitch + dragDeltaY * 0.005f * sens * invert).coerceIn(-0.35f, 0.85f)
            s.copy(cameraYaw = newYaw, cameraPitch = newPitch)
        }
    }

    fun onInteract() {
        val state = _uiState.value

        // Check NPC first
        val npc = state.activeNpc
        if (npc != null) {
            soundSystem.playRadioChirp()
            _uiState.update {
                it.copy(
                    currentScreen = GameScreenState.DIALOGUE_ACTIVE,
                    currentDialogueNode = npc.currentDialogueNode
                )
            }
            completeObjectiveIfMatches("m1_obj3")
            return
        }

        // Check World Object
        val obj = state.activeNearbyObject ?: return
        soundSystem.playAirlockHiss()

        when (obj.type) {
            WorldObjectType.HABITAT_AIRLOCK -> {
                showToast("Entering Pressurized Habitat: Life Support Vitals Restored")
                _uiState.update { s ->
                    s.copy(
                        playerStats = s.playerStats.copy(
                            oxygen = s.playerStats.maxOxygen,
                            stamina = s.playerStats.maxStamina,
                            health = s.playerStats.maxHealth
                        )
                    )
                }
                completeObjectiveIfMatches("m1_obj2")
                completeObjectiveIfMatches("m4_obj3")
            }
            WorldObjectType.OXYGEN_GENERATOR -> {
                openEngineeringDecisionModal("crisis_oxygen")
                completeObjectiveIfMatches("m2_obj1")
            }
            WorldObjectType.SOLAR_ARRAY -> {
                showToast("Electrostatic Dust Flush Complete: Solar Array Efficiency +25%")
                _uiState.update { s ->
                    s.copy(outpostStats = s.outpostStats.copy(powerKw = 95f, solarEfficiency = 1.0f))
                }
                completeObjectiveIfMatches("m2_obj3")
            }
            WorldObjectType.GREENHOUSE -> {
                showToast("Harvesting Fresh Spirulina & Radishes: Hydroponic Reserves +10 Days")
                _uiState.update { s ->
                    s.copy(outpostStats = s.outpostStats.copy(foodStockDays = s.outpostStats.foodStockDays + 10))
                }
            }
            WorldObjectType.COMMUNICATIONS_DISH -> {
                soundSystem.playSuccessFanfare()
                showToast("Surface Command Telemetry Sync: 100% Signal Uplink Established")
            }
            WorldObjectType.PLANETARY_ROVER -> {
                _uiState.update {
                    it.copy(
                        currentScreen = GameScreenState.ROVER_DRIVING,
                        playerPos = roverPos,
                        playerRotationY = roverRotY
                    )
                }
                showToast("Planetary Rover Systems Online: Six-Wheel Drive Active")
                completeObjectiveIfMatches("m3_obj1")
            }
            WorldObjectType.SCOUT_DRONE -> {
                _uiState.update { it.copy(currentScreen = GameScreenState.DRONE_FLYING) }
                showToast("AeroScout Quad Rotor Drone Deployed: Aerial Vantage Mode")
                completeObjectiveIfMatches("m6_obj1")
            }
            WorldObjectType.WATER_ICE_DEPOSIT -> {
                soundSystem.playSuccessFanfare()
                showToast("Core Drill Complete: 100 Liters of Pristine Water-Ice Added to Outpost Reserves!")
                _uiState.update { s ->
                    s.copy(
                        outpostStats = s.outpostStats.copy(
                            waterReservesLiters = s.outpostStats.waterReservesLiters + 100f
                        )
                    )
                }
                completeObjectiveIfMatches("m3_obj3")
            }
            WorldObjectType.MINERAL_OUTCROP -> {
                soundSystem.playScannerPing()
                showToast("Hematite Spectrum Analyzed: High-Grade Iron Oxide & Titanium Discovered")
            }
            WorldObjectType.SPACE_LANDER -> {
                showToast("Descent Stage Telemetry: Touchdown Delta-V 1.4 m/s (Nominal)")
            }
            else -> {}
        }
    }

    fun exitVehicle() {
        // Place player safely 1.8m to the side of the vehicle, avoiding overlap with vehicle obstacle radius
        val exitSideX = -sin(roverRotY + PI.toFloat() * 0.5f) * 1.85f
        val exitSideZ = cos(roverRotY + PI.toFloat() * 0.5f) * 1.85f
        val exitPos = Vector3(roverPos.x + exitSideX, MeshFactory.getTerrainHeight(roverPos.x + exitSideX, roverPos.z + exitSideZ), roverPos.z + exitSideZ)
        _uiState.update { it.copy(currentScreen = GameScreenState.PLAYING, playerPos = exitPos) }
        showToast("Dismounted to Foot Extravehicular Activity (EVA)")
    }

    fun selectDialogueChoice(choice: DialogueChoice) {
        soundSystem.playRadioChirp()
        showToast("Response Recorded: Trust +${choice.trustDelta}, Respect +${choice.respectDelta}")

        _uiState.update { s ->
            s.copy(
                currentScreen = GameScreenState.PLAYING,
                decisionHistory = s.decisionHistory + choice.text
            )
        }

        // Award XP for social engagement
        addXp(75)
    }

    fun openEducationalCard(factId: String) {
        val fact = EducationalDatabase.facts.find { it.id == factId }
            ?: EducationalDatabase.facts.first()
        soundSystem.playScannerPing()
        _uiState.update {
            it.copy(
                currentScreen = GameScreenState.EDUCATIONAL_MODAL,
                activeEducationalFact = fact
            )
        }
    }

    fun closeEducationalCard() {
        _uiState.update { it.copy(currentScreen = GameScreenState.PLAYING) }
    }

    fun openEngineeringDecisionModal(crisisId: String) {
        val crisis = EngineeringCrisis(
            id = "crisis_power_life_support",
            title = "CRITICAL ENGINEERING DECISION: OXYGEN ALLOCATION",
            context = "A micrometeorite severed the primary Sabatier cooling circuit. Oxygen production has dropped below survival rate. Four engineering protocols are available based on your role and risk tolerance.",
            options = listOf(
                EngineeringDecisionOption(
                    id = "opt_repair",
                    title = "Option A: Manual Sintered Titanium Weld",
                    description = "Suit up and weld the fractured pressurized cooling line with plasma arc.",
                    requiredRole = CharacterRole.ENGINEER,
                    pros = listOf("Restores 100% generator performance", "Preserves all secondary systems"),
                    cons = listOf("Consumes 2x repair parts", "Risk of coolant flare burn"),
                    outcomeLog = "You successfully welded the Sabatier cooling line. 100% oxygen production restored!"
                ),
                EngineeringDecisionOption(
                    id = "opt_reroute",
                    title = "Option B: Re-Route Secondary Coolant from Lab",
                    description = "Divert liquid nitrogen coolant from Dr. Chen's astrobiology laboratory to the generator.",
                    pros = listOf("Fast immediate fix (under 2 minutes)", "Zero EVA physical risk"),
                    cons = listOf("Suspends laboratory research for 48 hours", "Dr. Chen Respect -5"),
                    outcomeLog = "Coolant re-routed successfully. Oxygen stabilized, though lab experiments temporarily paused."
                ),
                EngineeringDecisionOption(
                    id = "opt_evac",
                    title = "Option C: Partial Habitat Decompression & Seal",
                    description = "Seal the damaged western wing and consolidate the 5 crew members in the command module.",
                    pros = listOf("Conserves 40% oxygen immediately", "Zero spare parts needed"),
                    cons = listOf("Cramped quarters", "Crew Morale -10"),
                    outcomeLog = "Western wing sealed. Life support strain mitigated through habitat consolidation."
                )
            )
        )

        _uiState.update {
            it.copy(
                currentScreen = GameScreenState.ENGINEERING_DECISION,
                activeCrisis = crisis
            )
        }
    }

    fun selectEngineeringOption(option: EngineeringDecisionOption) {
        soundSystem.playSuccessFanfare()
        showToast("DECISION EXECUTED: ${option.outcomeLog}")
        _uiState.update { s ->
            s.copy(
                currentScreen = GameScreenState.PLAYING,
                decisionHistory = s.decisionHistory + option.title
            )
        }
        completeObjectiveIfMatches("m2_obj2")
        completeObjectiveIfMatches("m5_obj2")
        addXp(300)
    }

    fun triggerEnvironmentalStorm() {
        soundSystem.playAlarmSiren()
        val isMars = _uiState.value.isMars
        val hazard = if (isMars) {
            EnvironmentalEvent(
                type = HazardType.MARTIAN_DUST_STORM,
                name = "Severe Martian Regolith Dust Storm",
                description = "Wind gusts exceeding 110 km/h with high electrostatic dust charging. Solar array output reduced by 60%.",
                radiationMultiplier = 1.4f,
                solarEfficiency = 0.4f,
                timeRemainingSeconds = 60f,
                isEmergencyAlert = true
            )
        } else {
            EnvironmentalEvent(
                type = HazardType.SOLAR_RADIATION_FLARE,
                name = "Solar Energetic Particle (SEP) Radiation Spike",
                description = "Coronal mass ejection proton storm incoming. Seek immediate regolith bunker shelter!",
                radiationMultiplier = 3.5f,
                solarEfficiency = 1.0f,
                timeRemainingSeconds = 60f,
                isEmergencyAlert = true
            )
        }

        _uiState.update { it.copy(environmentalEvent = hazard) }
        showToast("EMERGENCY PROTOCOL ACTIVATED: ${hazard.name}")
        completeObjectiveIfMatches("m4_obj1")
    }

    fun clearEnvironmentalHazard() {
        _uiState.update {
            it.copy(environmentalEvent = EnvironmentalEvent(type = HazardType.NONE))
        }
        showToast("Environmental Hazard Cleared: Return to Nominal Operations")
    }

    fun completeObjectiveIfMatches(objId: String) {
        _uiState.update { s ->
            val mission = s.currentMission ?: return@update s
            val updatedObjs = mission.objectives.map { obj ->
                if (obj.id == objId) obj.copy(isCompleted = true) else obj
            }
            val allDone = updatedObjs.all { it.isCompleted }
            val updatedMission = mission.copy(objectives = updatedObjs, isCompleted = allDone)

            if (allDone && !mission.isCompleted) {
                soundSystem.playSuccessFanfare()
                addXp(mission.xpReward)
                showToast("MISSION COMPLETE: ${mission.title}! +${mission.xpReward} XP")

                // Advance to next mission
                val nextChapter = mission.chapter + 1
                val nextM = s.allMissions.find { it.chapter == nextChapter }
                if (nextM != null) {
                    s.copy(currentMission = nextM.copy(isCurrentActive = true))
                } else {
                    // All missions finished -> evaluate Ending!
                    evaluateFinalEnding(s)
                }
            } else {
                s.copy(currentMission = updatedMission)
            }
        }
    }

    private fun evaluateFinalEnding(s: GameUiState): GameUiState {
        val power = s.outpostStats.powerKw
        val water = s.outpostStats.waterReservesLiters
        val o2 = s.outpostStats.oxygenLevelPercent

        val endingTitle: String
        val endingText: String

        when {
            water > 1400f && power > 75f && o2 > 85f -> {
                endingTitle = "ENDING A: SELF-SUSTAINING PERMANENT COLONY"
                endingText = "Through rigorous engineering, resourceful water harvesting, and harmonious crew collaboration, Outpost Alpha has achieved net-positive resource balance! Surface Command designates this base humanity's first self-sustaining interplanetary settlement."
            }
            water > 1400f -> {
                endingTitle = "ENDING B: SCIENTIFIC BREAKTHROUGH"
                endingText = "Your geological drill samples proved deep subsurface aquifers and ancient mineral deposits. Ground control confirms human exploration will expand across the Martian canyons and lunar highlands."
            }
            else -> {
                endingTitle = "ENDING E: EXPANSION TO SECOND OUTPOST"
                endingText = "Outpost Alpha laid the foundation for human survival beyond Earth. A secondary pressurized dome has been commissioned for landing next season."
            }
        }

        return s.copy(
            currentScreen = GameScreenState.ENDING_SCREEN,
            finalEndingTitle = endingTitle,
            finalEndingText = endingText
        )
    }

    fun upgradeScanner() {
        val currLvl = _uiState.value.scannerLevel
        if (currLvl < 3) {
            val nextLvl = currLvl + 1
            soundSystem.playSuccessFanfare()
            _uiState.update { it.copy(scannerLevel = nextLvl) }
            showToast("SCANNER UPGRADED TO LEVEL $nextLvl: Extended Range & Mineral Purity Filter Active!")
        }
    }

    fun addXp(amount: Int) {
        _uiState.update { s ->
            val newXp = s.playerStats.xp + amount
            if (newXp >= s.playerStats.xpToNextLevel) {
                soundSystem.playSuccessFanfare()
                s.copy(
                    playerStats = s.playerStats.copy(
                        level = s.playerStats.level + 1,
                        xp = newXp - s.playerStats.xpToNextLevel,
                        xpToNextLevel = (s.playerStats.xpToNextLevel * 1.5f).toInt(),
                        skillPoints = s.playerStats.skillPoints + 1
                    )
                )
            } else {
                s.copy(playerStats = s.playerStats.copy(xp = newXp))
            }
        }
    }

    fun switchCelestialBody(isMars: Boolean) {
        _uiState.update { it.copy(isMars = isMars) }
        initWorldMeshes()
        val name = if (isMars) "Mars (Valles Marineris Outpost)" else "Moon (South Pole Shackleton Outpost)"
        showToast("Celestial Destination Set: $name")
    }

    fun updateCharacterProfile(profile: CharacterProfile) {
        _uiState.update { it.copy(playerProfile = profile) }
    }

    fun navigateToScreen(screen: GameScreenState) {
        _uiState.update { it.copy(previousScreen = it.currentScreen, currentScreen = screen) }
    }

    fun closeSettings() {
        _uiState.update {
            val target = if (it.previousScreen == GameScreenState.MAIN_MENU) GameScreenState.MAIN_MENU else GameScreenState.PLAYING
            it.copy(currentScreen = target)
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        showToast("Settings Updated: Configuration Saved")
    }

    fun replenishOutpostResources() {
        _uiState.update { s ->
            s.copy(
                playerStats = s.playerStats.copy(
                    health = s.playerStats.maxHealth,
                    stamina = s.playerStats.maxStamina,
                    oxygen = s.playerStats.maxOxygen,
                    radiationExposure = 5f,
                    suitIntegrity = 100f,
                    powerCell = 100f
                ),
                outpostStats = s.outpostStats.copy(
                    powerKw = 100f,
                    oxygenLevelPercent = 100f,
                    waterReservesLiters = 2000f,
                    foodStockDays = 68
                )
            )
        }
        soundSystem.playSuccessFanfare()
        showToast("EMERGENCY OVERRIDE: Outpost Power, Oxygen & Water Replenished to 100%!")
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(notificationToast = msg) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            _uiState.update { if (it.notificationToast == msg) it.copy(notificationToast = null) else it }
        }
    }
}
