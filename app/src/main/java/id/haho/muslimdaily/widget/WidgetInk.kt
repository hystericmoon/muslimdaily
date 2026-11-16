package id.haho.muslimdaily.widget

import androidx.compose.runtime.staticCompositionLocalOf
import id.haho.muslimdaily.data.settings.WidgetTextMode

/**
 * Everything the widget draws in one of its two looks. [Light] is white text on graphite glass
 * (the default); [Dark] is near-black text on white glass, for a light wallpaper under a glass
 * turned very transparent. The tint and the text always change together: text only reads on the
 * glass it was made for.
 */
class WidgetInk(
    val on: Int,
    val muted: Int,
    val accent: Int,
    /** The glass's colour, without alpha. */
    val tint: Int,
    val edge: Int,
    val divider: Int,
    val track: Int,
    /** Text on the accent pill. */
    val pillInk: Int,
) {
    /**
     * Label and Clock callers name a role with the [OnSky], [Muted], [Accent] or [PillInk]
     * constants (the Light values); this swaps in this look's colour for it.
     */
    fun resolve(role: Int): Int = when (role) {
        OnSky -> on
        Muted -> muted
        Accent -> accent
        PillInk -> pillInk
        else -> role
    }

    companion object {
        val Light = WidgetInk(
            on = 0xFFFFFFFF.toInt(),
            muted = 0xA6FFFFFF.toInt(),
            accent = 0xFFEEF26B.toInt(),
            tint = 0x1F1F1F,
            edge = 0x24FFFFFF,
            divider = 0x1AFFFFFF,
            track = 0x38FFFFFF,
            pillInk = 0xFF1F1F1F.toInt(),
        )

        /** The accent is a dark olive (6.9:1 on white); the yellow-green would vanish on white glass. */
        val Dark = WidgetInk(
            on = 0xFF1F1F1F.toInt(),
            muted = 0xB81F1F1F.toInt(),
            accent = 0xFF5A5E00.toInt(),
            tint = 0xFFFFFF,
            edge = 0x24000000,
            divider = 0x1A000000,
            track = 0x38000000,
            pillInk = 0xFFFFFFFF.toInt(),
        )

        fun of(mode: WidgetTextMode): WidgetInk = if (mode == WidgetTextMode.DARK) Dark else Light
    }
}

/** The look the widget under composition is drawn in. */
val LocalInk = staticCompositionLocalOf { WidgetInk.Light }

/** The label on the accent pill: the glass graphite, 14:1 on the accent. */
const val PillInk: Int = 0xFF1F1F1F.toInt()
