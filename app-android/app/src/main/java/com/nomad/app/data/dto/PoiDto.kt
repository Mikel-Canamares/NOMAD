package com.nomad.app.data.dto

import com.google.android.gms.maps.model.LatLng
import com.nomad.app.model.POI
import com.nomad.app.model.POICategory
import kotlinx.serialization.Serializable

@Serializable
data class PoiDto(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val source: String? = null,
    val license: String? = null,
    val relevance: Double? = null
)

// Extensión para convertir DTO a modelo de dominio
fun PoiDto.toPOI(): POI {
    return POI(
        id = id,
        name = name,
        description = buildDescription(),
        location = LatLng(lat, lng),
        category = mapCategory(category),
        distance = null // El backend no devuelve distancia
    )
}

private fun PoiDto.buildDescription(): String {
    return buildString {
        append(category.replaceFirstChar { it.uppercase() })
        source?.let { append(" • $it") }
        relevance?.let { append(" • ${String.format("%.0f%%", it * 100)} relevante") }
    }
}

private fun mapCategory(category: String): POICategory {
    // Primero intentar mapeo directo con nuevas categorías
    POICategory.values().find { it.apiKey.equals(category, ignoreCase = true) }
        ?.let { return it }

    // Fallback: compatibilidad con categorías antiguas del backend
    return POICategory.fromLegacyCategory(category)
}
