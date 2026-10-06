package com.example.game.model

import com.example.game.engine3d.Vector3

data class DialogueChoice(
    val id: String,
    val text: String,
    val responseText: String,
    val trustDelta: Int = 0,
    val respectDelta: Int = 0,
    val friendshipDelta: Int = 0,
    val engineeringActionTrigger: String? = null,
    val requiredRole: CharacterRole? = null
)

data class DialogueNode(
    val nodeId: String,
    val speakerName: String,
    val speakerRole: String,
    val dialogueText: String,
    val choices: List<DialogueChoice>
)

data class NpcCharacter(
    val id: String,
    val name: String,
    val callsign: String,
    val roleTitle: String,
    val avatarColor: Long,
    val position: Vector3,
    var currentDialogueNode: DialogueNode,
    val trustScore: Int = 50,
    val respectScore: Int = 50,
    val friendshipScore: Int = 50,
    val routineDescription: String
)
