package id.haho.muslimdaily.feature.duas.components

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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import id.haho.muslimdaily.core.designsystem.component.Badge
import id.haho.muslimdaily.core.designsystem.component.StatusBadge
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.settings.TranslationMode
import java.util.Locale

/** Reading preferences that every dua view honours. */
data class ReadingPrefs(
    val translation: TranslationMode = TranslationMode.INDONESIAN,
    val transliteration: Boolean = false,
)

@Composable
fun gradeLabel(dua: Dua): String {
    val g = dua.grading ?: return stringResource(R.string.dua_ungraded)
    val grade = g.grade.replaceFirstChar { it.titlecase(Locale.ROOT) }
    return stringResource(R.string.dua_grade_by, grade, g.grader)
}

/** Translations and transliteration below the Arabic, per the reader's settings. */
@Composable
fun DuaTranslations(dua: Dua, prefs: ReadingPrefs, modifier: Modifier = Modifier) {
    val reading = AppTheme.reading
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        if (prefs.transliteration && dua.transliteration.isNotBlank()) {
            Text(
                dua.transliteration,
                style = reading.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val showId = prefs.translation != TranslationMode.ENGLISH && dua.translationId.isNotBlank()
        val showEn = prefs.translation != TranslationMode.INDONESIAN && dua.translationEn.isNotBlank()
        if (showId) Text(dua.translationId, style = reading.bodyLarge)
        if (showEn) {
            Text(
                dua.translationEn,
                style = reading.bodyLarge,
                color = if (showId) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Source and grading as a text-labelled chip; opens the full reference sheet. */
@Composable
fun SourceChip(dua: Dua, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val label = gradeLabel(dua)
    val description = stringResource(R.string.dua_source_chip_description, label)
    AssistChip(
        onClick = { open = true },
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_verified), null, Modifier.size(AppTheme.dimens.smallIcon)) },
        modifier = modifier.semantics { contentDescription = description },
    )
    if (open) SourceSheet(dua) { open = false }
}

@Composable
fun SourceSheet(dua: Dua, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = MaterialTheme.shapes.extraLarge) {
        Column(
            Modifier.padding(horizontal = AppTheme.dimens.screenGutter).padding(bottom = AppTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text(stringResource(R.string.dua_sources_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            StatusBadge(dua.status)
            SheetSection(stringResource(R.string.dua_sources)) {
                if (dua.sources.isEmpty()) {
                    Text(stringResource(R.string.dua_not_reviewed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    dua.sources.forEach { Text(listOf(it.book, it.no).filter(String::isNotBlank).joinToString(" no. "), style = MaterialTheme.typography.bodyLarge) }
                }
            }
            SheetSection(stringResource(R.string.dua_grading)) {
                Text(gradeLabel(dua), style = MaterialTheme.typography.bodyLarge)
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
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
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
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .clickable(onClickLabel = stringResource(R.string.dua_open), role = Role.Button, onClick = onClick)
                .padding(AppTheme.dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            header?.invoke()
            ArabicText(dua.arabic, style = AppTheme.arabic.body(dua.isQuran))
            DuaTranslations(dua, prefs)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                dua.count?.let { CountBadge(it, Modifier.align(Alignment.CenterVertically)) }
                SourceChip(dua)
                StatusBadge(dua.status, Modifier.align(Alignment.CenterVertically))
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
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val repeatState = stringResource(if (repeat) R.string.state_on else R.string.state_off)
            IconToggleButton(
                checked = repeat,
                onCheckedChange = onRepeat,
                enabled = enabled,
                modifier = Modifier
                    .size(AppTheme.dimens.largeTouchTarget)
                    .semantics { stateDescription = repeatState },
            ) {
                Icon(
                    painterResource(R.drawable.ic_repeat),
                    contentDescription = stringResource(R.string.player_repeat),
                    tint = if (repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = onToggle,
                enabled = enabled,
                modifier = Modifier.size(AppTheme.dimens.playerMainButton),
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    contentDescription = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                    modifier = Modifier.size(AppTheme.dimens.icon * 1.5f),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(
                    onClick = { speedMenu = true },
                    enabled = enabled,
                    modifier = Modifier.size(width = AppTheme.dimens.largeTouchTarget * 1.25f, height = AppTheme.dimens.largeTouchTarget),
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

fun speedLabel(speed: Float): String = String.format(Locale.forLanguageTag("id"), "%.2f×", speed).replace(",00", "").replace(Regex("(,\\d)0×"), "$1×")
