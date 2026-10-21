package id.haho.muslimdaily.feature.settings.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.SectionHeader
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.data.location.City
import id.haho.muslimdaily.data.location.CityRepository
import id.haho.muslimdaily.data.location.LocationRepository
import id.haho.muslimdaily.data.settings.LocationSource
import id.haho.muslimdaily.data.settings.SavedLocation
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val cities: CityRepository,
    private val locations: LocationRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val current: StateFlow<SavedLocation?> = settings.settings.mapLatest { it.location }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val results: StateFlow<List<City>?> = _query.debounce(150).mapLatest { q ->
        if (q.isBlank()) cities.popular() else cities.search(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun setQuery(q: String) { _query.value = q }

    fun select(city: City, done: () -> Unit) {
        viewModelScope.launch {
            locations.saveCity(city)
            done()
        }
    }

    fun useGps(onResult: (LocationRepository.FixResult) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            onResult(locations.updateFromGps())
            _busy.value = false
        }
    }
}

@Composable
fun LocationRoute(onBack: () -> Unit, viewModel: LocationViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current

    val onFix: (LocationRepository.FixResult) -> Unit = { result ->
        when (result) {
            is LocationRepository.FixResult.Success -> onBack()
            LocationRepository.FixResult.NoPermission ->
                scope.launch { snackbar.showSnackbar(resources.getString(R.string.location_permission_denied)) }
            LocationRepository.FixResult.Unavailable ->
                scope.launch { snackbar.showSnackbar(resources.getString(R.string.location_unavailable)) }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.useGps(onFix) else onFix(LocationRepository.FixResult.NoPermission)
    }

    AppScreen(title = stringResource(R.string.settings_location), onBack = onBack, snackbarHostState = snackbar) {
        LocationContent(
            current = current,
            query = query,
            results = results,
            busy = busy,
            onQueryChange = viewModel::setQuery,
            onUseGps = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            onSelect = { viewModel.select(it, onBack) },
        )
    }
}

@Composable
fun LocationContent(
    current: SavedLocation?,
    query: String,
    results: List<City>?,
    busy: Boolean,
    onQueryChange: (String) -> Unit,
    onUseGps: () -> Unit,
    onSelect: (City) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl)) {
        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(AppTheme.dimens.screenGutter),
            ) {
                Column(Modifier.padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    Text(stringResource(R.string.location_current), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (current == null) {
                        Text(stringResource(R.string.today_no_location_title), style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(current.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(
                                if (current.source == LocationSource.GPS) R.string.location_source_gps else R.string.location_source_manual,
                            ) + " · " + current.timeZoneId,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = onUseGps, enabled = !busy, modifier = Modifier.padding(top = AppTheme.spacing.sm)) {
                        Icon(painterResource(R.drawable.ic_my_location), null)
                        Spacer(Modifier.width(AppTheme.spacing.sm))
                        Text(stringResource(if (busy) R.string.location_locating else R.string.today_use_gps))
                    }
                    InfoNote(stringResource(R.string.location_privacy), Modifier.padding(top = AppTheme.spacing.xs), icon = R.drawable.ic_lock)
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.location_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter),
            )
        }
        item {
            SectionHeader(stringResource(if (query.isBlank()) R.string.location_popular else R.string.location_results))
        }
        val list = results.orEmpty()
        if (results != null && list.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.location_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppTheme.dimens.screenGutter),
                )
            }
        }
        items(list, key = { it.id }) { city ->
            ListItem(
                headlineContent = { Text(city.name) },
                supportingContent = { Text(listOf(city.region, city.countryName).filter { it.isNotBlank() }.joinToString(", ")) },
                leadingContent = { Icon(painterResource(R.drawable.ic_location_on), null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.selectable(selected = false, role = Role.Button, onClick = { onSelect(city) }),
            )
        }
        item {
            Text(
                stringResource(R.string.location_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(AppTheme.dimens.screenGutter),
            )
        }
    }
}
