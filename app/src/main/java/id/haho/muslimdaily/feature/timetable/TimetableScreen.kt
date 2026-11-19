package id.haho.muslimdaily.feature.timetable

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.design.component.EmptyState
import id.haho.design.component.InfoNote
import id.haho.design.glass.Glass
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassCard
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.formatLongDate
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.core.ui.rememberTimeFormatter
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.Timetable
import id.haho.muslimdaily.domain.prayer.TimetableRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TimetableMode { MONTH, RAMADAN }

sealed interface TimetableUiState {
    data object NoLocation : TimetableUiState
    data class Ready(
        val mode: TimetableMode,
        val month: YearMonth,
        val zone: ZoneId,
        val place: String,
        @StringRes val methodRes: Int,
        val rows: List<TimetableRow>,
        val today: LocalDate,
    ) : TimetableUiState
}

private val SLOTS = PrayerTimeSlot.entries

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val calculator: PrayerCalculator,
    private val clock: Clock,
) : ViewModel() {
    private val mode = MutableStateFlow(TimetableMode.MONTH)
    private val month = MutableStateFlow(YearMonth.now(clock))

    val state: StateFlow<TimetableUiState?> = combine(settings.settings, mode, month) { s, m, ym ->
        val location = s.location ?: return@combine TimetableUiState.NoLocation
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(clock.withZone(zone))
        val schedule = { d: LocalDate -> calculator.schedule(d, location.latitude, location.longitude, zone, s.calculation, location.elevation) }
        val rows = when (m) {
            TimetableMode.MONTH -> Timetable.month(ym, s.hijriAdjustment, schedule)
            TimetableMode.RAMADAN -> Timetable.ramadan(today, s.hijriAdjustment, schedule) ?: emptyList()
        }
        TimetableUiState.Ready(m, ym, zone, location.name, s.calculation.method.labelRes(), rows, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMode(m: TimetableMode) { mode.value = m }
    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    suspend fun palette() = settings.current().palette
}

@Composable
fun TimetableRoute(onBack: () -> Unit, onOpenLocation: () -> Unit, viewModel: TimetableViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val failed = stringResource(R.string.timetable_share_failed)
    var menu by remember { mutableStateOf(false) }
    val ready = state as? TimetableUiState.Ready
    AdwamScreen(
        title = stringResource(R.string.timetable_title),
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            if (ready != null && ready.rows.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(painterResource(R.drawable.ic_ios_share), stringResource(R.string.timetable_share))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        for ((format, label) in listOf(ShareFormat.PNG to R.string.timetable_share_image, ShareFormat.PDF to R.string.timetable_share_pdf)) {
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                onClick = {
                                    menu = false
                                    scope.launch {
                                        runCatching {
                                            TimetableShare.share(context, ready.toSheet(context), viewModel.palette(), format, ready.fileName())
                                        }.onFailure { snackbar.showSnackbar(failed) }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    ) {
        when (val s = state) {
            null -> Unit
            TimetableUiState.NoLocation -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = R.drawable.ic_location_off,
                    title = stringResource(R.string.today_no_location_title),
                    body = stringResource(R.string.timetable_no_location),
                ) {
                    GlassButton(onClick = onOpenLocation, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.today_pick_city)) }
                }
            }
            is TimetableUiState.Ready -> TimetableContent(s, viewModel::setMode, viewModel::previousMonth, viewModel::nextMonth)
        }
    }
}

@Composable
fun TimetableContent(
    state: TimetableUiState.Ready,
    onMode: (TimetableMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val gutter = HahoTheme.dimens.screenGutter
    val time = rememberTimeFormatter()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
                val modes = listOf(TimetableMode.MONTH to R.string.timetable_month, TimetableMode.RAMADAN to R.string.timetable_ramadan)
                modes.forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = state.mode == m,
                        onClick = { onMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) { Text(stringResource(label)) }
                }
            }
        }
        item { PeriodHeader(state, onPrevious, onNext, Modifier.padding(horizontal = gutter)) }
        item {
            GlassCard(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
                TimetableTable(state, time, Modifier.padding(vertical = HahoTheme.spacing.sm))
            }
        }
        item {
            Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                InfoNote(stringResource(R.string.timetable_basis, state.place, stringResource(state.methodRes)))
                if (state.mode == TimetableMode.RAMADAN) InfoNote(ramadanNote())
            }
        }
    }
}

@Composable
private fun ramadanNote() = stringResource(
    R.string.timetable_ramadan_note,
    stringResource(R.string.ramadan_suhur), stringResource(R.string.prayer_fajr),
    stringResource(R.string.ramadan_iftar), stringResource(R.string.prayer_maghrib),
)

@Composable
private fun PeriodHeader(state: TimetableUiState.Ready, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val title = periodTitle(state)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.mode == TimetableMode.MONTH) {
            IconButton(onClick = onPrevious) {
                Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.fasting_previous_month), Modifier.scale(scaleX = -1f, scaleY = 1f))
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            if (state.mode == TimetableMode.MONTH) {
                Text(hijriRange(state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        if (state.mode == TimetableMode.MONTH) {
            IconButton(onClick = onNext) { Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.fasting_next_month)) }
        }
    }
}

