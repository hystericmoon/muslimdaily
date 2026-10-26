package id.haho.muslimdaily.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import id.haho.muslimdaily.domain.prayer.PrayerSchedule
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The sky at one moment, as a background: four colours from top to bottom, one soft glow where
 * the light comes from, and how much the stars show. No sun, moon or clouds: text sits anywhere
 * on it, so every colour is dark enough for white text at 4.5:1 (see SkyContrastTest).
 *
 * The glow's centre and radii are fractions of the widget's width and height.
 */
data class SkyLook(
    val stops: IntArray,
    val glow: Int,
    val glowX: Float,
    val glowY: Float,
    val glowRx: Float,
    val glowRy: Float,
    val stars: Float,
) {
    override fun equals(other: Any?): Boolean = other is SkyLook && stops.contentEquals(other.stops) &&
        glow == other.glow && glowX == other.glowX && glowY == other.glowY && glowRx == other.glowRx && glowRy == other.glowRy && stars == other.stars

    override fun hashCode(): Int = stops.contentHashCode() * 31 + glow

    fun mix(to: SkyLook, t: Float): SkyLook = SkyLook(
        stops = IntArray(stops.size) { lerpColor(stops[it], to.stops[it], t) },
        glow = lerpColor(glow, to.glow, t),
        glowX = lerp(glowX, to.glowX, t),
        glowY = lerp(glowY, to.glowY, t),
        glowRx = lerp(glowRx, to.glowRx, t),
        glowRy = lerp(glowRy, to.glowRy, t),
        stars = lerp(stars, to.stars, t),
    )

    private companion object {
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        fun lerpColor(a: Int, b: Int, t: Float): Int {
            fun ch(shift: Int) = lerp(((a ushr shift) and 0xFF).toFloat(), ((b ushr shift) and 0xFF).toFloat(), t).roundToInt()
            return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}

/** The sky through the day, keyed to the prayer times and blended between them. */
object Sky {
    val NIGHT = SkyLook(intArrayOf(0xFF050A1C.toInt(), 0xFF0B1530.toInt(), 0xFF141E3A.toInt(), 0xFF1F2440.toInt()), 0x1FC88C6E, 0.5f, 1.15f, 1.2f, 0.4f, 1f)
    val PREDAWN = SkyLook(intArrayOf(0xFF08122A.toInt(), 0xFF142450.toInt(), 0xFF2A3360.toInt(), 0xFF3A3352.toInt()), 0x26D67850, 0.25f, 1.1f, 0.9f, 0.55f, 0.7f)
    val DAWN = SkyLook(intArrayOf(0xFF0A1734.toInt(), 0xFF1C2E5C.toInt(), 0xFF45406E.toInt(), 0xFF6E4652.toInt()), 0x66D67850, 0.25f, 1.1f, 0.9f, 0.55f, 0.35f)
    val MORNING = SkyLook(intArrayOf(0xFF154A8C.toInt(), 0xFF2462A6.toInt(), 0xFF3473B0.toInt(), 0xFF3D78AE.toInt()), 0x24FFF0D2, 0.18f, -0.15f, 0.8f, 0.7f, 0f)
    val NOON = SkyLook(intArrayOf(0xFF0F4282.toInt(), 0xFF1C5AA2.toInt(), 0xFF2B6AAE.toInt(), 0xFF3672AE.toInt()), 0x1AFFFFFF, 0.5f, -0.2f, 0.9f, 0.6f, 0f)
    val GOLDEN = SkyLook(intArrayOf(0xFF1D3566.toInt(), 0xFF3D4774.toInt(), 0xFF6E5260.toInt(), 0xFF82543F.toInt()), 0x4DE68C46, 0.85f, 1.1f, 0.9f, 0.55f, 0f)
    val DUSK = SkyLook(intArrayOf(0xFF0C1330.toInt(), 0xFF262654.toInt(), 0xFF4C3054.toInt(), 0xFF6A3838.toInt()), 0x4DE0704A, 0.85f, 1.12f, 0.9f, 0.5f, 0.35f)

    fun at(now: Instant, day: PrayerSchedule): SkyLook {
        fun m(minutes: Long) = Duration.ofMinutes(minutes)
        val keys = listOf(
            day.fajr.minus(m(60)) to NIGHT,
            day.fajr to PREDAWN,
            day.sunrise.minus(m(15)) to DAWN,
            day.sunrise.plus(m(45)) to MORNING,
            day.dhuhr to NOON,
            day.asr to NOON,
            day.maghrib.minus(m(30)) to GOLDEN,
            day.maghrib.plus(m(15)) to DUSK,
            day.isha to NIGHT,
        )
        if (!now.isAfter(keys.first().first)) return keys.first().second
        for ((a, b) in keys.zipWithNext()) {
            if (now.isBefore(b.first)) {
                val span = Duration.between(a.first, b.first).toMillis().coerceAtLeast(1)
                val t = Duration.between(a.first, now).toMillis().toFloat() / span
                return a.second.mix(b.second, t.coerceIn(0f, 1f))
            }
        }
        return keys.last().second
    }
}

/**
 * Paints a [SkyLook] as the widget's background bitmap, with rounded corners and a soft rim, and
 * optionally the card the text sits on: smoked glass inset from the edge, so the sky shows as a
 * frame and through the card, with a hairline edge and bracket corners.
 */
object SkyPainter {

    /** The card: dark enough that muted text and the accent pass AA on any sky behind it. */
    const val CARD = 0xA80E1218.toInt()
    const val ACCENT = 0xFFEEF26B.toInt()
    private const val CARD_EDGE = 0x14FFFFFF
    private const val BRACKET = 0xD9FFFFFF.toInt()
    private const val TRACK = 0x38FFFFFF

    fun paint(widthPx: Int, heightPx: Int, density: Float, look: SkyLook, cornerPx: Float, stars: Boolean = true, cardInsetPx: Float = 0f): Bitmap {
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val bmp = createBitmap(widthPx, heightPx)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.shader = LinearGradient(0f, 0f, 0f, h, look.stops, floatArrayOf(0f, 0.42f, 0.78f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)

        // The glow is an ellipse: a circular gradient squashed vertically about its centre.
        val cx = look.glowX * w
        val cy = look.glowY * h
        val rx = max(1f, look.glowRx * w)
        val ry = max(1f, look.glowRy * h)
        val clear = look.glow and 0x00FFFFFF
        p.shader = RadialGradient(cx, cy, rx, intArrayOf(look.glow, clear, clear), floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(1f, ry / rx, cx, cy) })
        }
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        if (stars && look.stars > 0.01f) stars(c, w, h, density, look.stars)

        val framed = roundAndRim(bmp, w, h, density, cornerPx)
        if (cardInsetPx > 0f) card(Canvas(framed), w, h, density, cardInsetPx, (cornerPx - cardInsetPx).coerceAtLeast(2f * density))
        return framed
    }

    private fun card(c: Canvas, w: Float, h: Float, d: Float, inset: Float, corner: Float) {
        val box = RectF(inset, inset, w - inset, h - inset)
        c.drawRoundRect(box, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD })
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
            color = CARD_EDGE
        }
        c.drawRoundRect(RectF(box.left + d / 2, box.top + d / 2, box.right - d / 2, box.bottom - d / 2), corner, corner, edge)
        // Brackets: each corner's curve plus a short straight run on both sides, in the margin
        // between the card and the sky's edge, so they frame the card instead of crowding its text.
        val bracket = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * d
            strokeCap = Paint.Cap.ROUND
            color = BRACKET
        }
        val run = 4f * d
        val half = bracket.strokeWidth / 2
        val out = inset / 2
        val l = box.left - out + half
        val t = box.top - out + half
        val r = box.right + out - half
        val b = box.bottom + out - half
        val k = corner + out - half
        fun arc(cx: Float, cy: Float, start: Float) = RectF(cx - k, cy - k, cx + k, cy + k).let { oval ->
            Path().apply { arcTo(oval, start, 90f, true) }
        }
        listOf(
            Triple(l + k, t + k, 180f), Triple(r - k, t + k, 270f), Triple(r - k, b - k, 0f), Triple(l + k, b - k, 90f),
        ).forEach { (cx, cy, start) -> c.drawPath(arc(cx, cy, start), bracket) }
        c.drawLine(l, t + k, l, t + k + run, bracket)
        c.drawLine(l + k, t, l + k + run, t, bracket)
        c.drawLine(r - k - run, t, r - k, t, bracket)
        c.drawLine(r, t + k, r, t + k + run, bracket)
        c.drawLine(r, b - k - run, r, b - k, bracket)
        c.drawLine(r - k - run, b, r - k, b, bracket)
        c.drawLine(l + k, b, l + k + run, b, bracket)
        c.drawLine(l, b - k - run, l, b - k, bracket)
    }

    /**
     * The progress bar: the accent from the start up to [progress] along [stopsPx] (the x of each
     * stop, under its label), then a dashed track to the end.
     */
    fun bar(widthPx: Int, heightPx: Int, density: Float, stopsPx: FloatArray, progress: Float): Bitmap {
        val bmp = createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1))
        val c = Canvas(bmp)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val pos = progress.coerceIn(0f, 1f) * (stopsPx.size - 1)
        val i = pos.toInt().coerceAtMost(stopsPx.size - 2)
        val end = if (progress >= 1f) w else stopsPx[i] + (stopsPx[i + 1] - stopsPx[i]) * (pos - i)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = density
        p.color = ACCENT
        if (end > 0f) c.drawRoundRect(RectF(0f, 0f, end, h), radius, radius, p)
        p.color = TRACK
        val dash = 2f * density
        val gap = 2f * density
        var x = end + gap * 1.5f
        while (x + dash <= w) {
            c.drawRect(x, 0f, x + dash, h, p)
            x += dash + gap
        }
        return bmp
    }

    /** A plain night sky, for a widget with nothing to show yet. */
    fun backdrop(widthPx: Int, heightPx: Int, density: Float, cornerPx: Float): Bitmap =
        paint(widthPx, heightPx, density, Sky.NIGHT, cornerPx)

    private fun stars(c: Canvas, w: Float, h: Float, d: Float, strength: Float) {
        val rnd = Random(7)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = ((w * h) / (d * d * 900f)).toInt().coerceIn(6, 40)
        repeat(count) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h * 0.75f
            val r = (0.5f + rnd.nextFloat() * 0.6f) * d
            // Faint: stars are texture behind text, not points to look at.
            p.color = Color.argb(((70 + rnd.nextInt(90)) * strength).toInt(), 255, 255, 255)
            c.drawCircle(x, y, r, p)
        }
    }

    /**
     * Rounded corners (launchers before Android 12 draw widgets edge to edge) and a soft rim. The
     * sky is drawn through a shader into an anti-aliased rounded rectangle on a clear bitmap, so
     * the corners are truly transparent.
     */
    private fun roundAndRim(sky: Bitmap, w: Float, h: Float, d: Float, corner: Float): Bitmap {
        val bmp = createBitmap(sky.width, sky.height)
        val c = Canvas(bmp)
        c.drawRoundRect(RectF(0f, 0f, w, h), corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(sky, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) })
        sky.recycle()
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
            shader = LinearGradient(0f, 0f, 0f, h, 0x40FFFFFF, 0x0DFFFFFF, Shader.TileMode.CLAMP)
        }
        val inset = rim.strokeWidth / 2
        c.drawRoundRect(RectF(inset, inset, w - inset, h - inset), corner, corner, rim)
        return bmp
    }
}
