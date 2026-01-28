package com.nomad.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Gestor de Speech-to-Text usando el reconocedor nativo de Android
 * VERSIÓN MEJORADA CON LOGGING EXTENSIVO
 */
class AndroidSTTManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldContinueListening = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var restartAttempts = 0
    private val MAX_RESTART_ATTEMPTS = 5

    var onListeningStarted: (() -> Unit)? = null
    var onTextRecognized: ((String) -> Unit)? = null
    var onListeningEnded: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "STT"  // Tag corto para ver mejor en logcat
    }

    /**
     * Verifica permiso de micrófono
     */
    private fun hasRecordAudioPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "🎙️ Permiso RECORD_AUDIO: $hasPermission")
        return hasPermission
    }

    /**
     * Inicializa el reconocedor de voz
     */
    fun initialize() {
        mainHandler.post {
            Log.d(TAG, "==== INITIALIZE START ====")

            // Verificar disponibilidad
            val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
            Log.d(TAG, "Reconocimiento disponible: $isAvailable")

            if (!isAvailable) {
                Log.e(TAG, "❌ Reconocimiento de voz NO disponible")
                onError?.invoke("Reconocimiento de voz no disponible")
                return@post
            }

            // Verificar permisos
            if (!hasRecordAudioPermission()) {
                Log.e(TAG, "❌ Sin permiso RECORD_AUDIO")
                onError?.invoke("Sin permiso de micrófono")
                return@post
            }

            // Destruir instancia anterior
            if (speechRecognizer != null) {
                Log.d(TAG, "Destruyendo reconocedor anterior")
                try {
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "Error destruyendo: ${e.message}")
                }
                speechRecognizer = null
            }

            // Crear nuevo reconocedor
            try {
                Log.d(TAG, "Creando SpeechRecognizer...")
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

                if (speechRecognizer == null) {
                    Log.e(TAG, "❌ SpeechRecognizer es NULL")
                    onError?.invoke("Error creando reconocedor")
                    return@post
                }

                setupRecognitionListener()
                Log.d(TAG, "✅ Speech recognizer INICIALIZADO")
                Log.d(TAG, "==== INITIALIZE END ====")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception en initialize: ${e.message}", e)
                e.printStackTrace()
                onError?.invoke("Error inicializando: ${e.message}")
            }
        }
    }

    /**
     * Configura el listener de reconocimiento con LOGGING EXTENSIVO
     */
    private fun setupRecognitionListener() {
        Log.d(TAG, "Configurando RecognitionListener...")

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🎤 READY FOR SPEECH")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                isListening = true
                restartAttempts = 0
                mainHandler.post {
                    onListeningStarted?.invoke()
                }
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🗣️ BEGINNING OF SPEECH - ¡Usuario HABLÓ!")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Log de nivel de audio para debugging
                if (rmsdB > 0) {
                    Log.v(TAG, "🔊 Audio: ${String.format("%.1f", rmsdB)} dB")
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.v(TAG, "📦 Buffer: ${buffer?.size ?: 0} bytes")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "🔇 END OF SPEECH - Usuario dejó de hablar")
            }

            override fun onError(error: Int) {
                val errorMsg = getErrorMessage(error)
                Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.w(TAG, "⚠️ ERROR: $errorMsg")
                Log.w(TAG, "   Código: $error")
                Log.w(TAG, "   shouldContinue: $shouldContinueListening")
                Log.w(TAG, "   restartAttempts: $restartAttempts/$MAX_RESTART_ATTEMPTS")
                Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

                isListening = false

                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        Log.e(TAG, "❌ ERROR CRÍTICO: Sin permisos")
                        shouldContinueListening = false
                        mainHandler.post {
                            onError?.invoke("Sin permisos de micrófono")
                        }
                    }
                    SpeechRecognizer.ERROR_AUDIO -> {
                        Log.e(TAG, "❌ ERROR DE AUDIO: Micrófono no disponible o ya en uso")
                        shouldContinueListening = false
                        mainHandler.post {
                            onError?.invoke("Error de audio. ¿El micrófono está disponible?")
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        Log.w(TAG, "⏳ Reconocedor ocupado, reintentando inmediatamente...")
                        mainHandler.postDelayed({
                            if (shouldContinueListening) {
                                startListening(continuous = true)
                            }
                        }, 100)
                    }
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        Log.d(TAG, "⏱️ Timeout/No match - Normal en espera de voz")
                        if (shouldContinueListening && restartAttempts < MAX_RESTART_ATTEMPTS) {
                            restartAttempts++
                            Log.d(TAG, "🔄 Reintentando (${restartAttempts}/$MAX_RESTART_ATTEMPTS)...")
                            mainHandler.postDelayed({
                                if (shouldContinueListening) {
                                    startListening(continuous = true)
                                }
                            }, 300)
                        } else if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
                            Log.e(TAG, "❌ Demasiados timeouts. Posible problema de micrófono.")
                            shouldContinueListening = false
                            mainHandler.post {
                                onError?.invoke("No se detecta voz. Verifica el micrófono.")
                            }
                        }
                    }
                    else -> {
                        Log.e(TAG, "❌ Error no recuperable: $errorMsg")
                        mainHandler.post {
                            onError?.invoke(errorMsg)
                        }
                        if (shouldContinueListening && restartAttempts < MAX_RESTART_ATTEMPTS) {
                            restartAttempts++
                            mainHandler.postDelayed({
                                if (shouldContinueListening) {
                                    startListening(continuous = true)
                                }
                            }, 1000)
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ RESULTS RECIBIDOS")

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "📝 TEXTO RECONOCIDO: '${matches[0]}'")
                    if (confidences != null && confidences.isNotEmpty()) {
                        Log.d(TAG, "   Confianza: ${String.format("%.1f", confidences[0] * 100)}%")
                    }

                    // Log todas las alternativas
                    matches.forEachIndexed { index, match ->
                        if (index > 0) {
                            val conf = confidences?.getOrNull(index) ?: 0f
                            Log.d(TAG, "   Alt $index: '$match' (${String.format("%.1f", conf * 100)}%)")
                        }
                    }

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    isListening = false
                    shouldContinueListening = false  // Detener hasta que TTS termine
                    restartAttempts = 0

                    mainHandler.post {
                        onTextRecognized?.invoke(matches[0])
                        onListeningEnded?.invoke()
                    }
                } else {
                    Log.w(TAG, "⚠️ Results vacío - Sin reconocimiento")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    isListening = false

                    // Reintentar si está activo
                    if (shouldContinueListening && restartAttempts < MAX_RESTART_ATTEMPTS) {
                        restartAttempts++
                        mainHandler.postDelayed({
                            if (shouldContinueListening) {
                                startListening(continuous = true)
                            }
                        }, 500)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "📝 Parcial: '${matches[0]}'")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.v(TAG, "📢 Event: $eventType")
            }
        })

        Log.d(TAG, "✓ RecognitionListener configurado")
    }

    /**
     * Inicia la escucha de voz
     */
    fun startListening(continuous: Boolean = true) {
        mainHandler.post {
            Log.d(TAG, "══════════════════════════════")
            Log.d(TAG, "▶️ START LISTENING")
            Log.d(TAG, "   continuous: $continuous")
            Log.d(TAG, "   isListening: $isListening")
            Log.d(TAG, "   speechRecognizer: ${if (speechRecognizer != null) "OK" else "NULL"}")

            // Verificar permiso
            if (!hasRecordAudioPermission()) {
                Log.e(TAG, "❌ No hay permiso de micrófono")
                onError?.invoke("Sin permiso de micrófono")
                return@post
            }

            // Inicializar si es necesario
            if (speechRecognizer == null) {
                Log.w(TAG, "⚠️ Reconocedor NULL, inicializando...")
                initialize()
                // Reintentar después de inicializar
                mainHandler.postDelayed({
                    startListening(continuous)
                }, 500)
                return@post
            }

            // Si ya está escuchando, ignorar
            if (isListening) {
                Log.w(TAG, "⚠️ Ya está escuchando, ignorando")
                Log.d(TAG, "══════════════════════════════")
                return@post
            }

            shouldContinueListening = continuous
            restartAttempts = 0

            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                    // Configuración de tiempos
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)

                    // Reconocimiento online
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                }

                Log.d(TAG, "🚀 Llamando a startListening()...")
                speechRecognizer?.startListening(intent)
                Log.d(TAG, "✓ startListening() ejecutado")
                Log.d(TAG, "══════════════════════════════")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception en startListening: ${e.message}", e)
                e.printStackTrace()
                isListening = false
                onError?.invoke("Error al iniciar: ${e.message}")
                Log.d(TAG, "══════════════════════════════")
            }
        }
    }

    /**
     * Detiene la escucha
     */
    fun stopListening() {
        Log.d(TAG, "⏸️ Deteniendo reconocimiento de voz")
        shouldContinueListening = false
        speechRecognizer?.stopListening()
        isListening = false
        onListeningEnded?.invoke()
    }

    /**
     * Cancela la escucha inmediatamente
     */
    fun cancel() {
        Log.d(TAG, "🛑 Cancelando reconocimiento de voz")
        shouldContinueListening = false
        speechRecognizer?.cancel()
        isListening = false
    }

    /**
     * Verifica si está escuchando
     */
    fun isListening(): Boolean {
        return isListening
    }

    /**
     * Libera recursos
     */
    fun shutdown() {
        Log.d(TAG, "Liberando recursos de STT")
        shouldContinueListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    /**
     * Convierte código de error a mensaje legible
     */
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
            SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó voz"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de voz"
            else -> "Error desconocido"
        }
    }
}
