package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow

/**
 * A preview of [text] in at most [maxLines] lines that, when cut, ends at a whole word plus "…"
 * instead of the framework's mid-word ellipsis ("dadaku…", audit M10). Like the Arabic preview
 * (ArabicText with clampToWords) the cut is measured in composition with a TextMeasurer, never from
 * onTextLayout, so the first frame already shows the final text. Display only: the result is always
 * a prefix of [text] and spans are kept. With no line limit it is a plain Text.
 */
@Composable
fun WordClampedText(
    text: AnnotatedString,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    if (maxLines !in 1 until Int.MAX_VALUE) {
        Text(text, modifier, style = style, color = color)
        return
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val resolved = LocalTextStyle.current.merge(style)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val displayed = remember(text, maxLines, width, resolved, density, direction) {
            clampedPreview(measurer, text, resolved, maxLines, width, density, direction)
        }
        Text(displayed, Modifier.fillMaxWidth(), style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}
