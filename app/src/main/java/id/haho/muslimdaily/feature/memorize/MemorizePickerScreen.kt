package id.haho.muslimdaily.feature.memorize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.feature.duas.components.DuaCard
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PickerGroup(val label: String, val duas: List<Dua>)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemorizePickerViewModel @Inject constructor(
    private val content: ContentRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val groups: StateFlow<List<PickerGroup>?> = _query.debounce(200).flatMapLatest { q ->
        if (q.isBlank()) {
            combine(content.allDuas(), content.categories()) { duas, cats ->
                cats.flatMap { it.events }.mapNotNull { e ->
                    duas.filter { it.event == e.id }.takeIf { it.isNotEmpty() }?.let { PickerGroup(e.label, it) }
                }
            }
        } else {
            flow { emit(listOf(PickerGroup("", content.search(q)))) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val prefs: StateFlow<ReadingPrefs> = settings.settings.map { ReadingPrefs(it.translationMode, it.showTransliteration) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingPrefs())

    fun setQuery(q: String) { _query.value = q }
}

@Composable
fun MemorizePickerRoute(onBack: () -> Unit, onPick: (String) -> Unit, viewModel: MemorizePickerViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    AppScreen(title = stringResource(R.string.memorize_pick_title), onBack = onBack) {
        MemorizePickerContent(query, groups, prefs, viewModel::setQuery, onPick)
    }
}

@Composable
fun MemorizePickerContent(
    query: String,
    groups: List<PickerGroup>?,
    prefs: ReadingPrefs,
    onQuery: (String) -> Unit,
    onPick: (String) -> Unit,
) {
    run {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = AppTheme.dimens.screenGutter, end = AppTheme.dimens.screenGutter, bottom = AppTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    label = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            groups.orEmpty().forEach { group ->
                if (group.label.isNotBlank()) {
                    item(key = "h:" + group.label) {
                        Text(
                            group.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = AppTheme.spacing.md).semantics { heading() },
                        )
                    }
                }
                items(group.duas, key = { group.label + it.id }) { dua -> DuaCard(dua, prefs, onClick = { onPick(dua.id) }) }
            }
        }
    }
}
