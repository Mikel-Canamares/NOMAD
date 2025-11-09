package com.nomad.app.ui.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.app.voice.RealtimeVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.PeerConnection

/**
 * ViewModel para el asistente de voz con OpenAI Realtime API
 */
class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private var voiceManager: RealtimeVoiceManager? = null

    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _connectionState = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState?> = _connectionState.asStateFlow()

    // Estados del asistente para feedback visual
    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    enum class AssistantState {
        IDLE,           // Inactivo
        CONNECTING,     // Conectando
        LISTENING,      // Escuchando al usuario
        THINKING,       // Procesando/pensando
        SPEAKING        // Hablando/respondiendo
    }

    /**
     * Actualiza el estado del permiso del micrófono
     */
    fun updateMicPermission(granted: Boolean) {
        _hasMicPermission.value = granted
        if (!granted) {
            // Si se revoca el permiso, detener cualquier actividad de voz
            stopVoiceAssistant()
        }
    }

    /**
     * Inicia el asistente de voz con OpenAI Realtime API
     */
    fun startVoiceAssistant() {
        if (!_hasMicPermission.value) {
            _lastError.value = "No hay permiso de micrófono"
            return
        }

        if (_isPreparing.value || _isActive.value) {
            return
        }

        _isPreparing.value = true
        _assistantState.value = AssistantState.CONNECTING
        _lastError.value = null

        // Crear RealtimeVoiceManager si no existe
        if (voiceManager == null) {
            voiceManager = RealtimeVoiceManager(getApplication()).apply {
                // Configurar listeners
                onConnectionStateChange = { state ->
                    _connectionState.value = state
                    when (state) {
                        PeerConnection.PeerConnectionState.CONNECTED -> {
                            _isActive.value = true
                            _isPreparing.value = false
                            _assistantState.value = AssistantState.LISTENING
                        }
                        PeerConnection.PeerConnectionState.FAILED -> {
                            _isActive.value = false
                            _isPreparing.value = false
                            _assistantState.value = AssistantState.IDLE
                            _lastError.value = "Conexión fallida"
                        }
                        PeerConnection.PeerConnectionState.DISCONNECTED -> {
                            _isActive.value = false
                            _isPreparing.value = false
                            _assistantState.value = AssistantState.IDLE
                        }
                        else -> {}
                    }
                }

                onError = { error ->
                    _lastError.value = error
                    _isPreparing.value = false
                    _isActive.value = false
                    _assistantState.value = AssistantState.IDLE
                }

                // Callback para estados del asistente (listening, thinking, speaking)
                onAssistantStateChange = { state ->
                    _assistantState.value = when (state) {
                        "listening" -> AssistantState.LISTENING
                        "thinking" -> AssistantState.THINKING
                        "speaking" -> AssistantState.SPEAKING
                        else -> AssistantState.LISTENING
                    }
                }
            }
        }

        // Iniciar sesión
        voiceManager?.startSession(viewModelScope)
    }

    /**
     * Detiene el asistente de voz
     */
    fun stopVoiceAssistant() {
        voiceManager?.endSession()
        voiceManager = null
        _isActive.value = false
        _isPreparing.value = false
        _assistantState.value = AssistantState.IDLE
        _connectionState.value = null
    }

    /**
     * Habilita/deshabilita el micrófono durante la llamada
     */
    fun toggleMicrophone() {
        voiceManager?.let { manager ->
            val currentState = manager.isMicrophoneEnabled()
            manager.setMicrophoneEnabled(!currentState)
        }
    }

    /**
     * Envía contexto de POI al asistente
     */
    fun sendPoiContext(poiId: String, lat: Double, lng: Double, name: String) {
        voiceManager?.sendPoiContext(poiId, lat, lng, name)
    }

    /**
     * Envía contexto inicial con ubicación y POIs cercanos
     */
    fun sendInitialContext(
        userLat: Double,
        userLng: Double,
        pois: List<Map<String, Any>>,
        selectedCategory: String? = null
    ) {
        voiceManager?.sendInitialContext(userLat, userLng, pois, selectedCategory)
    }

    /**
     * Solicita al asistente que salude al usuario
     */
    fun requestGreeting() {
        voiceManager?.requestGreeting()
    }

    /**
     * Envía mensaje de texto al asistente
     */
    fun sendTextMessage(message: String) {
        voiceManager?.sendTextMessage(message)
    }

    /**
     * Activa barge-in para interrumpir al asistente
     * Se llama automáticamente cuando se detecta que el usuario habla
     */
    fun triggerBargeIn() {
        voiceManager?.triggerBargeIn()
    }

    /**
     * Limpia el último error
     */
    fun clearError() {
        _lastError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceAssistant()
    }
}
