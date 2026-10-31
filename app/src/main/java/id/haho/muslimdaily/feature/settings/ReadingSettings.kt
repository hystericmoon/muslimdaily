package id.haho.muslimdaily.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import id.haho.design.component.RadioGroup
import id.haho.design.component.SettingsGroup
import id.haho.design.component.SwitchRow
import id.haho.design.glass.GlassBottomSheet
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.PhraseFill
import id.haho.muslimdaily.data.settings.PhraseLayout
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.data.settings.TranslationMode

/** How a dua's phrases are laid out and filled; in Display settings and the dua page's sheet. */
@Composable
fun PhraseLayoutGroup(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    SettingsGroup(stringResource(R.string.display_phrase_layout)) {
        RadioGroup(
            options = PhraseLayout.entries,
            selected = settings.phraseLayout,
            onSelect = { l -> update { phraseLayout = l } },
            label = {
                stringResource(
                    when (it) {
                        PhraseLayout.LINES -> R.string.display_phrase_lines
                        PhraseLayout.INLINE -> R.string.display_phrase_inline
                        PhraseLayout.FOCUS -> R.string.display_phrase_focus
                    },
                )
            },
            description = {
                stringResource(
                    when (it) {
                        PhraseLayout.LINES -> R.string.display_phrase_lines_summary
                        PhraseLayout.INLINE -> R.string.display_phrase_inline_summary
                        PhraseLayout.FOCUS -> R.string.display_phrase_focus_summary
                    },
                )
            },
        )
        // One phrase at a time has nothing to tell apart, so the fill only applies to the others.
        if (settings.phraseLayout != PhraseLayout.FOCUS) {
            // Radio buttons rather than a segmented row: "Selang-seling" doesn't fit a segment at large text.
            Text(
                stringResource(R.string.display_phrase_fill),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).padding(top = HahoTheme.spacing.sm),
            )
            Text(
                stringResource(R.string.display_phrase_fill_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter),
            )
            RadioGroup(
                options = PhraseFill.entries,
                selected = settings.phraseFill,
                onSelect = { f -> update { phraseFill = f } },
                label = {
                    stringResource(
                        when (it) {
                            PhraseFill.NONE -> R.string.display_fill_none
                            PhraseFill.ALTERNATING -> R.string.display_fill_alternating
                            PhraseFill.BUBBLES -> R.string.display_fill_bubbles
                        },
                    )
                },
            )
            if (settings.phraseFill != PhraseFill.NONE) {
                SwitchRow(
                    title = stringResource(R.string.display_fill_hide_playing),
                    summary = stringResource(R.string.display_fill_hide_playing_summary),
                    checked = settings.hideFillWhilePlaying,
                    onCheckedChange = { v -> update { hideFillWhilePlaying = v } },
                )
            }
        }
    }
}

/** Which translation shows, and the transliteration. */
@Composable
fun TranslationGroup(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
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

/**
 * The reading preferences, opened from the "Aa" button on the dua page so a reader can adjust
 * them where they read and see the result behind the sheet. Same settings as Display settings.
 */
@Composable
fun ReadingSheet(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit, onDismiss: () -> Unit) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = HahoTheme.spacing.xxl)) {
            Text(
                stringResource(R.string.dua_reading_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm)
                    .semantics { heading() },
            )
            PhraseLayoutGroup(settings, update)
            TranslationGroup(settings, update)
            SettingsGroup(stringResource(R.string.display_text_size)) {
                TextSizeRows(settings, update)
            }
        }
    }
}
