package id.haho.muslimdaily.feature.fasting

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.SectionHeader
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.formatLongDate
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.fasting.FastDay
import id.haho.muslimdaily.domain.fasting.FastKind
import id.haho.muslimdaily.domain.fasting.FastingCalendar
import id.haho.muslimdaily.domain.prayer.HijriDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import javax.inject.Inject

@StringRes
fun FastKind.labelRes(): Int = when (this) {
    FastKind.RAMADAN -> R.string.fast_ramadan
    FastKind.ARAFAH -> R.string.fast_arafah
    FastKind.TASUA -> R.string.fast_tasua
    FastKind.ASHURA -> R.string.fast_ashura
    FastKind.WHITE_DAYS -> R.string.fast_white_days
    FastKind.SHAWWAL -> R.string.fast_shawwal
    FastKind.MONDAY -> R.string.fast_monday
    FastKind.THURSDAY -> R.string.fast_thursday
    FastKind.EID_FITR -> R.string.fast_eid_fitr
    FastKind.EID_ADHA -> R.string.fast_eid_adha
    FastKind.TASHRIQ -> R.string.fast_tashriq
}

/** Where the day's ruling comes from, cited by collection and number. */
@StringRes
fun FastKind.dalilRes(): Int = when (this) {
    FastKind.RAMADAN -> R.string.fast_dalil_ramadan
    FastKind.ARAFAH, FastKind.ASHURA -> R.string.fast_dalil_muslim_1162
    FastKind.TASUA -> R.string.fast_dalil_tasua
    FastKind.WHITE_DAYS -> R.string.fast_dalil_white_days
    FastKind.SHAWWAL -> R.string.fast_dalil_shawwal
    FastKind.MONDAY -> R.string.fast_dalil_monday
    FastKind.THURSDAY -> R.string.fast_dalil_thursday
    FastKind.EID_FITR, FastKind.EID_ADHA -> R.string.fast_dalil_eid
    FastKind.TASHRIQ -> R.string.fast_dalil_tashriq
}

data class FastingUiState(
    val today: FastDay,
    val tomorrow: FastDay,
    val month: List<FastDay>,
    val selected: FastDay,
    val upcoming: List<FastDay>,
)

@HiltViewModel
class FastingViewModel @Inject constructor(settings: SettingsRepository, clock: Clock) : ViewModel() {
    private val anchor = MutableStateFlow(LocalDate.now(clock))
    private val selected = MutableStateFlow(LocalDate.now(clock))

