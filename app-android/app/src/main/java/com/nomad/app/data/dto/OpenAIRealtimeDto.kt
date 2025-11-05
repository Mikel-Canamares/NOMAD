package com.nomad.app.data.dto

import kotlinx.serialization.Serializable

/**
 * DTOs para comunicación con OpenAI Realtime API (WebRTC)
 */

@Serializable
data class SdpOfferRequest(
    val type: String = "offer",
    val sdp: String
)

@Serializable
data class SdpAnswerResponse(
    val type: String,
    val sdp: String
)

@Serializable
data class IceCandidateMessage(
    val type: String = "ice",
    val candidate: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int
)
