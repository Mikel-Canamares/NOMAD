package com.nomad.app.data.repository

import android.util.Log
import com.nomad.app.data.api.RetrofitClient
import com.nomad.app.data.dto.toPOI
import com.nomad.app.model.POI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

class PoiRepository {

    private val apiService = RetrofitClient.apiService

    // Caché en memoria
    private var cachedPois: List<POI>? = null
    private var lastCacheTime: Long = 0
    private val cacheValidityMillis = 10.minutes.inWholeMilliseconds

    /**
     * Obtiene POIs cercanos con caché de 10 minutos
     */
    suspend fun getNearbyPois(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 1000,
        forceRefresh: Boolean = false
    ): Result<List<POI>> = withContext(Dispatchers.IO) {
        try {
            // Verificar si el caché es válido
            val now = System.currentTimeMillis()
            if (!forceRefresh && cachedPois != null && (now - lastCacheTime) < cacheValidityMillis) {
                Log.d("PoiRepository", "Retornando ${cachedPois!!.size} POIs desde caché")
                return@withContext Result.success(cachedPois!!)
            }

            // Obtener datos del API
            Log.d("PoiRepository", "Solicitando POIs desde API: lat=$latitude, lng=$longitude, radius=$radiusMeters")
            val poisDto = apiService.getNearbyPois(
                lat = latitude,
                lng = longitude,
                radius = radiusMeters.toDouble()
            )
            Log.d("PoiRepository", "Respuesta recibida: ${poisDto.size} POIs")
            val pois = poisDto.map { it.toPOI() }

            // Actualizar caché
            cachedPois = pois
            lastCacheTime = now

            Result.success(pois)
        } catch (e: Exception) {
            Log.e("PoiRepository", "Error obteniendo POIs: ${e.message}", e)
            // Si hay error pero tenemos caché, devolver caché
            if (cachedPois != null) {
                Log.d("PoiRepository", "Usando caché tras error: ${cachedPois!!.size} POIs")
                Result.success(cachedPois!!)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Invalida el caché
     */
    fun clearCache() {
        cachedPois = null
        lastCacheTime = 0
    }

    /**
     * Verifica si el caché es válido
     */
    fun isCacheValid(): Boolean {
        val now = System.currentTimeMillis()
        return cachedPois != null && (now - lastCacheTime) < cacheValidityMillis
    }

    /**
     * Obtiene el tiempo restante del caché en minutos
     */
    fun getCacheTimeRemainingMinutes(): Long {
        if (cachedPois == null) return 0
        val now = System.currentTimeMillis()
        val elapsed = now - lastCacheTime
        val remaining = cacheValidityMillis - elapsed
        return if (remaining > 0) remaining / 60000 else 0
    }
}
