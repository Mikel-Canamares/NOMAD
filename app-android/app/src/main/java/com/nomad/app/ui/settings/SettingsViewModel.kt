package com.nomad.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nomad.app.data.preferences.UserPreferences
import com.nomad.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun updatePoiRadius(radiusMeters: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePoiRadius(radiusMeters)
        }
    }

    fun updateAlertFrequency(frequencyMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateAlertFrequency(frequencyMinutes)
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesRepository.updateTtsSpeed(speed)
        }
    }

    fun updateTtsPitch(pitch: Float) {
        viewModelScope.launch {
            preferencesRepository.updateTtsPitch(pitch)
        }
    }

    fun updateBackendUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.updateBackendUrl(url)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }
}
