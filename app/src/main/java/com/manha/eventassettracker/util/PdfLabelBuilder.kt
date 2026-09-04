package com.manha.eventassettracker.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import kotlin.math.ceil
import kotlin.math.min

data class LabelItem(
    val assetId: String,
    val name: String,
    val category: String,
    val sizeCm: Int
)

/** Builds a printable A4 sheet of QR labels, drawn natively (no WebView involved). */
object PdfLabelBuilder {
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 24f
    private const val HEADER_HEIGHT = 34f
    private const val COLUMNS = 3
    private const val ROWS = 4

    fun build(title: String, items: List<LabelItem>): PdfDocument {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 13f; isFakeBoldText = true }

        if (items.isEmpty()) {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
            val page = document.startPage(info)
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawText("$title — no labels to print.", MARGIN, PAGE_HEIGHT / 2f, titlePaint)
            document.finishPage(page)
            return document
        }

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true }
        val idPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 12f; isFakeBoldText = true }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 8f }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }

        val cellWidth = (PAGE_WIDTH - 2 * MARGIN) / COLUMNS
        val cellHeight = (PAGE_HEIGHT - 2 * MARGIN - HEADER_HEIGHT) / ROWS
        val perPage = COLUMNS * ROWS
        val pageCount = ceil(items.size / perPage.toDouble()).toInt()

        for (pageIndex in 0 until pageCount) {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageIndex + 1).create()
            val page = document.startPage(info)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.drawText("$title  (Page ${pageIndex + 1}/$pageCount)", MARGIN, MARGIN, titlePaint)

            val pageItems = items.drop(pageIndex * perPage).take(perPage)
            for ((index, item) in pageItems.withIndex()) {
                val col = index % COLUMNS
                val row = index / COLUMNS
                val cellLeft = MARGIN + col * cellWidth
                val cellTop = MARGIN + HEADER_HEIGHT + row * cellHeight
                val cellRight = cellLeft + cellWidth - 8f
                val cellBottom = cellTop + cellHeight - 8f
                canvas.drawRect(RectF(cellLeft, cellTop, cellRight, cellBottom), borderPaint)

                val qrSize = min(cellWidth, cellHeight) * 0.55f
                val qrBitmap = generateQrBitmap(QrPayload(item.assetId, item.name, item.category).encode(), 256)
                val qrLeft = cellLeft + (cellWidth - qrSize) / 2f
                val qrTop = cellTop + 6f
                canvas.drawBitmap(qrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

                val textY = qrTop + qrSize + 14f
                drawCenteredText(canvas, item.name, cellLeft, cellRight, textY, namePaint)
                drawCenteredText(canvas, item.assetId, cellLeft, cellRight, textY + 14f, idPaint)
                drawCenteredText(
                    canvas,
                    "${item.category.ifBlank { "Asset" }} • ${item.sizeCm}x${item.sizeCm} cm",
                    cellLeft, cellRight, textY + 26f, smallPaint
                )
                qrBitmap.recycle()
            }

            document.finishPage(page)
        }
        return document
    }

    private fun drawCenteredText(canvas: Canvas, text: String, left: Float, right: Float, y: Float, paint: Paint) {
        val maxWidth = right - left
        val truncated = truncateToWidth(text, paint, maxWidth)
        val textWidth = paint.measureText(truncated)
        val x = left + (maxWidth - textWidth) / 2f
        canvas.drawText(truncated, x, y, paint)
    }

    private fun truncateToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
