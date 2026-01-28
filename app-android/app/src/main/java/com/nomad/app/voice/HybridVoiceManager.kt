package com.nomad.app.voice

import android.content.Context
import android.util.Log
import com.nomad.app.data.api.RetrofitClient
import com.nomad.app.data.dto.ConversationMessage
import com.nomad.app.data.dto.VoiceChatRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gestor híbrido de voz usando:
 * - Android STT (Speech to Text) nativo - GRATIS
 * - OpenAI GPT-4 (texto a texto) - ~$0.01/1K tokens
 * - Android TTS (Text to Speech) nativo - GRATIS
 *
 * AHORRO: 97% vs OpenAI Realtime API
 * - Realtime API: ~$18/hora
 * - Sistema híbrido: ~$0.50/hora
 */
class HybridVoiceManager(
    private val context: Context,
    backendUrl: String? = null
) {

    private val ttsManager = AndroidTTSManager(context)
    private val sttManager = AndroidSTTManager(context)
    private val apiService = RetrofitClient.getInstance(backendUrl).voiceChatApiService

    private val conversationHistory = mutableListOf<ConversationMessage>()
    private var isActive = false

    // Contexto de la conversación
    private var userLat: Double? = null
    private var userLng: Double? = null
    private var selectedCategory: String? = null
    private var nearbyPois: List<Map<String, Any>>? = null

    // Callbacks
    var onStateChange: ((VoiceState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onUserSpeech: ((String) -> Unit)? = null
    var onAssistantResponse: ((String) -> Unit)? = null

    enum class VoiceState {
        IDLE,           // Inactivo
        INITIALIZING,   // Inicializando
        LISTENING,      // Escuchando al usuario
        PROCESSING,     // Procesando con GPT-4
        SPEAKING        // Hablando (TTS)
    }

    companion object {
        private const val TAG = "HybridVoiceManager"
    }

    /**
     * Inicializa el sistema de voz
     */
    fun initialize(onReady: () -> Unit = {}) {
        Log.d(TAG, "Inicializando sistema híbrido de voz")
        onStateChange?.invoke(VoiceState.INITIALIZING)

        // Inicializar TTS
        ttsManager.initialize {
            // Configurar callbacks de TTS
            ttsManager.onSpeakingStarted = {
                onStateChange?.invoke(VoiceState.SPEAKING)
            }

            ttsManager.onSpeakingCompleted = {
                if (isActive) {
                    // Cuando termina de hablar, volver a escuchar
                    Log.d(TAG, "TTS terminado, iniciando escucha...")
                    onStateChange?.invoke(VoiceState.LISTENING)
                    sttManager.startListening(continuous = true)
                } else {
                    onStateChange?.invoke(VoiceState.IDLE)
                }
            }

            ttsManager.onError = { error ->
                Log.e(TAG, "Error en TTS: $error")
                onError?.invoke(error)
            }

            // Inicializar STT
            sttManager.initialize()
            setupSTTCallbacks()

            Log.d(TAG, "✓ Sistema de voz inicializado")
            onStateChange?.invoke(VoiceState.IDLE)
            onReady()
        }
    }

    /**
     * Configura callbacks del STT
     */
    private fun setupSTTCallbacks() {
        sttManager.onListeningStarted = {
            if (isActive) {
                Log.d(TAG, "👂 Estado: LISTENING")
                onStateChange?.invoke(VoiceState.LISTENING)
            }
        }

        sttManager.onTextRecognized = { text ->
            Log.d(TAG, "📝 Texto reconocido: '$text'")
            onUserSpeech?.invoke(text)

            // Procesar inmediatamente con GPT-4
            processUserMessage(text)
        }

        sttManager.onListeningEnded = {
            Log.d(TAG, "🔇 STT detenido")
        }

        sttManager.onError = { error ->
            Log.w(TAG, "⚠️ Error en STT: $error")

            // Errores recuperables que no deben notificar al usuario
            val isRecoverableError = error.contains("No se detectó voz") ||
                                    error.contains("Timeout") ||
                                    error.contains("no match", ignoreCase = true) ||
                                    error.contains("ocupado", ignoreCase = true)

            if (!isRecoverableError) {
                onError?.invoke("Error de reconocimiento: $error")
            }

            // Si está activo y no está procesando/hablando, reintentar
            if (isActive && !ttsManager.isSpeaking()) {
                Log.d(TAG, "Reintentando escucha en 500ms...")
                CoroutineScope(Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(500)
                    if (isActive && !ttsManager.isSpeaking()) {
                        Log.d(TAG, "Reiniciando STT tras error")
                        sttManager.startListening(continuous = true)
                    }
                }
            }
        }
    }

    /**
     * Actualiza el contexto de ubicación y POIs
     */
    fun updateContext(
        lat: Double?,
        lng: Double?,
        category: String? = null,
        pois: List<Map<String, Any>>? = null
    ) {
        userLat = lat
        userLng = lng
        selectedCategory = category
        nearbyPois = pois
        Log.d(TAG, "Contexto actualizado: lat=$lat, lng=$lng, category=$category, pois=${pois?.size}")
    }

    /**
     * Inicia la sesión de voz
     */
    fun startSession(scope: CoroutineScope) {
        if (isActive) {
            Log.w(TAG, "Sesión ya está activa")
            return
        }

        isActive = true
        conversationHistory.clear()

        Log.d(TAG, "🎙️ Iniciando sesión de voz")

        // Saludo inicial
        val greeting = "Hola! Soy tu asistente de viaje. ¿En qué puedo ayudarte?"
        ttsManager.speak(greeting)

        // Agregar al historial
        conversationHistory.add(ConversationMessage("assistant", greeting))
        onAssistantResponse?.invoke(greeting)

        // Empezar a escuchar cuando termine el saludo
        // (el callback onSpeakingCompleted lo hará automáticamente)
    }

    /**
     * Detiene la sesión de voz
     */
    fun stopSession() {
        Log.d(TAG, "⏹️ Deteniendo sesión de voz")
        isActive = false
        sttManager.stopListening()
        ttsManager.stop()
        onStateChange?.invoke(VoiceState.IDLE)
    }

    /**
     * Procesa un mensaje del usuario con GPT-4
     */
    private fun processUserMessage(message: String) {
        Log.d(TAG, "💭 Estado: PROCESSING")
        onStateChange?.invoke(VoiceState.PROCESSING)

        // Agregar mensaje del usuario al historial
        conversationHistory.add(ConversationMessage("user", message))

        // Crear request
        val request = VoiceChatRequest(
            message = message,
            userLat = userLat,
            userLng = userLng,
            selectedCategory = selectedCategory,
            nearbyPois = nearbyPois,
            conversationHistory = conversationHistory.takeLast(10)  // Últimas 10 mensajes
        )

        // Llamar al backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🌐 Enviando a GPT-4: '$message'")
                val response = apiService.chat(request)
                Log.d(TAG, "✓ Respuesta GPT-4: '${response.response}'")

                // Agregar respuesta al historial
                conversationHistory.add(ConversationMessage("assistant", response.response))

                // Notificar y sintetizar en Main thread
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        onAssistantResponse?.invoke(response.response)

                        // Cambiar a estado SPEAKING se hace en el callback del TTS
                        Log.d(TAG, "🗣️ Iniciando TTS...")
                        ttsManager.speak(response.response)
                    }
                }

                // El callback onSpeakingCompleted reiniciará la escucha automáticamente

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error llamando a GPT-4: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("Unable to resolve host") == true ->
                            "Sin conexión a internet"
                        e.message?.contains("timeout") == true ->
                            "Timeout al conectar con el servidor"
                        else ->
                            "Error de comunicación: ${e.message}"
                    }

                    Log.e(TAG, "Error: $errorMsg")
                    onError?.invoke(errorMsg)

                    // Respuesta de error en TTS
                    if (isActive) {
                        ttsManager.speak("Lo siento, ha ocurrido un error. Por favor, inténtalo de nuevo.")
                    } else {
                        // Si no está activo, volver a IDLE
                        onStateChange?.invoke(VoiceState.IDLE)
                    }
                }
            }
        }
    }

    /**
     * Habla un mensaje específico (sin escucha)
     * Útil para hablar sobre un POI específico
     */
    fun speakAbout(topic: String, scope: CoroutineScope) {
        Log.d(TAG, "🗣️ Hablando sobre: $topic")

        // Detener cualquier síntesis anterior
        ttsManager.stop()

        // Procesar como si el usuario lo hubiera preguntado
        processUserMessage(topic)
    }

    /**
     * Interrumpe el TTS actual
     */
    fun stopSpeaking() {
        ttsManager.stop()

        // Si está activo, volver a escuchar
        if (isActive) {
            sttManager.startListening(continuous = true)
        }
    }

    /**
     * Actualiza la velocidad de TTS
     */
    fun updateTtsSpeed(speed: Float) {
        ttsManager.setSpeechRate(speed)
    }

    /**
     * Actualiza el pitch de TTS
     */
    fun updateTtsPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }

    /**
     * Libera recursos
     */
    fun shutdown() {
        Log.d(TAG, "Liberando recursos")
        isActive = false
        sttManager.shutdown()
        ttsManager.shutdown()
        conversationHistory.clear()
    }

    /**
     * Verifica si está activo
     */
    fun isActive(): Boolean {
        return isActive
    }

    /**
     * Verifica si está hablando
     */
    fun isSpeaking(): Boolean {
        return ttsManager.isSpeaking()
    }
}
