package com.nomad.app.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

/**
 * Gestiona la configuración y ciclo de vida de WebRTC
 * Configura PeerConnectionFactory, AudioSource y AudioTrack para comunicación de voz
 */
class WebRTCManager(private val context: Context) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private var dataChannel: DataChannel? = null
    private val pendingMessages = mutableListOf<String>()
    private var isDataChannelOpen = false

    // Listeners para eventos
    var onRemoteAudioTrackAdded: ((AudioTrack) -> Unit)? = null
    var onIceCandidate: ((IceCandidate) -> Unit)? = null
    var onConnectionStateChange: ((PeerConnection.PeerConnectionState) -> Unit)? = null
    var onDataChannelMessage: ((String) -> Unit)? = null
    var onDataChannelOpen: (() -> Unit)? = null

    companion object {
        private const val TAG = "WebRTCManager"
        private const val LOCAL_AUDIO_TRACK_ID = "local_audio_track"
        private const val LOCAL_STREAM_ID = "local_stream"
        private const val DATA_CHANNEL_LABEL = "nomad_data"
    }

    /**
     * Inicializa WebRTC y PeerConnectionFactory
     */
    fun initialize() {
        Log.d(TAG, "Inicializando WebRTC")

        // Configurar opciones de inicialización
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        // Crear PeerConnectionFactory (la versión de Threema usa configuración simplificada)
        val factoryOptions = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory creado exitosamente")
    }

    /**
     * Crea AudioSource y AudioTrack local (micrófono)
     * Retorna el AudioTrack que se puede añadir a un PeerConnection
     */
    fun createLocalAudioTrack(): AudioTrack? {
        if (peerConnectionFactory == null) {
            Log.e(TAG, "PeerConnectionFactory no inicializado")
            return null
        }

        // Configurar constraints de audio para mejorar calidad
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }

        // Crear AudioSource desde el micrófono
        audioSource = peerConnectionFactory!!.createAudioSource(audioConstraints)

        // Crear AudioTrack local
        localAudioTrack = peerConnectionFactory!!.createAudioTrack(
            LOCAL_AUDIO_TRACK_ID,
            audioSource
        )

        localAudioTrack?.setEnabled(true)

        Log.d(TAG, "AudioTrack local creado (micrófono activo)")
        return localAudioTrack
    }

    /**
     * Crea PeerConnection con configuración ICE
     * @param iceServers Lista de servidores STUN/TURN para NAT traversal
     */
    fun createPeerConnection(iceServers: List<PeerConnection.IceServer> = getDefaultIceServers()): PeerConnection? {
        if (peerConnectionFactory == null) {
            Log.e(TAG, "PeerConnectionFactory no inicializado")
            return null
        }

        // Configuración de RTC
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        // Observer para eventos de PeerConnection
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "Nuevo ICE Candidate: ${it.sdpMid}")
                    onIceCandidate?.invoke(it)
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "Track recibido: ${receiver?.track()?.kind()}")
                receiver?.track()?.let { track ->
                    if (track.kind() == "audio") {
                        val remoteAudioTrack = track as AudioTrack
                        remoteAudioTrack.setEnabled(true)
                        Log.d(TAG, "AudioTrack remoto activado (altavoz)")
                        onRemoteAudioTrackAdded?.invoke(remoteAudioTrack)
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                newState?.let {
                    Log.d(TAG, "Estado de conexión: $it")
                    onConnectionStateChange?.invoke(it)
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Cambio de señalización: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "Estado ICE Connection: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "ICE Receiving: $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "Estado ICE Gathering: $state")
            }

            override fun onAddStream(stream: MediaStream?) {
                // Método deprecated, usar onAddTrack
            }

            override fun onRemoveStream(stream: MediaStream?) {
                // Método deprecated
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                dataChannel?.let { dc ->
                    Log.d(TAG, "Data Channel recibido: ${dc.label()}")
                    this@WebRTCManager.dataChannel = dc
                    registerDataChannelObserver(dc)
                }
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegociación necesaria")
            }

            override fun onRemoveTrack(receiver: RtpReceiver?) {
                Log.d(TAG, "Track removido")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "ICE candidates removidos: ${candidates?.size ?: 0}")
            }
        }

        // Crear PeerConnection
        peerConnection = peerConnectionFactory!!.createPeerConnection(rtcConfig, observer)

        // Añadir track local si existe
        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf(LOCAL_STREAM_ID))
            Log.d(TAG, "AudioTrack local añadido a PeerConnection")
        }

        // Crear data channel para enviar contexto POI y texto
        createDataChannel()

        return peerConnection
    }

    /**
     * Habilita o deshabilita el micrófono
     */
    fun setMicrophoneEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "Micrófono ${if (enabled) "habilitado" else "deshabilitado"}")
    }

    /**
     * Verifica si el micrófono está habilitado
     */
    fun isMicrophoneEnabled(): Boolean {
        return localAudioTrack?.enabled() ?: false
    }

    /**
     * Crea una oferta SDP para iniciar conexión
     */
    fun createOffer(callback: (SessionDescription) -> Unit) {
        peerConnection?.let { pc ->
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }

            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp?.let {
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d(TAG, "Local description establecida")
                                callback(it)
                            }

                            override fun onSetFailure(error: String?) {
                                Log.e(TAG, "Error al establecer local description: $error")
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, it)
                    }
                }

                override fun onSetSuccess() {}

                override fun onCreateFailure(error: String?) {
                    Log.e(TAG, "Error al crear oferta: $error")
                }

                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }

    /**
     * Crea una respuesta SDP
     */
    fun createAnswer(callback: (SessionDescription) -> Unit) {
        peerConnection?.let { pc ->
            val constraints = MediaConstraints()

            pc.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp?.let {
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d(TAG, "Local description (answer) establecida")
                                callback(it)
                            }

                            override fun onSetFailure(error: String?) {
                                Log.e(TAG, "Error al establecer local description (answer): $error")
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, it)
                    }
                }

                override fun onSetSuccess() {}

                override fun onCreateFailure(error: String?) {
                    Log.e(TAG, "Error al crear respuesta: $error")
                }

                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }

    /**
     * Establece la descripción remota recibida
     */
    fun setRemoteDescription(sdp: SessionDescription, callback: () -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description establecida")
                callback()
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Error al establecer remote description: $error")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    /**
     * Añade un ICE candidate remoto
     */
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
        Log.d(TAG, "ICE Candidate añadido")
    }

    /**
     * Servidores ICE por defecto (Google STUN)
     */
    private fun getDefaultIceServers(): List<PeerConnection.IceServer> {
        return listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
    }

    /**
     * Crea data channel para enviar texto y contexto POI
     */
    private fun createDataChannel() {
        val init = DataChannel.Init().apply {
            ordered = true
            negotiated = false
        }

        dataChannel = peerConnection?.createDataChannel(DATA_CHANNEL_LABEL, init)
        dataChannel?.let { dc ->
            registerDataChannelObserver(dc)
            Log.d(TAG, "Data channel creado: ${dc.label()}")
        }
    }

    /**
     * Registra observer para data channel
     */
    private fun registerDataChannelObserver(dc: DataChannel) {
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}

            override fun onStateChange() {
                val state = dc.state()
                Log.d(TAG, "Data channel state: $state")

                if (state == DataChannel.State.OPEN && !isDataChannelOpen) {
                    isDataChannelOpen = true
                    Log.d(TAG, "✓ Data channel OPEN - enviando ${pendingMessages.size} mensajes pendientes")

                    // Enviar mensajes pendientes
                    pendingMessages.forEach { message ->
                        sendTextMessageNow(message)
                    }
                    pendingMessages.clear()

                    // Notificar que el canal está abierto
                    onDataChannelOpen?.invoke()
                } else if (state != DataChannel.State.OPEN) {
                    isDataChannelOpen = false
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let { buf ->
                    val data = ByteArray(buf.data.remaining())
                    buf.data.get(data)
                    val message = String(data, Charsets.UTF_8)
                    Log.d(TAG, "Mensaje recibido via data channel: $message")
                    onDataChannelMessage?.invoke(message)
                }
            }
        })
    }

    /**
     * Envía mensaje de texto via data channel (encola si no está abierto)
     */
    fun sendTextMessage(message: String) {
        if (isDataChannelOpen) {
            sendTextMessageNow(message)
        } else {
            pendingMessages.add(message)
            Log.d(TAG, "Data channel no abierto todavía - mensaje encolado (${pendingMessages.size} pendientes)")
        }
    }

    /**
     * Envía mensaje inmediatamente (solo cuando el canal está abierto)
     */
    private fun sendTextMessageNow(message: String) {
        dataChannel?.let { dc ->
            if (dc.state() == DataChannel.State.OPEN) {
                val buffer = DataChannel.Buffer(
                    java.nio.ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)),
                    false
                )
                dc.send(buffer)
                Log.d(TAG, "✓ Mensaje enviado via data channel: ${message.take(100)}...")
            } else {
                Log.w(TAG, "Data channel no está abierto. Estado: ${dc.state()}")
            }
        } ?: Log.e(TAG, "Data channel no inicializado")
    }

    /**
     * Envía contexto de POI via data channel (id y coordenadas)
     */
    fun sendPoiContext(poiId: String, lat: Double, lng: Double, name: String) {
        val jsonMessage = """{"type":"poi_context","id":"$poiId","lat":$lat,"lng":$lng,"name":"$name"}"""
        sendTextMessage(jsonMessage)
    }

    /**
     * Envía señal de interrupción (barge-in) cuando el usuario habla
     * Esto permite interrumpir la respuesta del asistente
     */
    fun sendBargeInSignal() {
        val jsonMessage = """{"type":"interrupt"}"""
        sendTextMessage(jsonMessage)
        Log.d(TAG, "Señal de barge-in enviada")
    }

    /**
     * Libera todos los recursos de WebRTC
     */
    fun release() {
        Log.d(TAG, "Liberando recursos WebRTC")

        dataChannel?.close()
        dataChannel?.dispose()
        dataChannel = null
        pendingMessages.clear()
        isDataChannelOpen = false

        localAudioTrack?.dispose()
        localAudioTrack = null

        audioSource?.dispose()
        audioSource = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }
}
