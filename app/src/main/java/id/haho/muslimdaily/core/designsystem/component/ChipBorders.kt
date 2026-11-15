package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Borders for tappable chips in the `outline` token (about 4:1 on the surfaces), not Material's
 * default `outlineVariant` (about 1.6:1 light, 1.2 to 1.4:1 dark), which fails WCAG 1.4.11 for a
 * control's boundary (audit R14). A selected filter chip keeps its filled container as the cue.
 */
@Composable
fun assistChipBorder(): BorderStroke =
    AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline)

@Composable
fun filterChipBorder(selected: Boolean): BorderStroke =
    FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = MaterialTheme.colorScheme.outline,
        selectedBorderColor = MaterialTheme.colorScheme.outline,
    )
