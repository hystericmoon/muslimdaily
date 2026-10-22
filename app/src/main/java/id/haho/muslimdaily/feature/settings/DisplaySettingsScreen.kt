package id.haho.muslimdaily.feature.settings

import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.glass.Glass
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.muslimdaily.core.designsystem.component.RadioGroup
import id.haho.muslimdaily.core.designsystem.component.SegmentedRow
import id.haho.muslimdaily.core.designsystem.component.SettingsGroup
import id.haho.muslimdaily.core.designsystem.component.StepperRow
import id.haho.muslimdaily.core.designsystem.component.SwitchRow
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.designsystem.theme.ThemeMode
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.data.settings.TranslationMode
import kotlin.math.roundToInt

@Composable
fun DisplaySettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AppScreen(title = stringResource(R.string.settings_display), onBack = onBack) {
        settings?.let { DisplaySettingsContent(it, viewModel::update) }
    }
}

private const val SCALE_MIN = 0.8f
private const val SCALE_MAX = 1.8f
private const val SCALE_STEP = 0.1f

private fun Float.step(delta: Float) = (((this + delta) * 10).roundToInt() / 10f).coerceIn(SCALE_MIN, SCALE_MAX)

@Composable
fun DisplaySettingsContent(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl)) {
        item {
            SettingsGroup(stringResource(R.string.display_translation)) {
                RadioGroup(
                    options = TranslationMode.entries,
                    selected = settings.translationMode,
                    onSelect = { m -> update { translationMode = m } },
                    label = {
                        stringResource(
                            when (it) {
                                TranslationMode.INDONESIAN -> R.string.display_translation_id
                                TranslationMode.ENGLISH -> R.string.display_translation_en
                                TranslationMode.BOTH -> R.string.display_translation_both
                            },
                        )
                    },
                )
                SwitchRow(
                    title = stringResource(R.string.display_transliteration),
                    summary = stringResource(R.string.display_transliteration_summary),
                    checked = settings.showTransliteration,
                    onCheckedChange = { v -> update { showTransliteration = v } },
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.display_text_size)) {
                val arabicTitle = stringResource(R.string.display_arabic_size)
                StepperRow(
                    title = arabicTitle,
                    valueText = "${(settings.arabicScale * 100).roundToInt()}%",
                    onDecrement = { update { arabicScale = settings.arabicScale.step(-SCALE_STEP) } },
                    onIncrement = { update { arabicScale = settings.arabicScale.step(SCALE_STEP) } },
                    decrementDescription = stringResource(R.string.settings_decrease, arabicTitle),
                    incrementDescription = stringResource(R.string.settings_increase, arabicTitle),
                    canDecrement = settings.arabicScale > SCALE_MIN,
                    canIncrement = settings.arabicScale < SCALE_MAX,
                )
                val translationTitle = stringResource(R.string.display_translation_size)
                StepperRow(
                    title = translationTitle,
                    valueText = "${(settings.translationScale * 100).roundToInt()}%",
                    onDecrement = { update { translationScale = settings.translationScale.step(-SCALE_STEP) } },
                    onIncrement = { update { translationScale = settings.translationScale.step(SCALE_STEP) } },
                    decrementDescription = stringResource(R.string.settings_decrease, translationTitle),
                    incrementDescription = stringResource(R.string.settings_increase, translationTitle),
                    canDecrement = settings.translationScale > SCALE_MIN,
                    canIncrement = settings.translationScale < SCALE_MAX,
                )
                Text(
                    stringResource(R.string.display_size_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
                )
                SizePreview()
            }
        }
        item {
            SettingsGroup(stringResource(R.string.display_theme)) {
                SegmentedRow(
                    title = stringResource(R.string.display_theme_mode),
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    onSelect = { m -> update { themeMode = m } },
                    label = {
                        stringResource(
                            when (it) {
                                ThemeMode.SYSTEM -> R.string.display_theme_system
                                ThemeMode.LIGHT -> R.string.display_theme_light
                                ThemeMode.DARK -> R.string.display_theme_dark
                            },
                        )
                    },
                )
                SwitchRow(
                    title = stringResource(R.string.display_dynamic_color),
                    summary = stringResource(R.string.display_dynamic_color_summary),
                    checked = settings.dynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = { v -> update { dynamicColor = v } },
                )
                SwitchRow(
                    title = stringResource(R.string.display_glass),
                    summary = stringResource(R.string.display_glass_summary),
                    checked = settings.glassEffect,
                    onCheckedChange = { v -> update { glassEffect = v } },
                )
            }
        }
    }
}

/** Live sample using the current Arabic and reading scales (applied by the theme). */
@Composable
private fun SizePreview() {
    GlassCard(
        shape = MaterialTheme.shapes.large,
        color = Glass.colors.clear,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
    ) {
        Column(Modifier.padding(AppTheme.spacing.lg)) {
            ArabicText(stringResource(R.string.display_sample_arabic), style = AppTheme.arabic.arabicBody)
            Text(stringResource(R.string.display_sample_translation), style = AppTheme.reading.bodyLarge)
        }
    }
}
