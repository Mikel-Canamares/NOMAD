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
    return when (category.lowercase()) {
        "restaurant", "cafe", "bar" -> POICategory.RESTAURANT
        "hotel", "hostel", "guest_house" -> POICategory.HOTEL
        "monument", "attraction", "viewpoint", "artwork" -> POICategory.ATTRACTION
        "museum", "gallery" -> POICategory.MUSEUM
        "park", "garden" -> POICategory.PARK
        "shop", "shopping" -> POICategory.SHOPPING
        else -> POICategory.OTHER
    }
}
