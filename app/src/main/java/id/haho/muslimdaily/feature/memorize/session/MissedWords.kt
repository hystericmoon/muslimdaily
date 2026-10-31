package id.haho.muslimdaily.feature.memorize.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.component.arabicAnnotated
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.domain.memorize.Word

/**
 * Check: the full text with each word as a toggle, so the learner marks the words they missed or
 * got wrong. A marked word is filled and outlined in the error colours (fill and a heavier border,
 * not colour alone). Tokens with no Arabic letter can't be marked. Each word is a checkbox to
 * TalkBack, read in Arabic.
 */
@Composable
fun MissedWords(
    words: List<Word>,
    missed: Set<Int>,
    style: TextStyle,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markable = Recall.recallable(words).map { it.index }.toSet()
    val shape = MaterialTheme.shapes.extraSmall
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
        ) {
            words.forEach { w ->
                if (w.index !in markable) {
                    Text(arabicAnnotated(w.text), style = style, color = LocalContentColor.current)
                    return@forEach
                }
                val marked = w.index in missed
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .then(
                            if (marked) {
                                Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, shape)
                                    .border(2.dp, MaterialTheme.colorScheme.error, shape)
                            } else {
                                Modifier.border(HahoTheme.dimens.divider, MaterialTheme.colorScheme.outlineVariant, shape)
                            },
                        )
                        .toggleable(value = marked, role = Role.Checkbox, onValueChange = { onToggle(w.index) })
                        .padding(horizontal = HahoTheme.spacing.xs),
                ) {
                    Text(
                        arabicAnnotated(w.text),
                        style = style,
                        color = if (marked) MaterialTheme.colorScheme.onErrorContainer else LocalContentColor.current,
                    )
                }
            }
        }
    }
}
