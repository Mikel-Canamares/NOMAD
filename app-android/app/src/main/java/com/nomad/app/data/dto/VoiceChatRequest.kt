package com.nomad.app.data.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class VoiceChatRequest(
    val message: String,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val selectedCategory: String? = null,
    val nearbyPois: List<@Contextual Map<String, @Contextual Any>>? = null,
    val conversationHistory: List<ConversationMessage>? = null
)

@Serializable
data class ConversationMessage(
    val role: String,  // "user" o "assistant"
    val content: String
)
