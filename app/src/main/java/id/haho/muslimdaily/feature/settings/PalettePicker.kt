package id.haho.muslimdaily.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.AppPalette
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.designsystem.theme.colorSchemeFor

@StringRes
fun AppPalette.labelRes(): Int = when (this) {
    AppPalette.TEAL -> R.string.palette_teal
    AppPalette.NAVY -> R.string.palette_navy
    AppPalette.FOREST -> R.string.palette_forest
    AppPalette.SLATE -> R.string.palette_slate
    AppPalette.OLIVE -> R.string.palette_olive
    AppPalette.ROSE -> R.string.palette_rose
    AppPalette.LAVENDER -> R.string.palette_lavender
    AppPalette.PEACH -> R.string.palette_peach
}

/**
 * Colour-theme picker: a radio group of swatches, each labelled by name so the choice never rests
 * on colour alone. Swatches preview the palette in the current light/dark mode. Disabled while
 * dynamic colour is on, because the wallpaper colours replace the palette then.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalettePicker(
    selected: AppPalette,
    enabled: Boolean,
    onSelect: (AppPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Text(stringResource(R.string.display_palette), style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            Modifier.fillMaxWidth().selectableGroup().alpha(if (enabled) 1f else 0.38f),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppPalette.entries.forEach { palette ->
                Swatch(palette, dark, palette == selected, enabled) { onSelect(palette) }
            }
        }
        if (!enabled) {
            Text(
                stringResource(R.string.display_palette_dynamic_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SwatchSize = 40.dp
private val SwatchCell = 80.dp

@Composable
private fun Swatch(palette: AppPalette, dark: Boolean, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = remember(palette, dark) { colorSchemeFor(palette, dark) }
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, AppTheme.motion.fade(), label = "ring")
    Column(
        Modifier
            .width(SwatchCell)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = AppTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Box(
            Modifier
                .size(SwatchSize + 8.dp)
                // A 0dp border still paints a hairline, so the ring only exists while it has width.
                .then(if (ring > 0.dp) Modifier.border(ring, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    // Primary on top (the check sits on it), the container tones below.
                    Brush.verticalGradient(
                        0f to scheme.primary, 0.64f to scheme.primary,
                        0.64f to scheme.primaryContainer, 1f to scheme.tertiaryContainer,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            stringResource(palette.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
