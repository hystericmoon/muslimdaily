package id.haho.muslimdaily.feature.duas.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import id.haho.muslimdaily.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import id.haho.muslimdaily.data.settings.PhraseLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.model.Phrase
import kotlin.math.max
import kotlin.math.min

/**
 * Arabic text with the phrase being recited marked by a gold fill behind it plus an underline in
 * the indicator colour, so the state never depends on colour alone. Text on the fill switches to
 * the on-fill colour to keep 4.5:1 contrast.
 */
@Composable
fun HighlightedArabic(
    text: String,
    phrases: List<Phrase>,
    activePhrase: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.LocalContentColor.current,
) {
    val colors = AdwamTheme.colors
    val active = phrases.getOrNull(activePhrase)
    val annotated = remember(text, active, colors) {
        buildAnnotatedString {
            withStyle(SpanStyle(localeList = LocaleList("ar"))) {
                if (active == null) {
                    append(text)
                } else {
                    append(text.substring(0, active.range.first))
                    withStyle(SpanStyle(color = colors.onHighlightFill)) { append(text.substring(active.range.first, active.range.last + 1)) }
                    append(text.substring(active.range.last + 1))
                }
            }
        }
    }
    HighlightedText(annotated, active?.range, style, modifier, color)
}

/** [annotated] with the gold fill and underline drawn behind [range], line by line. */
@Composable
private fun HighlightedText(
    annotated: AnnotatedString,
    range: IntRange?,
    style: TextStyle,
    modifier: Modifier,
    color: Color,
) {
    val colors = AdwamTheme.colors
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val underline = with(density) { AdwamDimens.highlightUnderline.toPx() }
    val radius = with(density) { HahoTheme.spacing.xs.toPx() }
    val padX = with(density) { HahoTheme.spacing.xs.toPx() }

    ArabicText(
        text = annotated,
        style = style,
        color = color,
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val l = layout ?: return@drawBehind
                if (range == null) return@drawBehind
                if (range.isEmpty()) return@drawBehind
                val firstLine = l.getLineForOffset(range.first)
                val lastLine = l.getLineForOffset(range.last)
                for (line in firstLine..lastLine) {
                    val lineStart = max(range.first, l.getLineStart(line))
                    val lineEnd = min(range.last + 1, l.getLineEnd(line, visibleEnd = true))
                    if (lineEnd <= lineStart) continue
                    val x1 = l.getHorizontalPosition(lineStart, usePrimaryDirection = true)
                    val x2 = l.getHorizontalPosition(lineEnd, usePrimaryDirection = true)
                    val left = min(x1, x2) - padX
                    val right = max(x1, x2) + padX
                    val top = l.getLineTop(line)
                    val bottom = l.getLineBottom(line)
                    drawRoundRect(
                        color = colors.highlightFill,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        cornerRadius = CornerRadius(radius, radius),
                    )
                    drawRect(
                        color = colors.highlightIndicator,
                        topLeft = Offset(left, bottom - underline),
                        size = Size(right - left, underline),
                    )
                }
            },
    )
}

/**
 * The dua's phrases with the book's stop/continue cues, in the reader's [layout]: one phrase per
 * line, flowing text with a pause mark between phrases, or one phrase at a time. The phrase being
 * recited gets the gold fill and underline; with [onPlayFrom], a phrase that has its own audio
 * segment plays from there when tapped. Falls back to [HighlightedArabic] when the dua is a
 * single phrase.
 */
@Composable
fun PhraseLines(
    text: String,
    phrases: List<Phrase>,
    activePhrase: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    layout: PhraseLayout = PhraseLayout.LINES,
    onPlayFrom: ((Int) -> Unit)? = null,
) {
    when {
        phrases.size < 2 -> HighlightedArabic(text, phrases, activePhrase, style, modifier)
        layout == PhraseLayout.INLINE -> PhraseFlow(phrases, activePhrase, style, modifier, onPlayFrom)
        layout == PhraseLayout.FOCUS -> PhraseFocus(phrases, activePhrase, style, modifier, onPlayFrom)
        else -> PhraseColumn(phrases, activePhrase, style, modifier, onPlayFrom)
    }
}

/** Layout A: one phrase per line; the line break is the pause. */
@Composable
private fun PhraseColumn(
    phrases: List<Phrase>,
    activePhrase: Int,
    style: TextStyle,
    modifier: Modifier,
    onPlayFrom: ((Int) -> Unit)?,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        phrases.forEach { phrase -> PhraseRow(phrase, phrase.index == activePhrase, style, onPlayFrom) }
    }
}

