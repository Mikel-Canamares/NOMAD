package com.nomad.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Categorías temáticas según especificación del documento
 * Cada categoría tiene:
 * - displayName: Nombre visible para el usuario
 * - color: Color del tema (según documento)
 * - apiKey: Identificador para API backend
 * - icon: Icono Material para UI
 */
enum class POICategory(
    val displayName: String,
    val color: Color,
    val apiKey: String,
    val icon: ImageVector
) {
    HISTORIA(
        displayName = "Historia",
        color = Color(0xFFFFEB3B), // Amarillo
        apiKey = "history",
        icon = Icons.Default.AccountBalance // Edificio histórico
    ),
    GASTRONOMIA(
        displayName = "Gastronomía",
        color = Color(0xFF9C27B0), // Morado
        apiKey = "food",
        icon = Icons.Default.Restaurant
    ),
    ARTE(
        displayName = "Arte y arquitectura",
        color = Color(0xFFE91E63), // Rosa
        apiKey = "art",
        icon = Icons.Default.Palette
    ),
    DEPORTES(
        displayName = "Deportes y ocio",
        color = Color(0xFFFF9800), // Naranja
        apiKey = "sports",
        icon = Icons.Default.SportsBasketball
    ),
    GEOGRAFIA(
        displayName = "Geografía",
        color = Color(0xFF2196F3), // Azul
        apiKey = "geography",
        icon = Icons.Default.Terrain // Montañas/paisaje
    ),
    INDUSTRIA(
        displayName = "Industria y agricultura",
        color = Color(0xFF4CAF50), // Verde
        apiKey = "industry",
        icon = Icons.Default.Business // Fábrica/industria
    );

    companion object {
        /**
         * Convierte un apiKey del backend a POICategory
         */
        fun fromApiKey(key: String): POICategory {
            return values().find { it.apiKey.equals(key, ignoreCase = true) }
                ?: HISTORIA // Default fallback
        }

        /**
         * Para compatibilidad con categorías antiguas del backend
         * Mapea las categorías antiguas (restaurant, museum, etc.) a las nuevas
         */
        @Deprecated("Solo para migración temporal")
        fun fromLegacyCategory(legacy: String): POICategory {
            return when (legacy.lowercase()) {
                "restaurant", "cafe" -> GASTRONOMIA
                "museum" -> when {
                    // Intentar distinguir tipo de museo (simplificado)
                    else -> ARTE // Por defecto museos van a Arte
                }
                "park" -> GEOGRAFIA
                "monument", "historical_landmark" -> HISTORIA
                "viewpoint" -> GEOGRAFIA
                "heritage", "cultural_landmark" -> HISTORIA
                else -> HISTORIA
            }
        }
    }
}

// Lista de categorías para filtros
fun getAvailableCategories(): List<POICategory> {
    return POICategory.values().toList()
}
