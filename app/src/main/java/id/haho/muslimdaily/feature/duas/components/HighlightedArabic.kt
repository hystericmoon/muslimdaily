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
                val range = active?.range ?: return@drawBehind
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
 * One phrase per line, so the reader sees where to pause and where to continue (the book's
 * stop/continue cues). The phrase being recited gets the gold fill and underline; with
 * [onPlayFrom], a phrase that has its own audio segment plays from there when tapped. Falls back
 * to [HighlightedArabic] when the dua is a single phrase.
 */
@Composable
fun PhraseLines(
    text: String,
    phrases: List<Phrase>,
    activePhrase: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onPlayFrom: ((Int) -> Unit)? = null,
) {
    if (phrases.size < 2) {
        HighlightedArabic(text, phrases, activePhrase, style, modifier)
        return
    }
    val colors = AdwamTheme.colors
    val density = LocalDensity.current
    val underline = with(density) { AdwamDimens.highlightUnderline.toPx() }
    val playLabel = stringResource(R.string.dua_play_from_phrase)
    val nowState = stringResource(R.string.dua_phrase_reciting)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        phrases.forEach { phrase ->
            val active = phrase.index == activePhrase
            val playable = onPlayFrom != null && phrase.segment != null
            Box(
                Modifier
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
    }
}