/** One phrase as its own row: a 48dp target that plays from the phrase when it has a segment. */
@Composable
private fun PhraseRow(phrase: Phrase, active: Boolean, style: TextStyle, onPlayFrom: ((Int) -> Unit)?, modifier: Modifier = Modifier) {
    val colors = AdwamTheme.colors
    val density = LocalDensity.current
    val underline = with(density) { AdwamDimens.highlightUnderline.toPx() }
    val playLabel = stringResource(R.string.dua_play_from_phrase)
    val nowState = stringResource(R.string.dua_phrase_reciting)
    val playable = onPlayFrom != null && phrase.segment != null
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HahoTheme.spacing.xs))
            .drawBehind {
                if (!active) return@drawBehind
                drawRect(colors.highlightFill)
                drawRect(colors.highlightIndicator, topLeft = Offset(0f, size.height - underline), size = Size(size.width, underline))
            }
            .then(
                if (playable) {
                    Modifier.clickable(onClickLabel = playLabel, role = Role.Button) { onPlayFrom?.invoke(phrase.index) }
                } else {
                    Modifier
                },
            )
            .then(if (active) Modifier.semantics { stateDescription = nowState } else Modifier)
            .heightIn(min = HahoTheme.dimens.minTouchTarget)
            .padding(horizontal = HahoTheme.spacing.xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        ArabicText(
            phrase.text,
            style = style,
            color = if (active) colors.onHighlightFill else androidx.compose.material3.LocalContentColor.current,
        )
    }
}

/**
 * Layout B: the text flows as in the book, with a pause mark between phrases. The mark is the
 * app's (a vertical four-dot ⁞, never a Qur'anic waqf sign, which carries its own meaning) and is
 * hidden from TalkBack, which reads each phrase as its own link when it can play.
 */
@Composable
private fun PhraseFlow(
    phrases: List<Phrase>,
    activePhrase: Int,
    style: TextStyle,
    modifier: Modifier,
    onPlayFrom: ((Int) -> Unit)?,
) {
    val colors = AdwamTheme.colors
    val markColor = MaterialTheme.colorScheme.primary
    var activeRange: IntRange? = null
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(localeList = LocaleList("ar"))) {
            phrases.forEachIndexed { i, phrase ->
                if (i > 0) {
                    append(" ")
                    withStyle(SpanStyle(color = markColor, fontSize = style.fontSize * PAUSE_MARK_SCALE)) { append(PAUSE_MARK) }
                    append(" ")
                }
                val start = length
                val active = phrase.index == activePhrase
                val span = SpanStyle(color = if (active) colors.onHighlightFill else Color.Unspecified)
                if (onPlayFrom != null && phrase.segment != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "phrase-${phrase.index}",
                            styles = TextLinkStyles(style = span),
                            linkInteractionListener = { onPlayFrom(phrase.index) },
                        ),
                    ) { append(phrase.text) }
                } else {
                    withStyle(span) { append(phrase.text) }
                }
                if (active) activeRange = start until length
            }
        }
    }
    HighlightedText(annotated, activeRange, style, modifier, androidx.compose.material3.LocalContentColor.current)
}

/**
 * Layout C: one phrase at a time with Back and Next, for slow reading and memorising. While the
 * audio plays, the phrase shown follows the recitation; the position is announced politely.
 */
@Composable
private fun PhraseFocus(
    phrases: List<Phrase>,
    activePhrase: Int,
    style: TextStyle,
    modifier: Modifier,
    onPlayFrom: ((Int) -> Unit)?,
) {
    var shown by rememberSaveable(phrases.size) { mutableIntStateOf(0) }
    LaunchedEffect(activePhrase) { if (activePhrase in phrases.indices) shown = activePhrase }
    val phrase = phrases[shown.coerceIn(phrases.indices)]
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        PhraseRow(phrase, phrase.index == activePhrase, style, onPlayFrom, Modifier.heightIn(min = HahoTheme.dimens.minTouchTarget * 2))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { shown -= 1 }, enabled = shown > 0) {
                Icon(painterResource(R.drawable.ic_chevron_left), null, Modifier.size(HahoTheme.dimens.smallIcon))
                Text(stringResource(R.string.dua_phrase_back), Modifier.padding(start = HahoTheme.spacing.xs))
            }
            Text(
                stringResource(R.string.dua_phrase_position, shown + 1, phrases.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = { shown += 1 }, enabled = shown < phrases.lastIndex) {
                Text(stringResource(R.string.dua_phrase_next), Modifier.padding(end = HahoTheme.spacing.xs))
                Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(HahoTheme.dimens.smallIcon))
            }
        }
    }
}

private const val PAUSE_MARK = "\u205E"
private const val PAUSE_MARK_SCALE = 0.8f
