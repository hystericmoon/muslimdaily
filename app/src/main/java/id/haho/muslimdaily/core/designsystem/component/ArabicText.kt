package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
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
import id.haho.muslimdaily.core.designsystem.theme.AppTheme

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
    style: TextStyle = AppTheme.arabic.arabicBody,
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
    style: TextStyle = AppTheme.arabic.arabicBody,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = text,
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
