package com.nomad.app.ui.map

import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.nomad.app.data.repository.PoiRepository
import com.nomad.app.location.LocationManager
import com.nomad.app.model.POI
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    locationManager: LocationManager,
    modifier: Modifier = Modifier
) {
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var voiceState by remember { mutableStateOf(VoiceState.INACTIVE) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var pois by remember { mutableStateOf<List<POI>>(emptyList()) }
    var isLoadingPois by remember { mutableStateOf(false) }
    var poisError by remember { mutableStateOf<String?>(null) }
    val poiRepository = remember { PoiRepository() }
    val scope = rememberCoroutineScope()

    // Obtener ubicación actual
    LaunchedEffect(Unit) {
        currentLocation = locationManager.getCurrentLocation()
    }

    // Cargar POIs cercanos cuando se obtiene la ubicación
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            Log.d("MapScreen", "Cargando POIs para ubicación: ${location.latitude}, ${location.longitude}")
            isLoadingPois = true
            poisError = null

            val result = poiRepository.getNearbyPois(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMeters = 1000 // Reducido a 1km para evitar exceder límite del buffer
            )

            result.onSuccess { loadedPois ->
                Log.d("MapScreen", "POIs cargados exitosamente: ${loadedPois.size} POIs")
                pois = loadedPois
            }.onFailure { error ->
                Log.e("MapScreen", "Error cargando POIs: ${error.message}", error)
                poisError = error.message
            }

            isLoadingPois = false
        }
    }

    val defaultLocation = LatLng(40.4169, -3.7035) // Madrid centro
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Centrar cámara en ubicación actual cuando esté disponible
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Mapa de Google
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = currentLocation != null
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true
            )
        ) {
            // Marcadores de POIs
            pois.forEach { poi ->
                Marker(
                    state = MarkerState(position = poi.location),
                    title = poi.name,
                    snippet = poi.description
                )
            }
        }

        // FAB Push-to-Talk
        PushToTalkFab(
            voiceState = voiceState,
            onToggle = {
                voiceState = when (voiceState) {
                    VoiceState.INACTIVE -> VoiceState.LISTENING
                    VoiceState.LISTENING -> VoiceState.INACTIVE
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // FAB para mostrar lista de POIs
        FloatingActionButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Mostrar POIs"
            )
        }

        // Bottom Sheet de POIs
        if (showBottomSheet) {
            POIBottomSheet(
                pois = pois,
                onDismiss = { showBottomSheet = false },
                onPOIClick = { poi ->
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(poi.location, 16f)
                        )
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}
