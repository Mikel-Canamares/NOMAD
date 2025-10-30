package com.nomad.app

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nomad.app.location.LocationManager
import com.nomad.app.location.rememberLocationPermissionState
import kotlinx.coroutines.launch

@Composable
fun NomadApp(
    locationManager: LocationManager,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionState = rememberLocationPermissionState(
        onPermissionResult = { granted ->
            if (granted) {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        currentLocation = locationManager.getCurrentLocation()
                        if (currentLocation == null) {
                            errorMessage = "No se pudo obtener ubicación. Intenta de nuevo."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            } else {
                errorMessage = "Permisos de ubicación denegados"
            }
        }
    )

    // Solicitar ubicación automáticamente si ya hay permisos
    LaunchedEffect(permissionState.isGranted) {
        if (permissionState.isGranted && currentLocation == null) {
            isLoading = true
            try {
                currentLocation = locationManager.getCurrentLocation()
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nomad App",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Obteniendo ubicación...")
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                currentLocation != null -> {
                    LocationCard(location = currentLocation!!)
                }

                !permissionState.isGranted -> {
                    Text(
                        text = "Se requieren permisos de ubicación",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!permissionState.isGranted) {
                Button(onClick = { permissionState.requestPermissions() }) {
                    Text("Solicitar Permisos")
                }
            } else {
                Button(onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            currentLocation = locationManager.getCurrentLocation()
                            if (currentLocation == null) {
                                errorMessage = "No hay ubicación disponible"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }) {
                    Text("Actualizar Ubicación")
                }
            }
        }
    }
}

@Composable
fun LocationCard(location: Location) {
    Card(
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ubicación Actual",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Latitud: ${String.format("%.6f", location.latitude)}")
            Text("Longitud: ${String.format("%.6f", location.longitude)}")
            Text("Precisión: ${String.format("%.1f", location.accuracy)}m")
            if (location.hasAltitude()) {
                Text("Altitud: ${String.format("%.1f", location.altitude)}m")
            }
            if (location.hasSpeed()) {
                Text("Velocidad: ${String.format("%.1f", location.speed * 3.6f)} km/h")
            }
        }
    }
}
