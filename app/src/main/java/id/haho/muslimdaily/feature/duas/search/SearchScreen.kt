package id.haho.muslimdaily.feature.duas.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.EmptyState
import id.haho.design.theme.HahoTheme
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import id.haho.design.component.HahoTextField

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val content: ContentRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Null before the first search; empty list when nothing matched. */
    val results: StateFlow<List<Dua>?> = _query.debounce(200).mapLatest { q ->
        if (q.isBlank()) null else content.search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val prefs: StateFlow<ReadingPrefs> = settings.settings.map { ReadingPrefs(it.translationMode, it.showTransliteration) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingPrefs())

    fun setQuery(q: String) { _query.value = q }
}

@Composable
fun SearchRoute(onBack: () -> Unit, onOpenDua: (String) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.search_title), onBack = onBack) {
        SearchContent(query, results, prefs, viewModel::setQuery, onOpenDua)
    }
}

@Composable
fun SearchContent(
    query: String,
    results: List<Dua>?,
    prefs: ReadingPrefs,
    onQueryChange: (String) -> Unit,
    onOpenDua: (String) -> Unit,
    autoFocus: Boolean = true,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = HahoTheme.dimens.screenGutter, end = HahoTheme.dimens.screenGutter, bottom = HahoTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        item {
            HahoTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear))
                        }
                    }
                },
                supportingText = { Text(stringResource(R.string.search_supporting)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        when {
            results == null -> Unit
            results.isEmpty() -> item {
                EmptyState(
                    icon = R.drawable.ic_search,
                    title = stringResource(R.string.search_empty_title),
                    body = stringResource(R.string.search_empty_body),
                )
            }
            else -> {
                item {
                    Text(
                        pluralStringResource(R.plurals.search_count, results.size, results.size),
                        modifier = Modifier.padding(top = HahoTheme.spacing.xs).semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                items(results, key = { it.id }) { dua -> DuaCard(dua, prefs, onClick = { onOpenDua(dua.id) }, modifier = Modifier.animateItem()) }
            }
        }
    }
}
