package com.nomad.app.webrtc

import android.content.Context
import android.util.Log

/**
 * Clase de prueba para verificar WebRTC
 * Se puede llamar desde MainActivity o cualquier Activity
 */
object WebRTCTest {
    private const val TAG = "WebRTCTest"

    /**
     * Ejecuta pruebas básicas de WebRTC
     */
    fun runBasicTest(context: Context) {
        Log.d(TAG, "==========================================")
        Log.d(TAG, "INICIANDO TEST DE WEBRTC")
        Log.d(TAG, "==========================================")

        try {
            // Test 1: Crear WebRTCManager
            Log.d(TAG, "Test 1: Creando WebRTCManager...")
            val webRTCManager = WebRTCManager(context)
            Log.d(TAG, "✓ WebRTCManager creado")

            // Test 2: Inicializar PeerConnectionFactory
            Log.d(TAG, "Test 2: Inicializando PeerConnectionFactory...")
            webRTCManager.initialize()
            Log.d(TAG, "✓ PeerConnectionFactory inicializado")

            // Test 3: Crear AudioTrack local (micrófono)
            Log.d(TAG, "Test 3: Creando AudioTrack local (micrófono)...")
            val audioTrack = webRTCManager.createLocalAudioTrack()
            if (audioTrack != null) {
                Log.d(TAG, "✓ AudioTrack local creado: ${audioTrack.id()}")
                Log.d(TAG, "✓ Micrófono habilitado: ${webRTCManager.isMicrophoneEnabled()}")
            } else {
                Log.e(TAG, "✗ AudioTrack es null")
            }

            // Test 4: Crear PeerConnection
            Log.d(TAG, "Test 4: Creando PeerConnection...")
            val peerConnection = webRTCManager.createPeerConnection()
            if (peerConnection != null) {
                Log.d(TAG, "✓ PeerConnection creado exitosamente")
                Log.d(TAG, "✓ Estado de señalización: ${peerConnection.signalingState()}")
                Log.d(TAG, "✓ Estado de conexión ICE: ${peerConnection.iceConnectionState()}")
            } else {
                Log.e(TAG, "✗ PeerConnection es null")
            }

            Log.d(TAG, "==========================================")
            Log.d(TAG, "TEST COMPLETADO - TODO OK ✓")
            Log.d(TAG, "==========================================")

            // Limpiar recursos después de 5 segundos
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Liberando recursos WebRTC...")
                webRTCManager.release()
                Log.d(TAG, "✓ Recursos liberados")
            }, 5000)

        } catch (e: Exception) {
            Log.e(TAG, "==========================================")
            Log.e(TAG, "ERROR EN TEST: ${e.message}", e)
            Log.e(TAG, "==========================================")
        }
    }
}
