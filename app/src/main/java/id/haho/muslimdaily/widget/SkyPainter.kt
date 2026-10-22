package id.haho.muslimdaily.widget

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.domain.prayer.PrayerSnapshot
import java.time.Instant
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** The sky's look for each stretch of the day, between two prayer times. */
enum class SkyPhase(val top: Int, val middle: Int, val horizon: Int, val stars: Boolean) {
    NIGHT(0xFF070E24.toInt(), 0xFF12204A.toInt(), 0xFF26386B.toInt(), true),
    DAWN(0xFF15204A.toInt(), 0xFF52427E.toInt(), 0xFFD98377.toInt(), true),
    MORNING(0xFF1A5A9E.toInt(), 0xFF3F86C9.toInt(), 0xFF8DC3EA.toInt(), false),
    NOON(0xFF134F92.toInt(), 0xFF2F79C2.toInt(), 0xFF79B7E6.toInt(), false),
    AFTERNOON(0xFF1E477F.toInt(), 0xFF8A6A7A.toInt(), 0xFFEAA963.toInt(), false),
    DUSK(0xFF1A1438.toInt(), 0xFF5E2F63.toInt(), 0xFFD8684F.toInt(), true),
    ;

    companion object {
        fun at(now: Instant, s: PrayerSnapshot): SkyPhase {
            fun t(slot: PrayerTimeSlot) = s.today.first { it.slot == slot }.time
            return when {
                now.isBefore(t(PrayerTimeSlot.FAJR)) -> NIGHT
                now.isBefore(t(PrayerTimeSlot.SUNRISE)) -> DAWN
                now.isBefore(t(PrayerTimeSlot.DHUHR)) -> MORNING
                now.isBefore(t(PrayerTimeSlot.ASR)) -> NOON
                now.isBefore(t(PrayerTimeSlot.MAGHRIB)) -> AFTERNOON
                now.isBefore(t(PrayerTimeSlot.ISHA)) -> DUSK
                else -> NIGHT
            }
        }
    }
}

/**
 * Paints a widget background: a time-of-day sky, the sun's path from Fajr to Isya with a marker
 * at each prayer time, the sun (or moon) where it is now, and an optional frosted strip for the
 * day's times. All text is laid over this by Glance; the sky keeps its upper part dark enough for
 * white text (see SkyContrastTest).
 */
object SkyPainter {

    class Spec(
        val widthPx: Int,
        val heightPx: Int,
        val density: Float,
        val snapshot: PrayerSnapshot,
        val now: Instant,
        /** Vertical band for the sun path, as fractions of the height. */
        val arcTop: Float,
        val arcBottom: Float,
        val markers: Boolean,
        /** False draws no path, only the sun or moon at the right edge (for short widgets). */
        val arc: Boolean = true,
        /** Height of the frosted strip at the bottom, in px; 0 for none. */
        val stripPx: Int = 0,
        val cornerPx: Float,
    )

    /** A plain evening sky with no path, for widgets that have nothing to show yet. */
    fun backdrop(widthPx: Int, heightPx: Int, density: Float, cornerPx: Float): Bitmap {
        val bmp = createBitmap(widthPx, heightPx)
        val c = Canvas(bmp)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val phase = SkyPhase.DUSK
        c.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(phase.top, phase.middle, phase.horizon), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawRect(0f, 0f, w, h, Paint().apply { color = 0x40000000 })
        moon(c, w - 28f * density, 28f * density, 8f * density, phase)
        roundAndRim(c, bmp, w, h, density, cornerPx)
        return bmp
    }

