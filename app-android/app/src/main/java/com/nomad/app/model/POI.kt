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

enum class POICategory {
    RESTAURANT,
    HOTEL,
    ATTRACTION,
    MUSEUM,
    PARK,
    SHOPPING,
    OTHER
}

// Datos de ejemplo
object SamplePOIs {
    val madridPOIs = listOf(
        POI(
            id = "1",
            name = "Museo del Prado",
            description = "Museo nacional de pintura",
            location = LatLng(40.4138, -3.6921),
            category = POICategory.MUSEUM
        ),
        POI(
            id = "2",
            name = "Plaza Mayor",
            description = "Plaza principal de Madrid",
            location = LatLng(40.4155, -3.7074),
            category = POICategory.ATTRACTION
        ),
        POI(
            id = "3",
            name = "Parque del Retiro",
            description = "Parque histórico de Madrid",
            location = LatLng(40.4153, -3.6838),
            category = POICategory.PARK
        ),
        POI(
            id = "4",
            name = "Palacio Real",
            description = "Residencia oficial de la familia real española",
            location = LatLng(40.4179, -3.7142),
            category = POICategory.ATTRACTION
        ),
        POI(
            id = "5",
            name = "Mercado de San Miguel",
            description = "Mercado gastronómico",
            location = LatLng(40.4154, -3.7089),
            category = POICategory.RESTAURANT
        )
    )
}
