package com.nomad.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AskRequest(
    val text: String,
    val locale: String = "es",
    val poiId: String? = null
)

@Serializable
data class AskResponse(
    val markdown: String,
    val facts: Map<String, String> = emptyMap(),
    val poi: PoiInfo? = null,
    val sources: List<SourceReference> = emptyList(),
    @SerialName("used_sources")
    val usedSources: List<String> = emptyList(),
    @SerialName("no_data")
    val noData: Boolean = false
)

@Serializable
data class PoiInfo(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: String? = null,
    val confidence: Double = 0.0
)

@Serializable
data class SourceReference(
    val title: String,
    val url: String
)
