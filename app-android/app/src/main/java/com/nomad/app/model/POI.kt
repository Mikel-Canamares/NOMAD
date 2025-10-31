package com.nomad.app.model

import com.google.android.gms.maps.model.LatLng

/**
 * Punto de Interés (Point of Interest)
 */
data class POI(
    val id: String,
    val name: String,
    val description: String,
    val location: LatLng,
    val category: POICategory,
    val distance: Float? = null // Distancia en metros desde ubicación actual
)

enum class POICategory(val displayName: String, val apiValue: String) {
    MONUMENT("Monumentos", "monument"),
    MUSEUM("Museos", "museum"),
    VIEWPOINT("Miradores", "viewpoint"),
    HERITAGE("Patrimonio", "heritage"),
    PARK("Parques", "park"),
    OTHER("Otros", "")
}

// Lista de categorías para filtros
fun getAvailableCategories(): List<POICategory> {
    return listOf(
        POICategory.MONUMENT,
        POICategory.MUSEUM,
        POICategory.VIEWPOINT,
        POICategory.HERITAGE,
        POICategory.PARK
    )
}
