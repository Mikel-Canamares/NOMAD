package com.nomad.app.data.repository

import android.util.Log
import com.nomad.app.data.api.RetrofitClient
import com.nomad.app.data.dto.toPOI
import com.nomad.app.model.POI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

class PoiRepository {

    private val apiService = RetrofitClient.apiService

    // Caché en memoria por (geohash5+cat)
    private val cache = mutableMapOf<String, CachedPoisData>()
    private val cacheValidityMillis = 10.minutes.inWholeMilliseconds

    data class CachedPoisData(
        val pois: List<POI>,
        val timestamp: Long
    )

    /**
     * Obtiene POIs cercanos con caché de 10 minutos y retry con backoff
     */
    suspend fun getNearbyPois(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 1200,
        category: String? = null,
        limit: Int = 25,
        forceRefresh: Boolean = false
    ): Result<List<POI>> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(latitude, longitude, category)

            // Verificar si el caché es válido
            val now = System.currentTimeMillis()
            val cachedData = cache[cacheKey]
            if (!forceRefresh && cachedData != null && (now - cachedData.timestamp) < cacheValidityMillis) {
                Log.d("PoiRepository", "Retornando ${cachedData.pois.size} POIs desde caché")
                return@withContext Result.success(cachedData.pois)
            }

            // Obtener datos del API con retry
            Log.d("PoiRepository", "Solicitando POIs desde API: lat=$latitude, lng=$longitude, radius=$radiusMeters, category=$category, limit=$limit")

            val pois = retryWithBackoff(maxRetries = 3) {
                val poisDto = apiService.getNearbyPois(
                    lat = latitude,
                    lng = longitude,
                    radius = radiusMeters.toDouble(),
                    category = category,
                    limit = limit
                )
                poisDto.map { it.toPOI() }
            }

            Log.d("PoiRepository", "Respuesta recibida: ${pois.size} POIs")

            // Actualizar caché
            cache[cacheKey] = CachedPoisData(pois, now)

            Result.success(pois)
        } catch (e: Exception) {
            Log.e("PoiRepository", "Error obteniendo POIs: ${e.message}", e)

            // Si hay error pero tenemos caché, devolver caché
            val cacheKey = generateCacheKey(latitude, longitude, category)
            val cachedData = cache[cacheKey]
            if (cachedData != null) {
                Log.d("PoiRepository", "Usando caché tras error: ${cachedData.pois.size} POIs")
                Result.success(cachedData.pois)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Retry con backoff exponencial simple
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Log.w("PoiRepository", "Intento ${attempt + 1} falló, reintentando en ${currentDelay}ms: ${e.message}")
                    delay(currentDelay)
                    currentDelay *= 2 // Backoff exponencial
                }
            }
        }

        throw lastException ?: Exception("Retry failed with unknown error")
    }

    /**
     * Genera clave de caché usando geohash5 aproximado + categoría
     */
    private fun generateCacheKey(lat: Double, lng: Double, category: String?): String {
        // Geohash5 simplificado: redondear a ~5km de precisión
        val latRounded = (lat * 20).toInt() / 20.0
        val lngRounded = (lng * 20).toInt() / 20.0
        return "${latRounded}_${lngRounded}_${category ?: "all"}"
    }

    /**
     * Invalida el caché
     */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Invalida caché para una categoría específica
     */
    fun clearCacheForCategory(latitude: Double, longitude: Double, category: String?) {
        val cacheKey = generateCacheKey(latitude, longitude, category)
        cache.remove(cacheKey)
    }
}
