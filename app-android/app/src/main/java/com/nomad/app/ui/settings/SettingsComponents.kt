package com.nomad.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nomad.app.data.preferences.UserPreferences

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun RadiusSelector(
    currentRadius: Int,
    onRadiusChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Radio de búsqueda",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Muestra POIs dentro de este radio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserPreferences.RADIUS_OPTIONS.forEach { (radius, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentRadius == radius,
                    onClick = { onRadiusChange(radius) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

@Composable
fun FrequencySelector(
    currentFrequency: Int,
    onFrequencyChange: (Int) -> Unit
) {
    Column {
        Text(
            text = "Frecuencia de avisos proactivos",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Cada cuánto tiempo el asistente te avisa de puntos cercanos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserPreferences.FREQUENCY_OPTIONS.forEach { (frequency, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentFrequency == frequency,
                    onClick = { onFrequencyChange(frequency) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

@Composable
fun TtsSpeedSlider(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Velocidad de narración: ${String.format("%.1fx", currentSpeed)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = currentSpeed,
            onValueChange = onSpeedChange,
            valueRange = 0.5f..2.0f,
            steps = 14 // 0.1 increments
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lento (0.5x)", style = MaterialTheme.typography.bodySmall)
            Text("Rápido (2.0x)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TtsPitchSlider(
    currentPitch: Float,
    onPitchChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Tono de voz: ${String.format("%.1f", currentPitch)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Slider(
            value = currentPitch,
            onValueChange = onPitchChange,
            valueRange = 0.5f..2.0f,
            steps = 14
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Grave (0.5)", style = MaterialTheme.typography.bodySmall)
            Text("Agudo (2.0)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BackendUrlInput(
    currentUrl: String,
    onUrlChange: (String) -> Unit
) {
    var editedUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "URL del servidor (desarrollo)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        if (isEditing) {
            OutlinedTextField(
                value = editedUrl,
                onValueChange = { editedUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL del backend") },
                supportingText = { Text("Ejemplo: http://10.0.2.2:8081/api/") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = {
                    isEditing = false
                    editedUrl = currentUrl
                }) {
                    Text("Cancelar")
                }
                Button(onClick = {
                    onUrlChange(editedUrl)
                    isEditing = false
                }) {
                    Text("Guardar")
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isEditing = true }
            ) {
                Text(
                    text = currentUrl,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
