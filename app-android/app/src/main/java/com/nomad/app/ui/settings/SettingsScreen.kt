package com.nomad.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, "Restablecer")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección: Mapa y POIs
            SettingsSection(
                title = "Mapa y Puntos de Interés",
                icon = Icons.Default.Place
            ) {
                RadiusSelector(
                    currentRadius = preferences.poiRadiusMeters,
                    onRadiusChange = viewModel::updatePoiRadius
                )
            }

            // Sección: Asistente de Voz
            SettingsSection(
                title = "Asistente de Voz",
                icon = Icons.Default.Person
            ) {
                FrequencySelector(
                    currentFrequency = preferences.proactiveAlertFrequencyMinutes,
                    onFrequencyChange = viewModel::updateAlertFrequency
                )

                Spacer(modifier = Modifier.height(16.dp))

                TtsSpeedSlider(
                    currentSpeed = preferences.ttsSpeed,
                    onSpeedChange = viewModel::updateTtsSpeed
                )

                Spacer(modifier = Modifier.height(16.dp))

                TtsPitchSlider(
                    currentPitch = preferences.ttsPitch,
                    onPitchChange = viewModel::updateTtsPitch
                )
            }

            // Sección: Avanzado (solo para desarrollo)
            SettingsSection(
                title = "Avanzado",
                icon = Icons.Default.Settings
            ) {
                BackendUrlInput(
                    currentUrl = preferences.backendUrl,
                    onUrlChange = viewModel::updateBackendUrl
                )
            }
        }
    }

    // Diálogo de confirmación para restablecer
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restablecer ajustes") },
            text = { Text("¿Quieres restablecer todos los ajustes a sus valores predeterminados?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Restablecer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
