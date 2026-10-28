package id.haho.muslimdaily.widget

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import id.haho.muslimdaily.R
import id.haho.muslimdaily.data.settings.WIDGET_OPACITY_DEFAULT
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Paints the widget's bitmaps: the translucent glass it sits on, the sun or the moon, and the bar
 * of the day. Launchers can't blur what is behind a widget, so the glass is a neutral graphite
 * tint, like the Google search bar's, with a faint sheen and an even hairline edge; the wallpaper
 * shows through it.
 */
object GlassPainter {

    /** The glass's colour, without its alpha: Google's dark surface graphite. */
    private const val GRAPHITE = 0x1F1F1F

    /**
     * The glass at the default opacity (85%): enough that muted text (65% white) still passes AA
     * where the wallpaper behind is pure white, sheen included (see GlassContrastTest). Readers
     * can make it lighter in Settings → Display.
     */
    val GLASS: Int = withOpacity(WIDGET_OPACITY_DEFAULT)
    const val ACCENT = 0xFFEEF26B.toInt()
    private const val TRACK = 0x38FFFFFF

    fun withOpacity(opacity: Float): Int = ((opacity.coerceIn(0f, 1f) * 255).roundToInt() shl 24) or GRAPHITE

    fun glass(widthPx: Int, heightPx: Int, density: Float, cornerPx: Float, opacity: Float = WIDGET_OPACITY_DEFAULT): Bitmap {
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val bmp = createBitmap(widthPx, heightPx)
        val c = Canvas(bmp)
        val box = RectF(0f, 0f, w, h)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = withOpacity(opacity)
        c.drawRoundRect(box, cornerPx, cornerPx, p)
        // The sheen: light catching the glass from the top left, fading across it.
        p.shader = LinearGradient(0f, 0f, w * 0.6f, h, intArrayOf(0x0FFFFFFF, 0x05FFFFFF, 0x08FFFFFF), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(box, cornerPx, cornerPx, p)
        // The edge: an even hairline all round, as on the search bar.
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            color = 0x24FFFFFF
        }
        val half = edge.strokeWidth / 2
        c.drawRoundRect(RectF(half, half, w - half, h - half), cornerPx - half, cornerPx - half, edge)
        return bmp
    }

