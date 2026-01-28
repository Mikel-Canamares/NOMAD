# Migración a Sistema de Voz Híbrido - AHORRO 97%

## 💰 Comparación de Costes

### OpenAI Realtime API (anterior):
- **Audio input**: $0.06/minuto
- **Audio output**: $0.24/minuto
- **TOTAL**: ~$18/hora de conversación

### Sistema Híbrido (nuevo):
- **STT** (Android nativo): GRATIS
- **GPT-4**: ~$0.01/1K tokens (~$0.30/hora)
- **TTS** (Android nativo): GRATIS
- **TOTAL**: ~$0.50/hora de conversación

**AHORRO: 97% de reducción de costes!**

## 🏗️ Arquitectura Nueva

```
Usuario habla
    ↓
Android STT (GRATIS) → Texto
    ↓
Backend: GPT-4 ($0.01/1K tokens) → Respuesta en texto
    ↓
Android TTS (GRATIS) → Voz
    ↓
Usuario escucha
```

## ✅ Archivos Creados

### Backend:
1. **VoiceChatRequest.java** - DTO para request
2. **VoiceChatResponse.java** - DTO para response
3. **VoiceChatService.java** - Servicio que llama a GPT-4
4. **VoiceChatController.java** - Endpoint POST /api/voice/chat

### Android:
1. **AndroidTTSManager.kt** - TTS nativo de Android
2. **AndroidSTTManager.kt** - STT nativo de Android (RecognizerIntent)
3. **HybridVoiceManager.kt** - Orquestador principal
4. **VoiceChatRequest.kt** - DTO
5. **VoiceChatResponse.kt** - DTO
6. **VoiceChatApiService.kt** - API Retrofit
7. **VoiceViewModel.kt** - ViewModel actualizado (REEMPLAZADO)

## 🔄 Cambios Necesarios en MapScreen

### 1. Actualizar contexto con nuevo método

```kotlin
// En LaunchedEffect donde se envía contexto
LaunchedEffect(isVoiceActive) {
    if (isVoiceActive && currentLocation != null) {
        kotlinx.coroutines.delay(500)

        val poisData = pois.map { poi ->
            mapOf<String, Any>(
                "name" to poi.name,
                "category" to (poi.category ?: "unknown"),
                "lat" to poi.location.latitude,
                "lng" to poi.location.longitude
            )
        }

        // CAMBIO: usar updateContext en lugar de sendInitialContext
        voiceViewModel.updateContext(
            userLat = currentLocation!!.latitude,
            userLng = currentLocation!!.longitude,
            selectedCategory = selectedCategory?.displayName,
            pois = poisData
        )
    }
}
```

### 2. Detener TTS al tocar un marker

```kotlin
// En el onClick del Marker
Marker(
    state = MarkerState(position = poi.location),
    title = poi.name,
    snippet = poi.description,
    onClick = {
        // NUEVO: Detener TTS si está hablando
        if (voiceViewModel.isSpeaking()) {
            voiceViewModel.stopSpeaking()
        }

        selectedPoi = poi
        selectedPoiId = poi.id
        showMarkerDetailSheet = true
        isLoadingAsk = true

        // ... resto del código
        true
    }
)
```

### 3. Añadir botón de voz en POIDetailBottomSheet

Modificar `POIDetailBottomSheet.kt`:

```kotlin
@Composable
fun POIDetailBottomSheet(
    poiName: String,
    askResponse: AskResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAskAboutPoi: (() -> Unit)? = null  // NUEVO parámetro
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con título y botón cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = poiName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // NUEVO: Botón de voz
                if (onAskAboutPoi != null) {
                    IconButton(onClick = onAskAboutPoi) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,  // o Icons.Default.VolumeUp
                            contentDescription = "Preguntar al asistente"
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }

            // ... resto del código
        }
    }
}
```

### 4. Conectar botón en MapScreen

```kotlin
// Cuando se muestra el detail sheet
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
        onAskAboutPoi = {  // NUEVO
            voiceViewModel.speakAbout("Cuéntame sobre ${selectedPoi!!.name}")
        }
    )
}
```

## 🎯 Estados del Asistente

