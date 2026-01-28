package com.nomad.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoiceChatResponse(
    val response: String,
    val conversationId: String
)
