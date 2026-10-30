package id.haho.muslimdaily.feature.today

import id.haho.design.component.checkRowTitleInset
import id.haho.design.component.CheckRowAnchor
import id.haho.design.glass.GlassBottomSheet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.Color
import id.haho.design.glass.Glass
import id.haho.design.glass.chevronRotation
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import id.haho.muslimdaily.R
import id.haho.design.component.Badge
import id.haho.muslimdaily.core.designsystem.component.AdwamCheckRow
import id.haho.design.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.TierBadge
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.model.Rawatib
import id.haho.muslimdaily.core.ui.durationLabel
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.core.ui.rememberTimeFormatter
import id.haho.muslimdaily.data.checklist.ChecklistRepository
import id.haho.muslimdaily.domain.prayer.TimeWindow
import id.haho.muslimdaily.domain.prayer.WindowType
import id.haho.muslimdaily.domain.rawatib.PrayerPlan
import id.haho.muslimdaily.domain.rawatib.RawatibLine

@Composable
internal fun PrayerListCard(
    data: TodayData,
    expanded: Set<String>,
    onExpandToggle: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var evidenceFor by remember { mutableStateOf<RawatibLine?>(null) }
    val time = rememberTimeFormatter()
    GlassCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            data.rows.forEachIndexed { i, row ->
                if (i > 0) ListDivider()
                PrayerRowView(
                    row = row,
                    data = data,
                    expanded = row.slot.name in expanded,
                    onExpandToggle = { onExpandToggle(row.slot.name) },
                    onToggle = onToggle,
                    onShowEvidence = { evidenceFor = it },
                )
                // Dhuha joins the list after Subuh only while its time is running.
                val duha = data.duha
                if (row.slot == PrayerTimeSlot.FAJR && duha != null) {
                    ListDivider()
                    SunnahWindowRow(
                        title = stringResource(R.string.duha_title),
                        range = stringResource(R.string.window_range, time.format(duha.start, data.zone), time.format(duha.end, data.zone)),
                        note = stringResource(R.string.note_duha_best),
                        checked = ChecklistRepository.DUHA in data.checked,
                        onCheckedChange = { onToggle(ChecklistRepository.DUHA, it) },
                    )
                }
            }
        }
    }
    evidenceFor?.let { line -> EvidenceSheet(line) { evidenceFor = null } }
}

