package com.nomad.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
 * Estado de permisos de ubicación
 */
sealed class LocationPermissionState {
    object Granted : LocationPermissionState()
    object Denied : LocationPermissionState()
    object ShowRationale : LocationPermissionState()
    object NotRequested : LocationPermissionState()
}

/**
 * Gestiona los permisos de ubicación usando ActivityResultContracts
 */
class LocationPermissionHandler(private val context: Context) {

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Verifica si los permisos de ubicación están concedidos
     */
    fun hasLocationPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Verifica si algún permiso está concedido (al menos COARSE)
     */
    fun hasAnyLocationPermission(): Boolean {
        return requiredPermissions.any { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * Composable para manejar permisos de ubicación
 */
@Composable
fun rememberLocationPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): LocationPermissionStateHolder {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf<LocationPermissionState>(LocationPermissionState.NotRequested)
    }

    val permissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.any { it }
        permissionState = if (granted) {
            LocationPermissionState.Granted
        } else {
            LocationPermissionState.Denied
        }
        onPermissionResult(granted)
    }

    // Verificar estado inicial
    remember {
        val hasPermissions = permissions.any { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        permissionState = if (hasPermissions) {
            LocationPermissionState.Granted
        } else {
            LocationPermissionState.NotRequested
        }
    }

    return remember {
        LocationPermissionStateHolder(
            state = permissionState,
            launcher = launcher,
            permissions = permissions,
            updateState = { newState -> permissionState = newState }
        )
    }
}

/**
 * Holder para el estado de permisos
 */
data class LocationPermissionStateHolder(
    val state: LocationPermissionState,
    private val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    private val permissions: Array<String>,
    private val updateState: (LocationPermissionState) -> Unit
) {
    fun requestPermissions() {
        launcher.launch(permissions)
    }

    val isGranted: Boolean
        get() = state is LocationPermissionState.Granted
}