    /** Today, re-read every minute so a screen left open past midnight moves on. */
    private val todays = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(60_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<FastingUiState?> = combine(settings.settings, anchor, selected, todays) { s, a, sel, todayDate ->
        val adj = s.hijriAdjustment
        val month = FastingCalendar.month(a, adj)
        FastingUiState(
            today = FastingCalendar.day(todayDate, adj),
            tomorrow = FastingCalendar.day(todayDate.plusDays(1), adj),
            month = month,
            selected = FastingCalendar.day(sel, adj),
            upcoming = FastingCalendar.upcoming(todayDate, adj, includeWeekly = false, limit = 6),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() {
        val first = state.value?.month?.first()?.date ?: return
        anchor.value = first.minusDays(1)
    }

    fun nextMonth() {
        val last = state.value?.month?.last()?.date ?: return
        anchor.value = last.plusDays(1)
    }

    fun select(date: LocalDate) {
        selected.value = date
    }
}

@Composable
fun FastingRoute(onBack: () -> Unit, viewModel: FastingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScreen(title = stringResource(R.string.fasting_title), onBack = onBack) {
        state?.let { FastingContent(it, viewModel::previousMonth, viewModel::nextMonth, viewModel::select) }
    }
}

@Composable
fun FastingContent(
    state: FastingUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val gutter = AppTheme.dimens.screenGutter
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        item { TodayCard(state.today, state.tomorrow, Modifier.padding(horizontal = gutter)) }
        item { MonthCard(state, onPrevious, onNext, onSelect, Modifier.padding(horizontal = gutter)) }
        item { DayDetails(state.selected, Modifier.padding(horizontal = gutter)) }
        item { SectionHeader(stringResource(R.string.fasting_upcoming)) }
        item {
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
                Column {
                    state.upcoming.forEachIndexed { i, d ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        UpcomingRow(d) { onSelect(d.date) }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                InfoNote(stringResource(R.string.fasting_hijri_note))
                InfoNote(stringResource(R.string.fasting_friday_note))
            }
        }
    }
}

@Composable
private fun hijriText(h: HijriDate): String {
    val months = stringArrayResource(R.array.hijri_months)
    return stringResource(R.string.hijri_date, h.day, months[(h.month - 1).coerceIn(0, 11)], h.year)
}

@Composable
private fun kindsText(day: FastDay): String {
    val res = LocalResources.current
    return if (day.kinds.isEmpty()) res.getString(R.string.fasting_none) else day.kinds.joinToString(", ") { res.getString(it.labelRes()) }
}

@Composable
private fun TodayCard(today: FastDay, tomorrow: FastDay, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth(), color = Glass.colors.tinted, contentColor = Glass.colors.onTinted) {
        Column(Modifier.padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            for ((label, day) in listOf(R.string.fasting_today to today, R.string.fasting_tomorrow to tomorrow)) {
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(stringResource(label) + " · " + hijriText(day.hijri), style = MaterialTheme.typography.labelLarge)
                    Text(
                        kindsText(day),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (day.forbidden) MaterialTheme.colorScheme.error else Glass.colors.onTinted,
                    )
                }
            }
        }
    }
}

private val GoldMark = Color(0xFFC9A227)

@Composable
private fun MonthCard(
    state: FastingUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = state.month.first()
    val months = stringArrayResource(R.array.hijri_months)
    val range = DateTimeFormatter.ofPattern("d MMM", AppLocale)
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppTheme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.fasting_previous_month), Modifier.scale(scaleX = -1f, scaleY = 1f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${months[first.hijri.month - 1]} ${first.hijri.year} H",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "${range.format(first.date)} – ${range.format(state.month.last().date)} ${state.month.last().date.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.fasting_next_month))
                }
            }
            // Week starts on Monday, as Indonesian calendars print it.
            Row(Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm)) {
                DayOfWeek.entries.forEach {
                    Text(
                        it.getDisplayName(TextStyle.SHORT, AppLocale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {},
                    )
                }
            }
            val lead = first.date.dayOfWeek.value - 1
            val cells: List<FastDay?> = List(lead) { null } + state.month
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).aspectRatio(0.86f)) {
                            if (day != null) DayCell(day, day.date == state.today.date, day.date == state.selected.date) { onSelect(day.date) }
                        }
                    }
                    repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                }
            }
            Legend(Modifier.padding(top = AppTheme.spacing.sm))
        }
    }
}

@Composable
private fun DayCell(day: FastDay, isToday: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = formatLongDate(day.date) + ", " + hijriText(day.hijri) + ". " + kindsText(day)
    val special = day.kinds.any { !it.weekly && !it.forbidden && !it.obligatory }
    val weekly = day.kinds.any { it.weekly }
    Column(
        Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                when {
                    selected -> colors.primary.copy(alpha = 0.18f)
                    day.ramadan -> Glass.colors.tinted
                    else -> Color.Transparent
                },
            )
            .then(if (isToday) Modifier.border(1.5.dp, colors.primary, MaterialTheme.shapes.medium) else Modifier)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.forbidden) colors.error else colors.onSurface,
        )
        Text(day.hijri.day.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (day.forbidden) Dot(colors.error)
            if (special) Dot(GoldMark)
            if (weekly) Dot(colors.primary)
        }
    }
}

@Composable
private fun Dot(color: Color) = Box(Modifier.size(5.dp).clip(CircleShape).background(color))

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterHorizontally)) {
        for ((color, label) in listOf(
            GoldMark to R.string.fasting_legend_special,
            MaterialTheme.colorScheme.primary to R.string.fasting_legend_weekly,
            MaterialTheme.colorScheme.error to R.string.fasting_legend_forbidden,
        )) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Dot(color)
                Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayDetails(day: FastDay, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Text(formatLongDate(day.date), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(hijriText(day.hijri), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (day.kinds.isEmpty()) {
                Text(stringResource(R.string.fasting_none_detail), style = MaterialTheme.typography.bodyMedium)
            }
            day.kinds.forEach { kind ->
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(
                        stringResource(kind.labelRes()),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (kind.forbidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(kind.dalilRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(day: FastDay, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            val res = LocalResources.current
            Text(
                day.kinds.filter { !it.weekly }.joinToString(", ") { res.getString(it.labelRes()) },
                style = MaterialTheme.typography.titleSmall,
                color = if (day.forbidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                formatLongDate(day.date) + " · " + hijriText(day.hijri),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
