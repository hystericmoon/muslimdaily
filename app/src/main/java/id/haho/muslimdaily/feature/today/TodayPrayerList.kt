package id.haho.muslimdaily.feature.today

import id.haho.muslimdaily.core.designsystem.glass.GlassBottomSheet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.Color
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.chevronRotation
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.Badge
import id.haho.muslimdaily.core.designsystem.component.CheckRow
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.StatusBadge
import id.haho.muslimdaily.core.designsystem.component.TierBadge
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.model.Rawatib
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
    GlassCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            data.rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PrayerRowView(
                    row = row,
                    data = data,
                    expanded = row.slot.name in expanded,
                    onExpandToggle = { onExpandToggle(row.slot.name) },
                    onToggle = onToggle,
                    onShowEvidence = { evidenceFor = it },
                )
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
    val isSunrise = row.slot == PrayerTimeSlot.SUNRISE
    val expandLabel = stringResource(if (expanded) R.string.today_collapse_rawatib else R.string.today_expand_rawatib)
    val stateLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val rowBackground by animateColorAsState(
        if (row.isCurrent) Glass.colors.tinted else Color.Transparent,
        AppTheme.motion.fade(),
        label = "rowBackground",
    )
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(Modifier.background(rowBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTheme.dimens.largeTouchTarget)
                .then(
                    if (plan != null) {
                        Modifier
                            .clickable(onClickLabel = expandLabel, role = Role.Button, onClick = onExpandToggle)
                            .semantics { stateDescription = stateLabel }
                    } else {
                        Modifier
                    },
                )
                .padding(start = AppTheme.dimens.screenGutter, end = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = AppTheme.spacing.md)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        name,
                        style = if (isSunrise) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                        color = if (isSunrise) MaterialTheme.colorScheme.onSurfaceVariant else contentColor,
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
                row.iqamah?.let {
                    Text(
                        stringResource(R.string.today_iqamah, time.format(it, data.zone)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.isCurrent) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                time.format(row.time, data.zone),
                style = if (isSunrise) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                color = if (isSunrise) MaterialTheme.colorScheme.onSurfaceVariant else contentColor,
            )
            Box(Modifier.size(AppTheme.dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                if (plan != null) {
                    Icon(
                        painterResource(R.drawable.ic_keyboard_arrow_down),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.chevronRotation(expanded),
                    )
                }
            }
        }
        if (plan != null) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(AppTheme.motion.layout()) + fadeIn(AppTheme.motion.fade()),
                exit = shrinkVertically(AppTheme.motion.layout()) + fadeOut(AppTheme.motion.fade()),
            ) {
                Timeline(plan, data, onToggle, onShowEvidence)
            }
        }
    }
}

/** qabliyah → fard → ba'diyah, each with its private check. */
@Composable
private fun Timeline(
    plan: PrayerPlan,
    data: TodayData,
    onToggle: (String, Boolean) -> Unit,
    onShowEvidence: (RawatibLine) -> Unit,
) {
    val steps = buildList {
        plan.before.forEach { add(TimelineStep.Sunnah(it, isBefore = true)) }
        plan.fard?.let { add(TimelineStep.Obligatory(it.rakah)) }
        plan.after.forEach { add(TimelineStep.Sunnah(it, isBefore = false)) }
    }
    GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.padding(
        start = AppTheme.spacing.md, end = AppTheme.spacing.md, bottom = AppTheme.spacing.md,
    )) {
        Column(Modifier.padding(vertical = AppTheme.spacing.sm)) {
            steps.forEachIndexed { i, step ->
                TimelineStepRow(step, plan, data, isFirst = i == 0, isLast = i == steps.lastIndex, onToggle, onShowEvidence)
            }
            if (plan.showJumuahNote) {
                InfoNote(stringResource(R.string.note_jumuah), Modifier.padding(AppTheme.spacing.md))
            }
        }
    }
}

private sealed interface TimelineStep {
    data class Sunnah(val line: RawatibLine, val isBefore: Boolean) : TimelineStep
    data class Obligatory(val rakah: Int) : TimelineStep
}

@Composable
private fun TimelineStepRow(
    step: TimelineStep,
    plan: PrayerPlan,
    data: TodayData,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onShowEvidence: (RawatibLine) -> Unit,
) {
    val key = when (step) {
        is TimelineStep.Obligatory -> ChecklistRepository.fardKey(plan.prayer.name)
        is TimelineStep.Sunnah -> ChecklistRepository.rawatibKey(step.line.primary.id)
    }
    val checked = key in data.checked
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = AppTheme.spacing.md)) {
        Rail(isFirst, isLast, emphasised = step is TimelineStep.Obligatory)
        Column(Modifier.weight(1f)) {
            CheckRow(
                checked = checked,
                onCheckedChange = { onToggle(key, it) },
                modifier = Modifier.padding(start = AppTheme.spacing.md, end = AppTheme.spacing.xs),
                minHeight = AppTheme.dimens.largeTouchTarget,
            ) {
                Column(Modifier.weight(1f).padding(vertical = AppTheme.spacing.sm)) {
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
                    }
                }
            }
            if (step is TimelineStep.Sunnah) {
                step.line.primary.notes?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = AppTheme.spacing.md, end = AppTheme.spacing.md),
                    )
                }
                TextButton(onClick = { onShowEvidence(step.line) }, modifier = Modifier.padding(start = AppTheme.spacing.xs)) {
                    Icon(painterResource(R.drawable.ic_format_list_numbered), null, Modifier.size(AppTheme.dimens.smallIcon))
                    Text(stringResource(R.string.rawatib_evidence), Modifier.padding(start = AppTheme.spacing.xs))
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
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        modifier = Modifier.padding(top = AppTheme.spacing.xs),
    ) {
        TierBadge(entry.tier)
        StatusBadge(entry.status)
    }
}

