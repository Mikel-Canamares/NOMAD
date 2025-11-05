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
                    RetrofitClient.apiService.createRealtimeSession()
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
