package com.nomad.app.data.dto

import kotlinx.serialization.Serializable

/**
 * Respuesta del endpoint /realtime/session
 * Contiene el token efímero para conectarse a OpenAI Realtime API
 */
@Serializable
data class RealtimeSessionResponse(
    val client_secret: String
)
