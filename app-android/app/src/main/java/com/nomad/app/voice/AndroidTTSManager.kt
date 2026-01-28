package com.nomad.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * Gestor de Text-to-Speech usando el TTS nativo de Android
 * Mucho más económico que OpenAI Realtime API
 */
class AndroidTTSManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false

    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingCompleted: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "AndroidTTSManager"
        private const val UTTERANCE_ID = "nomad_tts"
    }

    /**
     * Inicializa el motor TTS
     */
    fun initialize(onReady: () -> Unit = {}) {
        Log.d(TAG, "Inicializando TTS de Android")

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Configurar idioma español
                val result = tts?.setLanguage(Locale("es", "ES"))

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Idioma español no soportado, usando español latino")
                    tts?.setLanguage(Locale("es", "MX"))
                }

                // Configurar velocidad y tono
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)

                // Configurar listener de progreso
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "🗣️ TTS comenzó a hablar")
                        isSpeaking = true
                        onSpeakingStarted?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "✓ TTS terminó de hablar")
                        isSpeaking = false
                        onSpeakingCompleted?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "❌ Error en TTS")
                        isSpeaking = false
                        onError?.invoke("Error al sintetizar voz")
                    }
                })

                isInitialized = true
                Log.d(TAG, "✓ TTS inicializado correctamente")
                onReady()
            } else {
                Log.e(TAG, "Error al inicializar TTS")
                onError?.invoke("No se pudo inicializar el motor de voz")
            }
        }
    }

    /**
     * Sintetiza texto a voz
     */
    fun speak(text: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS no inicializado, inicializando ahora...")
            initialize {
                speakNow(text)
            }
            return
        }

        speakNow(text)
    }

    /**
     * Habla inmediatamente (asume que TTS está inicializado)
     */
    private fun speakNow(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Texto vacío, no se sintetiza")
            return
        }

        Log.d(TAG, "📢 Sintetizando: ${text.take(100)}...")

        // Detener cualquier síntesis en curso
        stop()

        // Sintetizar
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error al sintetizar texto")
            onError?.invoke("Error al sintetizar voz")
        }
    }

    /**
     * Detiene la síntesis actual
     */
    fun stop() {
        if (isSpeaking) {
            Log.d(TAG, "🛑 Deteniendo TTS")
            tts?.stop()
            isSpeaking = false
        }
    }

    /**
     * Verifica si está hablando
     */
    fun isSpeaking(): Boolean {
        return isSpeaking
    }

    /**
     * Ajusta la velocidad de habla (0.5 = lento, 1.0 = normal, 2.0 = rápido)
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    /**
     * Ajusta el tono de voz (0.5 = grave, 1.0 = normal, 2.0 = agudo)
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    /**
     * Libera recursos
     */
    fun shutdown() {
        Log.d(TAG, "Liberando recursos de TTS")
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
