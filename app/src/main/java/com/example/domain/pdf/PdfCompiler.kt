package com.example.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.domain.model.ImageFitMode
import com.example.domain.model.PaperMargin
import com.example.domain.model.PaperOrientation
import com.example.domain.model.PaperSize
import com.example.domain.model.PdfCompilerOptions
import com.example.domain.model.SelectedImage
import com.example.domain.model.TextToPdfOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfCompiler {

    suspend fun compileImagesToPdf(
        context: Context,
        images: List<SelectedImage>,
        options: PdfCompilerOptions,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val total = images.size

        try {
            for (index in images.indices) {
                onProgress(index + 1, total)
                val item = images[index]
                var bitmap = loadAndRotateBitmap(context, item.uri, item.rotationDegrees)
                    ?: continue

                // Optional compression to honor options.quality
                if (options.quality.qualityPercent < 100) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, options.quality.qualityPercent, stream)
                    val bytes = stream.toByteArray()
                    val compressedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (compressedBitmap != null) {
                        bitmap = compressedBitmap
                    }
                }

                val (pageWidth, pageHeight) = calculatePageDimensions(
                    bitmap.width,
                    bitmap.height,
                    options.paperSize,
                    options.orientation
                )

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw background
                canvas.drawColor(Color.WHITE)

                val marginPx = options.margin.paddingPt
                val footerReserve = if (options.showPageNumbers || options.addWatermark) 26 else 0
                val destRect = RectF(
                    marginPx.toFloat(),
                    marginPx.toFloat(),
                    (pageWidth - marginPx).toFloat(),
                    (pageHeight - marginPx - footerReserve).toFloat()
                )

                drawImageOnCanvas(canvas, bitmap, destRect, options.fitMode)

                // Draw Watermark and/or Page numbers at bottom
                val hasPageNumbers = options.showPageNumbers
                val hasWatermark = options.addWatermark && options.watermarkText.isNotBlank()

                if (hasPageNumbers || hasWatermark) {
                    val bottomY = (pageHeight - 10).toFloat()
                    if (hasWatermark) {
                        val watermarkPaint = Paint().apply {
                            color = Color.parseColor("#94A3B8") // soft subtle slate
                            textSize = 8.5f // very small, elegant and unobtrusive
                            textAlign = if (hasPageNumbers) Paint.Align.LEFT else Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            isAntiAlias = true
                        }
                        val watermarkX = if (hasPageNumbers) marginPx.toFloat().coerceAtLeast(16f) else (pageWidth / 2).toFloat()
                        canvas.drawText(options.watermarkText, watermarkX, bottomY, watermarkPaint)
                    }

                    if (hasPageNumbers) {
                        val textPaint = Paint().apply {
                            color = Color.parseColor("#64748B")
                            textSize = 9f
                            textAlign = if (hasWatermark) Paint.Align.RIGHT else Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        val pageX = if (hasWatermark) (pageWidth - marginPx.toFloat().coerceAtLeast(16f)) else (pageWidth / 2).toFloat()
                        val pageText = "${index + 1} / $total"
                        canvas.drawText(pageText, pageX, bottomY, textPaint)
                    }
                }

                // Draw header title if specified
                if (options.headerTitle.isNotBlank()) {
                    val headerPaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 10f
                        textAlign = Paint.Align.LEFT
                        isAntiAlias = true
                    }
                    canvas.drawText(options.headerTitle, marginPx.toFloat(), (marginPx - 4).toFloat(), headerPaint)
                }

                pdfDocument.finishPage(page)
                if (bitmap != null && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            // Save output file
            val outputDir = File(context.filesDir, "generated_pdfs").apply { mkdirs() }
            val fileName = if (options.outputFileName.isNotBlank()) {
                val clean = options.outputFileName.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
            } else {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                "Lumina_Doc_$timestamp.pdf"
            }

            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            outputFile
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun compileTextToPdf(
        context: Context,
        options: TextToPdfOptions
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        val pageWidth = options.paperSize.widthPt.let { if (it <= 0) 595 else it }
        val pageHeight = options.paperSize.heightPt.let { if (it <= 0) 842 else it }
        val margin = options.margin.paddingPt.toFloat()
        val contentWidth = (pageWidth - (margin * 2)).toInt()
        val contentHeight = pageHeight - (margin * 2) - 30f // Reserve 30pt for footer

        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#1E1B4B")
            textSize = options.fontSizeSp + 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.parseColor("#4B5563")
            textSize = options.fontSizeSp - 2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#1F2937")
            textSize = options.fontSizeSp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Measure body text layout
        val paragraphs = options.bodyText.split("\n")
        var pageNumber = 1
        var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = currentPage.canvas
        canvas.drawColor(Color.WHITE)

        var yOffset = margin

        // Draw title on page 1
        if (options.title.isNotBlank()) {
            val titleLayout = StaticLayout.Builder.obtain(
                options.title,
                0,
                options.title.length,
                titlePaint,
                contentWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

            canvas.save()
            canvas.translate(margin, yOffset)
            titleLayout.draw(canvas)
            canvas.restore()
            yOffset += titleLayout.height + 8f

            if (options.author.isNotBlank()) {
                val authorLayout = StaticLayout.Builder.obtain(
                    "By ${options.author}",
                    0,
                    "By ${options.author}".length,
                    metaPaint,
                    contentWidth
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                canvas.save()
                canvas.translate(margin, yOffset)
                authorLayout.draw(canvas)
                canvas.restore()
                yOffset += authorLayout.height + 12f
            }

            // Divider line
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 1.5f
            }
            canvas.drawLine(margin, yOffset, pageWidth - margin, yOffset, dividerPaint)
            yOffset += 16f
        }

        fun drawTextFooter(c: Canvas, pageNum: Int) {
            val bottomY = (pageHeight - 14).toFloat()
            val hasWatermark = options.addWatermark && options.watermarkText.isNotBlank()
            if (hasWatermark) {
                val watermarkPaint = Paint().apply {
                    color = Color.parseColor("#94A3B8")
                    textSize = 8.5f
                    textAlign = if (options.showPageNumbers) Paint.Align.LEFT else Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                val watermarkX = if (options.showPageNumbers) margin else (pageWidth / 2).toFloat()
                c.drawText(options.watermarkText, watermarkX, bottomY, watermarkPaint)
            }
            if (options.showPageNumbers) {
                val pPaint = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 9f
                    textAlign = if (hasWatermark) Paint.Align.RIGHT else Paint.Align.CENTER
                    isAntiAlias = true
                }
                val pageX = if (hasWatermark) (pageWidth - margin) else (pageWidth / 2).toFloat()
                c.drawText("Page $pageNum", pageX, bottomY, pPaint)
            }
        }

        for (paragraph in paragraphs) {
            val textToRender = if (paragraph.isEmpty()) " " else paragraph
            val layout = StaticLayout.Builder.obtain(
                textToRender,
                0,
                textToRender.length,
                bodyPaint,
                contentWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, options.lineSpacingMultiplier)
                .build()

            // Check if this paragraph fits on current page
            if (yOffset + layout.height > pageHeight - margin - 30f && yOffset > margin + 50f) {
                // Finish current page
                drawTextFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)

                // Start new page
                pageNumber++
                currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = currentPage.canvas
                canvas.drawColor(Color.WHITE)
                yOffset = margin
            }

            canvas.save()
            canvas.translate(margin, yOffset)
            layout.draw(canvas)
            canvas.restore()
            yOffset += layout.height + if (paragraph.isEmpty()) 8f else 12f
        }

        // Finish last page
        drawTextFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        val outputDir = File(context.filesDir, "generated_pdfs").apply { mkdirs() }
        val fileName = if (options.outputFileName.isNotBlank()) {
            val clean = options.outputFileName.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
        } else {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            "Lumina_Text_$timestamp.pdf"
        }

        val outputFile = File(outputDir, fileName)
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    private fun calculatePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        paperSize: PaperSize,
        orientation: PaperOrientation
    ): Pair<Int, Int> {
        if (paperSize == PaperSize.FIT_IMAGE) {
            return Pair(bitmapWidth, bitmapHeight)
        }

        var width = paperSize.widthPt
        var height = paperSize.heightPt

        val isLandscape = when (orientation) {
            PaperOrientation.PORTRAIT -> false
            PaperOrientation.LANDSCAPE -> true
            PaperOrientation.AUTO -> bitmapWidth > bitmapHeight
        }

        if (isLandscape && width < height) {
            val temp = width
            width = height
            height = temp
        } else if (!isLandscape && width > height) {
            val temp = width
            width = height
            height = temp
        }

        return Pair(width, height)
    }

    private fun drawImageOnCanvas(
        canvas: Canvas,
        bitmap: Bitmap,
        destRect: RectF,
        fitMode: ImageFitMode
    ) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val dw = destRect.width()
        val dh = destRect.height()

        val scale: Float
        val dx: Float
        val dy: Float

        if (fitMode == ImageFitMode.CONTAIN) {
            scale = min(dw / bw, dh / bh)
            dx = destRect.left + (dw - bw * scale) / 2f
            dy = destRect.top + (dh - bh * scale) / 2f
        } else {
            // COVER
            scale = max(dw / bw, dh / bh)
            dx = destRect.left + (dw - bw * scale) / 2f
            dy = destRect.top + (dh - bh * scale) / 2f
        }

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }

        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        canvas.save()
        canvas.clipRect(destRect)
        canvas.drawBitmap(bitmap, matrix, paint)
        canvas.restore()
    }

    private fun loadAndRotateBitmap(context: Context, uri: Uri, rotationDegrees: Int): Bitmap? {
        fun openStream(): java.io.InputStream? {
            return try {
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) file.inputStream() else context.contentResolver.openInputStream(uri)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            } catch (e: Exception) {
                null
            }
        }

        val inputStream = openStream() ?: return null
        
        // Decode bounds first to prevent OOM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        try { inputStream.close() } catch (_: Exception) {}

        val maxDimension = 2048
        var sampleSize = 1
        while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val stream2 = openStream() ?: return null
        val decoded = BitmapFactory.decodeStream(stream2, null, decodeOptions)
        try { stream2.close() } catch (_: Exception) {}

        if (decoded == null) return null

        if (rotationDegrees % 360 == 0) {
            return decoded
        }

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) {
            decoded.recycle()
        }
        return rotated
    }
}
