package com.nomad.app.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Estado de permisos de audio
 */
sealed class AudioPermissionState {
    object Granted : AudioPermissionState()
    object Denied : AudioPermissionState()
    object NotRequested : AudioPermissionState()
}

/**
 * Composable para manejar permisos de audio (micrófono)
 */
@Composable
fun rememberAudioPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): AudioPermissionStateHolder {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf<AudioPermissionState>(AudioPermissionState.NotRequested)
    }

    val permission = Manifest.permission.RECORD_AUDIO

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionState = if (granted) {
            AudioPermissionState.Granted
        } else {
            AudioPermissionState.Denied
        }
        onPermissionResult(granted)
    }

    // Verificar estado inicial
    remember {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        permissionState = if (hasPermission) {
            AudioPermissionState.Granted
        } else {
            AudioPermissionState.NotRequested
        }
    }

    return remember {
        AudioPermissionStateHolder(
            state = permissionState,
            launcher = launcher,
            permission = permission,
            updateState = { newState -> permissionState = newState }
        )
    }
}

/**
 * Holder para el estado de permisos de audio
 */
data class AudioPermissionStateHolder(
    val state: AudioPermissionState,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
    private val permission: String,
    private val updateState: (AudioPermissionState) -> Unit
) {
    fun requestPermission() {
        launcher.launch(permission)
    }

    val isGranted: Boolean
        get() = state is AudioPermissionState.Granted
}
