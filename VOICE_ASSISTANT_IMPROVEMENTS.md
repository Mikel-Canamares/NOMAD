# Mejoras del Asistente de Voz - Sistema Natural y Fluido

## Problemas Resueltos

### 1. ❌ Problema: Solo responde a la primera pregunta
**Causa**: No se estaban procesando los eventos del data channel de OpenAI
**Solución**: Implementado parser completo de eventos en `RealtimeVoiceManager.kt`

### 2. ❌ Problema: Estado siempre en "Escuchando"
**Causa**: No se actualizaban los estados según eventos de OpenAI
**Solución**: Sistema de eventos que detecta y actualiza estados en tiempo real

### 3. ❌ Problema: No se puede mantener conversación natural
**Causa**: Configuración incorrecta del flujo de conversación
**Solución**: Mejorado sistema de turnos y contexto con `create_response: true`

### 4. ❌ Problema: Barge-in no funcional
**Causa**: Server VAD no estaba configurado correctamente
**Solución**: Configuración optimizada de VAD con detección natural de interrupciones

## Cambios Implementados

### A. Procesamiento de Eventos OpenAI (RealtimeVoiceManager.kt)

Se implementó la función `processOpenAIEvent()` que procesa eventos del servidor:

**Eventos manejados:**
- `input_audio_buffer.speech_started` → Estado: **LISTENING** (usuario hablando)
- `input_audio_buffer.speech_stopped` → Estado: **THINKING** (usuario dejó de hablar)
- `response.created` → Estado: **THINKING** (asistente generando respuesta)
- `response.audio.delta` → Estado: **SPEAKING** (asistente hablando)
- `response.done` → Estado: **LISTENING** (respuesta completada)
- `conversation.item.input_audio_transcription.completed` → Transcripción del usuario
- `response.audio_transcript.done` → Transcripción del asistente

**Logs visuales:**
```
📨 Evento recibido: {...}
🎤 Usuario comenzó a hablar
🤔 Usuario dejó de hablar - procesando
💭 Asistente generando respuesta
🗣️ Asistente hablando
✓ Respuesta completada - listo para escuchar
📝 Usuario dijo: "¿Qué museos hay cerca?"
📝 Asistente dijo: "Cerca de ti hay 3 museos..."
```

### B. Configuración Backend Mejorada (RealtimeService.java)

**1. Instrucciones de conversación natural:**
```java
CONVERSACIÓN NATURAL:
- Mantén una conversación fluida y natural
- Responde a múltiples preguntas seguidas sin repetir el saludo
- Recuerda el contexto de preguntas anteriores en la misma sesión
- Si el usuario interrumpe, responde a su nueva pregunta directamente
- Sé conciso pero informativo (máximo 2-3 frases por respuesta)
```

**2. Transcripciones habilitadas:**
```java
requestBody.put("input_audio_transcription", objectMapper.createObjectNode()
    .put("model", "whisper-1"));
```

**3. VAD optimizado para conversación natural:**
```java
requestBody.put("turn_detection", objectMapper.createObjectNode()
    .put("type", "server_vad")
    .put("threshold", 0.5)              // Sensibilidad de detección
    .put("prefix_padding_ms", 300)      // Audio antes del habla
    .put("silence_duration_ms", 700)    // Silencio para terminar turno
    .put("create_response", true));     // Auto-responde al terminar
```

### C. Sistema de Contexto Mejorado

**Antes (problema):**
```kotlin
// Creaba un mensaje de usuario que requería respuesta
conversation.item.create → role: user → "El usuario está en..."
```

**Ahora (solución):**
```kotlin
// Actualiza instrucciones de sesión sin requerir respuesta
session.update → instructions: "Eres un asistente... Usuario en (lat, lng)..."
```

**Ventaja**: El contexto se actualiza sin interrumpir el flujo de conversación.

### D. Saludo Inicial Mejorado

**Antes:**
```kotlin
// Intentaba forzar instrucciones en response.create (no funciona bien)
response.create → instructions: "Saluda al usuario..."
```

**Ahora:**
```kotlin
// 1. Crea mensaje de usuario "Hola"
conversation.item.create → role: user → "Hola"
// 2. Solicita respuesta
response.create
```

**Ventaja**: El asistente responde naturalmente al saludo del usuario.

## Flujo de Conversación Natural

### Escenario 1: Primera conversación

