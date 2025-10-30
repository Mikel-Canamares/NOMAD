package com.nomad.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nomad.app.location.LocationManager
import com.nomad.app.location.rememberLocationPermissionState
import com.nomad.app.ui.map.MapScreen

@Composable
fun NomadApp(
    locationManager: LocationManager,
    modifier: Modifier = Modifier
) {
    val permissionState = rememberLocationPermissionState()

    if (permissionState.isGranted) {
        // Mostrar mapa cuando los permisos están concedidos
        MapScreen(
            locationManager = locationManager,
            modifier = modifier
        )
    } else {
        // Pantalla de solicitud de permisos
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

                Text(
                    text = "Se requieren permisos de ubicación para usar la aplicación",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { permissionState.requestPermissions() }) {
                    Text("Solicitar Permisos")
                }
            }
        }
    }
}