    fun paint(spec: Spec): Bitmap {
        val w = spec.widthPx.toFloat()
        val h = spec.heightPx.toFloat()
        val d = spec.density
        val bmp = createBitmap(spec.widthPx, spec.heightPx)
        val c = Canvas(bmp)
        val phase = SkyPhase.at(spec.now, spec.snapshot)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sky.
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(phase.top, phase.middle, phase.horizon), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        // A soft veil at the top-left, where the text sits, so white text keeps its contrast.
        p.shader = RadialGradient(0f, 0f, max(w, h) * 0.9f, intArrayOf(0x66000000, 0x00000000), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        if (phase.stars) stars(c, w, h * spec.arcBottom, d, phase)

        if (spec.arc) sunPath(c, spec, phase) else celestial(c, spec, phase)

        if (spec.stripPx > 0) strip(c, w, h, spec.stripPx.toFloat(), d)

        roundAndRim(c, bmp, w, h, d, spec.cornerPx)
        return bmp
    }

    /** Rounded corners (launchers draw widgets edge to edge, so the shape is ours) and a glass rim. */
    private fun roundAndRim(c: Canvas, bmp: Bitmap, w: Float, h: Float, d: Float, corner: Float) {
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        val shape = createBitmap(bmp.width, bmp.height, Bitmap.Config.ALPHA_8)
        Canvas(shape).drawRoundRect(RectF(0f, 0f, w, h), corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
        c.drawBitmap(shape, 0f, 0f, mask)
        shape.recycle()
        // Bright along the top edge, fading downward.
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
            shader = LinearGradient(0f, 0f, 0f, h, 0x66FFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP)
        }
        val inset = rim.strokeWidth / 2
        c.drawRoundRect(RectF(inset, inset, w - inset, h - inset), corner, corner, rim)
    }

    private fun stars(c: Canvas, w: Float, maxY: Float, d: Float, phase: SkyPhase) {
        val rnd = Random(7)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = ((w * maxY) / (d * d * 220f)).toInt().coerceIn(12, 90)
        val fade = if (phase == SkyPhase.NIGHT) 1f else 0.55f
        repeat(count) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * maxY * 0.85f
            val r = (0.5f + rnd.nextFloat() * 0.9f) * d
            // Keep the text corner clear: stars behind small white text read as noise.
            if (x < w * 0.6f && y < maxY * 0.7f) return@repeat
            p.color = Color.argb(((90 + rnd.nextInt(140)) * fade).toInt(), 255, 255, 255)
            c.drawCircle(x, y, r, p)
        }
    }