```
1. Usuario presiona botón Play
   └─> App: "Conectando..."

2. WebRTC conecta
   └─> App envía: "Hola" + response.create
   └─> App: "Escuchando..."

3. Asistente recibe "Hola"
   └─> Evento: response.created
   └─> App: "Pensando..."

4. Asistente genera saludo
   └─> Evento: response.audio.delta
   └─> App: "Respondiendo..."
   └─> Audio: "Hola! Soy tu asistente de viaje..."

5. Asistente termina
   └─> Evento: response.done
   └─> App: "Escuchando..."
```

### Escenario 2: Conversación continua

```
1. Usuario pregunta: "¿Qué museos hay cerca?"
   └─> Evento: input_audio_buffer.speech_started
   └─> App: "Escuchando..."

2. Usuario deja de hablar
   └─> Evento: input_audio_buffer.speech_stopped
   └─> App: "Pensando..."

3. VAD confirma fin de turno (700ms silencio)
   └─> create_response: true → genera respuesta automáticamente
   └─> Evento: response.created

4. Asistente responde
   └─> Evento: response.audio.delta
   └─> App: "Respondiendo..."
   └─> Audio: "Cerca de ti hay 3 museos: el Prado, Reina Sofía..."

5. Termina respuesta
   └─> Evento: response.done
   └─> App: "Escuchando..."

6. Usuario pregunta de nuevo: "Cuéntame más del Prado"
   └─> [Repite flujo desde paso 1]
   └─> Asistente mantiene contexto de conversación anterior
```

### Escenario 3: Barge-in (interrupción)

```
1. Asistente está hablando
   └─> App: "Respondiendo..."

2. Usuario interrumpe: "Espera, ¿y el Reina Sofía?"
   └─> Evento: input_audio_buffer.speech_started
   └─> Server VAD detecta voz del usuario
   └─> Asistente DETIENE respuesta actual
   └─> App: "Escuchando..."

3. Usuario termina de hablar
   └─> Evento: input_audio_buffer.speech_stopped
   └─> App: "Pensando..."

4. Asistente responde a la NUEVA pregunta
   └─> Evento: response.audio.delta
   └─> App: "Respondiendo..."
   └─> Audio: "El Reina Sofía es el museo de arte moderno..."
```

## Debugging y Logs

### Logs del cliente (Android)

**WebRTCManager.kt:**
```
✓ Data channel OPEN - enviando mensajes pendientes
✓ Mensaje enviado via data channel: {...}
```

**RealtimeVoiceManager.kt:**
```
✓ Conexión WebRTC establecida con OpenAI Realtime API
Solicitud de saludo enviada al asistente
Contexto inicial actualizado via session.update
📨 Evento recibido: {"type":"input_audio_buffer.speech_started"}
🎤 Usuario comenzó a hablar
🤔 Usuario dejó de hablar - procesando
💭 Asistente generando respuesta
🗣️ Asistente hablando
📝 Usuario dijo: "¿Qué museos hay cerca?"
📝 Asistente dijo: "Cerca de ti hay 3 museos principales..."
✓ Respuesta completada - listo para escuchar
```

**VoiceViewModel.kt:**
```
Estado de conexión: CONNECTED
Asistente state: LISTENING
Asistente state: THINKING
Asistente state: SPEAKING
Asistente state: LISTENING
```

**MapScreen.kt:**
```
Cargando POIs para ubicación: 40.4169, -3.7035, categoría: monument
POIs cargados exitosamente: 15 POIs
Contexto inicial enviado al asistente - POIs: 15, Ubicación: 40.4169, -3.7035
```

### Verificación de funcionamiento

✅ **Saludo inicial**: Debe escucharse "Hola! Soy tu asistente de viaje..."
✅ **Feedback visual**: Debe mostrar "Conectando", "Escuchando", "Pensando", "Respondiendo"
✅ **Múltiples preguntas**: Debe responder a todas las preguntas, no solo la primera
✅ **Contexto**: No debe preguntar "¿dónde estás?" (ya tiene la ubicación)
✅ **Categorías**: Debe entender "museos" si tienes esa categoría activa
✅ **Interrupciones**: Debe dejar de hablar cuando le interrumpes y responder la nueva pregunta

## Arquitectura Técnica

