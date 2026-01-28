package com.nomad.app.ui.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.app.voice.HybridVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel para el asistente de voz híbrido
 * Usa Android STT + GPT-4 + Android TTS (97% más barato que Realtime API)
 */
class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private var voiceManager: HybridVoiceManager? = null

    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // Estados del asistente para feedback visual
    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    // Transcripciones (para debugging y feedback)
    private val _userTranscript = MutableStateFlow<String>("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _assistantTranscript = MutableStateFlow<String>("")
    val assistantTranscript: StateFlow<String> = _assistantTranscript.asStateFlow()

    enum class AssistantState {
        IDLE,           // Inactivo
        INITIALIZING,   // Inicializando
        LISTENING,      // Escuchando al usuario
        PROCESSING,     // Procesando con GPT-4
        SPEAKING        // Hablando (TTS)
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
     * Inicia el asistente de voz híbrido
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
        _assistantState.value = AssistantState.INITIALIZING
        _lastError.value = null

        // Crear HybridVoiceManager si no existe
        if (voiceManager == null) {
            voiceManager = HybridVoiceManager(getApplication()).apply {
                // Configurar callbacks de estado
                onStateChange = { state ->
                    _assistantState.value = when (state) {
                        HybridVoiceManager.VoiceState.IDLE -> AssistantState.IDLE
                        HybridVoiceManager.VoiceState.INITIALIZING -> AssistantState.INITIALIZING
                        HybridVoiceManager.VoiceState.LISTENING -> AssistantState.LISTENING
                        HybridVoiceManager.VoiceState.PROCESSING -> AssistantState.PROCESSING
                        HybridVoiceManager.VoiceState.SPEAKING -> AssistantState.SPEAKING
                    }
                }

                onError = { error ->
                    _lastError.value = error
                }

                onUserSpeech = { text ->
                    _userTranscript.value = text
                }

                onAssistantResponse = { text ->
                    _assistantTranscript.value = text
                }

                // Inicializar
                initialize {
                    _isPreparing.value = false
                    _isActive.value = true

                    // Iniciar sesión
                    startSession(viewModelScope)
                }
            }
        } else {
            // Ya existe, solo iniciar sesión
            _isPreparing.value = false
            _isActive.value = true
            voiceManager?.startSession(viewModelScope)
        }
    }

    /**
     * Detiene el asistente de voz
     */
    fun stopVoiceAssistant() {
        voiceManager?.stopSession()
        _isActive.value = false
        _isPreparing.value = false
        _assistantState.value = AssistantState.IDLE
    }

    /**
     * Actualiza el contexto de ubicación y POIs
     */
    fun updateContext(
        userLat: Double?,
        userLng: Double?,
        selectedCategory: String? = null,
        pois: List<Map<String, Any>>? = null
    ) {
        voiceManager?.updateContext(userLat, userLng, selectedCategory, pois)
    }

    /**
     * Habla sobre un tema específico (para usar desde POI detail)
     * @param topic Tema sobre el que hablar (ej: "Cuéntame sobre el Museo del Prado")
     */
    fun speakAbout(topic: String) {
        if (!_hasMicPermission.value) {
            _lastError.value = "No hay permiso de micrófono"
            return
        }

        // Si no está inicializado, inicializar primero
        if (voiceManager == null) {
            _isPreparing.value = true
            _assistantState.value = AssistantState.INITIALIZING

            voiceManager = HybridVoiceManager(getApplication()).apply {
                onStateChange = { state ->
                    _assistantState.value = when (state) {
                        HybridVoiceManager.VoiceState.IDLE -> AssistantState.IDLE
                        HybridVoiceManager.VoiceState.INITIALIZING -> AssistantState.INITIALIZING
                        HybridVoiceManager.VoiceState.LISTENING -> AssistantState.LISTENING
                        HybridVoiceManager.VoiceState.PROCESSING -> AssistantState.PROCESSING
                        HybridVoiceManager.VoiceState.SPEAKING -> AssistantState.SPEAKING
                    }
                }

                onError = { error ->
                    _lastError.value = error
                }

                onUserSpeech = { text ->
                    _userTranscript.value = text
                }

                onAssistantResponse = { text ->
                    _assistantTranscript.value = text
                }

                initialize {
                    _isPreparing.value = false
                    speakAbout(topic, viewModelScope)
                }
            }
        } else {
            // Ya inicializado, hablar directamente
            voiceManager?.speakAbout(topic, viewModelScope)
        }
    }

    /**
     * Detiene la síntesis actual (útil cuando se toca un POI)
     */
    fun stopSpeaking() {
        voiceManager?.stopSpeaking()
    }

    /**
     * Limpia el último error
     */
    fun clearError() {
        _lastError.value = null
    }

    /**
     * Verifica si está hablando
     */
    fun isSpeaking(): Boolean {
        return voiceManager?.isSpeaking() ?: false
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.shutdown()
        voiceManager = null
    }
}
