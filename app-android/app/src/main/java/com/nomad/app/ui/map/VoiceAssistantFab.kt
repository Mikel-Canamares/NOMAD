package com.nomad.app.ui.map

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * FAB para el asistente de voz con animación de pulso cuando está activo
 */
@Composable
fun VoiceAssistantFab(
    isActive: Boolean,
    isPreparing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animación de pulso cuando está activo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val scale = if (isActive) pulseScale else 1f

    // Colores según estado
    val containerColor = when {
        isActive -> Color(0xFFFF5252) // Rojo cuando está activo
        isPreparing -> Color(0xFFFFA726) // Naranja cuando está preparando
        else -> Color(0xFF2196F3) // Azul normal
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(64.dp)
            .scale(scale),
        containerColor = containerColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (isActive) 12.dp else 6.dp
        )
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = if (isActive) "Detener asistente" else "Iniciar asistente",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
