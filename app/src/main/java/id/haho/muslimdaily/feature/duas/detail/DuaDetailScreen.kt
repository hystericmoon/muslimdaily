package id.haho.muslimdaily.feature.duas.detail

import id.haho.design.glass.GlassOutlinedButton
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import id.haho.muslimdaily.data.note.NoteRepository
import id.haho.design.glass.GlassAlertDialog
import id.haho.design.glass.Glass
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import id.haho.muslimdaily.core.model.Dua
import id.haho.design.glass.chevronRotation
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.design.glass.GlassBottomSheet
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.feature.duas.components.gradeLabel
import id.haho.muslimdaily.feature.duas.components.sourceLabel
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.InfoNote
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.ui.reportError
import id.haho.muslimdaily.feature.duas.components.CountBadge
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.feature.duas.components.PlayerBar
import id.haho.muslimdaily.feature.duas.components.SourceChip
import kotlinx.coroutines.launch

data class DuaDetailActions(
    val onTogglePlay: () -> Unit = {},
    val onSpeed: (Float) -> Unit = {},
    val onRepeat: (Boolean) -> Unit = {},
    val onMemorize: () -> Unit = {},
    val onSetInMySet: (Boolean) -> Unit = {},
    val onSaveNote: (String) -> Unit = {},
    val onSetInRoutine: (routineId: String, included: Boolean) -> Unit = { _, _ -> },
    val onNewRoutine: () -> Unit = {},
)

@Composable
fun DuaDetailRoute(
    onBack: () -> Unit,
    onMemorize: (String) -> Unit,
    onNewRoutine: (String) -> Unit,
    viewModel: DuaDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = state
    val title = s?.event?.label.orEmpty()
    AdwamScreen(
        title = title,
        onBack = onBack,
        actions = {
            if (s != null) {
                val source = buildString {
                    append(s.dua.sources.joinToString("; ") { sourceLabel(it) })
                    if (s.dua.grading != null) append(" · ").append(gradeLabel(s.dua))
                }
                IconButton(onClick = { scope.launch { DuaShareCard.share(context, s.dua, title, source, viewModel.palette(), viewModel.arabicFont()) } }) {
                    Icon(painterResource(R.drawable.ic_share), stringResource(R.string.share_dua))
                }
            }
            IconButton(onClick = { scope.launch { reportError(context, viewModel.duaId, viewModel.contentVersion()) } }) {
                Icon(painterResource(R.drawable.ic_flag), stringResource(R.string.settings_report))
            }
        },
        bottomBar = {
            if (s != null) {
                PlayerBar(
                    isPlaying = s.playback.isPlaying,
                    speed = s.playback.speed,
                    repeat = s.playback.repeat,
                    enabled = s.dua.audio != null,
                    onToggle = { viewModel.togglePlay(title) },
                    onSpeed = viewModel::setSpeed,
                    onRepeat = viewModel::setRepeat,
                )
            }
        },
    ) {
        if (s != null) {
            DuaDetailContent(
                state = s,
                actions = DuaDetailActions(
                    onMemorize = { onMemorize(s.dua.id) },
                    onSetInMySet = viewModel::setInMySet,
                    onSaveNote = viewModel::saveNote,
                    onSetInRoutine = viewModel::setInRoutine,
                    onNewRoutine = { onNewRoutine(s.dua.id) },
                ),
            )
        }
    }
}

