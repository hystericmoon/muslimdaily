package id.haho.muslimdaily.feature.duas.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.AppPalette
import id.haho.muslimdaily.core.designsystem.theme.colorSchemeFor
import id.haho.muslimdaily.core.model.Dua
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * A dua as a shareable image: the Arabic, the Indonesian translation and the source with its
 * grading, on the app's glass in the light version of the user's palette. The source travels
 * with the dua, so a forwarded image still says where it comes from.
 */
object DuaShareCard {
    private const val WIDTH = 1080
    private const val PAD = 64f
    private const val CARD_PAD = 72f
    private const val MAX_HEIGHT = 4800
    private const val MIN_TEXT_SCALE = 0.5f

    /**
     * A long dua is drawn smaller rather than cut off, so the source and footer always make it
     * onto the image.
     */
    fun render(context: Context, dua: Dua, title: String, source: String, palette: AppPalette, textScale: Float = 1f): Bitmap {
        val scheme = colorSchemeFor(palette, dark = false)
        val res = context.resources
        val arabicFace = ResourcesCompat.getFont(context, if (dua.isQuran) R.font.amiri_quran else R.font.amiri_regular) ?: Typeface.SERIF
        val latinFace = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
        val inner = (WIDTH - 2 * PAD - 2 * CARD_PAD).toInt()

        fun layout(
            text: String,
            face: Typeface,
            size: Float,
            color: Int,
            rtl: Boolean = false,
            spacing: Float = 1.3f,
            bold: Boolean = false,
            align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        ): StaticLayout {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = if (bold) Typeface.create(face, Typeface.BOLD) else face
                textSize = size
                this.color = color
            }
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, inner)
                .setAlignment(align)
                .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setLineSpacing(0f, spacing)
                .setIncludePad(true)
                .build()
        }

        val onSurface = scheme.onSurface.toArgb()
        val muted = scheme.onSurfaceVariant.toArgb()
        val label = layout(title.uppercase(Locale.forLanguageTag("id")), latinFace, 30f * textScale, scheme.primary.toArgb(), bold = true)
        val arabic = layout(dua.arabic, arabicFace, 64f * textScale, onSurface, rtl = true, spacing = 1.3f)
        val translation = dua.translationId.takeIf { it.isNotBlank() }?.let { layout(it, latinFace, 36f * textScale, onSurface, spacing = 1.35f) }
        val sourceLayout = layout(source, latinFace, 28f * textScale, muted)
        val footer = layout(res.getString(R.string.share_footer), latinFace, 26f, muted, bold = true, align = Layout.Alignment.ALIGN_CENTER)

        val gap = 44f
        val cardHeight = CARD_PAD * 2 + label.height + gap + arabic.height + gap + 6f + gap +
            (translation?.let { it.height + gap } ?: 0f) + sourceLayout.height
        val height = (PAD * 2 + cardHeight + 28f + footer.height).toInt()
        if (height > MAX_HEIGHT && textScale > MIN_TEXT_SCALE) {
            return render(context, dua, title, source, palette, (textScale * 0.85f).coerceAtLeast(MIN_TEXT_SCALE))
        }

        val bmp = createBitmap(WIDTH, height)
        val c = Canvas(bmp)
        // The app's aurora: parchment base and two very soft glows.
        c.drawColor(scheme.surface.toArgb())
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        for ((x, y, color) in listOf(
            Triple(0f, 0f, scheme.primaryContainer.copy(alpha = 0.75f).toArgb()),
            Triple(WIDTH.toFloat(), height * 0.65f, scheme.tertiaryContainer.copy(alpha = 0.8f).toArgb()),
        )) {
            glow.shader = RadialGradient(x, y, WIDTH * 0.95f, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), glow)
        }

        // Frosted card with a top-lit rim.
        val card = RectF(PAD, PAD, WIDTH - PAD, PAD + cardHeight)
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8FFFFFF.toInt() })
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            shader = LinearGradient(0f, card.top, 0f, card.bottom, 0xFFFFFFFF.toInt(), 0x4DFFFFFF, Shader.TileMode.CLAMP)
        })

        var y = card.top + CARD_PAD
        fun draw(l: StaticLayout) {
            c.withTranslation(card.left + CARD_PAD, y) { l.draw(this) }
            y += l.height
        }
        draw(label)
        y += gap
        draw(arabic)
        y += gap
        c.drawRoundRect(RectF(card.left + CARD_PAD, y, card.left + CARD_PAD + 96f, y + 6f), 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC9A227.toInt() })
        y += 6f + gap
        translation?.let { draw(it); y += gap }
        draw(sourceLayout)

        y = card.bottom + 28f
        draw(footer)
        return bmp
    }

    /** Renders and hands the image to the share sheet, with the source as text alongside. */
    suspend fun share(context: Context, dua: Dua, title: String, source: String, palette: AppPalette) {
        val file = withContext(Dispatchers.Default) {
            val bmp = render(context, dua, title, source, palette)
            withContext(Dispatchers.IO) {
                File(context.cacheDir, "export").apply { mkdirs() }.resolve("doa-${dua.id}.png").apply {
                    outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }.also { bmp.recycle() }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$title\n$source")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_dua)))
    }
}