    /**
     * The sun, with its glow, in a square of [sizePx]: NASA/SDO's visible-light photo of the
     * disc (granulation and all), darkened and reddened towards the limb as the eye sees it,
     * since that product has the limb darkening taken out.
     */
    fun sun(resources: Resources, sizePx: Int): Bitmap {
        val n = sizePx.coerceAtLeast(1)
        val bmp = createBitmap(n, n)
        val c = Canvas(bmp)
        val o = n / 2f
        val r = n * 0.3f
        val k = r / o
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(o, o, o, intArrayOf(0x8CFFB040.toInt(), 0x59FF9A30, 0x1AFF8020, 0x00FF8020), floatArrayOf(k, k * 1.1f, k + (1 - k) * 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(o, o, o, p)
        val photo = texture(resources, R.drawable.widget_sun)
        disc(c, n, r) { x, y, z ->
            val px = photo.sample((x * 0.985f + 1) / 2, (y * 0.985f + 1) / 2)
            // I(μ) = 1 − u(1 − μ), with the light warmer at the centre and redder at the limb.
            val i = 1f - 0.32f * (1f - z)
            val hot = 0.35f * z * z
            val red = 0.85f * (1f - z)
            // The photo's texture, with its contrast raised so the granulation shows at widget size.
            val grain = 1f + 1.6f * ((((px shr 8) and 0xFF) - 192f) / 255f)
            fun ch(v: Int, centre: Float, limb: Float) = lerp(lerp(v.toFloat(), centre, hot), limb, red) * i * grain
            rgb(ch((px shr 16) and 0xFF, 255f, 250f), ch((px shr 8) and 0xFF, 226f, 122f), ch(px and 0xFF, 140f, 22f))
        }
        return bmp
    }

    /**
     * The moon at [phase] (0 new, 0.5 full), in a square of [sizePx]: NASA's LRO colour map of
     * the surface wrapped on a sphere with the near side facing us, its contrast raised to look
     * as the moon does at night, and lit from the sun's direction for the phase (Lommel–Seeliger,
     * so it is nearly flat at full), with the unlit part faint in earthshine.
     */
    fun moon(resources: Resources, sizePx: Int, phase: Float): Bitmap {
        val n = sizePx.coerceAtLeast(1)
        val bmp = createBitmap(n, n)
        val c = Canvas(bmp)
        val o = n / 2f
        val r = n * 0.3f
        val k = r / o
        val lit = ((1 - cos(2 * PI * phase)) / 2).toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val glow = (0x0C + 0x34 * lit).toInt()
        p.shader = RadialGradient(o, o, o, intArrayOf((glow shl 24) or 0xC8D4F0, ((glow / 3) shl 24) or 0x8C9CC8, 0x008C9CC8), floatArrayOf(k, k + (1 - k) * 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(o, o, o, p)
        val map = texture(resources, R.drawable.widget_moon_map)
        // The sun's direction: from the right while waxing, from in front at full, from the left waning.
        val theta = 2 * PI * phase
        val lx = sin(theta).toFloat()
        val lz = -cos(theta).toFloat()
        disc(c, n, r) { x, y, z ->
            val lon = atan2(x, z)
            val lat = asin((-y).coerceIn(-1f, 1f))
            val px = map.sample(0.5f + lon / (2 * PI.toFloat()), 0.5f - lat / PI.toFloat())
            val grey = (((px shr 16) and 0xFF) * 0.3f + ((px shr 8) and 0xFF) * 0.59f + (px and 0xFF) * 0.11f) / 255f
            fun ch(v: Int): Float {
                // A little of the map's colour, mostly its brightness, with the contrast raised.
                val mixed = lerp(grey, v / 255f, 0.35f)
                return (mixed.pow(1.8f) * 1.18f).coerceIn(0f, 1f)
            }
            val cosI = x * lx + z * lz
            val shade = if (cosI <= 0f) 0f else (2f * cosI / (cosI + z.coerceAtLeast(0.05f))).coerceAtMost(1.15f) * smooth(-0.02f, 0.07f, cosI)
            val light = shade + 0.03f * (1f - smooth(-0.02f, 0.07f, cosI))
            rgb(ch((px shr 16) and 0xFF) * light * 255f, ch((px shr 8) and 0xFF) * light * 255f, ch(px and 0xFF) * light * 255f)
        }
        return bmp
    }

    /** A decoded image the sun and moon are drawn from, kept after its first use. */
    private class Texture(val w: Int, val h: Int, val px: IntArray) {
        /** Bilinear: [u] wraps round (longitude), [v] stops at the edges. */
        fun sample(u: Float, v: Float): Int {
            val fx = ((u % 1f + 1f) % 1f) * w - 0.5f
            val fy = (v.coerceIn(0f, 1f) * h - 0.5f).coerceIn(0f, h - 1f)
            val x0 = floor(fx).toInt()
            val y0 = fy.toInt().coerceAtMost(h - 2)
            val tx = fx - x0
            val ty = fy - y0
            fun at(x: Int, y: Int) = px[y * w + ((x % w) + w) % w]
            fun mix(a: Int, b: Int, t: Float, shift: Int) = lerp(((a shr shift) and 0xFF).toFloat(), ((b shr shift) and 0xFF).toFloat(), t)
            val a = at(x0, y0)
            val b = at(x0 + 1, y0)
            val cc = at(x0, y0 + 1)
            val d = at(x0 + 1, y0 + 1)
            fun ch(shift: Int) = lerp(mix(a, b, tx, shift), mix(cc, d, tx, shift), ty).toInt()
            return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }

    private val textures = HashMap<Int, Texture>()

    private fun texture(resources: Resources, id: Int): Texture = synchronized(textures) {
        textures.getOrPut(id) {
            val b = BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inScaled = false })
            val px = IntArray(b.width * b.height)
            b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
            Texture(b.width, b.height, px).also { b.recycle() }
        }
    }

    /**
     * Fills a disc of radius [r] in the middle of [c] pixel by pixel: [color] gets the point on the
     * unit sphere (x right, y down, z towards the viewer). The edge is anti-aliased.
     */
    private inline fun disc(c: Canvas, n: Int, r: Float, color: (Float, Float, Float) -> Int) {
        val o = n / 2f
        val px = IntArray(n * n)
        for (j in 0 until n) for (i in 0 until n) {
            val x = (i + 0.5f - o) / r
            val y = (j + 0.5f - o) / r
            val d2 = x * x + y * y
            val cover = ((1f - sqrt(d2)) * r + 0.5f).coerceIn(0f, 1f)
            if (cover <= 0f) continue
            val rgb = color(x, y, sqrt((1f - d2).coerceAtLeast(0f)))
            px[j * n + i] = ((cover * 255).toInt() shl 24) or (rgb and 0xFFFFFF)
        }
        val layer = createBitmap(n, n)
        layer.setPixels(px, 0, n, 0, 0, n, n)
        c.drawBitmap(layer, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        layer.recycle()
    }

    private fun rgb(r: Float, g: Float, b: Float): Int =
        (r.toInt().coerceIn(0, 255) shl 16) or (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
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
}