@Composable
fun DuaDetailContent(state: DuaDetailState, actions: DuaDetailActions, fullEntryOpen: Boolean = false) {
    var routineSheet by remember { mutableStateOf(false) }
    if (routineSheet) RoutineSheet(state, actions) { routineSheet = false }
    val dua = state.dua
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HahoTheme.dimens.screenGutter)
            .padding(bottom = HahoTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
    ) {
        GlassCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(HahoTheme.spacing.xl)) {
                HighlightedArabic(
                    text = dua.arabic,
                    phrases = dua.phrases,
                    activePhrase = state.playback.activePhrase,
                    style = AdwamTheme.arabic.large(dua.isQuran),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
        ) {
            dua.count?.let { CountBadge(it, Modifier.align(Alignment.CenterVertically)) }
            SourceChip(dua)
        }
        DuaTranslations(dua, state.prefs)
        if (dua.arabicFull.isNotBlank()) FullEntry(dua, fullEntryOpen)
        NoteCard(state.note, actions.onSaveNote)
        dua.virtue?.let { virtue ->
            Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
                Text(
                    stringResource(R.string.dua_virtue),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(virtue.text, style = HahoTheme.reading.bodyLarge)
                if (virtue.source.isNotBlank()) {
                    Text(virtue.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (dua.audio == null) InfoNote(stringResource(R.string.dua_no_audio), icon = R.drawable.ic_volume_off)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
        ) {
            GlassOutlinedButton(onClick = actions.onMemorize) {
                Icon(painterResource(R.drawable.ic_record_voice_over), null, Modifier.size(HahoTheme.dimens.smallIcon))
                Spacer(Modifier.width(HahoTheme.spacing.sm))
                Text(stringResource(R.string.dua_memorize))
            }
            GlassOutlinedButton(onClick = { actions.onSetInMySet(!state.inMySet) }) {
                Icon(
                    painterResource(if (state.inMySet) R.drawable.ic_bookmark_added_fill else R.drawable.ic_bookmark_add),
                    null,
                    Modifier.size(HahoTheme.dimens.smallIcon),
                )
                Spacer(Modifier.width(HahoTheme.spacing.sm))
                Text(stringResource(if (state.inMySet) R.string.dua_remove_from_set else R.string.dua_add_to_set))
            }
            GlassOutlinedButton(onClick = { routineSheet = true }) {
                Icon(painterResource(R.drawable.ic_format_list_numbered), null, Modifier.size(HahoTheme.dimens.smallIcon))
                Spacer(Modifier.width(HahoTheme.spacing.sm))
                Text(stringResource(R.string.dua_add_to_routine))
            }
        }
        InfoNote(
            stringResource(if (state.inMySet) R.string.dua_in_set_note else R.string.dua_not_in_set_note),
            Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The reader's private note: what the dua means to them, when they read it, who taught it.
 * Shown as their own words, apart from the sourced text, and never shared.
 */
/**
 * The book's whole entry, for a dua shown as the words to recite only: the hadith wording,
 * notes and repetition counts around it. Folded away by default; nothing from the book is lost.
 */
@Composable
private fun FullEntry(dua: Dua, initiallyOpen: Boolean) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        TextButton(onClick = { open = !open }) {
            Icon(painterResource(R.drawable.ic_keyboard_arrow_down), null, Modifier.chevronRotation(open).size(HahoTheme.dimens.smallIcon))
            Spacer(Modifier.width(HahoTheme.spacing.sm))
            Text(stringResource(if (open) R.string.dua_full_entry_hide else R.string.dua_full_entry_show))
        }
        if (open) {
            GlassCard(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(HahoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                    Text(
                        stringResource(R.string.dua_full_entry_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.dua_full_entry_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ArabicText(dua.arabicFull, style = AdwamTheme.arabic.body(dua.isQuran))
                    if (dua.translationIdFull.isNotBlank()) {
                        Text(dua.translationIdFull, style = HahoTheme.reading.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: String?, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    GlassCard(
        shape = MaterialTheme.shapes.large,
        color = Glass.colors.clear,
        onClick = { editing = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(HahoTheme.spacing.lg), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_edit_note), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(HahoTheme.dimens.icon))
            Spacer(Modifier.width(HahoTheme.spacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xxs)) {
                Text(stringResource(R.string.note_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                if (note.isNullOrBlank()) {
                    Text(stringResource(R.string.note_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(note, style = HahoTheme.reading.bodyLarge)
                }
            }
        }
    }
    if (editing) {
        var text by remember { mutableStateOf(note.orEmpty()) }
        GlassAlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.note_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(NoteRepository.MAX_LENGTH) },
                        placeholder = { Text(stringResource(R.string.note_hint)) },
                        minLines = 3,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.note_private), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { onSave(text); editing = false }) { Text(stringResource(R.string.routine_save)) } },
            dismissButton = {
                if (!note.isNullOrBlank()) {
                    TextButton(onClick = { onSave(""); editing = false }) { Text(stringResource(R.string.note_delete)) }
                } else {
                    TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        )
    }
}

/** The reader's routines with this dua ticked where it is in them, and a new routine that starts with it. */
@Composable
private fun RoutineSheet(state: DuaDetailState, actions: DuaDetailActions, onDismiss: () -> Unit) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = HahoTheme.spacing.xxl)) {
            Text(
                stringResource(R.string.dua_add_to_routine),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm).semantics { heading() },
            )
            state.routines.forEach { routine ->
                val checked = state.dua.id in routine.duaIds
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                        .toggleable(value = checked, role = Role.Checkbox, onValueChange = { actions.onSetInRoutine(routine.id, it) })
                        .padding(horizontal = HahoTheme.dimens.screenGutter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Spacer(Modifier.width(HahoTheme.spacing.md))
                    Text(routine.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                    .clickable(role = Role.Button) { onDismiss(); actions.onNewRoutine() }
                    .padding(horizontal = HahoTheme.dimens.screenGutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_add), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(HahoTheme.spacing.md))
                Text(stringResource(R.string.routine_new), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
