package com.nomad.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nomad.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val POI_RADIUS = intPreferencesKey("poi_radius_meters")
        val ALERT_FREQUENCY = intPreferencesKey("proactive_alert_frequency_minutes")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val LOCALE = stringPreferencesKey("locale")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                poiRadiusMeters = preferences[PreferencesKeys.POI_RADIUS] ?: 2000,
                proactiveAlertFrequencyMinutes = preferences[PreferencesKeys.ALERT_FREQUENCY] ?: 5,
                ttsSpeed = preferences[PreferencesKeys.TTS_SPEED] ?: 1.0f,
                ttsPitch = preferences[PreferencesKeys.TTS_PITCH] ?: 1.0f,
                backendUrl = preferences[PreferencesKeys.BACKEND_URL]
                    ?: BuildConfig.DEFAULT_BACKEND_URL,
                locale = preferences[PreferencesKeys.LOCALE] ?: "es"
            )
        }

    suspend fun updatePoiRadius(radiusMeters: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POI_RADIUS] = radiusMeters
        }
    }

    suspend fun updateAlertFrequency(frequencyMinutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_FREQUENCY] = frequencyMinutes
        }
    }

    suspend fun updateTtsSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_SPEED] = speed.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun updateTtsPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_PITCH] = pitch.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun updateBackendUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKEND_URL] = url
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
