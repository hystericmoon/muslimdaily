package id.haho.muslimdaily.feature.timetable

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.colorSchemeFor
import id.haho.muslimdaily.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A timetable as plain text, ready to draw: the screen and the shared file read the same rows. */
data class TimetableSheet(
    val title: String,
    /** Place and calculation method. */
    val subtitle: String,
    val columns: List<String>,
    val rows: List<List<String>>,
    val note: String? = null,
    val footer: String,
)

enum class ShareFormat(val extension: String, val mime: String) {
    PNG("png", "image/png"),
    PDF("pdf", "application/pdf"),
}

/**
 * Draws a [TimetableSheet] as an image or a one-page A4 PDF through the existing FileProvider.
 * Drawn in the light version of the user's palette on white: it is meant to be printed or
 * forwarded, so every text is the palette's on-surface colour, never a muted one.
 */
object TimetableShare {
    private const val WIDTH = 1080
    private const val PAD = 48f
    private const val TOP = 190f
    private const val HEADER_H = 64f
    private const val FOOT = 96f
    private const val ROW_H = 60f
    private const val PDF_W = 595
    private const val PDF_H = 842

    /** The height the image needs for [rows] lines. */
    private fun imageHeight(rows: Int): Int = (TOP + HEADER_H + rows * ROW_H + FOOT).toInt()

    fun render(context: Context, sheet: TimetableSheet, palette: HahoPalette, width: Int, height: Int, canvas: Canvas) {
        val scheme = colorSchemeFor(palette, dark = false)
        val face = ResourcesCompat.getFont(context, id.haho.design.R.font.haho_plus_jakarta_sans) ?: Typeface.SANS_SERIF
        val ink = scheme.onSurface.toArgb()
        fun paint(size: Float, color: Int = ink, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = if (bold) Typeface.create(face, Typeface.BOLD) else face
                textSize = size
                this.color = color
                textAlign = align
            }

        canvas.drawColor(0xFFFFFFFF.toInt())
        val w = width.toFloat()
        canvas.drawText(sheet.title, PAD, PAD + 52f, paint(46f, scheme.primary.toArgb(), bold = true))
        canvas.drawText(sheet.subtitle, PAD, PAD + 52f + 48f, paint(28f))

        val rowH = minOf(ROW_H, (height - TOP - HEADER_H - FOOT) / sheet.rows.size.coerceAtLeast(1))
        val textSize = minOf(26f, rowH * 0.5f)
        val inner = w - 2 * PAD
        // The first two columns (date, Hijri) are wider than the six times.
        val weights = List(sheet.columns.size) { when (it) { 0 -> 1.4f; 1 -> 1.8f; else -> 1f } }
        val total = weights.sum()
        val xs = weights.runningFold(0f) { acc, wt -> acc + inner * wt / total }

        val band = Paint().apply { color = scheme.primaryContainer.toArgb() }
        canvas.drawRect(PAD, TOP, w - PAD, TOP + HEADER_H, band)
        val head = paint(textSize, scheme.onPrimaryContainer.toArgb(), bold = true)
        sheet.columns.forEachIndexed { i, label ->
            drawCell(canvas, label, xs[i], xs[i + 1], TOP + HEADER_H / 2 + textSize * 0.35f, head, left = i < 2)
        }
        val zebra = Paint().apply { color = scheme.surfaceVariant.toArgb(); alpha = 90 }
        val cell = paint(textSize)
        sheet.rows.forEachIndexed { r, cells ->
            val y = TOP + HEADER_H + r * rowH
            if (r % 2 == 1) canvas.drawRect(PAD, y, w - PAD, y + rowH, zebra)
            cells.forEachIndexed { i, text ->
                drawCell(canvas, text, xs[i], xs[i + 1], y + rowH / 2 + textSize * 0.35f, cell, left = i < 2)
            }
        }
        val bottom = TOP + HEADER_H + sheet.rows.size * rowH
        sheet.note?.let { canvas.drawText(it, PAD, bottom + 40f, paint(24f)) }
        canvas.drawText(sheet.footer, w / 2, bottom + 80f, paint(24f, bold = true, align = Paint.Align.CENTER))
    }

    private fun drawCell(canvas: Canvas, text: String, x0: Float, x1: Float, baseline: Float, paint: Paint, left: Boolean) {
        val pad = 14f
        if (left) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(text, PAD + x0 + pad, baseline, paint)
        } else {
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, PAD + (x0 + x1) / 2, baseline, paint)
        }
    }

    /** Draws the sheet to a file in the export cache and offers it to the share sheet. */
    suspend fun share(context: Context, sheet: TimetableSheet, palette: HahoPalette, format: ShareFormat, name: String) {
        val file = withContext(Dispatchers.Default) {
            val dir = File(context.cacheDir, "export").apply { mkdirs() }
            val out = dir.resolve("$name.${format.extension}")
            when (format) {
                ShareFormat.PNG -> {
                    val h = imageHeight(sheet.rows.size)
                    val bmp = createBitmap(WIDTH, h)
                    render(context, sheet, palette, WIDTH, h, Canvas(bmp))
                    withContext(Dispatchers.IO) { out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                    bmp.recycle()
                }
                ShareFormat.PDF -> {
                    val doc = PdfDocument()
                    val page = doc.startPage(PdfDocument.PageInfo.Builder(PDF_W, PDF_H, 1).create())
                    val scale = PDF_W / WIDTH.toFloat()
                    page.canvas.scale(scale, scale)
                    render(context, sheet, palette, WIDTH, (PDF_H / scale).toInt(), page.canvas)
                    doc.finishPage(page)
                    withContext(Dispatchers.IO) { out.outputStream().use { doc.writeTo(it) } }
                    doc.close()
                }
            }
            out
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${sheet.title}\n${sheet.subtitle}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.timetable_share)))
    }
}