/** The Hijri months the Gregorian month spans, e.g. "Jumadilawal – Jumadilakhir 1448 H". */
@Composable
private fun hijriRange(state: TimetableUiState.Ready): String {
    val months = stringArrayResource(R.array.hijri_months)
    val first = state.rows.first().hijri
    val last = state.rows.last().hijri
    fun name(m: Int) = months[(m - 1).coerceIn(0, 11)]
    return when {
        first.year != last.year -> stringResource(R.string.timetable_hijri_range_years, name(first.month), first.year, name(last.month), last.year)
        first.month != last.month -> stringResource(R.string.timetable_hijri_range, name(first.month), name(last.month), last.year)
        else -> stringResource(R.string.timetable_hijri_single, name(first.month), first.year)
    }
}

@Composable
private fun periodTitle(state: TimetableUiState.Ready): String = when (state.mode) {
    TimetableMode.MONTH -> monthTitle(state.month)
    TimetableMode.RAMADAN -> state.rows.firstOrNull()?.let { stringResource(R.string.timetable_ramadan_title, it.hijri.year) } ?: ""
}

private fun monthTitle(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", AppLocale)).replaceFirstChar { it.uppercase(AppLocale) }

private val DATE_COLUMN = 80.dp

/** Columns keep a readable width: at large text or 12-hour clocks the table scrolls sideways. */
@Composable
private fun TimetableTable(state: TimetableUiState.Ready, time: TimeFormatter, modifier: Modifier = Modifier) {
    val scale = maxOf(1f, LocalDensity.current.fontScale)
    val minTime: Dp = (if (time.is24Hour) 48.dp else 72.dp) * scale
    val dateWidth = DATE_COLUMN * scale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val timeWidth = maxOf((maxWidth - dateWidth) / SLOTS.size, minTime)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(Modifier.clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.timetable_date),
                    Modifier.width(dateWidth).padding(horizontal = HahoTheme.spacing.sm),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                SLOTS.forEach {
                    Text(
                        stringResource(it.labelRes()),
                        Modifier.width(timeWidth),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            state.rows.forEach { row -> TimetableLine(row, state, time, dateWidth, timeWidth) }
        }
    }
}

@Composable
private fun TimetableLine(row: TimetableRow, state: TimetableUiState.Ready, time: TimeFormatter, dateWidth: Dp, timeWidth: Dp) {
    val res = LocalResources.current
    val months = stringArrayResource(R.array.hijri_months)
    val isToday = row.date == state.today
    val hijri = stringResource(R.string.timetable_hijri_short, row.hijri.day, months[(row.hijri.month - 1).coerceIn(0, 11)])
    val times = SLOTS.map { time.format(row.schedule.time(it), state.zone) }
    val description = buildString {
        append(formatLongDate(row.date)).append(", ").append(hijri)
        if (isToday) append(", ").append(res.getString(R.string.timetable_today))
        append(". ")
        append(SLOTS.indices.joinToString(", ") { res.getString(SLOTS[it].labelRes()) + " " + times[it] })
    }
    val weight = if (isToday) FontWeight.Bold else FontWeight.Normal
    Row(
        Modifier
            .then(if (isToday) Modifier.background(Glass.colors.tinted) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = HahoTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(dateWidth).padding(horizontal = HahoTheme.spacing.sm)) {
            Text(shortDay(row.date), style = MaterialTheme.typography.bodyMedium, fontWeight = weight, maxLines = 1)
            Text(row.hijri.day.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        times.forEach { t ->
            Text(t, Modifier.width(timeWidth), style = MaterialTheme.typography.bodyMedium, fontWeight = weight, textAlign = TextAlign.Center)
        }
    }
}

private val ShortDate get() = DateTimeFormatter.ofPattern("EEE d MMM", AppLocale)

private val ShortDay get() = DateTimeFormatter.ofPattern("EEE d", AppLocale)

/** Weekday and day for the screen's narrow date column; the month is in the heading. */
private fun shortDay(date: LocalDate): String = ShortDay.format(date)

private fun shortDate(date: LocalDate): String = ShortDate.format(date)

/** The plain-text table the share image and PDF are drawn from. */
fun TimetableUiState.Ready.toSheet(context: Context): TimetableSheet {
    val res = context.resources
    val months = res.getStringArray(R.array.hijri_months)
    val title = when (mode) {
        TimetableMode.MONTH -> res.getString(R.string.timetable_title) + " · " + monthTitle(month)
        TimetableMode.RAMADAN -> res.getString(R.string.timetable_ramadan_title, rows.first().hijri.year)
    }
    val formatter = TimeFormatter(android.text.format.DateFormat.is24HourFormat(context))
    return TimetableSheet(
        title = title,
        subtitle = res.getString(R.string.timetable_basis, place, res.getString(methodRes)),
        columns = listOf(res.getString(R.string.timetable_date), res.getString(R.string.timetable_hijri)) + SLOTS.map { res.getString(it.labelRes()) },
        rows = rows.map { row ->
            listOf(
                shortDate(row.date),
                res.getString(R.string.timetable_hijri_short, row.hijri.day, months[(row.hijri.month - 1).coerceIn(0, 11)]),
            ) + SLOTS.map { formatter.format(row.schedule.time(it), zone) }
        },
        note = if (mode == TimetableMode.RAMADAN) {
            res.getString(
                R.string.timetable_ramadan_note,
                res.getString(R.string.ramadan_suhur), res.getString(R.string.prayer_fajr),
                res.getString(R.string.ramadan_iftar), res.getString(R.string.prayer_maghrib),
            )
        } else {
            null
        },
        footer = res.getString(R.string.share_footer),
    )
}

fun TimetableUiState.Ready.fileName(): String = when (mode) {
    TimetableMode.MONTH -> "jadwal-salat-$month"
    TimetableMode.RAMADAN -> "imsakiyah-${rows.first().hijri.year}"
}
