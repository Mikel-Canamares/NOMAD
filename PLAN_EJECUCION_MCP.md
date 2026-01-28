# Plan de Ejecución para MCP - NOMAD Travel Assistant

## 📋 Resumen Ejecutivo

**Estado Actual**: 70% implementado - Sistema funcional con gaps críticos
**Objetivo**: Preparar app para demo con clientes en dispositivo físico
**Tiempo Estimado**: 21-32 días (3-6 semanas)
**Primera Demo**: Posible en 2 semanas con priorización

---

## 🎯 Decisiones Estratégicas Confirmadas

- ✅ **Sin autenticación** de usuarios (ahorra 5-7 días)
- ✅ **Sin Android Auto** en MCP (ahorra 10-15 días, v2)
- ✅ **Cambiar a categorías del documento** (Historia, Gastronomía, Arte, etc.)
- ✅ **Ruta demo**: Madrid - Toledo (1 hora, rica en POIs variados)

**Balance neto**: Ahorro de 13-19 días vs implementación completa

---

## 📊 Estado Actual vs Requisitos

### ✅ Implementado (9/14 requisitos imprescindibles)

| Req | Funcionalidad | Estado |
|-----|---------------|--------|
| F1 | Geolocalización en mapa | ✅ 100% |
| F2 | 6 categorías temáticas | ⚠️ 90% (necesita remapeo) |
| F3 | Menú de intereses | ✅ 100% |
| F4 | Botón grande de interacción | ✅ 100% |
| F5 | Generación contenido IA | ⚠️ 60% |
| F6 | TTS nativo | ✅ 100% |
| F9 | Compatibilidad Android | ✅ 100% |
| F10 | Interacción proactiva | ✅ 100% |
| F11 | Conversación por voz | ✅ 100% |

### ❌ Pendiente (5 requisitos críticos)

| Req | Funcionalidad | Prioridad |
|-----|---------------|-----------|
| F7 | Menú ajustes básicos | **ALTA** |
| F12 | Ajustes de frecuencia | **ALTA** |
| F13 | Ajustes de radio | **ALTA** |
| F8 | Estética modo conductor | **ALTA** |
| - | Infraestructura producción | **ALTA** |

---

# 🚀 FASE 1: Funcionalidades Críticas (7-10 días)

## 1.0 Remapeo de Categorías (2-3 días) 🔴 PRIORIDAD CRÍTICA

### Objetivo
Cambiar de categorías actuales (restaurante, museo, parque...) a las 6 categorías especificadas en el documento con colores correctos.

### Categorías Nuevas

| Categoría | Color | Código Hex | Icono Sugerido |
|-----------|-------|------------|----------------|
| Historia | 🟡 Amarillo | `0xFFFFEB3B` | `Icons.Default.Castle` |
| Gastronomía | 🟣 Morado | `0xFF9C27B0` | `Icons.Default.Restaurant` |
| Arte y arquitectura | 🌸 Rosa | `0xFFE91E63` | `Icons.Default.Palette` |
| Deportes y ocio | 🟠 Naranja | `0xFFFF9800` | `Icons.Default.SportsBasketball` |
| Geografía | 🔵 Azul | `0xFF2196F3` | `Icons.Default.Landscape` |
| Industria y agricultura | 🟢 Verde | `0xFF4CAF50` | `Icons.Default.Factory` |

### Paso 1.0.1: Modificar Modelo POI (Android)

**Archivo**: `app-android/app/src/main/java/com/nomad/app/model/POI.kt`

```kotlin
package com.nomad.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class POI(
    val id: String,
    val name: String,
    val description: String,
    val location: LatLng,
    val category: POICategory,
    val distance: Float? = null
)

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
        fun fromApiKey(key: String): POICategory {
            return values().find { it.apiKey.equals(key, ignoreCase = true) }
                ?: HISTORIA // Default fallback
        }
    }
}
```

### Paso 1.0.2: Actualizar DTO (Android)

**Archivo**: `app-android/app/src/main/java/com/nomad/app/data/dto/PoiDto.kt`

```kotlin
package com.nomad.app.data.dto

import com.google.android.gms.maps.model.LatLng
import com.nomad.app.model.POI
import com.nomad.app.model.POICategory
import kotlinx.serialization.Serializable

@Serializable
data class PoiDto(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val source: String? = null,
    val license: String? = null,
    val relevance: Double? = null
)

fun PoiDto.toPOI(): POI {
    return POI(
        id = id,
        name = name,
        description = "", // Se llenará con /ask si es necesario
        location = LatLng(lat, lng),
        category = POICategory.fromApiKey(category)
    )
}
```

### Paso 1.0.3: Actualizar MapScreen con Nuevos Colores

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/map/MapScreen.kt`

**Cambios en línea ~50-80** (Category filter chips):

```kotlin
@Composable
private fun CategoryFilterChips(
    selectedCategories: Set<POICategory>,
    onCategoryToggle: (POICategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(POICategory.values()) { category ->
            FilterChip(
                selected = category in selectedCategories,
                onClick = { onCategoryToggle(category) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(category.displayName)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color.copy(alpha = 0.3f),
                    selectedLabelColor = category.color,
                    selectedLeadingIconColor = category.color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = category.color,
                    selectedBorderWidth = 2.dp
                )
            )
        }
    }
}
```

**Cambios en marcadores del mapa** (línea ~200-250):

```kotlin
// Renderizar marcadores con colores correctos
pois.forEach { poi ->
    val markerColor = when (poi.category) {
        POICategory.HISTORIA -> BitmapDescriptorFactory.HUE_YELLOW
        POICategory.GASTRONOMIA -> BitmapDescriptorFactory.HUE_VIOLET
        POICategory.ARTE -> BitmapDescriptorFactory.HUE_ROSE
        POICategory.DEPORTES -> BitmapDescriptorFactory.HUE_ORANGE
        POICategory.GEOGRAFIA -> BitmapDescriptorFactory.HUE_AZURE
        POICategory.INDUSTRIA -> BitmapDescriptorFactory.HUE_GREEN
    }

    Marker(
        state = rememberMarkerState(position = poi.location),
        title = poi.name,
        snippet = poi.category.displayName,
        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
        onClick = {
            selectedPoi = poi
            showDetailSheet = true
            true
        }
    )
}
```

### Paso 1.0.4: Actualizar Backend - Controller

**Archivo**: `backend/src/main/java/com/nomad/controller/PoiController.java`

```java
package com.nomad.controller;

import com.nomad.dto.PoiResponse;
import com.nomad.service.PoiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/poi")
public class PoiController {

    // Categorías permitidas según documento
    private static final Set<String> VALID_CATEGORIES = Set.of(
        "history",      // Historia
        "food",         // Gastronomía
        "art",          // Arte y arquitectura
        "sports",       // Deportes y ocio
        "geography",    // Geografía
        "industry"      // Industria y agricultura
    );

    @Autowired
    private PoiService poiService;

