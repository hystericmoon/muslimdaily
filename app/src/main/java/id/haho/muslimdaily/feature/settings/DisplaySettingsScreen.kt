package id.haho.muslimdaily.feature.settings

import id.haho.design.glass.GlassCard
import id.haho.design.glass.Glass
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
import id.haho.muslimdaily.core.designsystem.theme.ArabicTypography
import id.haho.muslimdaily.core.designsystem.theme.ArabicFont
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.design.component.SegmentedRow
import id.haho.design.component.SettingsGroup
import id.haho.design.component.StepperRow
import id.haho.design.component.SwitchRow
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.design.theme.ThemeMode
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.data.settings.WIDGET_OPACITY_DEFAULT
import id.haho.muslimdaily.data.settings.WIDGET_OPACITY_MIN
import kotlin.math.roundToInt

@Composable
fun DisplaySettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.settings_display), onBack = onBack) {
        settings?.let { DisplaySettingsContent(it, viewModel::update) }
    }
}

private const val SCALE_MIN = 0.8f
private const val SCALE_MAX = 1.8f
private const val SCALE_STEP = 0.1f

private const val OPACITY_STEP = 0.05f

private fun Float.opacityStep(delta: Float) = (((this + delta) * 20).roundToInt() / 20f).coerceIn(WIDGET_OPACITY_MIN, 1f)

private fun Float.step(delta: Float) = (((this + delta) * 10).roundToInt() / 10f).coerceIn(SCALE_MIN, SCALE_MAX)

@Composable
fun DisplaySettingsContent(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        item {
            SettingsGroup(stringResource(R.string.display_language)) {
                LanguageRow()
            }
        }
        item { TranslationGroup(settings, update) }
        item { PhraseLayoutGroup(settings, update) }
        item {
            SettingsGroup(stringResource(R.string.display_arabic_font)) {
                ArabicFontPicker(settings.arabicFont, settings.arabicScale) { f -> update { arabicFont = f } }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.display_text_size)) {
                TextSizeRows(settings, update)
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
                PalettePicker(
                    selected = settings.palette,
                    enabled = !settings.dynamicColor,
                    onSelect = { p -> update { palette = p } },
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
        item {
            SettingsGroup(stringResource(R.string.display_widget)) {
                val title = stringResource(R.string.display_widget_opacity)
                val opacity = settings.widgetOpacity
                StepperRow(
                    title = title,
                    valueText = "${(opacity * 100).roundToInt()}%",
                    onDecrement = { update { widgetOpacity = opacity.opacityStep(-OPACITY_STEP) } },
                    onIncrement = { update { widgetOpacity = opacity.opacityStep(OPACITY_STEP) } },
                    decrementDescription = stringResource(R.string.settings_decrease, title),
                    incrementDescription = stringResource(R.string.settings_increase, title),
                    canDecrement = opacity > WIDGET_OPACITY_MIN,
                    canIncrement = opacity < 1f,
                )
                Text(
                    stringResource(
                        if (opacity < WIDGET_OPACITY_DEFAULT) R.string.display_widget_opacity_low else R.string.display_widget_opacity_note,
                        (WIDGET_OPACITY_DEFAULT * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
                )
            }
        }
    }
}

/** Live sample using the current Arabic and reading scales (applied by the theme). */
/** Arabic and translation size steppers with a live sample; also offered during onboarding. */
@Composable
fun TextSizeRows(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
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
        modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
    )
    SizePreview()
}

@Composable
private fun SizePreview() {
    GlassCard(
        shape = MaterialTheme.shapes.large,
        color = Glass.colors.clear,
        modifier = Modifier.fillMaxWidth().padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
    ) {
        Column(Modifier.padding(HahoTheme.spacing.lg)) {
            ArabicText(stringResource(R.string.display_sample_arabic), style = AdwamTheme.arabic.arabicBody)
            Text(stringResource(R.string.display_sample_translation), style = HahoTheme.reading.bodyLarge)
        }
    }
}

/**
 * Each option is shown in its own typeface, with the same vocalised sample, so the choice is
 * made by reading rather than by name. A radio group: the row is the target.
 */
@Composable
private fun ArabicFontPicker(selected: ArabicFont, scale: Float, onSelect: (ArabicFont) -> Unit) {
    Column(Modifier.selectableGroup()) {
        ArabicFont.entries.forEach { font ->
            val isSelected = font == selected
            val style = remember(font, scale) { ArabicTypography.of(font).scaled(scale).arabicBody }
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(font) })
                    .padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Spacer(Modifier.width(HahoTheme.spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(font.displayName(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(font.descriptionRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ArabicText(stringResource(R.string.display_font_sample), style = style, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun ArabicFont.displayName(): String = when (this) {
    ArabicFont.NASKH -> "Noto Naskh Arabic"
    ArabicFont.SCHEHERAZADE -> "Scheherazade New"
    ArabicFont.AMIRI -> "Amiri"
}

private fun ArabicFont.descriptionRes(): Int = when (this) {
    ArabicFont.NASKH -> R.string.display_font_naskh
    ArabicFont.SCHEHERAZADE -> R.string.display_font_scheherazade
    ArabicFont.AMIRI -> R.string.display_font_amiri
}
