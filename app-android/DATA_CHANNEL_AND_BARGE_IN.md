# Data Channel y Barge-In - Documentación

## Data Channel para contexto POI y mensajes

### Implementación completada ✅

El data channel WebRTC permite enviar datos adicionales durante la sesión de voz:
- Contexto de POIs (id, coordenadas, nombre)
- Mensajes de texto
- Señales de interrupción (barge-in)

### Archivos modificados:

1. **[WebRTCManager.kt](app-android/app/src/main/java/com/nomad/app/webrtc/WebRTCManager.kt)**
   - `createDataChannel()` - Crea data channel ordenado
   - `registerDataChannelObserver()` - Maneja mensajes recibidos
   - `sendTextMessage(message)` - Envía texto
   - `sendPoiContext(id, lat, lng, name)` - Envía contexto POI
   - `sendBargeInSignal()` - Envía señal de interrupción

2. **[RealtimeVoiceManager.kt](app-android/app/src/main/java/com/nomad/app/voice/RealtimeVoiceManager.kt)**
   - Expone métodos de data channel
   - `sendPoiContext()` - Wrapper para contexto POI
   - `sendTextMessage()` - Wrapper para mensajes
   - `triggerBargeIn()` - Wrapper para interrupción

3. **[VoiceViewModel.kt](app-android/app/src/main/java/com/nomad/app/ui/voice/VoiceViewModel.kt)**
   - Métodos públicos para UI:
     - `sendPoiContext(id, lat, lng, name)`
     - `sendTextMessage(message)`
     - `triggerBargeIn()`

## Uso desde la UI

### Enviar contexto de POI:

```kotlin
// En VoiceScreen o donde tengas acceso al ViewModel
voiceViewModel.sendPoiContext(
    poiId = "poi_12345",
    lat = 40.4169,
    lng = -3.7035,
    name = "Puerta del Sol"
)
```

### Enviar mensaje de texto:

```kotlin
voiceViewModel.sendTextMessage("Cuéntame más sobre este lugar")
```

### Activar barge-in (interrumpir al asistente):

```kotlin
// Se puede llamar automáticamente cuando se detecta voz del usuario
voiceViewModel.triggerBargeIn()
```

## Formato de mensajes JSON

### Contexto POI:
```json
{
  "type": "poi_context",
  "id": "poi_12345",
  "lat": 40.4169,
  "lng": -3.7035,
  "name": "Puerta del Sol"
}
```

### Interrupción (barge-in):
```json
{
  "type": "interrupt"
}
```

### Mensajes de texto:
```
Cualquier string sin formato JSON
```

## Barge-In (Interrupción)

### ¿Qué es barge-in?

Barge-in permite al usuario interrumpir la respuesta del asistente de voz empezando a hablar. Esto hace que la conversación sea más natural.

### Flujo de barge-in:

```
1. Asistente está respondiendo (hablando)
2. Usuario empieza a hablar
3. Se detecta actividad de voz del usuario
4. Se llama a triggerBargeIn()
5. Se envía señal {"type":"interrupt"} via data channel
6. El backend/OpenAI interrumpe la respuesta actual
7. El asistente procesa la nueva pregunta del usuario
```

### Implementación actual:

**Método manual**: Por ahora, `triggerBargeIn()` se debe llamar manualmente desde la UI.

**Para implementación automática** (futuro):
- Usar VAD (Voice Activity Detection) en Android
- Detectar cuando el usuario habla mientras el asistente responde
- Llamar automáticamente a `triggerBargeIn()`

### Ejemplo de VAD simple (opcional):

```kotlin
// En WebRTCManager o RealtimeVoiceManager
private var isAssistantSpeaking = false
private val audioLevelThreshold = 0.05f

// Monitorizar nivel de audio del micrófono
private fun monitorAudioLevel() {
    // Si el asistente está hablando Y se detecta voz del usuario
    if (isAssistantSpeaking && userAudioLevel > audioLevelThreshold) {
        triggerBargeIn()
        isAssistantSpeaking = false
    }
}
```

## Logs esperados:

### Data channel creado:
```
WebRTCManager: Data channel creado: nomad_data
WebRTCManager: Data channel state: CONNECTING
WebRTCManager: Data channel state: OPEN
```

### Envío de contexto POI:
```
WebRTCManager: Mensaje enviado via data channel: {"type":"poi_context","id":"poi_123","lat":40.4169,"lng":-3.7035,"name":"Puerta del Sol"}
RealtimeVoiceManager: Contexto POI enviado: Puerta del Sol (poi_123)
```

### Barge-in:
```
WebRTCManager: Mensaje enviado via data channel: {"type":"interrupt"}
WebRTCManager: Señal de barge-in enviada
RealtimeVoiceManager: Barge-in activado - interrumpiendo respuesta del asistente
```

### Mensajes recibidos del servidor:
```
WebRTCManager: Mensaje recibido via data channel: <mensaje_del_servidor>
```

## Integración con el backend

El backend (Spring Boot) recibirá estos mensajes via el data channel de OpenAI Realtime API y puede:

1. **Para contexto POI**: Ejecutar automáticamente `poi_context` tool call con las coordenadas recibidas
2. **Para interrupciones**: Detener la generación de audio actual y procesar nueva entrada
3. **Para mensajes de texto**: Añadir contexto a la conversación

## Tool calls (resueltas por el servidor)

Como especificaste, las tool calls (`poi_nearby`, `poi_context`) se resuelven en el backend:

1. OpenAI Realtime API detecta que necesita información de un POI
2. OpenAI hace tool call al backend via WebRTC
3. Backend ejecuta la función (llama a Google Places API, etc.)
4. Backend devuelve resultado a OpenAI
5. OpenAI incorpora la información en su respuesta
6. Usuario escucha la respuesta con la información solicitada

## Ejemplo de flujo completo:

```
Usuario: [Pulsa POI en mapa]
    ↓
App: sendPoiContext("poi_123", 40.4169, -3.7035, "Puerta del Sol")
    ↓
Data channel: {"type":"poi_context","id":"poi_123",...}
    ↓
Backend: Recibe contexto POI
    ↓
Usuario: "Cuéntame la historia de este lugar"
    ↓
OpenAI: Tiene contexto POI, genera respuesta
    ↓
Usuario escucha: "La Puerta del Sol es una plaza de Madrid..."
    ↓
[Mientras habla el asistente]
    ↓
Usuario: [Empieza a hablar] "Y qué hay cerca?"
    ↓
App: triggerBargeIn()
    ↓
Data channel: {"type":"interrupt"}
    ↓
OpenAI: Interrumpe respuesta actual, procesa nueva pregunta
    ↓
OpenAI: Hace tool call poi_nearby con las coordenadas
    ↓
Backend: Ejecuta poi_nearby, devuelve POIs cercanos
    ↓
Usuario escucha: "Cerca de la Puerta del Sol hay..."
```

## Estado: ✅ IMPLEMENTADO

Todas las funcionalidades de data channel y barge-in están implementadas y listas para usar.