    @GetMapping("/nearby")
    public ResponseEntity<List<PoiResponse>> getNearbyPois(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1200") int radius,
            @RequestParam(required = false) String cat,
            @RequestParam(defaultValue = "40") int limit,
            @RequestParam(defaultValue = "es") String locale,
            @RequestParam(defaultValue = "ES") String region
    ) {
        // Validar categoría
        if (cat != null && !VALID_CATEGORIES.contains(cat.toLowerCase())) {
            return ResponseEntity.badRequest()
                .header("X-Error", "Invalid category. Allowed: " + VALID_CATEGORIES)
                .build();
        }

        List<PoiResponse> pois = poiService.getNearbyPois(
            lat, lng, radius, cat, limit, locale, region
        );

        return ResponseEntity.ok(pois);
    }
}
```

### Paso 1.0.5: Actualizar Backend - Service (Mapeo Google Places)

**Archivo**: `backend/src/main/java/com/nomad/service/GooglePlacesService.java`

Buscar el método `getCategoryTypes()` (alrededor línea 150-200) y reemplazar con:

```java
private List<String> getCategoryTypes(String category) {
    if (category == null) return List.of();

    return switch (category.toLowerCase()) {
        case "history" -> List.of(
            "historical_landmark",
            "monument",
            "cultural_landmark",
            "museum",  // Museos históricos
            "archaeological_site"
        );

        case "food" -> List.of(
            "restaurant",
            "cafe",
            "bakery",
            "bar",
            "food"
        );

        case "art" -> List.of(
            "art_gallery",
            "museum",  // Museos de arte
            "cultural_center",
            "performing_arts_theater",
            "library",
            "architectural_monument"
        );

        case "sports" -> List.of(
            "stadium",
            "gym",
            "sports_complex",
            "park",
            "amusement_park",
            "tourist_attraction",
            "aquarium",
            "zoo"
        );

        case "geography" -> List.of(
            "natural_feature",
            "park",
            "national_park",
            "hiking_area",
            "mountain_peak",
            "viewpoint",
            "scenic_point"
        );

        case "industry" -> List.of(
            "farm",
            "winery",
            "brewery",
            "visitor_center",
            "factory_tour"
        );

        default -> List.of();
    };
}
```

### Paso 1.0.6: Testing del Remapeo

**Checklist de validación**:
- [ ] Compilar app Android sin errores
- [ ] Compilar backend sin errores
- [ ] Verificar que chips de categorías muestren colores correctos
- [ ] Probar filtro de cada categoría individualmente
- [ ] Verificar que marcadores en mapa tengan colores correctos
- [ ] Probar endpoint `/api/poi/nearby?cat=history`
- [ ] Verificar que backend rechace categorías inválidas

---

## 1.1 Implementar Menú de Ajustes (3-4 días) 🔴 PRIORIDAD CRÍTICA

### Objetivo
Crear pantalla de configuración con ajustes persistentes usando DataStore.

### Paso 1.1.1: Añadir Dependencias

**Archivo**: `app-android/app/build.gradle.kts`

```kotlin
dependencies {
    // Existing dependencies...

    // DataStore para preferencias
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Navigation para Settings (si no está)
    implementation("androidx.navigation:navigation-compose:2.7.5")
}
```

### Paso 1.1.2: Crear Data Classes de Preferencias

**Archivo NUEVO**: `app-android/app/src/main/java/com/nomad/app/data/preferences/UserPreferences.kt`

```kotlin
package com.nomad.app.data.preferences

