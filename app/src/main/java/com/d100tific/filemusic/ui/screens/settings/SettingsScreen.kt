package com.d100tific.filemusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.BuildConfig
import com.d100tific.filemusic.R
import com.d100tific.filemusic.core.locale.LocaleManager
import com.d100tific.filemusic.data.settings.WaveformStyle
import com.d100tific.filemusic.ui.components.Win2kCheckboxRow
import com.d100tific.filemusic.ui.components.Win2kRadioRow
import com.d100tific.filemusic.ui.components.Win2kTitleBar
import com.d100tific.filemusic.ui.theme.AccentPresets
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kOutline

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onLanguageChanged: () -> Unit,
) {
    val colors = LocalWinColors.current
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LocaleManager.getLanguage(context)) }

    Column(modifier = Modifier.fillMaxSize().background(colors.page).verticalScroll(rememberScrollState())) {
        Win2kTitleBar(title = stringResource(R.string.settings_title), onBack = onBack)

        SectionHeader(stringResource(R.string.settings_appearance))
        Win2kCheckboxRow(
            label = stringResource(R.string.settings_dark_mode),
            checked = state.isDarkMode,
            onCheckedChange = viewModel::setDarkMode,
        )

        Text(
            text = stringResource(R.string.settings_accent_color),
            color = colors.onPage,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPresets.forEach { preset ->
                AccentSwatch(
                    color = preset,
                    selected = preset.toArgb() == state.accentColor.toArgb(),
                    onClick = { viewModel.setAccentColor(preset) },
                )
            }
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
        CustomColorPicker(current = state.accentColor, onColorChange = viewModel::setAccentColor)

        HorizontalDivider(color = colors.onPage, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        SectionHeader(stringResource(R.string.settings_waveform))
        Win2kRadioRow(
            label = stringResource(R.string.settings_waveform_cosine),
            selected = state.waveform == WaveformStyle.COSINE,
            onClick = { viewModel.setWaveformStyle(WaveformStyle.COSINE) },
        )
        Win2kRadioRow(
            label = stringResource(R.string.settings_waveform_oscilloscope),
            selected = state.waveform == WaveformStyle.OSCILLOSCOPE,
            onClick = { viewModel.setWaveformStyle(WaveformStyle.OSCILLOSCOPE) },
        )

        HorizontalDivider(color = colors.onPage, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        SectionHeader(stringResource(R.string.settings_language))
        Win2kRadioRow(
            label = stringResource(R.string.settings_language_system),
            selected = currentLanguage == LocaleManager.LANG_SYSTEM,
            onClick = {
                currentLanguage = LocaleManager.LANG_SYSTEM
                LocaleManager.setLanguage(context, LocaleManager.LANG_SYSTEM)
                onLanguageChanged()
            },
        )
        Win2kRadioRow(
            label = stringResource(R.string.settings_language_en),
            selected = currentLanguage == LocaleManager.LANG_EN,
            onClick = {
                currentLanguage = LocaleManager.LANG_EN
                LocaleManager.setLanguage(context, LocaleManager.LANG_EN)
                onLanguageChanged()
            },
        )
        Win2kRadioRow(
            label = stringResource(R.string.settings_language_es),
            selected = currentLanguage == LocaleManager.LANG_ES,
            onClick = {
                currentLanguage = LocaleManager.LANG_ES
                LocaleManager.setLanguage(context, LocaleManager.LANG_ES)
                onLanguageChanged()
            },
        )

        HorizontalDivider(color = colors.onPage, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        SectionHeader(stringResource(R.string.settings_about))
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            color = colors.disabled,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = LocalWinColors.current
    Text(
        text = text,
        color = colors.accent,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalWinColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color)
            .win2kOutline(colors.onPage, thickness = if (selected) 2 else 1)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun CustomColorPicker(current: Color, onColorChange: (Color) -> Unit) {
    val colors = LocalWinColors.current
    var r by remember(current) { mutableFloatStateOf(current.red) }
    var g by remember(current) { mutableFloatStateOf(current.green) }
    var b by remember(current) { mutableFloatStateOf(current.blue) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp).background(Color(r, g, b)))
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(stringResource(R.string.settings_accent_color), color = colors.disabled)
        }
        ColorSlider(label = "R", value = r, onValueChange = { r = it; onColorChange(Color(r, g, b)) })
        ColorSlider(label = "G", value = g, onValueChange = { g = it; onColorChange(Color(r, g, b)) })
        ColorSlider(label = "B", value = b, onValueChange = { b = it; onColorChange(Color(r, g, b)) })
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    val colors = LocalWinColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.onPage, modifier = Modifier.padding(end = 8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
