package id.haho.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG contrast ratio between two opaque colours. */
internal fun ratio(a: Color, b: Color): Double {
    val la = a.luminance() + 0.05
    val lb = b.luminance() + 0.05
    return (max(la, lb) / min(la, lb)).toDouble()
}

/** Seeds a second app might bring: one per hue family, plus a grey and a very light/dark pair. */
internal val SampleSeeds = listOf(
    Color(0xFFD32F2F), // red
    Color(0xFFFFB300), // amber
    Color(0xFF7CB342), // light green
    Color(0xFF00ACC1), // cyan
    Color(0xFF3949AB), // indigo
    Color(0xFF8E24AA), // purple
    Color(0xFF607D8B), // blue grey
    Color(0xFF9E9E9E), // grey
    Color(0xFFF5F5F5), // near white
    Color(0xFF101010), // near black
)
