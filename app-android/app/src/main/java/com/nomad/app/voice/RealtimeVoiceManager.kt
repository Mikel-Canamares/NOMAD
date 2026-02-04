package com.nomad.app.voice

import android.content.Context
import android.util.Log
import com.nomad.app.data.api.RetrofitClient
import com.nomad.app.data.dto.SdpAnswerResponse
import com.nomad.app.data.dto.SdpOfferRequest
import com.nomad.app.webrtc.WebRTCManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gestiona la sesión de voz en tiempo real con OpenAI Realtime API usando WebRTC
 *
 * Flujo:
 * 1. Obtener token efímero del backend
 * 2. Crear PeerConnection y AudioTrack local
 * 3. Crear SDP offer
 * 4. Enviar offer a OpenAI Realtime API con Authorization: Bearer <token>
 * 5. Aplicar SDP answer de OpenAI
 * 6. Intercambiar ICE candidates
 * 7. Conexión establecida - el audio fluye automáticamente
 */
class RealtimeVoiceManager(private val context: Context) {

    private var webRTCManager: WebRTCManager? = null
    private var ephemeralToken: String? = null
    private var isConnecting = false
    private var isConnected = false
    private var coroutineScope: CoroutineScope? = null

    // Cliente HTTP para OpenAI
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    // Callbacks
    var onConnectionStateChange: ((PeerConnection.PeerConnectionState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onIceConnectionChange: ((PeerConnection.IceConnectionState) -> Unit)? = null
    var onAssistantStateChange: ((String) -> Unit)? = null // listening, thinking, speaking
    var onTranscriptReceived: ((String, String) -> Unit)? = null // (role, text) - para debugging

    companion object {
        private const val TAG = "RealtimeVoiceManager"
        private const val OPENAI_REALTIME_URL = "https://api.openai.com/v1/realtime"
        private const val MODEL = "gpt-4o-realtime-preview-2024-12-17"
    }

    /**
     * Inicia la sesión de voz con OpenAI Realtime API
     */
    fun startSession(scope: CoroutineScope) {
        if (isConnecting || isConnected) {
            Log.w(TAG, "Sesión ya está activa o conectando")
            return
        }

        isConnecting = true
        coroutineScope = scope
        Log.d(TAG, "Iniciando sesión de voz con OpenAI Realtime API")

        scope.launch {
            try {
                // Paso 1: Obtener token efímero del backend
                val sessionResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.getInstance().apiService.createRealtimeSession()
                }
                ephemeralToken = sessionResponse.client_secret
                Log.d(TAG, "Token efímero obtenido: ${ephemeralToken?.take(20)}...")

                // Paso 2: Inicializar WebRTC
                withContext(Dispatchers.Main) {
                    initializeWebRTC()
                }

                // Paso 3: Crear SDP offer
                createAndSendOffer()

            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar sesión: ${e.message}", e)
                isConnecting = false
                withContext(Dispatchers.Main) {
                    onError?.invoke("Error al iniciar sesión: ${e.message}")
                }
            }
        }
    }

    /**
     * Inicializa WebRTC y configura listeners
     */
    private fun initializeWebRTC() {
        webRTCManager = WebRTCManager(context)
        webRTCManager?.initialize()
        webRTCManager?.createLocalAudioTrack()

        // Configurar listeners
        webRTCManager?.onConnectionStateChange = { state ->
            Log.d(TAG, "Estado de conexión: $state")
            onConnectionStateChange?.invoke(state)

            when (state) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    isConnected = true
                    isConnecting = false
                    Log.d(TAG, "✓ Conexión WebRTC establecida con OpenAI Realtime API")

                    // Solicitar saludo inicial del asistente
                    coroutineScope?.launch {
                        kotlinx.coroutines.delay(500) // Esperar a que el data channel esté listo
                        requestGreeting()
                    }
                }
                PeerConnection.PeerConnectionState.FAILED -> {
                    isConnecting = false
                    isConnected = false
                    onError?.invoke("Conexión WebRTC fallida")
                }
                PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    isConnected = false
                }
                else -> {}
            }
        }

        webRTCManager?.onIceCandidate = { candidate ->
            Log.d(TAG, "Nuevo ICE candidate: ${candidate.sdpMid}")
            // Los ICE candidates se intercambian automáticamente con el flujo SDP
            // OpenAI Realtime API incluye los candidates en el SDP answer
        }

        // Callback para mensajes del data channel (eventos de OpenAI)
        webRTCManager?.onDataChannelMessage = { message ->
            processOpenAIEvent(message)
        }

        // Callback cuando el data channel se abre
        webRTCManager?.onDataChannelOpen = {
            Log.d(TAG, "✓ Data channel abierto - listo para enviar contexto")
        }

        // Crear PeerConnection
        webRTCManager?.createPeerConnection()
    }

    /**
     * Crea SDP offer y lo envía a OpenAI Realtime API
     */
    private fun createAndSendOffer() {
        webRTCManager?.createOffer { sdp ->
            Log.d(TAG, "SDP offer creado, enviando a OpenAI Realtime API")
            Log.d(TAG, "SDP type: ${sdp.type}, length: ${sdp.description.length}")
            sendOfferToOpenAI(sdp)
        }
    }

    /**
     * Envía la SDP offer a OpenAI Realtime API vía HTTP POST
     */
    private fun sendOfferToOpenAI(sdp: SessionDescription) {
        val token = ephemeralToken
        if (token == null) {
            onError?.invoke("Token efímero no disponible")
            isConnecting = false
            return
        }

        try {
            // Crear el request body
            val offerRequest = SdpOfferRequest(
                type = "offer",
                sdp = sdp.description
            )
            val jsonBody = json.encodeToString(SdpOfferRequest.serializer(), offerRequest)

            // Construir request HTTP
            val request = Request.Builder()
                .url("$OPENAI_REALTIME_URL?model=$MODEL")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/sdp")
                .post(sdp.description.toRequestBody("application/sdp".toMediaType()))
                .build()

            Log.d(TAG, "Enviando SDP offer a OpenAI...")

            // Enviar request asíncrono
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Error al enviar SDP offer: ${e.message}", e)
                    isConnecting = false
                    coroutineScope?.launch(Dispatchers.Main) {
                        onError?.invoke("Error de red: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: "Sin cuerpo de error"
                            Log.e(TAG, "Error HTTP ${response.code}: $errorBody")
                            isConnecting = false
                            coroutineScope?.launch(Dispatchers.Main) {
                                onError?.invoke("Error HTTP ${response.code}: ${response.message}")
                            }
                            return
                        }

                        // Parsear SDP answer
                        val answerSdp = response.body?.string()
                        if (answerSdp == null) {
                            Log.e(TAG, "Respuesta vacía de OpenAI")
                            isConnecting = false
                            coroutineScope?.launch(Dispatchers.Main) {
                                onError?.invoke("Respuesta vacía de OpenAI")
                            }
                            return
                        }

                        Log.d(TAG, "SDP answer recibido de OpenAI, length: ${answerSdp.length}")

                        // Aplicar SDP answer
                        val sessionDescription = SessionDescription(
                            SessionDescription.Type.ANSWER,
                            answerSdp
                        )

                        coroutineScope?.launch(Dispatchers.Main) {
                            webRTCManager?.setRemoteDescription(sessionDescription) {
                                Log.d(TAG, "✓ SDP answer aplicado correctamente")
                                Log.d(TAG, "Esperando conexión ICE...")
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar respuesta: ${e.message}", e)
                        isConnecting = false
                        coroutineScope?.launch(Dispatchers.Main) {
                            onError?.invoke("Error al procesar respuesta: ${e.message}")
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Error al preparar request: ${e.message}", e)
            isConnecting = false
            onError?.invoke("Error al preparar request: ${e.message}")
        }
    }

    /**
     * Habilita o deshabilita el micrófono
     */
    fun setMicrophoneEnabled(enabled: Boolean) {
        webRTCManager?.setMicrophoneEnabled(enabled)
        Log.d(TAG, "Micrófono ${if (enabled) "habilitado" else "deshabilitado"}")
    }

    /**
     * Verifica si el micrófono está habilitado
     */
    fun isMicrophoneEnabled(): Boolean {
        return webRTCManager?.isMicrophoneEnabled() ?: false
    }

    /**
     * Envía contexto de POI al asistente vía data channel
     * @param poiId ID del POI
     * @param lat Latitud
     * @param lng Longitud
     * @param name Nombre del POI
     */
    fun sendPoiContext(poiId: String, lat: Double, lng: Double, name: String) {
        webRTCManager?.sendPoiContext(poiId, lat, lng, name)
        Log.d(TAG, "Contexto POI enviado: $name ($poiId)")
    }

    /**
     * Envía mensaje de texto al asistente vía data channel
     */
    fun sendTextMessage(message: String) {
        webRTCManager?.sendTextMessage(message)
        Log.d(TAG, "Mensaje enviado: $message")
    }

    /**
     * Activa barge-in: interrumpe la respuesta del asistente
     * Se llama cuando el usuario comienza a hablar mientras el asistente responde
     */
    fun triggerBargeIn() {
        webRTCManager?.sendBargeInSignal()
        Log.d(TAG, "Barge-in activado - interrumpiendo respuesta del asistente")
    }

    /**
     * Solicita al asistente que salude al usuario
     * Envía un evento response.create para generar el saludo inicial
     */
    fun requestGreeting() {
        // Primero enviamos un mensaje de sistema para establecer el contexto
        val contextEvent = """
            {
                "type": "conversation.item.create",
                "item": {
                    "type": "message",
                    "role": "user",
                    "content": [{
                        "type": "input_text",
                        "text": "Hola"
                    }]
                }
            }
        """.trimIndent()

        webRTCManager?.sendTextMessage(contextEvent)

        // Luego solicitamos la respuesta
        coroutineScope?.launch {
            kotlinx.coroutines.delay(100)
            val greetingEvent = """
                {
                    "type": "response.create"
                }
            """.trimIndent()
            webRTCManager?.sendTextMessage(greetingEvent)
            Log.d(TAG, "Solicitud de saludo enviada al asistente")
        }
    }

    /**
     * Envía contexto inicial con ubicación del usuario y POIs cercanos
     * Este contexto se envía como un mensaje de sistema que no requiere respuesta
     */
    fun sendInitialContext(
        userLat: Double,
        userLng: Double,
        pois: List<Map<String, Any>>,
        selectedCategory: String? = null
    ) {
        val poisSummary = pois.take(10).joinToString(", ") { poi ->
            poi["name"] as? String ?: "POI"
        }

        val contextMessage = buildString {
            append("Contexto de ubicación: El usuario está en las coordenadas $userLat, $userLng. ")
            if (selectedCategory != null) {
                append("Está viendo la categoría: $selectedCategory. ")
            }
            if (pois.isNotEmpty()) {
                append("Hay ${pois.size} POIs cercanos: $poisSummary.")
            }
        }

        // Usar session.update para actualizar el contexto sin generar respuesta
        val sessionUpdateEvent = """
            {
                "type": "session.update",
                "session": {
                    "instructions": "Eres un asistente de viaje. El usuario está en ubicación ($userLat, $userLng). ${if (selectedCategory != null) "Categoría activa: $selectedCategory. " else ""}${if (pois.isNotEmpty()) "POIs cercanos: $poisSummary. " else ""}Responde de forma concisa y natural a sus preguntas sobre estos lugares."
                }
            }
        """.trimIndent()

        webRTCManager?.sendTextMessage(sessionUpdateEvent)
        Log.d(TAG, "Contexto inicial actualizado via session.update")
    }

    /**
     * Procesa eventos recibidos del servidor OpenAI via data channel
     * Estos eventos indican cambios de estado del asistente
     */
    private fun processOpenAIEvent(eventJson: String) {
        try {
            Log.d(TAG, "📨 Evento recibido: ${eventJson.take(200)}")

            // Parsear el JSON del evento
            val jsonObject = org.json.JSONObject(eventJson)
            val eventType = jsonObject.optString("type", "")

            when (eventType) {
                // El servidor ha detectado que el usuario está hablando
                "input_audio_buffer.speech_started" -> {
                    Log.d(TAG, "🎤 Usuario comenzó a hablar")
                    onAssistantStateChange?.invoke("listening")
                }

                // El usuario dejó de hablar, procesando entrada
                "input_audio_buffer.speech_stopped" -> {
                    Log.d(TAG, "🤔 Usuario dejó de hablar - procesando")
                    onAssistantStateChange?.invoke("thinking")
                }

                // El usuario terminó de hablar y se confirmó el input
                "input_audio_buffer.committed" -> {
                    Log.d(TAG, "✓ Audio del usuario confirmado")
                }

                // El asistente está generando una respuesta
                "response.created" -> {
                    Log.d(TAG, "💭 Asistente generando respuesta")
                    onAssistantStateChange?.invoke("thinking")
                }

                // El asistente comenzó a generar audio de respuesta
                "response.audio.delta" -> {
                    // Primer delta de audio = asistente está hablando
                    onAssistantStateChange?.invoke("speaking")
                }

                // El asistente comenzó a hablar
                "response.audio_transcript.delta" -> {
                    Log.d(TAG, "🗣️ Asistente hablando")
                    onAssistantStateChange?.invoke("speaking")
                }

                // La respuesta del asistente ha terminado
                "response.done" -> {
                    Log.d(TAG, "✓ Respuesta completada - listo para escuchar")
                    onAssistantStateChange?.invoke("listening")
                }

                // Transcripción del usuario (útil para debugging)
                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = jsonObject.optJSONObject("transcript")?.optString("text", "")
                    if (!transcript.isNullOrEmpty()) {
                        Log.d(TAG, "📝 Usuario dijo: $transcript")
                        onTranscriptReceived?.invoke("user", transcript)
                    }
                }

                // Transcripción del asistente (útil para debugging)
                "response.audio_transcript.done" -> {
                    val transcript = jsonObject.optString("transcript", "")
                    if (transcript.isNotEmpty()) {
                        Log.d(TAG, "📝 Asistente dijo: $transcript")
                        onTranscriptReceived?.invoke("assistant", transcript)
                    }
                }

                // Error del servidor
                "error" -> {
                    val error = jsonObject.optJSONObject("error")
                    val errorMessage = error?.optString("message", "Error desconocido") ?: "Error desconocido"
                    Log.e(TAG, "❌ Error del servidor: $errorMessage")
                    onError?.invoke("Error: $errorMessage")
                }

                // Evento de sesión actualizada (confirmación de configuración)
                "session.updated" -> {
                    Log.d(TAG, "✓ Sesión actualizada correctamente")
                }

                // Otros eventos (logging para debugging)
                else -> {
                    if (eventType.isNotEmpty()) {
                        Log.d(TAG, "ℹ️ Evento no manejado: $eventType")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando evento de OpenAI: ${e.message}", e)
        }
    }

    /**
     * Finaliza la sesión de voz y libera recursos
     */
    fun endSession() {
        Log.d(TAG, "Finalizando sesión de voz")
        isConnected = false
        isConnecting = false
        ephemeralToken = null
        webRTCManager?.release()
        webRTCManager = null
        coroutineScope = null
    }

    /**
     * Verifica si la sesión está activa
     */
    fun isSessionActive(): Boolean {
        return isConnected
    }
}