El ViewModel ahora tiene estados simplificados:
- **IDLE** - Inactivo
- **INITIALIZING** - Inicializando TTS
- **LISTENING** - Escuchando (STT activo)
- **PROCESSING** - Enviando a GPT-4
- **SPEAKING** - Hablando (TTS activo)

## 📱 Funcionalidades Nuevas

### 1. Conversación continua
- El STT se reinicia automáticamente después de cada respuesta
- Mantiene historial de conversación (últimas 10 interacciones)

### 2. Hablar sobre POI desde el mapa
- Tocar un marker → Detiene TTS si está hablando
- Botón en detail sheet → Pregunta directamente sobre ese POI

### 3. Transcripciones
- `voiceViewModel.userTranscript` - Lo que dijo el usuario
- `voiceViewModel.assistantTranscript` - Lo que respondió el asistente

## 🔧 Permisos Necesarios

AndroidManifest.xml ya debería tener:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

## 🚀 Flujo de Conversación

1. Usuario presiona botón Play
2. TTS: "Hola! Soy tu asistente de viaje..."
3. STT comienza a escuchar
4. Usuario: "¿Qué museos hay cerca?"
5. STT → texto enviado a backend
6. GPT-4 procesa con contexto (ubicación, POIs, categoría)
7. GPT-4 responde: "Cerca de ti hay 3 museos..."
8. TTS sintetiza la respuesta
9. Vuelve a STT (escucha continua)

## 🐛 Debugging

### Logs esperados:

```
AndroidTTSManager: Inicializando TTS de Android
AndroidTTSManager: ✓ TTS inicializado correctamente
AndroidSTTManager: ✓ Speech recognizer inicializado
HybridVoiceManager: Inicializando sistema híbrido de voz
HybridVoiceManager: ✓ Sistema de voz inicializado
HybridVoiceManager: 🎙️ Iniciando sesión de voz
AndroidTTSManager: 📢 Sintetizando: Hola! Soy tu asistente de viaje...
AndroidTTSManager: 🗣️ TTS comenzó a hablar
AndroidSTTManager: 🎤 Listo para escuchar
AndroidSTTManager: 📝 Texto reconocido: ¿Qué museos hay cerca?
HybridVoiceManager: 🌐 Enviando mensaje a GPT-4
HybridVoiceManager: ✓ Respuesta de GPT-4: Cerca de ti hay 3 museos...
AndroidTTSManager: 📢 Sintetizando: Cerca de ti hay 3 museos...
```

## 📊 Ventajas del Nuevo Sistema

✅ **97% más barato**
✅ **Funciona sin conexión continua** (solo necesita red para GPT-4)
✅ **Más control sobre TTS** (velocidad, tono, pausas)
✅ **Transcripciones gratis** (STT nativo las proporciona)
✅ **No depende de WebRTC** (más simple)
✅ **Funciona en más dispositivos** (no requiere hardware específico)
✅ **Historial de conversación** (para contexto)

## ⚠️ Limitaciones

- **No streaming de audio**: El asistente espera a que GPT-4 termine
- **Latencia STT**: ~1-2 segundos para reconocimiento
- **Calidad de voz**: TTS nativo (no tan natural como OpenAI, pero aceptable)
- **Idioma**: Requiere TTS español instalado en el dispositivo

## 🔄 Migración Completa

Para completar la migración, ejecutar estos cambios en MapScreen:

1. Cambiar `sendInitialContext` por `updateContext`
2. Añadir `voiceViewModel.stopSpeaking()` en marker onClick
3. Actualizar `POIDetailBottomSheet` con parámetro `onAskAboutPoi`
4. Conectar botón de voz en detail sheet

El resto ya está implementado y compilará correctamente.

## 🏁 Resultado Final

Un asistente de voz completamente funcional que:
- Cuesta 97% menos que Realtime API
- Mantiene conversación natural
- Conoce ubicación y POIs cercanos
- Puede ser consultado sobre POIs específicos
- Detiene la voz al tocar marcadores
- Proporciona transcripciones
- Mantiene historial de conversación

Ideal para fase de pruebas y para producción a largo plazo.