/** "4 rakaat", or "2 rakaat di rumah atau 4 rakaat di masjid" for alternatives. */
@Composable
private fun rakahText(line: RawatibLine): String? {
    val parts = line.entries.mapNotNull { r ->
        r.rakah?.let { n ->
            val count = pluralStringResource(R.plurals.rakah_count, n, n)
            if (line.entries.size > 1 && r.variantLabel != null) stringResource(R.string.rakah_with_label, count, r.variantLabel) else count
        }
    }
    if (parts.isEmpty()) return null
    return parts.reduce { acc, s -> stringResource(R.string.rakah_or, acc, s) }
}

@Composable
private fun Rail(isFirst: Boolean, isLast: Boolean, emphasised: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxHeight().width(AppTheme.dimens.timelineDot), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).width(AppTheme.dimens.divider * 2).background(if (isFirst) lineColor.copy(alpha = 0f) else lineColor))
        Box(Modifier.size(AppTheme.dimens.timelineDot).background(dotColor, CircleShape))
        Box(Modifier.weight(1f).width(AppTheme.dimens.divider * 2).background(if (isLast) lineColor.copy(alpha = 0f) else lineColor))
    }
}

@Composable
private fun EvidenceSheet(line: RawatibLine, onDismiss: () -> Unit) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = AppTheme.dimens.screenGutter).padding(bottom = AppTheme.spacing.xxl)) {
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
            Row(Modifier.padding(vertical = AppTheme.spacing.sm), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                TierBadge(entry.tier)
                StatusBadge(entry.status)
            }
            val evidence = line.entries.flatMap { it.evidence }.filter { it.book.isNotBlank() }
            Text(stringResource(R.string.rawatib_evidence), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = AppTheme.spacing.md))
            if (evidence.isEmpty()) {
                Text(stringResource(R.string.rawatib_evidence_missing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                evidence.forEach { Text("${it.book} ${it.no}".trim(), style = MaterialTheme.typography.bodyMedium) }
            }
            line.entries.mapNotNull(Rawatib::notes).distinct().forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = AppTheme.spacing.sm))
            }
        }
    }
}

@Composable
internal fun OtherSunnahCard(data: TodayData, onToggle: (String, Boolean) -> Unit, modifier: Modifier = Modifier) {
    val time = rememberTimeFormatter()
    GlassCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(vertical = AppTheme.spacing.sm)) {
            data.duha?.let { w ->
                SunnahWindowRow(
                    title = stringResource(R.string.duha_title),
                    range = stringResource(R.string.window_range, time.format(w.start, data.zone), time.format(w.end, data.zone)),
                    note = stringResource(R.string.note_duha_best),
                    checked = ChecklistRepository.DUHA in data.checked,
                    active = data.now in w,
                    onCheckedChange = { onToggle(ChecklistRepository.DUHA, it) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = AppTheme.dimens.screenGutter))
            data.witr?.let { w ->
                SunnahWindowRow(
                    title = stringResource(R.string.witr_title),
                    range = stringResource(R.string.window_range, time.format(w.start, data.zone), time.format(w.end, data.zone)),
                    note = null,
                    checked = ChecklistRepository.WITR in data.checked,
                    active = data.now in w,
                    onCheckedChange = { onToggle(ChecklistRepository.WITR, it) },
                )
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
    active: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    CheckRow(
        checked = checked,
        onCheckedChange = onCheckedChange,
        minHeight = AppTheme.dimens.largeTouchTarget,
        modifier = Modifier.padding(start = AppTheme.dimens.screenGutter, end = AppTheme.spacing.xs),
    ) {
        Column(Modifier.weight(1f).padding(vertical = AppTheme.spacing.sm)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
                if (active) {
                    Badge(
                        stringResource(R.string.today_now_badge),
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            Text(range, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
internal fun ProhibitedCard(data: TodayData, modifier: Modifier = Modifier) {
    val time = rememberTimeFormatter()
    GlassCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            data.prohibited.forEach { w -> ProhibitedRow(w, data, time.format(w.start, data.zone), time.format(w.end, data.zone)) }
            InfoNote(stringResource(R.string.note_prohibited_cause))
        }
    }
}

@Composable
private fun ProhibitedRow(window: TimeWindow, data: TodayData, start: String, end: String) {
    val active = data.now in window
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
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Icon(
            painterResource(R.drawable.ic_block),
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.dimens.icon),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.window_range, start, end),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (active) {
            Badge(
                stringResource(R.string.today_now_badge),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
