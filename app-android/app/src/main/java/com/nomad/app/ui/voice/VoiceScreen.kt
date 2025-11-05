package com.nomad.app.ui.voice

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nomad.app.permission.rememberAudioPermissionState

/**
 * Pantalla del asistente de voz
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceViewModel = viewModel()
) {
    val audioPermissionState = rememberAudioPermissionState()
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val isPreparing by viewModel.isPreparing.collectAsState()
    val isActive by viewModel.isActive.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    // Actualizar estado del permiso en el ViewModel
    LaunchedEffect(audioPermissionState.isGranted) {
        viewModel.updateMicPermission(audioPermissionState.isGranted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistente de Voz") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de micrófono
            Icon(
                imageVector = if (isActive) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isActive) "Micrófono activo" else "Micrófono inactivo",
                modifier = Modifier.size(120.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Estado
            Text(
                text = when {
                    !hasMicPermission -> "Sin permiso de micrófono"
                    isPreparing -> "Preparando..."
                    isActive -> "Escuchando..."
                    else -> "Toca el botón para hablar"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error si existe
            lastError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de control
            if (hasMicPermission) {
                Button(
                    onClick = {
                        if (isActive) {
                            viewModel.stopVoiceAssistant()
                        } else {
                            viewModel.startVoiceAssistant()
                        }
                    },
                    enabled = !isPreparing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isActive) "Detener" else "Iniciar asistente")
                }
            } else {
                Text(
                    text = "Necesitas conceder permiso de micrófono para usar esta función",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Cómo usar el asistente de voz",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Presiona 'Iniciar asistente' para comenzar\n" +
                                "• Habla claramente cerca del micrófono\n" +
                                "• El asistente te ayudará a encontrar lugares cercanos\n" +
                                "• Presiona 'Detener' cuando termines",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
