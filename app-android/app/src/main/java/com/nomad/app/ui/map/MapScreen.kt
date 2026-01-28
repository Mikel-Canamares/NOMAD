package com.nomad.app.ui.map

import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.nomad.app.data.preferences.UserPreferencesRepository
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
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ViewModel para asistente de voz integrado
    val voiceViewModel: VoiceViewModel = viewModel()
    val isVoiceActive by voiceViewModel.isActive.collectAsState()
    val isPreparing by voiceViewModel.isPreparing.collectAsState()
    val assistantState by voiceViewModel.assistantState.collectAsState()
    val audioPermissionState = rememberAudioPermissionState()

    // Preferencias de usuario
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = com.nomad.app.data.preferences.UserPreferences())

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

    // Repositorios con URL dinámica desde preferencias
    val poiRepository = remember(userPreferences.backendUrl) {
        PoiRepository(userPreferences.backendUrl)
    }
    val askRepository = remember(userPreferences.backendUrl) {
        AskRepository(userPreferences.backendUrl)
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Control de actualización de POIs: solo actualizar si la ubicación cambió al menos 200m
    var lastPoiLoadLocation by remember { mutableStateOf<LatLng?>(null) }
    val minDistanceForUpdateMeters = 200.0

    fun shouldUpdatePois(newLocation: Location): Boolean {
        val lastLocation = lastPoiLoadLocation ?: return true
        val newLatLng = LatLng(newLocation.latitude, newLocation.longitude)

        // Calcular distancia aproximada en metros (fórmula de Haversine simplificada)
        val latDiff = Math.abs(newLatLng.latitude - lastLocation.latitude)
        val lngDiff = Math.abs(newLatLng.longitude - lastLocation.longitude)
        val avgLat = (newLatLng.latitude + lastLocation.latitude) / 2

        // 1 grado de latitud ≈ 111km, 1 grado de longitud ≈ 111km * cos(lat)
        val distanceMeters = Math.sqrt(
            Math.pow(latDiff * 111000, 2.0) +
            Math.pow(lngDiff * 111000 * Math.cos(Math.toRadians(avgLat)), 2.0)
        )

        return distanceMeters >= minDistanceForUpdateMeters
    }

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

            // Actualizar contexto
            voiceViewModel.updateContext(
                userLat = currentLocation!!.latitude,
                userLng = currentLocation!!.longitude,
                selectedCategory = selectedCategory?.displayName,
                pois = poisData
            )

            Log.d("MapScreen", "Contexto inicial enviado al asistente - POIs: ${pois.size}, Ubicación: ${currentLocation?.latitude}, ${currentLocation?.longitude}, Categoría: ${selectedCategory?.displayName}")
        }
    }

    // Cargar POIs cercanos cuando cambia ubicación o categoría
    // Solo actualiza si la ubicación cambió al menos 200m O si cambió la categoría
    LaunchedEffect(currentLocation, selectedCategory) {
        currentLocation?.let { location ->
            // Verificar si debemos actualizar POIs
            val categoryChanged = selectedCategory != null // Si hay categoría seleccionada, siempre actualizar
            val locationChanged = shouldUpdatePois(location)

            if (locationChanged || categoryChanged) {
                Log.d("MapScreen", "Cargando POIs para ubicación: ${location.latitude}, ${location.longitude}, categoría: ${selectedCategory?.apiValue} (distancia: $locationChanged, categoría: $categoryChanged)")
                isLoadingPois = true
                errorMessage = null

                val result = poiRepository.getNearbyPois(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusMeters = userPreferences.poiRadiusMeters,
                    category = selectedCategory?.apiValue,
                    limit = 40
                )

                result.onSuccess { loadedPois ->
                    Log.d("MapScreen", "POIs cargados exitosamente: ${loadedPois.size} POIs")
                    pois = loadedPois
                    lastPoiLoadLocation = LatLng(location.latitude, location.longitude)
                }.onFailure { error ->
                    Log.e("MapScreen", "Error cargando POIs: ${error.message}", error)
                    errorMessage = "Fuente temporalmente saturada. Inténtalo de nuevo."
                    snackbarHostState.showSnackbar(errorMessage!!)
                }

                isLoadingPois = false
            } else {
                Log.d("MapScreen", "Ubicación demasiado cerca de la anterior, usando POIs cacheados")
            }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NOMAD",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(56.dp) // Touch target grande
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Chips de filtro de categorías con colores e iconos
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
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(20.dp)
                                )
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .defaultMinSize(minHeight = 48.dp), // Mínimo 48dp para touch target
                        leadingIcon = {
                            if (selectedCategory == category) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Mapa de Google - Optimizado para modo conductor
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
                    myLocationButtonEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = false, // Desactivar toolbar de Google Maps para simplificar UI
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = false, // Desactivar inclinación para simplificar navegación
                    zoomGesturesEnabled = true
                )
            ) {
                // Mostrar solo los primeros 40 marcadores si hay muchos POIs
                val poisToShow = if (pois.size > 40) {
                    Log.d("MapScreen", "Showing only first 40 of ${pois.size} POIs")
                    pois.take(40)
                } else {
                    pois
                }

                // Marcadores con colores según categoría
                poisToShow.forEach { poi ->
                    // Determinar color del marcador según categoría
                    val markerColor = when (poi.category) {
                        POICategory.HISTORIA -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW
                        POICategory.GASTRONOMIA -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                        POICategory.ARTE -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ROSE
                        POICategory.DEPORTES -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                        POICategory.GEOGRAFIA -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                        POICategory.INDUSTRIA -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    }

                    Marker(
                        state = MarkerState(position = poi.location),
                        title = poi.name,
                        snippet = poi.description,
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor),
                        onClick = {
                            // NUEVO: Detener TTS si está hablando
                            voiceViewModel.stopSpeaking()

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

        // Mensaje de estado del asistente - Alto contraste para visibilidad
        if (isVoiceActive || isPreparing) {
            val statusText = when {
                isPreparing -> "Inicializando..."
                assistantState == VoiceViewModel.AssistantState.INITIALIZING -> "Inicializando..."
                assistantState == VoiceViewModel.AssistantState.LISTENING -> "Escuchando..."
                assistantState == VoiceViewModel.AssistantState.PROCESSING -> "Procesando..."
                assistantState == VoiceViewModel.AssistantState.SPEAKING -> "Respondiendo..."
                else -> "Activo"
            }

            androidx.compose.material3.Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 104.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                tonalElevation = 6.dp
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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

        // FAB para mostrar lista de POIs - Tamaño grande para modo conductor
        FloatingActionButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp), // Tamaño grande para accesibilidad
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Mostrar POIs",
                modifier = Modifier.size(28.dp) // Icono grande
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
                },
                onAskAssistant = { query ->
                    // Usar el asistente de voz para hablar sobre este POI
                    voiceViewModel.speakAbout(query)
                }
            )
        }
        }
    }
}
