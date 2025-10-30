package com.nomad.app.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class VoiceState {
    INACTIVE,
    LISTENING
}

@Composable
fun PushToTalkFab(
    voiceState: VoiceState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (voiceState == VoiceState.LISTENING) 1.2f else 1f,
        label = "fab_scale"
    )

    FloatingActionButton(
        onClick = onToggle,
        modifier = modifier
            .size(64.dp)
            .scale(scale),
        containerColor = when (voiceState) {
            VoiceState.INACTIVE -> MaterialTheme.colorScheme.primary
            VoiceState.LISTENING -> MaterialTheme.colorScheme.error
        }
    ) {
        Icon(
            imageVector = when (voiceState) {
                VoiceState.INACTIVE -> Icons.Default.PlayArrow
                VoiceState.LISTENING -> Icons.Default.Close
            },
            contentDescription = when (voiceState) {
                VoiceState.INACTIVE -> "Iniciar grabación"
                VoiceState.LISTENING -> "Detener grabación"
            },
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
