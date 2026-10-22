package id.haho.muslimdaily.feature.duas.library

import id.haho.muslimdaily.core.designsystem.glass.enterReveal
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.SectionHeader
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.content.ContentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DuaLibraryViewModel @Inject constructor(content: ContentRepository) : ViewModel() {
    val categories: StateFlow<List<DuaCategory>?> = content.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun DuaLibraryRoute(
    onOpenEvent: (String) -> Unit,
    onSearch: () -> Unit,
    viewModel: DuaLibraryViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    AppScreen(
        title = stringResource(R.string.duas_title),
        actions = {
            IconButton(onClick = onSearch) {
                Icon(painterResource(R.drawable.ic_search), stringResource(R.string.action_search))
            }
        },
    ) {
        categories?.let { DuaLibraryContent(it, onOpenEvent) }
    }
}

@Composable
fun DuaLibraryContent(categories: List<DuaCategory>, onOpenEvent: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl)) {
        categories.forEachIndexed { index, category ->
            item(key = category.id) {
                SectionHeader(category.label)
                GlassCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter).enterReveal(index),
                ) {
                    Column {
                        val timeline = category.id == DAILY
                        category.events.forEachIndexed { i, event ->
                            if (i > 0 && !timeline) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            EventRow(
                                event = event,
                                timeline = timeline,
                                isFirst = i == 0,
                                isLast = i == category.events.lastIndex,
                                onClick = { onOpenEvent(event.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * An event row. In the daily category the rows sit on a vertical rail, reading as the day from
 * waking to sleeping.
 */
@Composable
private fun EventRow(event: DuaEvent, timeline: Boolean, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = AppTheme.dimens.largeTouchTarget)
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(start = AppTheme.dimens.screenGutter, end = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (timeline) {
            val line = MaterialTheme.colorScheme.outlineVariant
            Column(
                Modifier.fillMaxHeight().width(AppTheme.dimens.timelineDot),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.weight(1f).width(AppTheme.dimens.divider * 2).background(if (isFirst) line.copy(alpha = 0f) else line))
                Box(Modifier.size(AppTheme.dimens.timelineDot).background(MaterialTheme.colorScheme.primary, CircleShape))
                Box(Modifier.weight(1f).width(AppTheme.dimens.divider * 2).background(if (isLast) line.copy(alpha = 0f) else line))
            }
        }
        Column(
            Modifier.weight(1f).padding(start = if (timeline) AppTheme.spacing.lg else 0.dp, top = AppTheme.spacing.md, bottom = AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            Text(event.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                pluralStringResource(R.plurals.duas_in_event, event.duaCount, event.duaCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(AppTheme.spacing.md),
        )
    }
}

private const val DAILY = "daily"
