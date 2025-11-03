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
    RESTAURANT("Restaurantes", "restaurant"),
    MUSEUM("Museos", "museum"),
    PARK("Parques", "park"),
    MONUMENT("Monumentos", "monument"),
    VIEWPOINT("Miradores", "viewpoint"),
    HERITAGE("Patrimonio", "heritage"),
    OTHER("Otros", "")
}

// Lista de categorías para filtros
fun getAvailableCategories(): List<POICategory> {
    return listOf(
        POICategory.RESTAURANT,
        POICategory.MUSEUM,
        POICategory.PARK,
        POICategory.MONUMENT,
        POICategory.VIEWPOINT,
        POICategory.HERITAGE
    )
}
