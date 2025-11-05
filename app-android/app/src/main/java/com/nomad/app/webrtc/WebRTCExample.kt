package com.nomad.app.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

/**
 * Ejemplo de uso de WebRTCManager
 *
 * Este archivo muestra cómo inicializar y usar WebRTC para comunicación de voz
 * en tiempo real. NO es necesario para el funcionamiento de la app, solo como referencia.
 */
class WebRTCExample(private val context: Context) {

    private var webRTCManager: WebRTCManager? = null

    /**
     * Ejemplo 1: Inicialización básica
     */
    fun initializeWebRTC() {
        webRTCManager = WebRTCManager(context)

        // Inicializar PeerConnectionFactory
        webRTCManager?.initialize()

        // Crear AudioTrack local (micrófono)
        val localAudioTrack = webRTCManager?.createLocalAudioTrack()
        Log.d("WebRTCExample", "AudioTrack local creado: $localAudioTrack")
    }

    /**
     * Ejemplo 2: Crear conexión peer-to-peer
     */
    fun createConnection() {
        // Configurar listeners para eventos
        webRTCManager?.onIceCandidate = { candidate ->
            Log.d("WebRTCExample", "Nuevo ICE candidate: ${candidate.sdp}")
            // TODO: Enviar candidate al otro peer a través del servidor de señalización
            sendIceCandidateToRemotePeer(candidate)
        }

        webRTCManager?.onRemoteAudioTrackAdded = { remoteAudioTrack ->
            Log.d("WebRTCExample", "Audio remoto recibido (altavoz)")
            // El audio remoto se reproduce automáticamente en el altavoz
        }

        webRTCManager?.onConnectionStateChange = { state ->
            Log.d("WebRTCExample", "Estado de conexión: $state")
            when (state) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    Log.d("WebRTCExample", "¡Conexión establecida!")
                }
                PeerConnection.PeerConnectionState.FAILED -> {
                    Log.e("WebRTCExample", "Conexión fallida")
                }
                PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    Log.d("WebRTCExample", "Conexión desconectada")
                }
                else -> {}
            }
        }

        // Crear PeerConnection con servidores STUN por defecto
        val peerConnection = webRTCManager?.createPeerConnection()
        Log.d("WebRTCExample", "PeerConnection creado: $peerConnection")
    }

    /**
     * Ejemplo 3: Crear y enviar oferta SDP (iniciador de la llamada)
     */
    fun createOffer() {
        webRTCManager?.createOffer { sdp ->
            Log.d("WebRTCExample", "Oferta SDP creada: ${sdp.type}")
            // TODO: Enviar oferta al otro peer a través del servidor de señalización
            sendSdpToRemotePeer(sdp)
        }
    }

    /**
     * Ejemplo 4: Responder a una oferta SDP (receptor de la llamada)
     */
    fun handleOfferAndCreateAnswer(remoteSdp: SessionDescription) {
        // Establecer la descripción remota recibida
        webRTCManager?.setRemoteDescription(remoteSdp) {
            Log.d("WebRTCExample", "Remote description establecida")

            // Crear respuesta
            webRTCManager?.createAnswer { answerSdp ->
                Log.d("WebRTCExample", "Respuesta SDP creada: ${answerSdp.type}")
                // TODO: Enviar respuesta al otro peer
                sendSdpToRemotePeer(answerSdp)
            }
        }
    }

    /**
     * Ejemplo 5: Procesar respuesta SDP (iniciador de la llamada)
     */
    fun handleAnswer(remoteSdp: SessionDescription) {
        webRTCManager?.setRemoteDescription(remoteSdp) {
            Log.d("WebRTCExample", "Respuesta remota establecida")
        }
    }

    /**
     * Ejemplo 6: Añadir ICE candidates recibidos del otro peer
     */
    fun addRemoteIceCandidate(candidate: IceCandidate) {
        webRTCManager?.addIceCandidate(candidate)
        Log.d("WebRTCExample", "ICE candidate remoto añadido")
    }

    /**
     * Ejemplo 7: Controlar el micrófono
     */
    fun toggleMicrophone() {
        val isEnabled = webRTCManager?.isMicrophoneEnabled() ?: false
        webRTCManager?.setMicrophoneEnabled(!isEnabled)
        Log.d("WebRTCExample", "Micrófono ${if (!isEnabled) "habilitado" else "deshabilitado"}")
    }

    /**
     * Ejemplo 8: Liberar recursos al terminar la llamada
     */
    fun cleanup() {
        webRTCManager?.release()
        webRTCManager = null
        Log.d("WebRTCExample", "Recursos WebRTC liberados")
    }

    // ====================================================================================
    // Funciones stub para enviar/recibir señalización - Debes implementarlas con tu backend
    // ====================================================================================

    /**
     * TODO: Implementar envío de SDP a través de WebSocket/Socket.IO al servidor
     */
    private fun sendSdpToRemotePeer(sdp: SessionDescription) {
        // Ejemplo de estructura a enviar:
        // {
        //   "type": "offer" o "answer",
        //   "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n..."
        // }
        Log.d("WebRTCExample", "TODO: Enviar SDP al servidor de señalización")
    }

    /**
     * TODO: Implementar envío de ICE candidate a través de WebSocket/Socket.IO
     */
    private fun sendIceCandidateToRemotePeer(candidate: IceCandidate) {
        // Ejemplo de estructura a enviar:
        // {
        //   "sdpMid": "audio",
        //   "sdpMLineIndex": 0,
        //   "candidate": "candidate:842163049 1 udp 1677729535 192.168.0.1 51000 typ srflx..."
        // }
        Log.d("WebRTCExample", "TODO: Enviar ICE candidate al servidor de señalización")
    }
}

/**
 * FLUJO COMPLETO DE UNA LLAMADA WEBRTC:
 *
 * PEER A (Iniciador):
 * 1. initializeWebRTC()
 * 2. createConnection()
 * 3. createOffer() -> envía oferta a Peer B
 * 4. Recibe respuesta de Peer B -> handleAnswer(answer)
 * 5. Recibe ICE candidates de Peer B -> addRemoteIceCandidate(candidate)
 *
 * PEER B (Receptor):
 * 1. initializeWebRTC()
 * 2. createConnection()
 * 3. Recibe oferta de Peer A -> handleOfferAndCreateAnswer(offer)
 * 4. Recibe ICE candidates de Peer A -> addRemoteIceCandidate(candidate)
 *
 * AMBOS PEERS:
 * - Los ICE candidates se intercambian continuamente hasta establecer conexión
 * - Una vez conectados (PeerConnectionState.CONNECTED), el audio fluye automáticamente
 * - Usar toggleMicrophone() para mutear/desmutear
 * - Llamar cleanup() al terminar la llamada
 *
 * REQUISITOS ADICIONALES:
 * - Servidor de señalización (WebSocket/Socket.IO) para intercambiar SDP y ICE candidates
 * - Servidores STUN/TURN para NAT traversal (ya incluidos por defecto en WebRTCManager)
 */
