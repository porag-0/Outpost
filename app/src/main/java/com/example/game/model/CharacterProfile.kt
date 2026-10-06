package com.example.game.model

data class CharacterProfile(
    val name: String = "Commander Hayes",
    val genderPresentation: String = "Neutral / Astronaut",
    val faceShape: String = "Chiseled",
    val skinTone: Long = 0xFFD7A786,
    val hairStyle: String = "Crew Cut",
    val hairColor: Long = 0xFF3E2723,
    val heightMultiplier: Float = 1.0f,
    val bodyType: String = "Athletic",
    val voiceProfile: String = "Comms Alpha (Neutral)",
    val suitColor: Long = 0xFFECEFF1,       // Classic White
    val suitColorName: String = "Lunar Horizon White",
    val helmetType: String = "Mark-IV Extravehicular",
    val visorStyle: Long = 0xFFFFD700,      // Reflective Gold Foil
    val visorName: String = "Solar Gold Foil",
    val suitDecals: String = "Orbital Apex Vector",
    val missionPatch: String = "Outpost One Expedition",
    val role: CharacterRole = CharacterRole.ENGINEER
)

enum class CharacterRole(
    val title: String,
    val abilityName: String,
    val abilityDescription: String,
    val perks: List<String>,
    val startingGear: List<String>
) {
    ENGINEER(
        title = "Engineer",
        abilityName = "Rapid Diagnostic & Overclock",
        abilityDescription = "Repairs outpost modules twice as fast and consumes 50% fewer replacement components.",
        perks = listOf("+40% Repair Speed", "+25% Power Efficiency", "+30% Suit Durability", "Hab Construction Perk"),
        startingGear = listOf("Engineering Diagnostic Tablet", "Pneumatic Repair Multi-Tool", "High-Capacity Power Cell")
    ),
    SCIENTIST(
        title = "Scientist",
        abilityName = "Spectrometric Resonance",
        abilityDescription = "Advanced scanner reveals subsurface ice reserves, rare minerals, and analyzes anomalies with deep data.",
        perks = listOf("+50% Scanner Range", "Subsurface Ice Detection", "+30% Research Yield", "Mineral Purity +40%"),
        startingGear = listOf("Geological Spectrometer", "Cryogenic Sample Depository", "Research Data Terminal")
    ),
    MEDICAL_OFFICER(
        title = "Medical Officer",
        abilityName = "Triage Protocol",
        abilityDescription = "Rapidly treats crew injuries, lowers radiation exposure rate, and auto-administers med-gel when low on health.",
        perks = listOf("+35% Crew Health Recovery", "-40% Radiation Damage", "Regenerative Oxygen Pacing", "Trauma Kit Mastery"),
        startingGear = listOf("Bio-Scanner Diagnostic", "Pressurized Med-Gel Injector", "Emergency Oxygen Reserves")
    ),
    EXPLORER(
        title = "Explorer",
        abilityName = "Low-Gravity Kinetic Sprint",
        abilityDescription = "Leaps higher in low gravity, moves 25% faster with reduced stamina consumption, and navigates rough terrain.",
        perks = listOf("+25% Movement Speed", "+40% Jump Height", "-35% Stamina Drain", "Terrain Hazard Resilience"),
        startingGear = listOf("Long-Range Nav Beacon", "Ultra-Lightweight PLSS Pack", "High-Traction Mag-Boots")
    ),
    SYSTEMS_SPECIALIST(
        title = "Systems Specialist",
        abilityName = "Grid Re-Route Architecture",
        abilityDescription = "Balances outpost power, communications array, and life-support to avert brownouts during dust storms.",
        perks = listOf("+45% Battery Life", "Deep-Space Comms Boost", "Zero Power-Surge Risk", "Automated Drone Routing"),
        startingGear = listOf("Network Uplink Tablet", "Power Shunt Modulator", "Smart Drone Controller")
    )
}
