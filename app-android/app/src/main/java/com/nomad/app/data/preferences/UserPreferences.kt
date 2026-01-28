package com.nomad.app.data.preferences

import com.nomad.app.BuildConfig

/**
 * Preferencias del usuario para la aplicación NOMAD
 */
data class UserPreferences(
    // Radio de POIs en metros
    val poiRadiusMeters: Int = 2000,

    // Frecuencia de avisos proactivos (en minutos, 0 = desactivado)
    val proactiveAlertFrequencyMinutes: Int = 5,

    // Velocidad de narración TTS (0.5 - 2.0)
    val ttsSpeed: Float = 1.0f,

    // Pitch de voz TTS (0.5 - 2.0)
    val ttsPitch: Float = 1.0f,

    // URL del backend (configurable para testing)
    val backendUrl: String = BuildConfig.DEFAULT_BACKEND_URL,

    // Idioma/locale (futuro)
    val locale: String = "es"
) {
    companion object {
        // Ya no necesitamos la constante, se usa BuildConfig

        // Opciones predefinidas para radio
        val RADIUS_OPTIONS = listOf(
            100 to "100 metros",
            500 to "500 metros",
            1000 to "1 kilómetro",
            2000 to "2 kilómetros",
            5000 to "5 kilómetros"
        )

        // Opciones para frecuencia de avisos
        val FREQUENCY_OPTIONS = listOf(
            0 to "Solo bajo demanda",
            2 to "Cada 2 minutos",
            5 to "Cada 5 minutos",
            10 to "Cada 10 minutos",
            15 to "Cada 15 minutos"
        )
    }
}