    /**
     * The sun's path as a sine: rising at sunrise, highest at noon, setting at Maghrib, and dipping
     * below the horizon before and after. The x axis runs from Fajr to Isya.
     */
    private fun sunPath(c: Canvas, spec: Spec, phase: SkyPhase) {
        val d = spec.density
        val w = spec.widthPx.toFloat()
        val h = spec.heightPx.toFloat()
        val s = spec.snapshot
        fun t(slot: PrayerTimeSlot) = s.today.first { it.slot == slot }.time.toEpochMilli().toDouble()
        val start = t(PrayerTimeSlot.FAJR)
        val end = t(PrayerTimeSlot.ISHA)
        val rise = t(PrayerTimeSlot.SUNRISE)
        val set = t(PrayerTimeSlot.MAGHRIB)
        val padX = 18f * d
        val top = h * spec.arcTop
        val horizon = h * spec.arcBottom
        val amp = (horizon - top)
        fun x(ms: Double) = (padX + (ms - start) / (end - start) * (w - 2 * padX)).toFloat()
        fun y(ms: Double) = (horizon - amp * sin(PI * (ms - rise) / (set - rise))).toFloat()

        // Horizon glow and line.
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, horizon - 18 * d, 0f, horizon + 6 * d, 0x00FFFFFF, 0x26FFFFFF, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, horizon - 18 * d, w, horizon + 1 * d, glow)
        c.drawLine(padX * 0.5f, horizon, w - padX * 0.5f, horizon, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF; strokeWidth = 1f * d })

        // The path: dashed ahead, solid behind.
        val now = spec.now.toEpochMilli().toDouble().coerceIn(start, end)
        fun path(from: Double, to: Double) = Path().apply {
            val steps = 64
            for (i in 0..steps) {
                val ms = from + (to - from) * i / steps
                if (i == 0) moveTo(x(ms), y(ms)) else lineTo(x(ms), y(ms))
            }
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f * d; strokeCap = Paint.Cap.ROUND }
        line.color = 0x59FFFFFF
        line.pathEffect = DashPathEffect(floatArrayOf(3f * d, 4f * d), 0f)
        c.drawPath(path(now, end), line)
        line.pathEffect = null
        line.color = 0xC8FFFFFF.toInt()
        c.drawPath(path(start, now), line)

        // A marker at each prayer time; the next one is a lit ring.
        if (spec.markers) {
            val next = s.nextSlot
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            s.today.forEach { st ->
                val ms = st.time.toEpochMilli().toDouble()
                val px = x(ms)
                val py = y(ms)
                when {
                    st.slot == next -> {
                        dot.style = Paint.Style.FILL
                        dot.color = 0x40FFFFFF
                        c.drawCircle(px, py, 8f * d, dot)
                        dot.style = Paint.Style.STROKE
                        dot.strokeWidth = 2f * d
                        dot.color = Color.WHITE
                        c.drawCircle(px, py, 5.5f * d, dot)
                    }
                    ms <= now -> {
                        dot.style = Paint.Style.FILL
                        dot.color = 0xE6FFFFFF.toInt()
                        c.drawCircle(px, py, 3f * d, dot)
                    }
                    else -> {
                        dot.style = Paint.Style.FILL
                        dot.color = 0x80FFFFFF.toInt()
                        c.drawCircle(px, py, 2.5f * d, dot)
                    }
                }
            }
        }

        // The sun where it is now, or the moon after sunset and before dawn.
        val sunUp = spec.now.toEpochMilli() in rise.toLong()..set.toLong()
        if (sunUp) {
            val sx = x(now)
            val sy = y(now)
            val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(sx, sy, 26f * d, intArrayOf(0x99FFE7A8.toInt(), 0x33FFD27A, 0x00FFD27A), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            }
            c.drawCircle(sx, sy, 26f * d, halo)
            c.drawCircle(sx, sy, 6.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF4D6.toInt() })
        } else {
            moon(c, w - 30f * d, 30f * d, 8f * d, phase)
        }
    }

    /** Just the sun or moon, at the right edge, for widgets too short for a path. */
    private fun celestial(c: Canvas, spec: Spec, phase: SkyPhase) {
        val d = spec.density
        val cx = spec.widthPx - 30f * d
        val cy = spec.heightPx * 0.42f
        val s = spec.snapshot
        val rise = s.today.first { it.slot == PrayerTimeSlot.SUNRISE }.time
        val set = s.today.first { it.slot == PrayerTimeSlot.MAGHRIB }.time
        if (!spec.now.isBefore(rise) && spec.now.isBefore(set)) {
            c.drawCircle(cx, cy, 30f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx, cy, 30f * d, intArrayOf(0x99FFE7A8.toInt(), 0x33FFD27A, 0x00FFD27A), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            })
            c.drawCircle(cx, cy, 8f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF4D6.toInt() })
        } else {
            moon(c, cx, cy, 9f * d, phase)
        }
    }

    private fun moon(c: Canvas, cx: Float, cy: Float, r: Float, phase: SkyPhase) {
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 3.2f, intArrayOf(0x40E8EEFF, 0x00E8EEFF), null, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, r * 3.2f, glow)
        // Crescent: a full disc with an offset disc cut out.
        val layer = c.saveLayer(cx - r * 2, cy - r * 2, cx + r * 2, cy + r * 2, null)
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1F3FF.toInt() })
        c.drawCircle(cx + r * 0.45f, cy - r * 0.25f, r * 0.85f, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) })
        c.restoreToCount(layer)
    }

    /** Frosted strip along the bottom for the day's times: dark glass with a light top edge. */
    private fun strip(c: Canvas, w: Float, h: Float, stripH: Float, d: Float) {
        val m = 8f * d
        val rect = RectF(m, h - stripH - m, w - m, h - m)
        val r = 16f * d
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x52000000 })
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0x1FFFFFFF, 0x05FFFFFF, Shader.TileMode.CLAMP)
        })
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
            shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0x4DFFFFFF, 0x0DFFFFFF, Shader.TileMode.CLAMP)
        })
    }

    /** Lit glass pill behind the next prayer in the strip. */
    fun pill(widthPx: Int, heightPx: Int, density: Float): Bitmap {
        val bmp = createBitmap(max(1, widthPx), max(1, heightPx), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val r = min(rect.height(), rect.width()) * 0.32f
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, rect.height(), 0x47FFFFFF, 0x24FFFFFF, Shader.TileMode.CLAMP)
        })
        val inset = density / 2
        c.drawRoundRect(RectF(inset, inset, rect.right - inset, rect.bottom - inset), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density
            color = 0x80FFFFFF.toInt()
            maskFilter = BlurMaskFilter(0.5f * density, BlurMaskFilter.Blur.SOLID)
        })
        return bmp
    }
}
