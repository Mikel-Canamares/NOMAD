package com.nomad.app.ui.map

import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nomad.app.data.dto.AskResponse
import com.nomad.app.data.repository.AskRepository
import com.nomad.app.data.repository.PoiRepository
import com.nomad.app.location.LocationManager
import com.nomad.app.model.POI
import com.nomad.app.model.POICategory
import com.nomad.app.model.getAvailableCategories
import com.nomad.app.permission.rememberAudioPermissionState
import com.nomad.app.ui.voice.VoiceViewModel
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    locationManager: LocationManager,
    modifier: Modifier = Modifier
) {
    // ViewModel para asistente de voz integrado
    val voiceViewModel: VoiceViewModel = viewModel()
    val isVoiceActive by voiceViewModel.isActive.collectAsState()
    val isPreparing by voiceViewModel.isPreparing.collectAsState()
    val assistantState by voiceViewModel.assistantState.collectAsState()
    val audioPermissionState = rememberAudioPermissionState()

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var pois by remember { mutableStateOf<List<POI>>(emptyList()) }
    var selectedPoi by remember { mutableStateOf<POI?>(null) }
    var selectedPoiId by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<POICategory?>(POICategory.MONUMENT) }
    var isLoadingPois by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showInfoBottomSheet by remember { mutableStateOf(false) }
    var showMarkerDetailSheet by remember { mutableStateOf(false) }
    var askResponse by remember { mutableStateOf<AskResponse?>(null) }
    var isLoadingAsk by remember { mutableStateOf(false) }
    val poiRepository = remember { PoiRepository() }
    val askRepository = remember { AskRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Obtener ubicación actual
    LaunchedEffect(Unit) {
        currentLocation = locationManager.getCurrentLocation()
    }

    // Actualizar permiso de audio en el ViewModel
    LaunchedEffect(audioPermissionState.isGranted) {
        voiceViewModel.updateMicPermission(audioPermissionState.isGranted)
    }

    // Enviar contexto inicial al asistente cuando se conecte
    LaunchedEffect(isVoiceActive) {
        if (isVoiceActive && currentLocation != null) {
            // Esperar un poco para que el data channel esté listo
            kotlinx.coroutines.delay(1000)

            // Preparar lista de POIs como mapas
            val poisData = pois.map { poi ->
                mapOf<String, Any>(
                    "name" to poi.name,
                    "category" to (poi.category ?: "unknown"),
                    "lat" to poi.location.latitude,
                    "lng" to poi.location.longitude
                )
            }

            // Enviar contexto inicial
            voiceViewModel.sendInitialContext(
                userLat = currentLocation!!.latitude,
                userLng = currentLocation!!.longitude,
                pois = poisData,
                selectedCategory = selectedCategory?.displayName
            )

            Log.d("MapScreen", "Contexto inicial enviado al asistente - POIs: ${pois.size}, Ubicación: ${currentLocation?.latitude}, ${currentLocation?.longitude}, Categoría: ${selectedCategory?.displayName}")
        }
    }

    // Cargar POIs cercanos cuando cambia ubicación o categoría
    LaunchedEffect(currentLocation, selectedCategory) {
        currentLocation?.let { location ->
            Log.d("MapScreen", "Cargando POIs para ubicación: ${location.latitude}, ${location.longitude}, categoría: ${selectedCategory?.apiValue}")
            isLoadingPois = true
            errorMessage = null

            val result = poiRepository.getNearbyPois(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMeters = 1200,
                category = selectedCategory?.apiValue,
                limit = 15
            )

            result.onSuccess { loadedPois ->
                Log.d("MapScreen", "POIs cargados exitosamente: ${loadedPois.size} POIs")
                pois = loadedPois
            }.onFailure { error ->
                Log.e("MapScreen", "Error cargando POIs: ${error.message}", error)
                errorMessage = "Fuente temporalmente saturada. Inténtalo de nuevo."
                snackbarHostState.showSnackbar(errorMessage!!)
            }

            isLoadingPois = false
        }
    }

    val defaultLocation = LatLng(40.4169, -3.7035) // Madrid centro
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Centrar cámara en ubicación actual cuando esté disponible (solo una vez)
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            if (cameraPositionState.position.target == defaultLocation) {
                val latLng = LatLng(location.latitude, location.longitude)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chips de filtro de categorías
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(getAvailableCategories()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(category.displayName) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Mapa de Google
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = currentLocation != null
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true
                )
            ) {
                // Mostrar solo los primeros 40 marcadores si hay muchos POIs
                val poisToShow = if (pois.size > 40) {
                    Log.d("MapScreen", "Showing only first 40 of ${pois.size} POIs")
                    pois.take(40)
                } else {
                    pois
                }

                // Marcadores
                poisToShow.forEach { poi ->
                    Marker(
                        state = MarkerState(position = poi.location),
                        title = poi.name,
                        snippet = poi.description,
                        onClick = {
                            selectedPoi = poi
                            selectedPoiId = poi.id
                            showMarkerDetailSheet = true
                            isLoadingAsk = true

                            scope.launch {
                                // Animar cámara
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(poi.location, 16f)
                                )

                                // Llamar al endpoint /ask
                                val result = askRepository.ask(
                                    text = poi.name,
                                    locale = "es",
                                    poiId = poi.id
                                )

                                result.onSuccess { response ->
                                    askResponse = response
                                    isLoadingAsk = false
                                }.onFailure { error ->
                                    Log.e("MapScreen", "Error loading POI details: ${error.message}")
                                    askResponse = null
                                    isLoadingAsk = false
                                }
                            }
                            true
                        }
                    )
                }
            }
        }

        // Mensaje de estado del asistente
        if (isVoiceActive || isPreparing) {
            val statusText = when {
                isPreparing -> "Conectando..."
                assistantState == VoiceViewModel.AssistantState.CONNECTING -> "Conectando..."
                assistantState == VoiceViewModel.AssistantState.LISTENING -> "Escuchando..."
                assistantState == VoiceViewModel.AssistantState.THINKING -> "Pensando..."
                assistantState == VoiceViewModel.AssistantState.SPEAKING -> "Respondiendo..."
                else -> "Activo"
            }

            androidx.compose.material3.Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 104.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }

        // FAB Asistente de Voz (integrado en el mapa)
        VoiceAssistantFab(
            isActive = isVoiceActive,
            isPreparing = isPreparing,
            onClick = {
                if (!audioPermissionState.isGranted) {
                    audioPermissionState.requestPermission()
                } else {
                    if (isVoiceActive) {
                        voiceViewModel.stopVoiceAssistant()
                    } else {
                        voiceViewModel.startVoiceAssistant()
                    }
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

        // Snackbar para errores
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        )

        // Bottom Sheet de POIs
        if (showBottomSheet) {
            POIBottomSheet(
                pois = pois,
                selectedCategory = selectedCategory,
                onDismiss = { showBottomSheet = false },
                onPOIClick = { poi ->
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(poi.location, 16f)
                        )
                        showBottomSheet = false
                        selectedPoi = poi
                        selectedPoiId = poi.id
                    }
                },
                onTryAnotherCategory = {
                    showBottomSheet = false
                    // La selección se hace con los chips
                },
                onMoreInfoClick = { poi ->
                    scope.launch {
                        showBottomSheet = false
                        selectedPoi = poi
                        selectedPoiId = poi.id
                        isLoadingAsk = true
                        showInfoBottomSheet = true

                        val result = askRepository.ask(
                            text = poi.name,
                            locale = "es",
                            poiId = poi.id
                        )

                        result.onSuccess { response ->
                            askResponse = response
                            isLoadingAsk = false
                        }.onFailure { error ->
                            Log.e("MapScreen", "Error fetching POI info: ${error.message}", error)
                            snackbarHostState.showSnackbar("Error al obtener información. Inténtalo de nuevo.")
                            isLoadingAsk = false
                            showInfoBottomSheet = false
                        }
                    }
                }
            )
        }

        // Bottom Sheet de información del POI (desde lista con botón "Más info")
        if (showInfoBottomSheet && selectedPoi != null) {
            POIInfoBottomSheet(
                poi = selectedPoi!!,
                askResponse = askResponse,
                isLoading = isLoadingAsk,
                onDismiss = {
                    showInfoBottomSheet = false
                    askResponse = null
                }
            )
        }

        // Bottom Sheet de detalles del POI (desde marker click)
        if (showMarkerDetailSheet && selectedPoi != null) {
            POIDetailBottomSheet(
                poiName = selectedPoi!!.name,
                askResponse = askResponse,
                isLoading = isLoadingAsk,
                onDismiss = {
                    showMarkerDetailSheet = false
                    askResponse = null
                    selectedPoi = null
                }
            )
        }
    }
}
