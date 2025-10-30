package com.nomad.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationManager(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Obtiene la última ubicación conocida
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene ubicación actual forzando una actualización
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Flow de actualizaciones de ubicación en tiempo real
     * @param intervalMillis Intervalo entre actualizaciones en milisegundos
     * @param priority Prioridad de precisión (PRIORITY_HIGH_ACCURACY, PRIORITY_BALANCED_POWER_ACCURACY, etc)
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(
        intervalMillis: Long = 10000L,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).await()

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Detiene todas las actualizaciones de ubicación
     */
    fun stopLocationUpdates() {
        // Las actualizaciones se detienen automáticamente cuando se cancela el Flow
    }
}
