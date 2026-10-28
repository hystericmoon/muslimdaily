package id.haho.muslimdaily.feature.duas.components

import id.haho.muslimdaily.core.ui.AppLocale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import id.haho.design.glass.pressScale
import id.haho.design.glass.GlassBottomSheet
import id.haho.design.glass.GlassIconButton
import id.haho.design.glass.DockedBar
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.design.component.Badge
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.data.settings.TranslationMode
import java.util.Locale

/** Reading preferences that every dua view honours. */
data class ReadingPrefs(
    val translation: TranslationMode = TranslationMode.INDONESIAN,
    val transliteration: Boolean = false,
)

/** "Al-Bukhari 7/162", "Hisnul Muslim no. 100": book and number as the source prints them. */
fun sourceLabel(ref: SourceRef): String = listOf(ref.book, ref.no).filter(String::isNotBlank).joinToString(" ")

/** The chip shows the grading when the source states one, otherwise the first source. */
@Composable
fun sourceChipLabel(dua: Dua): String =
    if (dua.grading != null) gradeLabel(dua) else dua.sources.firstOrNull()?.let(::sourceLabel).orEmpty()

@Composable
fun gradeLabel(dua: Dua): String {
    val g = dua.grading ?: return ""
    val grade = g.grade.replaceFirstChar { it.titlecase(Locale.ROOT) }
    return stringResource(R.string.dua_grade_by, grade, g.grader)
}

/** Translations and transliteration below the Arabic, per the reader's settings. */
@Composable
fun DuaTranslations(dua: Dua, prefs: ReadingPrefs, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val reading = HahoTheme.reading
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        if (prefs.transliteration && dua.transliteration.isNotBlank()) {
            Text(
                dua.transliteration,
                style = reading.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val showEn = prefs.translation != TranslationMode.INDONESIAN && dua.translationEn.isNotBlank()
        // Most Hisnul Muslim entries have no licensed English yet; Indonesian stands in rather
        // than showing the Arabic alone.
        val showId = dua.translationId.isNotBlank() && (prefs.translation != TranslationMode.ENGLISH || !showEn)
        if (showId) Text(dua.translationId, style = reading.bodyLarge, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        if (showEn) {
            // Tagged as English so TalkBack switches voice inside the Indonesian UI.
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(localeList = androidx.compose.ui.text.intl.LocaleList("en")))
                    append(dua.translationEn)
                    pop()
                },
                style = reading.bodyLarge,
                color = if (showId) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Source and grading as a text-labelled chip; opens the full reference sheet. */
@Composable
fun SourceChip(dua: Dua, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val label = sourceChipLabel(dua)
    val description = stringResource(R.string.dua_source_chip_description, label)
    AssistChip(
        onClick = { open = true },
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(
                painterResource(if (dua.grading != null) R.drawable.ic_verified else R.drawable.ic_menu_book),
                null,
                Modifier.size(HahoTheme.dimens.smallIcon),
            )
        },
        modifier = modifier.semantics { contentDescription = description },
    )
    if (open) SourceSheet(dua) { open = false }
}

@Composable
fun SourceSheet(dua: Dua, onDismiss: () -> Unit) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        SourceDetails(dua, Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).padding(bottom = HahoTheme.spacing.xxl))
    }
}

/** Sources, takhrij and grading of a dua: the body of [SourceSheet]. */
@Composable
fun SourceDetails(dua: Dua, modifier: Modifier = Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        Text(stringResource(R.string.dua_sources_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SheetSection(stringResource(R.string.dua_sources)) {
            dua.sources.forEach { Text(sourceLabel(it), style = MaterialTheme.typography.bodyLarge) }
        }
        if (dua.reference.isNotBlank()) {
            SheetSection(stringResource(R.string.dua_reference)) {
                Text(dua.reference, style = HahoTheme.reading.bodyMedium)
            }
        }
        if (dua.grading != null) {
            SheetSection(stringResource(R.string.dua_grading)) {
                Text(gradeLabel(dua), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (dua.altGradings.isNotEmpty()) {
            SheetSection(stringResource(R.string.dua_alt_gradings)) {
                dua.altGradings.forEach {
                    Text(stringResource(R.string.dua_grade_by, it.grade.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }, it.grader))
                }
            }
        }
        Text(
            stringResource(R.string.dua_grading_policy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Badge(
        text = pluralStringResource(R.plurals.dua_count, count, count),
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        icon = R.drawable.ic_repeat,
        modifier = modifier,
    )
}

/** A dua in a list. Tapping opens the detail. */
@Composable
fun DuaCard(
    dua: Dua,
    prefs: ReadingPrefs,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val press = remember { MutableInteractionSource() }
    GlassCard(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth().pressScale(press, pressed = 0.985f),
    ) {
        Column(
            Modifier
                .clickable(
                    interactionSource = press,
                    indication = ripple(),
                    onClickLabel = stringResource(R.string.dua_open),
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(HahoTheme.dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
        ) {
            header?.invoke()
            // A preview: long texts (e.g. Ayat al-Kursi) are clamped; the detail shows them in full.
            ArabicText(dua.arabic, style = AdwamTheme.arabic.body(dua.isQuran), maxLines = CARD_ARABIC_LINES)
            DuaTranslations(dua, prefs, maxLines = CARD_TRANSLATION_LINES)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
            ) {
                dua.count?.let { CountBadge(it, Modifier.align(Alignment.CenterVertically)) }
                SourceChip(dua)
            }
        }
    }
}

/** Play/pause, speed and repeat for a dua's recitation. Controls exceed the 48dp minimum. */
@Composable
fun PlayerBar(
    isPlaying: Boolean,
    speed: Float,
    repeat: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var speedMenu by remember { mutableStateOf(false) }
    // Docked on the bottom navigation, full width, like a media mini-player.
    DockedBar(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val repeatState = stringResource(if (repeat) R.string.state_enabled else R.string.state_disabled)
            IconToggleButton(
                checked = repeat,
                onCheckedChange = onRepeat,
                enabled = enabled,
                modifier = Modifier
                    .size(HahoTheme.dimens.largeTouchTarget)
                    .semantics { stateDescription = repeatState },
            ) {
                Icon(
                    painterResource(R.drawable.ic_repeat),
                    contentDescription = stringResource(R.string.player_repeat),
                    tint = if (repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlassIconButton(
                onClick = onToggle,
                enabled = enabled,
                modifier = Modifier.size(AdwamDimens.playerMainButton),
            ) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    contentDescription = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                    modifier = Modifier.size(HahoTheme.dimens.icon * 1.5f),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val speedDescription = stringResource(R.string.player_speed, speedLabel(speed))
                TextButton(
                    onClick = { speedMenu = true },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = speedDescription }.size(width = HahoTheme.dimens.largeTouchTarget * 1.25f, height = HahoTheme.dimens.largeTouchTarget),
                ) {
                    Text(speedLabel(speed), style = MaterialTheme.typography.labelLarge)
                }
                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                    SPEEDS.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(speedLabel(s)) },
                            onClick = {
                                onSpeed(s)
                                speedMenu = false
                            },
                            leadingIcon = if (s == speed) { { Icon(painterResource(R.drawable.ic_check), null) } } else null,
                        )
                    }
                }
            }
        }
    }
}

private val SPEEDS = listOf(0.5f, 0.75f, 0.85f, 1f, 1.25f)

fun speedLabel(speed: Float): String = String.format(AppLocale, "%.2f×", speed).replace(Regex("[.,]00×"), "×").replace(Regex("([.,]\\d)0×"), "$1×")

private const val CARD_ARABIC_LINES = 4
private const val CARD_TRANSLATION_LINES = 3
