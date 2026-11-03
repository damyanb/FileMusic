package com.d100tific.filemusic.ui.screens.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d100tific.filemusic.data.settings.SettingsRepository
import com.d100tific.filemusic.data.settings.WaveformStyle
import com.d100tific.filemusic.ui.theme.DefaultAccent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val accentColor: Color = DefaultAccent,
    val waveform: WaveformStyle = WaveformStyle.COSINE,
)

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepo.isDarkMode,
        settingsRepo.accentColorArgb,
        settingsRepo.waveformStyle,
    ) { dark, argb, waveform -> SettingsUiState(dark, Color(argb), waveform) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDarkMode(enabled) }
    }

    fun setAccentColor(color: Color) {
        viewModelScope.launch { settingsRepo.setAccentColor(color.toArgb()) }
    }

    fun setWaveformStyle(style: WaveformStyle) {
        viewModelScope.launch { settingsRepo.setWaveformStyle(style) }
    }
}
