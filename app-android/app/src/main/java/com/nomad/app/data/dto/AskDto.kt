package com.nomad.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AskRequest(
    val query: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class AskResponse(
    val answer: String,
    val relatedPois: List<PoiDto>? = null,
    val timestamp: Long = System.currentTimeMillis()
)
