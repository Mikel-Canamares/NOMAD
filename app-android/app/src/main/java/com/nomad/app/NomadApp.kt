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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nomad.app.location.LocationManager
import com.nomad.app.location.rememberLocationPermissionState
import com.nomad.app.ui.map.MapScreen
import com.nomad.app.ui.settings.SettingsScreen

@Composable
fun NomadApp(
    locationManager: LocationManager,
    modifier: Modifier = Modifier
) {
    val permissionState = rememberLocationPermissionState()
    val navController = rememberNavController()

    if (permissionState.isGranted) {
        // Sistema de navegación
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = modifier
        ) {
            composable("map") {
                MapScreen(
                    locationManager = locationManager,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
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