data class UserPreferences(
    // Radio de POIs en metros
    val poiRadiusMeters: Int = 2000,

    // Frecuencia de avisos proactivos (en minutos, 0 = desactivado)
    val proactiveAlertFrequencyMinutes: Int = 5,

    // Velocidad de narración TTS (0.5 - 2.0)
    val ttsSpeed: Float = 1.0f,

    // Pitch de voz TTS (0.5 - 2.0)
    val ttsPitch: Float = 1.0f,

    // URL del backend (configurable para testing)
    val backendUrl: String = DEFAULT_BACKEND_URL,

    // Idioma/locale (futuro)
    val locale: String = "es"
) {
    companion object {
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8081/api/"

        // Opciones predefinidas para radio
        val RADIUS_OPTIONS = listOf(
            100 to "100 metros",
            500 to "500 metros",
            1000 to "1 kilómetro",
            2000 to "2 kilómetros",
            5000 to "5 kilómetros"
        )

        // Opciones para frecuencia de avisos
        val FREQUENCY_OPTIONS = listOf(
            0 to "Solo bajo demanda",
            2 to "Cada 2 minutos",
            5 to "Cada 5 minutos",
            10 to "Cada 10 minutos",
            15 to "Cada 15 minutos"
        )
    }
}
```

### Paso 1.1.3: Crear Repository de Preferencias

**Archivo NUEVO**: `app-android/app/src/main/java/com/nomad/app/data/preferences/UserPreferencesRepository.kt`

```kotlin
package com.nomad.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val POI_RADIUS = intPreferencesKey("poi_radius_meters")
        val ALERT_FREQUENCY = intPreferencesKey("proactive_alert_frequency_minutes")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val LOCALE = stringPreferencesKey("locale")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                poiRadiusMeters = preferences[PreferencesKeys.POI_RADIUS] ?: 2000,
                proactiveAlertFrequencyMinutes = preferences[PreferencesKeys.ALERT_FREQUENCY] ?: 5,
                ttsSpeed = preferences[PreferencesKeys.TTS_SPEED] ?: 1.0f,
                ttsPitch = preferences[PreferencesKeys.TTS_PITCH] ?: 1.0f,
                backendUrl = preferences[PreferencesKeys.BACKEND_URL]
                    ?: UserPreferences.DEFAULT_BACKEND_URL,
                locale = preferences[PreferencesKeys.LOCALE] ?: "es"
            )
        }

    suspend fun updatePoiRadius(radiusMeters: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POI_RADIUS] = radiusMeters
        }
    }

    suspend fun updateAlertFrequency(frequencyMinutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_FREQUENCY] = frequencyMinutes
        }
    }

    suspend fun updateTtsSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_SPEED] = speed.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun updateTtsPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_PITCH] = pitch.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun updateBackendUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKEND_URL] = url
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
```

### Paso 1.1.4: Crear ViewModel de Settings

**Archivo NUEVO**: `app-android/app/src/main/java/com/nomad/app/ui/settings/SettingsViewModel.kt`

```kotlin
package com.nomad.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.app.data.preferences.UserPreferences
import com.nomad.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun updatePoiRadius(radiusMeters: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePoiRadius(radiusMeters)
        }
    }

    fun updateAlertFrequency(frequencyMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateAlertFrequency(frequencyMinutes)
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesRepository.updateTtsSpeed(speed)
        }
    }

    fun updateTtsPitch(pitch: Float) {
        viewModelScope.launch {
            preferencesRepository.updateTtsPitch(pitch)
        }
    }

    fun updateBackendUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.updateBackendUrl(url)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }
}
```

### Paso 1.1.5: Crear UI de Settings Screen

**Archivo NUEVO**: `app-android/app/src/main/java/com/nomad/app/ui/settings/SettingsScreen.kt`

```kotlin
package com.nomad.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nomad.app.data.preferences.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, "Restablecer")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección: Mapa y POIs
            SettingsSection(
                title = "Mapa y Puntos de Interés",
                icon = Icons.Default.Map
            ) {
                RadiusSelector(
                    currentRadius = preferences.poiRadiusMeters,
                    onRadiusChange = viewModel::updatePoiRadius
                )
            }

            // Sección: Asistente de Voz
            SettingsSection(
                title = "Asistente de Voz",
                icon = Icons.Default.RecordVoiceOver
            ) {
                FrequencySelector(
                    currentFrequency = preferences.proactiveAlertFrequencyMinutes,
                    onFrequencyChange = viewModel::updateAlertFrequency
                )

                Spacer(modifier = Modifier.height(16.dp))

                TtsSpeedSlider(
                    currentSpeed = preferences.ttsSpeed,
                    onSpeedChange = viewModel::updateTtsSpeed
                )

                Spacer(modifier = Modifier.height(16.dp))

                TtsPitchSlider(
                    currentPitch = preferences.ttsPitch,
                    onPitchChange = viewModel::updateTtsPitch
                )
            }

            // Sección: Avanzado (solo para desarrollo)
            SettingsSection(
                title = "Avanzado",
                icon = Icons.Default.DeveloperMode
            ) {
                BackendUrlInput(
                    currentUrl = preferences.backendUrl,
                    onUrlChange = viewModel::updateBackendUrl
                )
            }
        }
    }

    // Diálogo de confirmación para restablecer
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restablecer ajustes") },
            text = { Text("¿Quieres restablecer todos los ajustes a sus valores predeterminados?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Restablecer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun RadiusSelector(
    currentRadius: Int,
    onRadiusChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Radio de búsqueda",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Muestra POIs dentro de este radio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserPreferences.RADIUS_OPTIONS.forEach { (radius, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentRadius == radius,
                    onClick = { onRadiusChange(radius) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun FrequencySelector(
    currentFrequency: Int,
    onFrequencyChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Frecuencia de avisos proactivos",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Cada cuánto tiempo el asistente te avisa de puntos cercanos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserPreferences.FREQUENCY_OPTIONS.forEach { (frequency, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentFrequency == frequency,
                    onClick = { onFrequencyChange(frequency) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun TtsSpeedSlider(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Velocidad de narración: ${String.format("%.1fx", currentSpeed)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = currentSpeed,
            onValueChange = onSpeedChange,
            valueRange = 0.5f..2.0f,
            steps = 14 // 0.1 increments
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lento (0.5x)", style = MaterialTheme.typography.bodySmall)
            Text("Rápido (2.0x)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TtsPitchSlider(
    currentPitch: Float,
    onPitchChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Tono de voz: ${String.format("%.1f", currentPitch)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = currentPitch,
            onValueChange = onPitchChange,
            valueRange = 0.5f..2.0f,
            steps = 14
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Grave (0.5)", style = MaterialTheme.typography.bodySmall)
            Text("Agudo (2.0)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BackendUrlInput(
    currentUrl: String,
    onUrlChange: (String) -> Unit
) {
    var editedUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "URL del servidor (desarrollo)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        if (isEditing) {
            OutlinedTextField(
                value = editedUrl,
                onValueChange = { editedUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL del backend") },
                supportingText = { Text("Ejemplo: http://10.0.2.2:8081/api/") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    isEditing = false
                    editedUrl = currentUrl
                }) {
                    Text("Cancelar")
                }
                Button(onClick = {
                    onUrlChange(editedUrl)
                    isEditing = false
                }) {
                    Text("Guardar")
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isEditing = true }
            ) {
                Text(
                    text = currentUrl,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

### Paso 1.1.6: Añadir Navigation a Settings

**Archivo**: `app-android/app/src/main/java/com/nomad/app/NomadApp.kt`

Modificar la navegación para incluir SettingsScreen:

```kotlin
package com.nomad.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nomad.app.ui.map.MapScreen
import com.nomad.app.ui.settings.SettingsScreen

@Composable
fun NomadApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "map"
    ) {
        composable("map") {
            MapScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
```

### Paso 1.1.7: Añadir Botón Settings en MapScreen

En `MapScreen.kt`, añadir botón en TopAppBar:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NOMAD") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Ajustes")
                    }
                }
            )
        },
        // ... resto del código
    )
}
```

### Paso 1.1.8: Integrar Preferencias en RetrofitClient

**Archivo**: `app-android/app/src/main/java/com/nomad/app/data/api/RetrofitClient.kt`

```kotlin
package com.nomad.app.data.api

import android.content.Context
import com.nomad.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext
    }

    private fun getBaseUrl(): String {
        return context?.let { ctx ->
            val repository = UserPreferencesRepository(ctx)
            runBlocking {
                repository.userPreferencesFlow.first().backendUrl
            }
        } ?: "http://10.0.2.2:8081/api/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val nomadApiService: NomadApiService by lazy {
        retrofit.create(NomadApiService::class.java)
    }

    val voiceChatApiService: VoiceChatApiService by lazy {
        retrofit.create(VoiceChatApiService::class.java)
    }

    // Método para recrear Retrofit con nueva URL
    fun refreshWithNewUrl() {
        // Forzar recreación del retrofit con nueva URL
        // Nota: Esto es simplificado, en producción usar Dagger/Hilt
    }
}
```

En `MainActivity.kt`, inicializar:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Inicializar RetrofitClient con contexto
    RetrofitClient.initialize(applicationContext)

    setContent {
        NomadTheme {
            NomadApp()
        }
    }
}
```

### Paso 1.1.9: Testing de Settings

**Checklist**:
- [ ] Navegar a Settings desde MapScreen
- [ ] Cambiar radio de POIs y verificar persistencia
- [ ] Cambiar frecuencia de avisos y verificar persistencia
- [ ] Ajustar velocidad TTS y probar en voz
- [ ] Ajustar pitch TTS y probar en voz
- [ ] Modificar URL backend (solo testing)
- [ ] Cerrar app y verificar que ajustes se mantienen
- [ ] Restablecer a valores por defecto

---

## 1.2 Completar Integración de Voz en UI (1 día)

### Objetivo
Mejorar UX de voz: detener narración al hacer clic en marcador POI y añadir botón de voz en hoja de detalles.

### Paso 1.2.1: Detener TTS al Hacer Clic en Marcador

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/map/MapScreen.kt`

En el `onClick` de `Marker` (alrededor línea 200-210):

```kotlin
Marker(
    state = rememberMarkerState(position = poi.location),
    title = poi.name,
    snippet = poi.category.displayName,
    icon = BitmapDescriptorFactory.defaultMarker(markerColor),
    onClick = {
        // NUEVO: Detener TTS si está hablando
        voiceViewModel.stopSpeaking()

        selectedPoi = poi
        showDetailSheet = true
        true
    }
)
```

### Paso 1.2.2: Añadir Botón de Voz en POIDetailBottomSheet

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/map/POIDetailBottomSheet.kt`

```kotlin
package com.nomad.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nomad.app.model.POI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POIDetailBottomSheet(
    poi: POI,
    onDismiss: () -> Unit,
    onAskAboutPoi: (POI) -> Unit, // NUEVO callback
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineSmall
            )

            // Categoría
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = poi.category.icon,
                    contentDescription = null,
                    tint = poi.category.color
                )
                Text(
                    text = poi.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = poi.category.color
                )
            }

            // Distancia (si está disponible)
            poi.distance?.let { distance ->
                Text(
                    text = "A ${String.format("%.0f", distance)} metros",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider()

            // NUEVO: Botón para preguntar al asistente
            Button(
                onClick = {
                    onAskAboutPoi(poi)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pregunta al asistente sobre este lugar")
            }

            // Botón cerrar
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar")
            }
        }
    }
}
```

### Paso 1.2.3: Conectar Botón con VoiceViewModel

En `MapScreen.kt`, actualizar el llamado a `POIDetailBottomSheet`:

```kotlin
if (showDetailSheet && selectedPoi != null) {
    POIDetailBottomSheet(
        poi = selectedPoi!!,
        onDismiss = { showDetailSheet = false },
        onAskAboutPoi = { poi ->
            // Llamar al asistente con contexto del POI
            val query = "Cuéntame sobre ${poi.name}"
            voiceViewModel.speakAbout(query)
        }
    )
}
```

### Paso 1.2.4: Añadir Método en VoiceViewModel (si no existe)

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/voice/VoiceViewModel.kt`

Verificar que existe el método `stopSpeaking()`:

```kotlin
fun stopSpeaking() {
    hybridVoiceManager?.stopSpeaking()
}
```

### Paso 1.2.5: Testing de Integración de Voz

**Checklist**:
- [ ] Iniciar narración de voz
- [ ] Hacer clic en un marcador mientras habla
- [ ] Verificar que TTS se detiene inmediatamente
- [ ] Abrir hoja de detalles de POI
- [ ] Presionar botón "Pregunta al asistente"
- [ ] Verificar que inicia nueva consulta sobre ese POI

---

## 1.3 Mejorar Generación de Contenido IA (1-2 días)

### Objetivo
Optimizar prompts de GPT-4 para respuestas más naturales, concisas y orientadas a modo conducción.

### Paso 1.3.1: Mejorar Prompt del Sistema

**Archivo**: `backend/src/main/java/com/nomad/service/VoiceChatService.java`

Buscar el método donde se construye el system prompt (alrededor línea 100-150) y reemplazar:

```java
private String buildSystemPrompt(VoiceChatRequest request) {
    StringBuilder systemPrompt = new StringBuilder();

    systemPrompt.append("Eres un asistente turístico especializado en España. ");
    systemPrompt.append("Tu nombre es NOMAD y ayudas a viajeros mientras conducen. ");
    systemPrompt.append("\n\n");

    systemPrompt.append("INSTRUCCIONES IMPORTANTES:\n");
    systemPrompt.append("1. Responde en español de forma CONVERSACIONAL y NATURAL\n");
    systemPrompt.append("2. Sé CONCISO - las respuestas deben ser cortas (máximo 3-4 frases)\n");
    systemPrompt.append("3. El usuario está CONDUCIENDO, evita información que distraiga\n");
    systemPrompt.append("4. Enfócate en lo MÁS INTERESANTE e IMPORTANTE\n");
    systemPrompt.append("5. Usa un tono AMIGABLE y ENTUSIASTA, como un copiloto\n");
    systemPrompt.append("6. Si te preguntan por un POI específico, da DATOS CLAVE (año, arquitecto, curiosidades)\n");
    systemPrompt.append("7. Si te preguntan \"qué hay cerca\", menciona los 2-3 lugares MÁS RELEVANTES\n");
    systemPrompt.append("8. Para gastronomía, recomienda PLATOS TÍPICOS de la zona\n");
    systemPrompt.append("9. NO uses listas numeradas ni formato complejo, habla naturalmente\n");
    systemPrompt.append("10. Si no sabes algo, sé honesto pero ofrece información relacionada\n");
    systemPrompt.append("\n");

    // Contexto de ubicación
    if (request.userLat() != null && request.userLng() != null) {
        systemPrompt.append(String.format(
            "UBICACIÓN ACTUAL: Latitud %.4f, Longitud %.4f\n",
            request.userLat(), request.userLng()
        ));

        // Determinar provincia/región aproximada (opcional, requiere geocoding reverso)
        String location = approximateLocation(request.userLat(), request.userLng());
        if (location != null) {
            systemPrompt.append(String.format("Estás cerca de: %s\n", location));
        }
    }

    // Contexto de categoría seleccionada
    if (request.selectedCategory() != null && !request.selectedCategory().isEmpty()) {
        String categoryContext = switch (request.selectedCategory().toLowerCase()) {
            case "history" -> "El usuario está interesado en HISTORIA. Enfócate en eventos históricos, personajes, batallas, etc.";
            case "food" -> "El usuario está interesado en GASTRONOMÍA. Recomienda platos típicos, restaurantes, productos locales.";
            case "art" -> "El usuario está interesado en ARTE Y ARQUITECTURA. Habla de estilos, artistas, obras importantes.";
            case "sports" -> "El usuario está interesado en DEPORTES Y OCIO. Sugiere actividades, parques, eventos deportivos.";
            case "geography" -> "El usuario está interesado en GEOGRAFÍA. Describe paisajes, formaciones naturales, ecosistemas.";
            case "industry" -> "El usuario está interesado en INDUSTRIA Y AGRICULTURA. Menciona productos locales, tradiciones artesanales.";
            default -> "";
        };

        if (!categoryContext.isEmpty()) {
            systemPrompt.append(categoryContext).append("\n");
        }
    }

    // POIs cercanos
    if (request.nearbyPois() != null && !request.nearbyPois().isEmpty()) {
        systemPrompt.append("\nPUNTOS DE INTERÉS CERCANOS:\n");

        int count = 0;
        for (Map<String, Object> poi : request.nearbyPois()) {
            if (count >= 10) break; // Máximo 10 POIs

            String name = (String) poi.get("name");
            String category = (String) poi.get("category");
            Double distance = poi.get("distance") instanceof Number
                ? ((Number) poi.get("distance")).doubleValue()
                : null;

            systemPrompt.append(String.format(
                "- %s (%s)%s\n",
                name,
                category,
                distance != null ? String.format(" - %.0f metros", distance) : ""
            ));

            count++;
        }
    }

    systemPrompt.append("\nRECUERDA: Responde como si hablaras con un amigo en el coche. ");
    systemPrompt.append("Breve, interesante y sin tecnicismos innecesarios.");

    return systemPrompt.toString();
}

// Método auxiliar para aproximar ubicación (simplificado)
private String approximateLocation(double lat, double lng) {
    // Rangos aproximados de coordenadas para ciudades principales
    if (lat >= 40.3 && lat <= 40.5 && lng >= -3.8 && lng <= -3.6) {
        return "Madrid";
    } else if (lat >= 39.4 && lat <= 39.5 && lng >= -0.4 && lng <= -0.3) {
        return "Valencia";
    } else if (lat >= 41.3 && lat <= 41.5 && lng >= 2.0 && lng <= 2.2) {
        return "Barcelona";
    } else if (lat >= 37.3 && lat <= 37.5 && lng >= -6.0 && lng <= -5.9) {
        return "Sevilla";
    } else if (lat >= 39.8 && lat <= 40.0 && lng >= -4.1 && lng <= -4.0) {
        return "Toledo";
    }
    // Añadir más ciudades según necesidad
    return null;
}
```

### Paso 1.3.2: Ajustar Parámetros de GPT-4

En el mismo archivo, buscar donde se hace la llamada a OpenAI (alrededor línea 200-250):

```java
private String callOpenAI(String systemPrompt, String userMessage, List<Map<String, String>> history) {
    // ... configuración existente ...

    ChatCompletionRequest request = ChatCompletionRequest.builder()
        .model("gpt-4-turbo-preview") // o "gpt-4-1106-preview"
        .messages(messages)
        .maxTokens(200) // REDUCIR de 300 a 200 para respuestas más cortas
        .temperature(0.7) // Ligeramente creativo pero consistente
        .topP(0.9)
        .frequencyPenalty(0.3) // Evitar repeticiones
        .presencePenalty(0.3) // Fomentar variedad
        .build();

    // ... resto del código ...
}
```

### Paso 1.3.3: Añadir Ejemplos de Conversación (Few-Shot Learning)

Añadir ejemplos al system prompt para guiar el tono:

```java
systemPrompt.append("\nEJEMPLOS DE RESPUESTAS CORRECTAS:\n\n");

systemPrompt.append("Usuario: \"¿Qué hay cerca?\"\n");
systemPrompt.append("Tú: \"Tienes el Palacio Real a 500 metros, un espectáculo de arquitectura ");
systemPrompt.append("barroca del siglo XVIII. También está la Plaza Mayor, perfecta para tapas. ");
systemPrompt.append("¿Quieres que te cuente más de alguno?\"\n\n");

systemPrompt.append("Usuario: \"Cuéntame sobre el Alcázar de Toledo\"\n");
systemPrompt.append("Tú: \"El Alcázar es una fortaleza romana del siglo III, reconstruida varias veces. ");
systemPrompt.append("En la Guerra Civil fue escenario de un asedio famoso. Hoy es el Museo del Ejército. ");
systemPrompt.append("Tiene unas vistas increíbles de Toledo.\"\n\n");

systemPrompt.append("Usuario: \"¿Dónde puedo comer bien por aquí?\"\n");
systemPrompt.append("Tú: \"Para cochinillo asado, Casa Botín es legendaria, el restaurante más antiguo ");
systemPrompt.append("del mundo según Guinness. Si prefieres tapas, el Mercado de San Miguel tiene ");
systemPrompt.append("de todo y está súper céntrico.\"\n\n");
```

### Paso 1.3.4: Testing de Mejoras de IA

**Checklist de prompts a probar**:
- [ ] "¿Qué hay cerca de aquí?" → Debe listar 2-3 POIs relevantes
- [ ] "Cuéntame sobre [POI específico]" → Respuesta de 3-4 frases con datos clave
- [ ] "¿Dónde puedo comer?" → Recomendar restaurantes típicos
- [ ] "¿Qué ver en Toledo?" → Sugerir top 3 atracciones con brevedad
- [ ] "Cuéntame la historia de Madrid en 1 minuto" → Resumen ultra-conciso
- [ ] Verificar que respuestas sean conversacionales, no tipo Wikipedia

---

# 🚀 FASE 2: Backend para Producción (4-6 días)

## 2.1 Añadir PostgreSQL (Opcional para MCP - 2-3 días)

### Decisión
Para el MCP, PostgreSQL es **opcional**. Se puede hacer demo sin base de datos usando solo caché en memoria. **Recomendado diferir a v2** para ahorrar tiempo.

Si se decide implementar:

### Paso 2.1.1: Añadir Dependencias

**Archivo**: `backend/build.gradle`

```gradle
dependencies {
    // Existing dependencies...

    // PostgreSQL
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql:42.7.1'

    // Flyway para migraciones
    implementation 'org.flywaydb:flyway-core:10.4.1'
}
```

### Paso 2.1.2: Configuración de Base de Datos

**Archivo**: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/nomad}
    username: ${DATABASE_USERNAME:nomad}
    password: ${DATABASE_PASSWORD:nomad}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate # Flyway maneja schema
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  flyway:
    enabled: true
    baseline-on-migrate: true
```

### Paso 2.1.3: Schema de Analytics (Opcional)

**Archivo NUEVO**: `backend/src/main/resources/db/migration/V1__initial_schema.sql`

```sql
-- Tabla de eventos de analytics (anónimo)
CREATE TABLE IF NOT EXISTS analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_event_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_created_at ON analytics_events(created_at);

-- Tabla de caché persistente de POIs (opcional, para reducir llamadas a Google Places)
CREATE TABLE IF NOT EXISTS cached_pois (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    source VARCHAR(50),
    metadata JSONB,
    cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_cached_pois_location ON cached_pois(latitude, longitude);
CREATE INDEX idx_cached_pois_category ON cached_pois(category);
CREATE INDEX idx_cached_pois_expires_at ON cached_pois(expires_at);
```

**NOTA**: Para el MCP, es suficiente con skip this step y usar solo caché en memoria.

---

## 2.2 Configuración Railway (1-2 días) 🔴 PRIORIDAD CRÍTICA

### Objetivo
Desplegar backend en Railway para que Android app pueda conectarse desde cualquier dispositivo físico.

### Paso 2.2.1: Crear Dockerfile

**Archivo NUEVO**: `backend/Dockerfile`

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar archivos de Gradle
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Descargar dependencias (cacheado)
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente
COPY src src

# Compilar aplicación
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR desde stage de build
COPY --from=build /app/build/libs/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando de inicio
CMD java $JAVA_OPTS -jar app.jar
```

### Paso 2.2.2: Crear railway.toml

**Archivo NUEVO**: `backend/railway.toml`

```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"

[deploy]
startCommand = "java -Xmx512m -jar app.jar"
healthcheckPath = "/actuator/health"
healthcheckTimeout = 300
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 3
```

### Paso 2.2.3: Crear .dockerignore

**Archivo NUEVO**: `backend/.dockerignore`

```
.gradle/
build/
.idea/
*.log
.env
*.iml
```

### Paso 2.2.4: Ajustar application.yml para Producción

**Archivo NUEVO**: `backend/src/main/resources/application-production.yml`

```yaml
server:
  port: ${PORT:8080}

spring:
  profiles:
    active: production

  # Si usas PostgreSQL (opcional)
  datasource:
    url: ${DATABASE_URL}
    # Railway proporciona DATABASE_URL automáticamente

# Logging para producción
logging:
  level:
    root: INFO
    com.nomad: INFO
    org.springframework.web: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  info:
    env:
      enabled: true
```

### Paso 2.2.5: Guía de Despliegue en Railway

**Archivo NUEVO**: `DEPLOYMENT.md`

```markdown
# Guía de Despliegue - Railway

## Paso 1: Crear Cuenta en Railway

1. Ir a [railway.app](https://railway.app)
2. Registrarse con GitHub (recomendado)
3. Verificar email

## Paso 2: Crear Nuevo Proyecto

1. Click en "New Project"
2. Seleccionar "Deploy from GitHub repo"
3. Autorizar Railway a acceder a tu repositorio
4. Seleccionar el repositorio NOMAD

## Paso 3: Configurar Variables de Entorno

En el dashboard de Railway, ir a "Variables" y añadir:

```
OPENAI_API_KEY=sk-...tu-clave-aqui
GOOGLE_PLACES_API_KEY=AIza...tu-clave-aqui
SPRING_PROFILES_ACTIVE=production
PORT=8080
```

Opcionales (si usas otros servicios):
```
OPENTRIPMAP_API_KEY=...
FOURSQUARE_API_KEY=...
```

## Paso 4: (Opcional) Añadir PostgreSQL

1. En el proyecto Railway, click en "New"
2. Seleccionar "Database" → "PostgreSQL"
3. Railway creará automáticamente la variable `DATABASE_URL`
4. No necesitas configurar nada más

## Paso 5: Deploy

1. Railway detectará automáticamente el Dockerfile
2. Iniciará build automáticamente
3. Esperar 3-5 minutos
4. Una vez completado, Railway asignará una URL pública

## Paso 6: Verificar Deployment

1. Copiar la URL pública (ej: `https://nomad-backend-production.up.railway.app`)
2. Probar health check:
   ```
   curl https://tu-url.railway.app/actuator/health
   ```
3. Deberías ver: `{"status":"UP"}`

4. Probar endpoint de POIs:
   ```
   curl "https://tu-url.railway.app/api/poi/nearby?lat=40.4169&lng=-3.7035&radius=2000&cat=history"
   ```

## Paso 7: Configurar Android App

1. Abrir `app-android/app/build.gradle.kts`
2. En `buildTypes.release`, actualizar:
   ```kotlin
   buildConfigField("String", "DEFAULT_BACKEND_URL", "\"https://tu-url.railway.app/api/\"")
   ```
3. Recompilar app

## Paso 8: Generar APK de Producción

```bash
cd app-android
./gradlew assembleRelease
```

El APK estará en:
```
app-android/app/build/outputs/apk/release/app-release-unsigned.apk
```

## Paso 9: Instalar en Dispositivo Físico

1. Habilitar "Orígenes desconocidos" en Android
2. Transferir APK al dispositivo (email, USB, Drive)
3. Instalar APK
4. Abrir app y ir a Ajustes
5. Verificar que URL del backend apunta a Railway

## Troubleshooting

### Error: "Unable to connect to backend"
- Verificar que Railway deployment esté "Active"
- Probar URL en navegador
- Revisar logs en Railway dashboard

### Error: "Health check failed"
- Revisar variables de entorno (especialmente OPENAI_API_KEY)
- Ver logs: Railway Dashboard → Deployments → View Logs

### App muy lenta
- Verificar región de Railway (elegir Europa si demo en España)
- Considerar upgrade a plan Pro si hay timeouts

## Costos Estimados

- **Plan Hobby** (gratis): $5 de crédito/mes, suficiente para demos
- **Plan Pro** ($20/mes): Sin límites, recomendado para producción
```

### Paso 2.2.6: Testing del Deployment

**Checklist**:
- [ ] Crear proyecto en Railway
- [ ] Configurar variables de entorno
- [ ] Verificar que build completa sin errores
- [ ] Probar health check endpoint
- [ ] Probar endpoint `/api/poi/nearby`
- [ ] Probar endpoint `/api/voice/chat`
- [ ] Medir latencia (debe ser <2s desde España)

---

## 2.3 Monitoreo Básico (1 día)

### Objetivo
Añadir métricas básicas y logging estructurado para detectar errores en producción.

### Paso 2.3.1: Añadir Dependencias de Actuator

**Archivo**: `backend/build.gradle`

```gradle
dependencies {
    // Existing...

    // Actuator para métricas
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Micrometer para Prometheus (opcional)
    runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
}
```

### Paso 2.3.2: Configurar Actuator

En `application-production.yml` (ya añadido en paso anterior):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

### Paso 2.3.3: Añadir Info de Versión

**Archivo**: `backend/src/main/resources/application.yml`

```yaml
info:
  app:
    name: NOMAD Travel Assistant Backend
    version: 1.0.0-MCP
    description: Backend API para asistente turístico con voz híbrida
```

### Paso 2.3.4: Testing de Monitoreo

**Endpoints a probar**:
- `GET /actuator/health` → Status de la app
- `GET /actuator/info` → Información de versión
- `GET /actuator/metrics` → Listado de métricas disponibles
- `GET /actuator/metrics/http.server.requests` → Estadísticas de requests
- `GET /actuator/prometheus` → Métricas en formato Prometheus (opcional)

---

# 🚀 FASE 3: Android App para Demo (5-8 días)

## 3.1 URL Dinámica de Backend (1 día)

Ya implementado en Fase 1.1 (Settings Screen).

### Testing

**Checklist**:
- [ ] Build debug usa `http://10.0.2.2:8081/api/`
- [ ] Build release usa URL de Railway
- [ ] Usuario puede cambiar URL desde Settings
- [ ] App reconecta automáticamente con nueva URL

---

## 3.2 Optimizaciones de Rendimiento (1-2 días)

### Objetivo
Reducir consumo de batería y mejorar fluidez del mapa.

### Paso 3.2.1: Debounce de Actualización de POIs

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/map/MapScreen.kt`

```kotlin
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun MapScreen(...) {
    val cameraPositionState = rememberCameraPositionState()

    // Debounce para evitar llamadas excesivas al mover cámara
    LaunchedEffect(cameraPositionState.position.target) {
        delay(1000) // Esperar 1 segundo después de que se detenga el movimiento

        val center = cameraPositionState.position.target
        val radius = userPreferences.poiRadiusMeters

        // Actualizar POIs solo si la posición cambió significativamente
        if (shouldUpdatePois(center, lastFetchedCenter)) {
            selectedCategories.forEach { category ->
                // Cargar POIs
            }
            lastFetchedCenter = center
        }
    }
}

private fun shouldUpdatePois(current: LatLng, last: LatLng?): Boolean {
    if (last == null) return true

    // Actualizar solo si se movió más de 500 metros
    val distance = FloatArray(1)
    Location.distanceBetween(
        current.latitude, current.longitude,
        last.latitude, last.longitude,
        distance
    )

    return distance[0] > 500
}
```

### Paso 3.2.2: Limitar Marcadores Visibles

Ya implementado (límite de 40 marcadores), pero añadir priorización:

```kotlin
// Ordenar POIs por relevancia antes de limitar
val sortedPois = allPois
    .sortedWith(
        compareBy<POI> { poi ->
            // Priorizar POIs de categoría seleccionada
            if (poi.category in selectedCategories) 0 else 1
        }.thenBy { poi ->
            // Luego por distancia
            poi.distance ?: Float.MAX_VALUE
        }
    )
    .take(40)
```

### Paso 3.2.3: Optimizar Requests de Voice Chat

**Archivo**: `app-android/app/src/main/java/com/nomad/app/voice/HybridVoiceManager.kt`

Reducir POIs enviados en contexto:

```kotlin
private fun buildVoiceChatRequest(userMessage: String): VoiceChatRequest {
    // ... código existente ...

    // CAMBIO: Enviar solo top 5 POIs más cercanos
    val nearbyPoisData = currentNearbyPois
        .sortedBy { it.distance }
        .take(5) // Reducido de 10 a 5
        .map { poi ->
            mapOf(
                "name" to poi.name,
                "category" to poi.category.apiKey,
                "distance" to (poi.distance ?: 0f)
            )
        }

    return VoiceChatRequest(
        message = userMessage,
        userLat = currentUserLat,
        userLng = currentUserLng,
        selectedCategory = currentSelectedCategory,
        nearbyPois = nearbyPoisData,
        conversationHistory = conversationHistory.takeLast(10)
    )
}
```

---

## 3.3 Mejoras UX Modo Conductor (1-2 días) 🔴 PRIORIDAD ALTA

### Objetivo
Adaptar UI para uso seguro mientras se conduce: botones grandes, alto contraste, mínima distracción.

### Paso 3.3.1: Aumentar Tamaño de Botones

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/map/VoiceAssistantFab.kt`

```kotlin
package com.nomad.app.ui.map

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoiceAssistantFab(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(80.dp), // AUMENTADO de 56dp a 80dp
        containerColor = if (isActive)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isActive) "Desactivar asistente" else "Activar asistente",
            modifier = Modifier.size(40.dp) // Icono grande
        )
    }
}
```

### Paso 3.3.2: Mejorar Contraste de Category Chips

**Archivo**: `app-android/app/src/main/java/com/nomad/app/ui/map/MapScreen.kt`

```kotlin
@Composable
private fun CategoryFilterChips(...) {
    LazyRow(...) {
        items(POICategory.values()) { category ->
            FilterChip(
                selected = category in selectedCategories,
                onClick = { onCategoryToggle(category) },
                label = {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium, // Texto más grande
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.height(56.dp), // Chips más altos
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color,
                    selectedLabelColor = Color.White, // Blanco para alto contraste
                    selectedLeadingIconColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = Color.Black,
                    selectedBorderWidth = 3.dp // Borde más grueso
                )
            )
        }
    }
}
```

### Paso 3.3.3: Modo Noche Automático

**Archivo NUEVO**: `app-android/app/src/main/java/com/nomad/app/ui/theme/Theme.kt`

```kotlin
package com.nomad.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores con alto contraste para modo día
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    secondary = Color(0xFFFF9800),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Colores para modo noche (conducción nocturna)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    secondary = Color(0xFFFFB74D),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun NomadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Paso 3.3.4: Simplificar Bottom Sheets

**Reducir información mostrada en hojas de detalles para evitar distracción**:

```kotlin
@Composable
fun POIDetailBottomSheet(...) {
    ModalBottomSheet(...) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Solo información esencial
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineMedium, // Más grande
                fontWeight = FontWeight.Bold
            )

            // Botón de acción principal - MUY GRANDE
            Button(
                onClick = { onAskAboutPoi(poi) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp), // Botón extra alto
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Pregunta al asistente",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
```

### Paso 3.3.5: Indicador Visual de Estado de Voz

Añadir feedback visual claro cuando el asistente está escuchando/hablando:

```kotlin
@Composable
fun VoiceStatusIndicator(
    assistantState: AssistantState,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (assistantState) {
        AssistantState.LISTENING -> "Escuchando..." to Color(0xFF4CAF50)
        AssistantState.PROCESSING -> "Procesando..." to Color(0xFFFF9800)
        AssistantState.SPEAKING -> "Hablando..." to Color(0xFF2196F3)
        else -> return // No mostrar nada si está idle
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

Integrarlo en MapScreen:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    GoogleMap(...)

    // Indicador de estado de voz en la parte superior
    voiceState.assistantState.let { state ->
        if (state != AssistantState.IDLE) {
            VoiceStatusIndicator(
                assistantState = state,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }
    }
}
```

---

## 3.4 Testing en Dispositivo Físico (2-3 días) 🔴 CRÍTICO

### Objetivo
Validar funcionalidad completa en condiciones reales antes de demo con cliente.

### Paso 3.4.1: Generar APK de Debug

```bash
cd app-android
./gradlew assembleDebug
```

APK en: `app/build/outputs/apk/debug/app-debug.apk`

### Paso 3.4.2: Instalar en Dispositivo

**Opción A: Via USB**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Opción B: Compartir APK**
- Subir a Google Drive
- Enviar por email
- Instalar desde dispositivo (habilitar "Fuentes desconocidas")

### Paso 3.4.3: Checklist de Testing Completo

#### Permisos
- [ ] App solicita permiso de ubicación al iniciar
- [ ] App solicita permiso de micrófono al activar voz
- [ ] Permisos se mantienen después de cerrar app

#### Geolocalización
- [ ] Ubicación se actualiza en tiempo real
- [ ] Cámara se centra en ubicación actual
- [ ] POIs se cargan según ubicación real
- [ ] Funciona en exteriores (GPS real)
- [ ] Funciona en interiores (WiFi/cell tower)

#### Categorías y Filtros
- [ ] 6 categorías visibles con colores correctos
- [ ] Filtrar por Historia muestra solo POIs amarillos
- [ ] Filtrar por Gastronomía muestra solo POIs morados
- [ ] Filtros múltiples funcionan correctamente
- [ ] Marcadores cambian dinámicamente

#### Voz - Modo Reactivo
- [ ] Presionar botón de voz activa micrófono
- [ ] Saludo inicial se escucha claramente
- [ ] STT reconoce comandos en español
- [ ] Respuesta de GPT-4 es relevante
- [ ] TTS se escucha claramente
- [ ] Conversación continúa después de respuesta
- [ ] Botón cierra sesión de voz

#### Voz - Modo Proactivo (Futuro)
- [ ] Sistema avisa según frecuencia configurada
- [ ] Avisos son relevantes a ubicación actual
- [ ] Usuario puede aceptar/rechazar avisos

#### POI Detail
- [ ] Clic en marcador abre hoja de detalles
- [ ] Clic en marcador detiene TTS si está activo
- [ ] Botón "Pregunta al asistente" funciona
- [ ] Consulta específica de POI es precisa

#### Settings
- [ ] Navegar a Settings funciona
- [ ] Cambiar radio de POIs actualiza mapa
- [ ] Cambiar frecuencia de avisos persiste
- [ ] Cambiar velocidad TTS afecta narración
- [ ] Cambiar pitch TTS afecta tono de voz
- [ ] Cerrar y reabrir app mantiene ajustes

#### Rendimiento
- [ ] Mapa es fluido (60 FPS) al hacer zoom/pan
- [ ] No hay lag al activar/desactivar categorías
- [ ] Voz responde en <3 segundos
- [ ] No hay crashes durante uso prolongado (30+ min)

#### Batería
- [ ] Consumo es razonable (<20%/hora con GPS+voz)
- [ ] App no causa sobrecalentamiento

#### Conectividad
- [ ] Funciona con WiFi
- [ ] Funciona con datos móviles (4G/5G)
- [ ] Muestra error claro si no hay conexión
- [ ] Reconecta automáticamente al recuperar red

#### En Movimiento (Coche)
- [ ] POIs se actualizan mientras te mueves
- [ ] Voz es audible con ruido de fondo
- [ ] STT reconoce voz con ruido ambiental
- [ ] No distrae al conductor
- [ ] Funciona con Google Maps en paralelo

### Paso 3.4.4: Testing de Ruta Madrid-Toledo

**Requisitos previos**:
- Dispositivo con GPS y 4G
- Backend desplegado en Railway
- Coche o simulación de ruta con app de GPS spoofing

**Test Plan** (1 hora):
1. **Inicio en Madrid (Plaza de España)**
   - [ ] Activar categoría "Historia"
   - [ ] Verificar POIs: Palacio Real, Plaza Mayor, Templo de Debod
   - [ ] Preguntar: "¿Qué hay cerca?" → Debe listar 2-3 POIs

2. **Salida de Madrid (A-42)**
   - [ ] Activar "Gastronomía"
   - [ ] Verificar Casa Botín, Mercado San Miguel
   - [ ] Preguntar: "¿Dónde puedo comer cochinillo?"

3. **Camino a Toledo (Getafe, Illescas)**
   - [ ] Modo proactivo cada 5 min (si implementado)
   - [ ] Verificar avisos de POIs intermedios

4. **Llegada a Toledo**
   - [ ] Activar "Arte y arquitectura"
   - [ ] Verificar: Catedral, Alcázar, Sinagoga del Tránsito
   - [ ] Hacer clic en Catedral → Preguntar detalles
   - [ ] Verificar respuesta histórica precisa

5. **Mirador del Valle**
   - [ ] Activar "Geografía"
   - [ ] Verificar descripción del paisaje
   - [ ] Tomar screenshot para demo

---

# 🚀 FASE 4: Documentación y Demo (2-3 días)

## 4.1 Guía de Demo Completa

### Ya incluida en DEPLOYMENT.md (Fase 2.2.5)

---

## 4.2 Script de Presentación (30 minutos)

### Preparación Previa (Checklist del Presentador)

**24 horas antes**:
- [ ] Backend en Railway funcionando
- [ ] Variables de entorno configuradas
- [ ] Health check: `200 OK`
- [ ] APK instalado en dispositivo de demo
- [ ] Ajustes configurados (radio 2km, frecuencia 5min)
- [ ] Batería del dispositivo >80%
- [ ] Plan de datos móviles activo
- [ ] Coche limpio y listo (si demo en movimiento)
- [ ] Ruta Madrid-Toledo planificada en GPS

**1 hora antes**:
- [ ] Probar conexión backend desde dispositivo
- [ ] Probar voz (STT + TTS)
- [ ] Verificar permisos otorgados
- [ ] Limpiar historial de conversación
- [ ] Reiniciar dispositivo
- [ ] Cargar música/podcast de respaldo (si falla demo)

### Minuto 0-5: Introducción

**Guión**:
> "Buenos días. Hoy os voy a mostrar NOMAD, el asistente turístico inteligente que convierte cualquier viaje en carretera en una experiencia cultural.
>
> La idea es simple: mientras conduces, NOMAD te cuenta historias fascinantes sobre los lugares que atraviesas. Sin distracciones, sin pantallas, solo conversación natural.
>
> Vamos a hacer un recorrido de Madrid a Toledo, una ruta de 1 hora perfecta para probar el sistema."

**Acción**: Mostrar pantalla inicial de la app en Madrid.

---

### Minuto 5-10: Categorías Temáticas

**Guión**:
> "NOMAD organiza el conocimiento en 6 categorías temáticas. Cada una tiene su color:
> - Historia en amarillo
> - Gastronomía en morado
> - Arte y arquitectura en rosa
> - Deportes y ocio en naranja
> - Geografía en azul
> - Industria y agricultura en verde
>
> Podéis activar las que os interesen y NOMAD solo os hablará de esos temas."

**Acción**:
1. Tocar chip "Historia" → Marcadores amarillos aparecen
2. Tocar chip "Gastronomía" → Marcadores morados se añaden
3. Zoom out para mostrar densidad
4. Desactivar todo y activar solo "Arte" → Solo rosa

---

### Minuto 10-15: Conversación con el Asistente

**Guión**:
> "Ahora viene la magia. En lugar de leer pantallas, simplemente hablas con NOMAD como si fuera tu copiloto."

**Acción**:
1. Presionar botón flotante de voz (80dp, grande)
2. Esperar saludo: "Hola! Soy tu asistente de viaje..."
3. Preguntar en voz alta: **"¿Qué hay cerca de aquí?"**
4. Escuchar respuesta (debería mencionar Palacio Real, Plaza Mayor)
5. Preguntar: **"Cuéntame sobre el Palacio Real"**
6. Escuchar narración histórica (2-3 frases)

**Guión**:
> "Como veis, las respuestas son naturales, concisas y pensadas para no distraer mientras conduces. Nada de Wikipedia interminable."

---

### Minuto 15-20: Navegación en Ruta

**Guión**:
> "Ahora vamos a simular el viaje a Toledo. Fijaos cómo NOMAD muestra puntos de interés a lo largo de toda la ruta."

**Acción**:
1. Establecer ruta en Google Maps: Madrid → Toledo
2. Activar todas las categorías
3. Mostrar POIs en el trayecto:
   - Getafe: Catedral de la Magdalena (arte)
   - Illescas: Hospital de la Caridad (historia)
   - Toledo: múltiples marcadores
4. Hacer zoom en Toledo para mostrar densidad

**Guión**:
> "NOMAD funciona en paralelo con Google Maps. Tu navegación no se interrumpe, simplemente añade la capa cultural."

---

### Minuto 20-25: Ajustes Personalizados

**Guión**:
> "Cada viajero es diferente. Por eso NOMAD es totalmente personalizable."

**Acción**:
1. Ir a Ajustes (icono engranaje)
2. Cambiar **Radio de búsqueda** a 5km → Mostrar más marcadores
3. Cambiar **Frecuencia de avisos** a cada 2 minutos
4. Ajustar **Velocidad de narración** a 1.5x
5. Demostrar voz más rápida con nueva consulta

**Guión**:
> "Si vas con prisa, acelera la narración. Si quieres explorar a fondo, amplía el radio de búsqueda. Todo se adapta a ti."

---

### Minuto 25-30: Caso de Uso Real y Cierre

**Guión**:
> "Imaginad una familia conduciendo a la playa. Los niños aburridos en el asiento trasero. De repente, NOMAD les cuenta que están pasando por el lugar donde se libró la batalla de Bailén en 1808. O que ese castillo de ahí fue escenario de una leyenda medieval.
>
> El viaje se transforma. Ya no es solo llegar al destino, es disfrutar el camino.
>
> Y lo mejor: todo esto funciona con voz nativa de Android. Cero costes adicionales. La IA solo procesa el lenguaje, no la síntesis de voz. Eso reduce el coste operativo en un 97% vs soluciones comerciales."

**Acción**:
1. Consultar por voz: **"¿Cuál es el mejor restaurante de cochinillo en Toledo?"**
2. Escuchar recomendaciones (Casa Botín, etc.)
3. Hacer clic en un POI → Mostrar botón "Pregunta al asistente"
4. Consulta específica del POI

**Guión de cierre**:
> "NOMAD está listo para demo. Backend desplegado en cloud, funciona en cualquier dispositivo Android, y la experiencia es fluida incluso con 4G.
>
> ¿Preguntas?"

---

## 4.3 Materiales de Apoyo

### Slide Deck (Opcional)

**Diapositiva 1: Portada**
- Logo NOMAD
- Subtítulo: "Tu copiloto cultural"

**Diapositiva 2: El Problema**
- Viajes largos = tiempo perdido
- Google Maps solo da direcciones
- Audioguías son caras y rígidas

**Diapositiva 3: La Solución**
- Asistente de voz + IA + Geolocalización
- Conversación natural en español
- 6 categorías temáticas

**Diapositiva 4: Tecnología**
- Android nativo (TTS/STT gratis)
- GPT-4 para contenido (~$0.50/hora)
- Google Maps + Google Places API
- Backend en Railway (cloud)

**Diapositiva 5: Demo**
- Video de 2 min mostrando uso en coche
- Screenshots de categorías
- Grabación de conversación de voz

**Diapositiva 6: Roadmap**
- v1.0 (MCP): Funcionalidad core
- v2.0: Favoritos, historial, Android Auto
- v3.0: iOS, multi-idioma, gamificación

---

## 📱 Mejoras de UI - Resumen de Cambios

### Cambios Visuales Clave

1. **Botón de Voz Gigante** (80dp vs 56dp estándar)
2. **Category Chips con Alto Contraste** (texto bold, bordes gruesos)
3. **Indicador de Estado de Voz** (card flotante arriba: "Escuchando...", "Procesando...", "Hablando...")
4. **Bottom Sheets Simplificadas** (solo info esencial, botón de acción grande)
5. **Modo Noche Automático** (para conducción nocturna)
6. **Settings Screen Completa** (con iconos, secciones claras)

### Principios de Diseño para Modo Conductor

- ✅ Botones mínimo 48dp (NOMAD usa 56-80dp)
- ✅ Contraste alto (WCAG AAA)
- ✅ Texto grande (mínimo 16sp)
- ✅ Información mínima en pantalla
- ✅ Feedback visual claro (loading, estado)
- ✅ Soporte modo oscuro

---

## 📝 Resumen de Archivos a Crear/Modificar

### Nuevos Archivos Android (9)

1. `app/model/POI.kt` - Actualizado con nuevas categorías
2. `app/data/preferences/UserPreferences.kt` - Data class
3. `app/data/preferences/UserPreferencesRepository.kt` - DataStore
4. `app/ui/settings/SettingsScreen.kt` - UI completa
5. `app/ui/settings/SettingsViewModel.kt` - Lógica
6. `app/ui/map/VoiceAssistantFab.kt` - Botón grande
7. `app/ui/map/VoiceStatusIndicator.kt` - Indicador visual
8. `app/ui/theme/Theme.kt` - Modo noche
9. `app/NomadApp.kt` - Navigation actualizada

### Modificaciones Android (5)

1. `app/build.gradle.kts` - Dependencias DataStore
2. `app/ui/map/MapScreen.kt` - Categorías, stopSpeaking, indicadores
3. `app/ui/map/POIDetailBottomSheet.kt` - Botón voz, UI simplificada
4. `app/data/api/RetrofitClient.kt` - URL dinámica
5. `app/data/dto/PoiDto.kt` - Mapeo nuevas categorías

### Nuevos Archivos Backend (6)

1. `Dockerfile` - Contenedor
2. `railway.toml` - Config Railway
3. `.dockerignore` - Excluir archivos
4. `src/main/resources/application-production.yml` - Config producción
5. `DEPLOYMENT.md` - Guía completa
6. (Opcional) `src/main/resources/db/migration/V1__initial_schema.sql` - PostgreSQL

### Modificaciones Backend (3)

1. `src/main/java/com/nomad/controller/PoiController.java` - Validación categorías
2. `src/main/java/com/nomad/service/GooglePlacesService.java` - Mapeo POI types
3. `src/main/java/com/nomad/service/VoiceChatService.java` - Prompts mejorados

### Archivos de Documentación (1)

1. `PLAN_EJECUCION_MCP.md` - Este archivo

---

## ⏱️ Timeline Realista

### Semana 1 (5 días)
- **Día 1-2**: Fase 1.0 - Remapeo de categorías
- **Día 3-4**: Fase 1.1 - Menú de ajustes
- **Día 5**: Fase 1.2 - Integración de voz en UI

### Semana 2 (5 días)
- **Día 1**: Fase 1.3 - Mejoras de IA
- **Día 2-3**: Fase 2.2 - Despliegue Railway
- **Día 4-5**: Fase 3.2-3.3 - Optimizaciones + UX modo conductor

### Semana 3 (5 días)
- **Día 1-3**: Fase 3.4 - Testing intensivo en dispositivo
- **Día 4**: Fase 4 - Documentación y preparación demo
- **Día 5**: Primera demo interna + ajustes

### Semana 4 (Buffer)
- **Día 1-3**: Corrección de bugs encontrados
- **Día 4**: Ensayo de presentación
- **Día 5**: **DEMO CON CLIENTE** 🎉

**Total**: 20 días hábiles (4 semanas)

---

## 🚨 Riesgos y Mitigaciones

### Riesgo 1: Latencia de Backend desde Dispositivo Móvil
**Probabilidad**: Media | **Impacto**: Alto

**Mitigación**:
- Desplegar Railway en región Europa (Frankfurt)
- Aumentar timeouts a 45s
- Caché agresiva de POIs (24h en producción)
- Pre-cargar POIs de ruta planificada

### Riesgo 2: Consumo Excesivo de Google Places API
**Probabilidad**: Alta | **Impacto**: Alto (costes)

**Mitigación**:
- Monitorear cuota en Google Cloud Console
- Implementar rate limiting (máx 100 requests/hora por IP)
- Caché de 30min → 24h para POIs estáticos
- PostgreSQL para caché persistente (opcional)

### Riesgo 3: STT No Reconoce Voz con Ruido de Coche
**Probabilidad**: Media | **Impacto**: Crítico

**Mitigación**:
- Testing previo en coche real
- Configurar STT para reducir ruido ambiente
- Considerar micrófonos externos Bluetooth
- Fallback: transcripción parcial aceptable

### Riesgo 4: GPS Impreciso en Interiores/Túneles
**Probabilidad**: Alta | **Impacto**: Medio

**Mitigación**:
- Filtro de suavizado de ubicación (Kalman)
- Usar última ubicación conocida válida
- Indicador visual de "GPS débil"
- No actualizar POIs si señal es mala

### Riesgo 5: Batería se Agota Rápido
**Probabilidad**: Media | **Impacto**: Medio

**Mitigación**:
- Optimizar frecuencia de actualización GPS
- Debounce de 1s en movimientos de cámara
- Limitar marcadores a 40
- Recomendar cargador de coche en demo

---

## ✅ Criterios de Éxito para MCP

### Funcionalidad Core
- [x] 6 categorías temáticas con colores correctos
- [ ] Filtrado de POIs por categoría funcional
- [ ] Conversación de voz natural en español
- [ ] Respuestas de IA relevantes y concisas (<3 frases)
- [ ] Backend desplegado y accesible desde internet
- [ ] APK instalable en cualquier Android 8.0+

### UX y Rendimiento
- [ ] Botones táctiles ≥56dp (voz 80dp)
- [ ] Contraste WCAG AA mínimo
- [ ] Mapa fluido a 60 FPS
- [ ] Respuesta de voz <3s
- [ ] Consumo batería <20%/hora
- [ ] App no crashea en 1 hora de uso

### Demo-Ready
- [ ] Guía de despliegue completa y validada
- [ ] Script de demo ensayado (30 min)
- [ ] Ruta Madrid-Toledo probada end-to-end
- [ ] 5 consultas de voz preparadas y testeadas
- [ ] Screenshots de alta calidad tomadas
- [ ] Video de demo grabado (2-3 min)

---

## 📞 Contacto y Soporte

**Equipo de Desarrollo**:
- Desarrollador Android: [Nombre]
- Desarrollador Backend: [Nombre]
- Product Owner: [Nombre]

**Repositorio**: `https://github.com/tu-org/nomad`
**Backend Producción**: `https://nomad-backend.railway.app`
**Documentación**: Este archivo + `DEPLOYMENT.md`

---

## 🎉 ¡Siguiente Paso!

**Acción Inmediata**: Comenzar con **Fase 1.0 - Remapeo de Categorías**

```bash
# 1. Crear rama de desarrollo
git checkout -b feature/mcp-preparation

# 2. Crear archivo POI.kt con nuevas categorías
# (Ver Paso 1.0.1)

# 3. Compilar y testear
cd app-android
./gradlew build

# 4. Commit inicial
git add .
git commit -m "feat: remapear categorías según especificación (Historia, Gastronomía, Arte, etc.)"
git push origin feature/mcp-preparation
```

**¡Éxito con el desarrollo!** 🚀
