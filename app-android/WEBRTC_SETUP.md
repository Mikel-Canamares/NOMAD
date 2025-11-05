# Configuración de WebRTC para Nomad App

## Estado Actual

✅ Permiso RECORD_AUDIO agregado al AndroidManifest
✅ Permiso MODIFY_AUDIO_SETTINGS agregado
✅ Estructura base preparada para WebRTC

## Problema con Dependencias

Las librerías de WebRTC públicas tienen problemas de disponibilidad en los repositorios Maven/JitPack. Se intentaron:

- `org.webrtc:google-webrtc:1.0.32006` ❌ No encontrado
- `io.getstream:stream-webrtc-android:1.1.5` ❌ No encontrado
- `com.github.webrtc-sdk:android:114.5735.08` ❌ Requiere autenticación GitHub

## Soluciones Recomendadas

### Opción 1: Usar AAR Local (Recomendado)

1. Descarga el WebRTC AAR desde:
   - https://github.com/shiguredo/sora-android-sdk/releases
   - O compila desde: https://webrtc.googlesource.com/src/

2. Crea la carpeta `app/libs/` si no existe

3. Copia el archivo `libwebrtc.aar` a `app/libs/`

4. Añade al `build.gradle.kts`:
```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
```

### Opción 2: Usar Repositorio Maven de Stream (Requiere configuración)

Añade al `settings.gradle.kts`:
```kotlin
maven {
    url = uri("https://maven.stream-io.com/releases")
}
```

Luego en `build.gradle.kts`:
```kotlin
implementation("io.getstream:stream-webrtc-android:1.1.5")
```

### Opción 3: Compilar WebRTC Manualmente

Sigue la guía oficial:
https://webrtc.googlesource.com/src/+/main/docs/native-code/android/

## Código de Ejemplo WebRTCManager

Una vez que tengas la dependencia, crea `app/src/main/java/com/nomad/app/webrtc/WebRTCManager.kt`:

```kotlin
package com.nomad.app.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCManager(private val context: Context) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null

    fun initialize() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioEncoderFactory(DefaultAudioEncoderFactory())
            .setAudioDecoderFactory(DefaultAudioDecoderFactory())
            .createPeerConnectionFactory()
    }

    fun createLocalAudioTrack(): AudioTrack {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        audioSource = peerConnectionFactory!!.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory!!.createAudioTrack("local_audio", audioSource)
        localAudioTrack!!.setEnabled(true)

        return localAudioTrack!!
    }

    fun createPeerConnection(
        iceServers: List<PeerConnection.IceServer>,
        observer: PeerConnection.Observer
    ): PeerConnection {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory!!.createPeerConnection(rtcConfig, observer)!!
        peerConnection!!.addTrack(localAudioTrack, listOf("stream_id"))
        return peerConnection!!
    }

    fun release() {
        localAudioTrack?.dispose()
        audioSource?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
    }
}
```

## Uso Básico

```kotlin
// 1. Inicializar
val webRTCManager = WebRTCManager(context)
webRTCManager.initialize()

// 2. Crear AudioTrack local (micrófono)
val localAudio = webRTCManager.createLocalAudioTrack()

// 3. Configurar ICE servers (STUN/TURN)
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
)

// 4. Crear PeerConnection
val peerConnection = webRTCManager.createPeerConnection(iceServers, object : PeerConnection.Observer {
    override fun onIceCandidate(candidate: IceCandidate?) {
        // Enviar candidate al otro peer
    }

    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
        // Recibir audio remoto (altavoz)
        receiver?.track()?.let { track ->
            if (track.kind() == "audio") {
                val remoteAudio = track as AudioTrack
                remoteAudio.setEnabled(true)
            }
        }
    }

    // Implementar otros métodos...
})

// 5. Liberar recursos al terminar
webRTCManager.release()
```

## Permisos Necesarios (✅ Ya configurados)

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Próximos Pasos

1. Decide qué método usar para obtener la librería WebRTC
2. Implementa WebRTCManager.kt con el código de ejemplo
3. Integra WebRTC en VoiceScreen o crea una nueva pantalla
4. Configura servidor de señalización (WebSocket/Socket.IO)
5. Implementa intercambio de SDP y ICE candidates

## Referencias

- [WebRTC Official](https://webrtc.org/)
- [Android WebRTC Guide](https://webrtc.github.io/webrtc-org/native-code/android/)
- [Stream WebRTC SDK](https://getstream.io/video/sdk/android/)
