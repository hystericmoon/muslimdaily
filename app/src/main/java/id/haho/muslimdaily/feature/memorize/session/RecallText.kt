package id.haho.muslimdaily.feature.memorize.session

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import id.haho.muslimdaily.core.designsystem.component.arabicAnnotated
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.domain.memorize.Word

/**
 * Recall view. Hidden words become blank boxes exactly as wide as the word they replace, so the
 * line layout never shifts and nothing is shown as faint grey text. TalkBack users get a single
 * description instead of the hidden words; their recall happens by ear (hidden phrases muted).
 */
@Composable
fun RecallText(
    words: List<Word>,
    hidden: Set<Int>,
    style: TextStyle,
    hiddenDescription: String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widths = remember(words, style, density) {
        words.associate { w -> w.index to with(density) { measurer.measure(arabicAnnotated(w.text), style).size.width.toDp() } }
    }
    val lineHeight = with(density) { style.fontSize.toDp() * 1.3f }
    val semanticsModifier = if (hidden.isEmpty()) {
        Modifier
    } else {
        Modifier.clearAndSetSemantics { contentDescription = hiddenDescription }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = modifier.fillMaxWidth().then(semanticsModifier),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            words.forEach { w ->
                if (w.index in hidden) {
                    Box(Modifier.size(width = widths.getValue(w.index), height = with(density) { style.lineHeight.toDp() })) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(width = widths.getValue(w.index), height = lineHeight)
                                .align(androidx.compose.ui.Alignment.Center)
                                .border(AppTheme.dimens.divider, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall),
                        ) {}
                    }
                } else {
                    Text(arabicAnnotated(w.text), style = style, color = LocalContentColor.current)
                }
            }
        }
    }
}
