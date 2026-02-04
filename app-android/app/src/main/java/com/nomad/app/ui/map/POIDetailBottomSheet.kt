package com.nomad.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nomad.app.data.dto.AskResponse
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * BottomSheet que muestra detalles de un POI cuando se hace click en un marker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POIDetailBottomSheet(
    poiName: String,
    askResponse: AskResponse?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAskAssistant: ((String) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con título y botón cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = poiName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (askResponse != null) {
                // Mostrar información del POI
                if (askResponse.noData) {
                    Text(
                        text = "No hay información disponible para este lugar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Traducir markdown a español
                    val markdownEs = translateMarkdownToSpanish(askResponse.markdown)

                    MarkdownText(
                        markdown = markdownEs,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Información adicional de facts
                    if (askResponse.facts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        askResponse.facts.forEach { (key, value) ->
                            when (key) {
                                "official_site" -> {
                                    Text(
                                        text = "Sitio web oficial:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                "opening_hours" -> {
                                    Text(
                                        text = "Horario:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Error al cargar información",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Botón para preguntar al asistente
            if (onAskAssistant != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onAskAssistant("Cuéntame sobre $poiName")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pregunta al asistente",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Traduce términos comunes del inglés al español
 */
private fun translateMarkdownToSpanish(markdown: String): String {
    return markdown
        .replace("Website:", "Sitio web:")
        .replace("Official site", "Sitio oficial")
        .replace("Sources", "Fuentes")
        .replace("Google Maps", "Google Maps")
        .replace("Opening hours:", "Horario:")
        .replace("Address:", "Dirección:")
        .replace("Phone:", "Teléfono:")
}