```
┌─────────────────────────────────────────────────────────────┐
│                        MapScreen.kt                         │
│  • Muestra mapa con POIs                                   │
│  • VoiceAssistantFab (botón Play/Stop)                     │
│  • Feedback visual de estado                                │
│  • Envía contexto inicial (ubicación + POIs)               │
└────────────┬────────────────────────────────────────────────┘
             │
             ↓ collectAsState()
┌─────────────────────────────────────────────────────────────┐
│                      VoiceViewModel.kt                      │
│  • Gestiona estados: isPreparing, isActive, assistantState  │
│  • AssistantState: IDLE, CONNECTING, LISTENING,             │
│    THINKING, SPEAKING                                       │
│  • Métodos: startVoiceAssistant(), sendInitialContext()    │
└────────────┬────────────────────────────────────────────────┘
             │
             ↓ delegates to
┌─────────────────────────────────────────────────────────────┐
│                  RealtimeVoiceManager.kt                    │
│  • Obtiene ephemeral token del backend                      │
│  • Crea conexión WebRTC                                     │
│  • Procesa eventos de OpenAI (processOpenAIEvent)          │
│  • Callbacks: onAssistantStateChange, onTranscriptReceived  │
│  • Métodos: requestGreeting(), sendInitialContext()        │
└────────────┬─────────┬──────────────────────────────────────┘
             │         │
             │         └─> HTTP POST /realtime/session
             │                     │
             ↓                     ↓
┌────────────────────┐    ┌──────────────────────────────────┐
│  WebRTCManager.kt  │    │    Backend (RealtimeService)    │
│  • PeerConnection  │    │  • Crea sesión OpenAI            │
│  • AudioTrack      │    │  • Configura VAD                 │
│  • DataChannel     │    │  • Habilita transcripciones      │
│  • Eventos SDP     │    │  • Define tools (poi_nearby,     │
│  • onDataChannel   │    │    poi_context)                  │
│    Message         │    │  • Instrucciones de conversación│
└────────────┬───────┘    └──────────────────────────────────┘
             │
             ↓ WebRTC Data Channel
┌─────────────────────────────────────────────────────────────┐
│              OpenAI Realtime API (gpt-4o)                   │
│  • Recibe audio del usuario                                 │
│  • Detecta voz con Server VAD                               │
│  • Genera respuestas (audio + texto)                        │
│  • Envía eventos: speech_started, response.created, etc.    │
│  • Ejecuta tool calls (poi_nearby, poi_context)            │
└─────────────────────────────────────────────────────────────┘
```

## Próximos pasos opcionales

### 1. Mostrar transcripciones en UI
Actualmente las transcripciones se loguean, pero podrían mostrarse en pantalla:

```kotlin
// En VoiceViewModel.kt
private val _userTranscript = MutableStateFlow<String>("")
val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

// En RealtimeVoiceManager
onTranscriptReceived = { role, text ->
    if (role == "user") {
        _userTranscript.value = text
    }
}
```

### 2. Historial de conversación
Guardar el historial de preguntas y respuestas para referencia.

### 3. Detección avanzada de barge-in
Implementar VAD local en el cliente para feedback visual antes de que el servidor responda.

### 4. Feedback táctil
Añadir vibración cuando el asistente detecta que empiezas a hablar.

## Configuración actual

**Server VAD:**
- `threshold`: 0.5 (sensibilidad media)
- `silence_duration_ms`: 700ms (0.7 segundos de silencio para terminar turno)
- `create_response`: true (auto-genera respuesta al terminar de hablar)

**Modelo:**
- `gpt-4o-realtime-preview-2024-12-17`
- Voice: `alloy`
- Modalities: `audio` + `text`
- Transcriptions: `whisper-1`

**Tools disponibles:**
- `poi_nearby`: Busca POIs cercanos
- `poi_context`: Obtiene información detallada de un POI

## Resumen de cambios

✅ Procesamiento completo de eventos OpenAI
✅ Estados actualizados en tiempo real (Escuchando/Pensando/Respondiendo)
✅ Conversación continua y natural
✅ Transcripciones habilitadas
✅ VAD optimizado con auto-respuesta
✅ Barge-in funcional
✅ Contexto mediante session.update (no interrumpe conversación)
✅ Saludo inicial natural
✅ Logs detallados con emojis para debugging
✅ Backend y Android compilados exitosamente

El asistente ahora debería funcionar de forma fluida, natural y mantener conversaciones complejas sobre los POIs del mapa.
