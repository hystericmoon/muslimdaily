package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import id.haho.muslimdaily.core.designsystem.theme.AmiriQuran
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme

private val ArabicLocale = LocaleList("ar")

/**
 * The only way Arabic is rendered in the app. Always RTL, always tagged with the Arabic locale so
 * TalkBack switches to an Arabic voice (the locale is carried to accessibility as a LocaleSpan),
 * regardless of the surrounding LTR layout.
 */
@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AdwamTheme.arabic.arabicBody,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
) {
    val annotated = remember(text) { arabicAnnotated(text) }
    ArabicText(annotated, modifier, style, color, textAlign, maxLines = maxLines)
}

@Composable
fun ArabicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = AdwamTheme.arabic.arabicBody,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val noteColor = MaterialTheme.colorScheme.onSurfaceVariant
    val shown = remember(text, noteColor) { withBookNotes(withQuranOrnaments(text), noteColor) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = shown,
            modifier = modifier.fillMaxWidth(),
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = onTextLayout,
        )
    }
}

/**
 * The verse-end sign (۝) and the ornate Qur'anic brackets (﴾ ﴿) are drawn with Amiri Quran in
 * every typeface: Naskh-style UI faces draw them as a plain circle and brace-like brackets, which
 * read as stray punctuation inside a dua.
 */
private val ORNAMENTS = setOf('\u06DD', '\uFD3E', '\uFD3F')

private fun withQuranOrnaments(text: AnnotatedString): AnnotatedString {
    if (text.none { it in ORNAMENTS }) return text
    return buildAnnotatedString {
        append(text)
        text.forEachIndexed { i, c ->
            if (c in ORNAMENTS) addStyle(SpanStyle(fontFamily = AmiriQuran), i, i + 1)
        }
    }
}

/**
 * The book's notes inside an entry, in single brackets: "[وإذا أمسى قال …]", "(ثلاثاً)". They are
 * not recited, so they are drawn smaller and in the secondary text colour (onSurfaceVariant keeps
 * them above 4.5:1; the size is the cue that doesn't rely on colour). "((" … "))" marks the quoted
 * dua itself and is left alone.
 */
fun withBookNotes(text: AnnotatedString, color: Color): AnnotatedString {
    val ranges = bookNoteRanges(text.text)
    if (ranges.isEmpty()) return text
    // Text that already has its own colour (the phrase on the gold fill) keeps it.
    val coloured = text.spanStyles.filter { it.item.color != Color.Unspecified }
    return buildAnnotatedString {
        append(text)
        ranges.forEach { r ->
            addStyle(SpanStyle(fontSize = NOTE_SIZE), r.first, r.last + 1)
            if (coloured.none { it.start <= r.last && it.end > r.first }) addStyle(SpanStyle(color = color), r.first, r.last + 1)
        }
    }
}

/** A plain-text [withBookNotes], for translations: "(yang berhak disembah)", "( 127 )". */
fun bookNotesMuted(text: String, color: Color): AnnotatedString =
    withBookNotes(AnnotatedString(text), color)

private val NOTE_SIZE = 0.85.em

/** Index ranges of single-bracket groups, outermost only; "((" and "))" are not groups. */
internal fun bookNoteRanges(text: String): List<IntRange> {
    val out = mutableListOf<IntRange>()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val doubled = c == '(' && (text.getOrNull(i + 1) == '(' || text.getOrNull(i - 1) == '(')
        if ((c == '[' || c == '(') && !doubled) {
            val close = if (c == '[') ']' else ')'
            var depth = 0
            var j = i
            while (j < text.length) {
                if (text[j] == c) depth++
                if (text[j] == close && --depth == 0) break
                j++
            }
            if (j < text.length) {
                out += i..j
                i = j + 1
                continue
            }
        }
        i++
    }
    return out
}

/** Wraps Arabic in an AnnotatedString whose single span carries the Arabic locale. */
fun arabicAnnotated(text: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(localeList = ArabicLocale)) { append(text) }
}

/**
 * Isolates a run of opposite-direction text (e.g. an Arabic word inside an Indonesian sentence) with
 * Unicode FSI/PDI so neighbouring punctuation and numbers keep their order.
 */
fun bidiIsolate(text: String): String = FSI + text + PDI

private const val FSI = "\u2068"
private const val PDI = "\u2069"