@Composable
private fun PrayerRowView(
    row: PrayerRow,
    data: TodayData,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onShowEvidence: (RawatibLine) -> Unit,
) {
    val time = rememberTimeFormatter()
    // Nothing to expand when no reviewed rawatib or fard data has shipped yet.
    val plan = row.plan?.takeIf { it.before.isNotEmpty() || it.after.isNotEmpty() || it.fard != null }
    val name = stringResource(row.plan?.prayer?.labelRes() ?: row.slot.labelRes())
    // When the time ends, the first lines inside the row. Isya's three ends are stacked, unranked.
    val ends = when (row.slot) {
        PrayerTimeSlot.FAJR -> listOf(stringResource(R.string.today_fajr_ends, time.format(data.sunrise, data.zone)))
        PrayerTimeSlot.ISHA -> listOf(
            stringResource(R.string.today_isha_ends_third, time.format(data.ishaEnds.thirdOfNight, data.zone)),
            stringResource(R.string.today_isha_ends_middle, time.format(data.ishaEnds.midnight, data.zone)),
            stringResource(R.string.today_isha_ends_fajr, time.format(data.ishaEnds.fajr, data.zone)),
        )
        else -> null
    }
    // Witr is checked inside Isya, after its ba'diyah.
    val witr = data.witr?.takeIf { row.slot == PrayerTimeSlot.ISHA }
    val expandable = plan != null || ends != null || witr != null
    val expandLabel = stringResource(if (expanded) R.string.today_collapse_rawatib else R.string.today_expand_rawatib)
    val stateLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val rowBackground by animateColorAsState(
        if (row.isCurrent) Glass.colors.tinted else Color.Transparent,
        HahoTheme.motion.fade(),
        label = "rowBackground",
    )
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(Modifier.background(rowBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                .then(
                    if (expandable) {
                        Modifier
                            .clickable(onClickLabel = expandLabel, role = Role.Button, onClick = onExpandToggle)
                            .semantics { stateDescription = stateLabel }
                    } else {
                        Modifier
                    },
                )
                .padding(start = HahoTheme.dimens.screenGutter, end = HahoTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = HahoTheme.spacing.md)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    if (row.isCurrent) {
                        Badge(
                            stringResource(R.string.today_now_badge),
                            container = MaterialTheme.colorScheme.primary,
                            content = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
                val detailColor = if (row.isCurrent) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
                if (row.isNext) {
                    Text(
                        stringResource(R.string.countdown_in, durationLabel(data.next.remaining)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                row.iqamah?.let {
                    Text(
                        stringResource(R.string.today_iqamah, time.format(it, data.zone)),
                        style = MaterialTheme.typography.bodySmall,
                        color = detailColor,
                    )
                }
            }
            Text(time.format(row.time, data.zone), style = MaterialTheme.typography.titleLarge, color = contentColor)
            Box(Modifier.size(HahoTheme.dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                if (expandable) {
                    Icon(
                        painterResource(R.drawable.ic_keyboard_arrow_down),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.chevronRotation(expanded),
                    )
                }
            }
        }
        if (expandable) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(HahoTheme.motion.layout()) + fadeIn(HahoTheme.motion.fade()),
                exit = shrinkVertically(HahoTheme.motion.layout()) + fadeOut(HahoTheme.motion.fade()),
            ) {
                Column {
                    ends?.let { lines ->
                        // One TalkBack stop for all of a prayer's end times.
                        Column(
                            Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).padding(bottom = HahoTheme.spacing.sm)
                                .semantics(mergeDescendants = true) {},
                        ) {
                            lines.forEach {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (row.isCurrent) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Timeline(plan, witr, data, onToggle, onShowEvidence)
                }
            }
        }
    }
}

/** qabliyah → fard → ba'diyah (→ witr after Isya), each with its private check. */
@Composable
private fun Timeline(
    plan: PrayerPlan?,
    witr: TimeWindow?,
    data: TodayData,
    onToggle: (String, Boolean) -> Unit,
    onShowEvidence: (RawatibLine) -> Unit,
) {
    val steps = buildList {
        plan?.before?.forEach { add(TimelineStep.Sunnah(it, isBefore = true)) }
        plan?.fard?.let { add(TimelineStep.Obligatory(plan.prayer.name, it.rakah)) }
        plan?.after?.forEach { add(TimelineStep.Sunnah(it, isBefore = false)) }
        witr?.let { add(TimelineStep.Witr(it)) }
    }
    if (steps.isEmpty()) return
    // Sits directly under the prayer row (no inner card): the rail groups the steps, and the
    // checkboxes line up with every other checklist on the screen.
    Column(Modifier.padding(start = HahoTheme.spacing.sm, bottom = HahoTheme.spacing.sm)) {
        steps.forEachIndexed { i, step ->
            TimelineStepRow(step, data, isFirst = i == 0, isLast = i == steps.lastIndex, onToggle, onShowEvidence)
        }
        if (plan?.showJumuahNote == true) {
            InfoNote(stringResource(R.string.note_jumuah), Modifier.padding(HahoTheme.spacing.md))
        }
    }
}

private sealed interface TimelineStep {
    data class Sunnah(val line: RawatibLine, val isBefore: Boolean) : TimelineStep
    data class Obligatory(val prayer: String, val rakah: Int) : TimelineStep
    data class Witr(val window: TimeWindow) : TimelineStep
}

@Composable
private fun TimelineStepRow(
    step: TimelineStep,
    data: TodayData,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onShowEvidence: (RawatibLine) -> Unit,
) {
    val key = when (step) {
        is TimelineStep.Obligatory -> ChecklistRepository.fardKey(step.prayer)
        is TimelineStep.Sunnah -> ChecklistRepository.rawatibKey(step.line.primary.id)
        is TimelineStep.Witr -> ChecklistRepository.WITR
    }
    val checked = key in data.checked
    val sunnah = step as? TimelineStep.Sunnah
    val notes = sunnah?.line?.primary?.notes
    // "Dalil" only when there is evidence to show; an empty sheet is a dead end.
    val hasEvidence = sunnah?.line?.entries?.any { e -> e.evidence.any { it.book.isNotBlank() } } == true
    val hasExtras = notes != null || hasEvidence
    Column(Modifier.fillMaxWidth().padding(start = HahoTheme.spacing.md)) {
        // Dot, title and checkbox share one line: the step's first line.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Rail(isFirst, isLast && !hasExtras, emphasised = step is TimelineStep.Obligatory)
            AdwamCheckRow(
                checked = checked,
                onCheckedChange = { onToggle(key, it) },
                modifier = Modifier.padding(start = HahoTheme.spacing.md, end = HahoTheme.spacing.xs),
                minHeight = HahoTheme.dimens.largeTouchTarget,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f).padding(
                        top = checkRowTitleInset(MaterialTheme.typography.titleSmall),
                        bottom = HahoTheme.spacing.sm,
                    ),
                ) {
                    when (step) {
                        is TimelineStep.Obligatory -> {
                            Text(stringResource(R.string.rawatib_fard), style = MaterialTheme.typography.titleSmall)
                            Text(
                                pluralStringResource(R.plurals.rakah_count, step.rakah, step.rakah),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        is TimelineStep.Sunnah -> SunnahLabel(step)
                        is TimelineStep.Witr -> {
                            val time = rememberTimeFormatter()
                            Text(stringResource(R.string.witr_title), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.window_range, time.format(step.window.start, data.zone), time.format(step.window.end, data.zone)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        if (sunnah != null && hasExtras) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                RailLine(visible = !isLast)
                // Notes line up with the step title; the text button's own inset does the same for "Dalil".
                Column(Modifier.weight(1f).padding(start = HahoTheme.spacing.xs)) {
                    notes?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = HahoTheme.spacing.md - HahoTheme.spacing.xs, end = HahoTheme.spacing.md),
                        )
                    }
                    if (hasEvidence) {
                        TextButton(onClick = { onShowEvidence(sunnah.line) }) {
                            Icon(painterResource(R.drawable.ic_format_list_numbered), null, Modifier.size(HahoTheme.dimens.smallIcon))
                            Text(stringResource(R.string.rawatib_evidence), Modifier.padding(start = HahoTheme.spacing.xs))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SunnahLabel(step: TimelineStep.Sunnah) {
    val entry = step.line.primary
    val title = when {
        entry.prayer == null -> stringResource(R.string.rawatib_between_adhan)
        step.isBefore -> stringResource(R.string.rawatib_before)
        else -> stringResource(R.string.rawatib_after)
    }
    Text(title, style = MaterialTheme.typography.titleSmall)
    rakahText(step.line)?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
        modifier = Modifier.padding(top = HahoTheme.spacing.xs),
    ) {
        TierBadge(entry.tier)
    }
}

/** "4 rakaat", or "2 rakaat di rumah atau 4 rakaat di masjid" for alternatives. */
@Composable
private fun rakahText(line: RawatibLine): String? {
    val parts = line.entries.mapNotNull { r ->
        r.rakah?.let { n ->
            val count = pluralStringResource(R.plurals.rakah_count, n, n)
            if (line.entries.size > 1 && r.variantLabel != null) stringResource(R.string.rakah_with_label, count, variantLabel(r.variantLabel)) else count
        }
    }
    if (parts.isEmpty()) return null
    return parts.reduce { acc, s -> stringResource(R.string.rakah_or, acc, s) }
}

/** The rail continuing past a step's notes and evidence link, without a dot. */
@Composable
private fun RailLine(visible: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.fillMaxHeight().width(AdwamDimens.timelineDot), contentAlignment = Alignment.Center) {
        if (visible) Box(Modifier.fillMaxHeight().width(HahoTheme.dimens.divider * 2).background(lineColor))
    }
}

@Composable
private fun Rail(isFirst: Boolean, isLast: Boolean, emphasised: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxHeight().width(AdwamDimens.timelineDot), horizontalAlignment = Alignment.CenterHorizontally) {
        // Dot centred on the checkbox anchor, i.e. level with the step's title.
        Box(
            Modifier.height(CheckRowAnchor - AdwamDimens.timelineDot / 2).width(HahoTheme.dimens.divider * 2)
                .background(if (isFirst) lineColor.copy(alpha = 0f) else lineColor),
        )
        Box(Modifier.size(AdwamDimens.timelineDot).background(dotColor, CircleShape))
        Box(Modifier.weight(1f).width(HahoTheme.dimens.divider * 2).background(if (isLast) lineColor.copy(alpha = 0f) else lineColor))
    }
}

@Composable
private fun EvidenceSheet(line: RawatibLine, onDismiss: () -> Unit) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).padding(bottom = HahoTheme.spacing.xxl)) {
            val entry = line.primary
            Text(
                stringResource(
                    when {
                        entry.prayer == null -> R.string.rawatib_between_adhan
                        entry.position == id.haho.muslimdaily.core.model.Position.BEFORE -> R.string.rawatib_before
                        else -> R.string.rawatib_after
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            rakahText(line)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Row(Modifier.padding(vertical = HahoTheme.spacing.sm), horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
                TierBadge(entry.tier)
            }
            val evidence = line.entries.flatMap { it.evidence }.filter { it.book.isNotBlank() }
            Text(stringResource(R.string.rawatib_evidence), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = HahoTheme.spacing.md))
            if (evidence.isEmpty()) {
                Text(stringResource(R.string.rawatib_evidence_missing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                evidence.forEach { Text("${it.book} ${it.no}".trim(), style = MaterialTheme.typography.bodyMedium) }
            }
            line.entries.mapNotNull(Rawatib::notes).distinct().forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = HahoTheme.spacing.sm))
            }
        }
    }
}

@Composable
private fun SunnahWindowRow(
    title: String,
    range: String,
    note: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    AdwamCheckRow(
        checked = checked,
        onCheckedChange = onCheckedChange,
        minHeight = HahoTheme.dimens.largeTouchTarget,
        modifier = Modifier.padding(start = HahoTheme.dimens.screenGutter, end = HahoTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).padding(
                top = checkRowTitleInset(MaterialTheme.typography.titleMedium),
                bottom = HahoTheme.spacing.sm,
            ),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
                Badge(
                    stringResource(R.string.today_now_badge),
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            Text(range, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // TalkBack adds its own full stop between merged lines; drop the note's so it isn't read twice.
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = it.trimEnd('.') },
                )
            }
        }
    }
}

@Composable
internal fun ProhibitedCard(data: TodayData, modifier: Modifier = Modifier) {
    val time = rememberTimeFormatter()
    GlassCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(HahoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
            Text(
                stringResource(R.string.today_prohibited_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            data.prohibited.forEach { w -> ProhibitedRow(w, time.format(w.start, data.zone), time.format(w.end, data.zone)) }
            InfoNote(stringResource(R.string.note_prohibited_cause))
        }
    }
}

@Composable
private fun ProhibitedRow(window: TimeWindow, start: String, end: String) {
    val label = stringResource(
        when (window.type) {
            WindowType.PROHIBITED_AFTER_FAJR -> R.string.prohibited_after_fajr_short
            WindowType.PROHIBITED_ZAWAL -> R.string.prohibited_zawal_short
            else -> R.string.prohibited_after_asr_short
        },
    )
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        Icon(
            painterResource(R.drawable.ic_block),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(HahoTheme.dimens.icon),
        )
        // The badge flows under the label when the text is large, rather than squeezing it.
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.window_range, start, end),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Badge(
                stringResource(R.string.today_now_badge),
                container = Glass.colors.alert,
                content = Glass.colors.onAlert,
            )
        }
    }
}

/** Dividers inside glass cards are hairlines; a solid line reads as heavy on translucent fills. */
private const val DIVIDER_ALPHA = 0.5f

@Composable
private fun ListDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))

/** The rawatib variant labels are Indonesian in the content; the known ones are translated here. */
@Composable
private fun variantLabel(label: String): String = when (label) {
    "di rumah" -> stringResource(R.string.rawatib_variant_home)
    "di masjid" -> stringResource(R.string.rawatib_variant_masjid)
    "2 rakaat" -> pluralStringResource(R.plurals.rakah_count, 2, 2)
    "4 rakaat" -> pluralStringResource(R.plurals.rakah_count, 4, 4)
    else -> label
}
