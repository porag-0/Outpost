package com.example.game.model

data class Objective(
    val id: String,
    val description: String,
    var isCompleted: Boolean = false,
    val targetLocationName: String? = null,
    val targetRadiusMeters: Float = 5.0f
)

data class Mission(
    val id: String,
    val chapter: Int,
    val title: String,
    val subtitle: String,
    val briefing: String,
    val objectives: List<Objective>,
    val xpReward: Int,
    val isMainStory: Boolean = true,
    var isCompleted: Boolean = false,
    var isCurrentActive: Boolean = false
)

data class EngineeringDecisionOption(
    val id: String,
    val title: String,
    val description: String,
    val requiredRole: CharacterRole? = null,
    val requiredSkillLevel: Int = 0,
    val pros: List<String>,
    val cons: List<String>,
    val outcomeLog: String
)

data class EngineeringCrisis(
    val id: String,
    val title: String,
    val context: String,
    val options: List<EngineeringDecisionOption>
)
