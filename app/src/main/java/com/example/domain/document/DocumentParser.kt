package com.example.domain.document

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
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.domain.ocr.OcrLanguageScript
import com.example.domain.ocr.OcrScanner
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlin.math.min

object DocumentParser {

    suspend fun parseDocument(
        context: Context,
        uri: Uri,
        fileNameOverride: String? = null
    ): ParsedDocument = withContext(Dispatchers.IO) {
        var name = fileNameOverride ?: "Document"
        var size = 0L

        try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) {
                    name = f.name
                    size = f.length()
                }
            } else {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {}

        val docType = detectDocumentType(name, uri, context)
        val title = name.substringBeforeLast(".")

        when (docType) {
            DocumentType.PDF -> parsePdf(context, uri, name, size, title)
            DocumentType.DOCX -> parseDocx(context, uri, name, size, title)
            DocumentType.IMAGE -> parseImage(context, uri, name, size, title)
            else -> parseTextBased(context, uri, name, size, title, docType)
        }
    }

    private fun detectDocumentType(name: String, uri: Uri, context: Context): DocumentType {
        val lower = name.lowercase()
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase() ?: ""

        return when {
            lower.endsWith(".pdf") || mime.contains("pdf") -> DocumentType.PDF
            lower.endsWith(".docx") || mime.contains("wordprocessingml") -> DocumentType.DOCX
            lower.endsWith(".doc") || mime.contains("msword") -> DocumentType.DOC
            lower.endsWith(".md") -> DocumentType.MARKDOWN
            lower.endsWith(".csv") -> DocumentType.CSV
            lower.endsWith(".txt") || lower.endsWith(".log") || mime.contains("text/plain") -> DocumentType.TEXT
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || mime.startsWith("image/") -> DocumentType.IMAGE
            else -> DocumentType.OTHER
        }
    }

    private fun parsePdf(
        context: Context,
        uri: Uri,
        name: String,
        size: Long,
        title: String
    ): ParsedDocument {
        val pages = mutableListOf<DocumentPage>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = if (uri.scheme == "file") {
                ParcelFileDescriptor.open(File(uri.path ?: ""), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val count = minOf(renderer.pageCount, 60)
                for (i in 0 until count) {
                    val page = renderer.openPage(i)
                    val scale = 1.5f
                    val w = (page.width * scale).toInt()
                    val h = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pages.add(DocumentPage(pageIndex = i, bitmap = bitmap))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }

        return ParsedDocument(
            uri = uri,
            fileName = name,
            documentType = DocumentType.PDF,
            fileSizeBytes = size,
            title = title,
            bodyText = "",
            pages = pages,
            watermarkText = "created by lumina",
            addWatermark = false
        )
    }

    private fun parseDocx(
        context: Context,
        uri: Uri,
        name: String,
        size: Long,
        title: String
    ): ParsedDocument {
        val textBuilder = StringBuilder()
        val pages = mutableListOf<DocumentPage>()

        try {
            val inputStream = openStream(context, uri)
            if (inputStream != null) {
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName == "word/document.xml") {
                            val xmlContent = zis.bufferedReader(Charsets.UTF_8).readText()
                            val parsedText = extractTextFromDocxXml(xmlContent)
                            textBuilder.append(parsedText)
                        } else if (entryName.startsWith("word/media/") &&
                            (entryName.endsWith(".png", true) || entryName.endsWith(".jpg", true) || entryName.endsWith(".jpeg", true))
                        ) {
                            val bytes = zis.readBytes()
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                pages.add(DocumentPage(pageIndex = pages.size, bitmap = bitmap))
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            textBuilder.append("Could not fully parse Word file: ${e.localizedMessage}")
        }

        return ParsedDocument(
            uri = uri,
            fileName = name,
            documentType = DocumentType.DOCX,
            fileSizeBytes = size,
            title = title,
            bodyText = textBuilder.toString().trim(),
            pages = pages,
            watermarkText = "created by lumina",
            addWatermark = false
        )
    }

    private fun extractTextFromDocxXml(xml: String): String {
        val paragraphs = mutableListOf<String>()
        // Match each <w:p>...</w:p> paragraph
        val pRegex = Regex("<w:p[ >](.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("<w:t[ >](.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)

        val matches = pRegex.findAll(xml)
        for (match in matches) {
            val pContent = match.value
            val texts = tRegex.findAll(pContent).map { it.groupValues[1] }.joinToString("")
            if (texts.isNotBlank()) {
                paragraphs.add(texts)
            }
        }

        if (paragraphs.isEmpty()) {
            // Fallback: extract all w:t tags
            return tRegex.findAll(xml).map { it.groupValues[1] }.joinToString(" ")
        }
        return paragraphs.joinToString("\n\n")
    }

    private fun parseTextBased(
        context: Context,
        uri: Uri,
        name: String,
        size: Long,
        title: String,
        docType: DocumentType
    ): ParsedDocument {
        var content = ""
        try {
            val inputStream = openStream(context, uri)
            if (inputStream != null) {
                content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            content = "Error reading file: ${e.localizedMessage}"
        }

        return ParsedDocument(
            uri = uri,
            fileName = name,
            documentType = docType,
            fileSizeBytes = size,
            title = title,
            bodyText = content,
            pages = emptyList(),
            watermarkText = "created by lumina",
            addWatermark = false
        )
    }

    private fun parseImage(
        context: Context,
        uri: Uri,
        name: String,
        size: Long,
        title: String
    ): ParsedDocument {
        val pages = mutableListOf<DocumentPage>()
        try {
            val inputStream = openStream(context, uri)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmap != null) {
                    pages.add(DocumentPage(pageIndex = 0, bitmap = bitmap))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedDocument(
            uri = uri,
            fileName = name,
            documentType = DocumentType.IMAGE,
            fileSizeBytes = size,
            title = title,
            bodyText = "",
            pages = pages,
            watermarkText = "created by lumina",
            addWatermark = false
        )
    }

    private fun openStream(context: Context, uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (file.exists()) file.inputStream() else context.contentResolver.openInputStream(uri)
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    suspend fun runOcrOnDocument(context: Context, doc: ParsedDocument): String = withContext(Dispatchers.IO) {
        val pages = doc.pages
        if (pages.isEmpty()) return@withContext ""

        val fullTextBuilder = StringBuilder()
        for ((idx, page) in pages.withIndex()) {
            val bmp = page.bitmap ?: continue
            try {
                // Save temp image for ML Kit
                val tempFile = File(context.cacheDir, "ocr_temp_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val result = OcrScanner.recognizeText(context, Uri.fromFile(tempFile), OcrLanguageScript.AUTO)
                tempFile.delete()

                if (result.fullText.isNotBlank()) {
                    if (fullTextBuilder.isNotEmpty()) fullTextBuilder.append("\n\n--- Page ${idx + 1} ---\n\n")
                    fullTextBuilder.append(result.fullText.trim())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fullTextBuilder.toString()
    }

    suspend fun compileEditedDocumentToPdf(
        context: Context,
        doc: ParsedDocument,
        customName: String
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val sanitized = customName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Edited_Document" }
        val fileName = if (sanitized.endsWith(".pdf", true)) sanitized else "$sanitized.pdf"

        val outputDir = File(context.filesDir, "lumina_documents").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        try {
            if (doc.pages.isNotEmpty()) {
                // Compile page bitmaps (with rotations, modifications, watermark)
                val total = doc.pages.size
                for (i in 0 until total) {
                    val pageItem = doc.pages[i]
                    val srcBitmap = pageItem.bitmap ?: continue

                    // Apply rotation if needed
                    val bmp = if (pageItem.rotation % 360 != 0) {
                        val matrix = Matrix().apply { postRotate(pageItem.rotation.toFloat()) }
                        Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
                    } else {
                        srcBitmap
                    }

                    val pw = bmp.width
                    val ph = bmp.height
                    val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, i + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    val canvas = pdfPage.canvas

                    canvas.drawColor(Color.WHITE)
                    canvas.drawBitmap(bmp, 0f, 0f, null)

                    // Draw Watermark if enabled
                    if (doc.addWatermark && doc.watermarkText.isNotBlank()) {
                        val wmPaint = Paint().apply {
                            color = Color.parseColor("#888888")
                            alpha = 140
                            textSize = min(pw, ph) * 0.024f
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        }
                        val wmY = ph - (ph * 0.03f)
                        canvas.drawText("${doc.watermarkText} • Page ${i + 1} of $total", pw / 2f, wmY, wmPaint)
                    }

                    pdfDocument.finishPage(pdfPage)
                    if (bmp != srcBitmap) {
                        bmp.recycle()
                    }
                }
            } else {
                // Text-only document compilation
                compileTextContentToPdf(pdfDocument, doc)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        // Also save to device Downloads/LuminaConvert so user can access it anywhere
        FileUtils.saveFileToDeviceStorage(context, outputFile, fileName, "application/pdf")

        outputFile
    }

    private fun compileTextContentToPdf(pdfDocument: PdfDocument, doc: ParsedDocument) {
        val pw = 595
        val ph = 842
        val margin = 40f
        val contentWidth = (pw - (margin * 2)).toInt()
        val contentHeight = ph - (margin * 2) - 40f

        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paragraphs = doc.bodyText.split("\n")
        var pageNumber = 1

        var currentLayouts = mutableListOf<StaticLayout>()
        var accumulatedHeight = 0f

        val titleLayout = if (doc.title.isNotBlank()) {
            StaticLayout.Builder.obtain(doc.title, 0, doc.title.length, titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .build()
        } else null

        if (titleLayout != null) {
            accumulatedHeight += titleLayout.height + 20f
        }

        for (p in paragraphs) {
            val text = if (p.isEmpty()) " " else p
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .build()

            if (accumulatedHeight + layout.height > contentHeight && currentLayouts.isNotEmpty()) {
                // Finish page
                val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                var y = margin
                if (pageNumber == 1 && titleLayout != null) {
                    canvas.save()
                    canvas.translate(margin, y)
                    titleLayout.draw(canvas)
                    canvas.restore()
                    y += titleLayout.height + 20f
                }

                for (l in currentLayouts) {
                    canvas.save()
                    canvas.translate(margin, y)
                    l.draw(canvas)
                    canvas.restore()
                    y += l.height + 6f
                }

                val footerText = if (doc.addWatermark && doc.watermarkText.isNotBlank()) {
                    "${doc.watermarkText} • Page $pageNumber"
                } else {
                    "Page $pageNumber"
                }
                canvas.drawText(footerText, pw / 2f, ph - 25f, footerPaint)

                pdfDocument.finishPage(page)
                pageNumber++
                currentLayouts = mutableListOf()
                accumulatedHeight = 0f
            }

            currentLayouts.add(layout)
            accumulatedHeight += layout.height + 6f
        }

        // Draw last page
        val pageInfo = PdfDocument.PageInfo.Builder(pw, ph, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        var y = margin
        if (pageNumber == 1 && titleLayout != null) {
            canvas.save()
            canvas.translate(margin, y)
            titleLayout.draw(canvas)
            canvas.restore()
            y += titleLayout.height + 20f
        }

        for (l in currentLayouts) {
            canvas.save()
            canvas.translate(margin, y)
            l.draw(canvas)
            canvas.restore()
            y += l.height + 6f
        }

        val footerText = if (doc.addWatermark && doc.watermarkText.isNotBlank()) {
            "${doc.watermarkText} • Page $pageNumber"
        } else {
            "Page $pageNumber"
        }
        canvas.drawText(footerText, pw / 2f, ph - 25f, footerPaint)

        pdfDocument.finishPage(page)
    }

    suspend fun saveEditedDocumentAsText(
        context: Context,
        doc: ParsedDocument,
        customName: String
    ): File = withContext(Dispatchers.IO) {
        val sanitized = customName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Document" }
        val ext = when (doc.documentType) {
            DocumentType.CSV -> "csv"
            DocumentType.MARKDOWN -> "md"
            else -> "txt"
        }
        val fileName = if (sanitized.endsWith(".$ext", true)) sanitized else "$sanitized.$ext"
        val outputDir = File(context.filesDir, "lumina_documents").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        outputFile.writeText(doc.bodyText, Charsets.UTF_8)
        FileUtils.saveFileToDeviceStorage(context, outputFile, fileName, "text/plain")

        outputFile
    }

    fun removeParagraphFromPageBitmap(page: DocumentPage, block: PdfTextBlock) {
        val bmp = page.bitmap ?: return
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pad = 4f
        val rect = RectF(
            (block.boundingBox.left - pad).coerceAtLeast(0f),
            (block.boundingBox.top - pad).coerceAtLeast(0f),
            (block.boundingBox.right + pad).coerceAtMost(bmp.width.toFloat()),
            (block.boundingBox.bottom + pad).coerceAtMost(bmp.height.toFloat())
        )
        canvas.drawRect(rect, paint)
        block.isDeleted = true
    }

    fun replaceParagraphOnPageBitmap(
        page: DocumentPage,
        block: PdfTextBlock,
        newText: String,
        textColor: Int = Color.BLACK,
        isBold: Boolean = false
    ) {
        val bmp = page.bitmap ?: return
        val canvas = Canvas(bmp)
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pad = 4f
        val rect = RectF(
            (block.boundingBox.left - pad).coerceAtLeast(0f),
            (block.boundingBox.top - pad).coerceAtLeast(0f),
            (block.boundingBox.right + pad).coerceAtMost(bmp.width.toFloat()),
            (block.boundingBox.bottom + pad).coerceAtMost(bmp.height.toFloat())
        )
        canvas.drawRect(rect, bgPaint)

        val targetWidth = maxOf(60, rect.width().toInt())
        val estimatedLines = maxOf(1, newText.split("\n").size)
        val approxLineHeight = (rect.height() / estimatedLines).coerceIn(12f, 48f)
        val calculatedFontSize = (approxLineHeight * 0.75f).coerceIn(12f, 36f)

        val textPaint = TextPaint().apply {
            color = textColor
            textSize = calculatedFontSize
            typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            isAntiAlias = true
        }

        val layout = StaticLayout.Builder.obtain(newText, 0, newText.length, textPaint, targetWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

        canvas.save()
        canvas.translate(rect.left + pad, rect.top + pad)
        layout.draw(canvas)
        canvas.restore()

        block.text = newText
        block.isEdited = true
        block.isDeleted = false
        block.textColor = textColor
        block.isBold = isBold
    }

    fun addTextToPageBitmap(
        page: DocumentPage,
        text: String,
        x: Float,
        y: Float,
        textColor: Int = Color.BLACK,
        textSize: Float = 24f,
        isBold: Boolean = false
    ): PdfTextBlock? {
        val bmp = page.bitmap ?: return null
        val canvas = Canvas(bmp)
        val textPaint = TextPaint().apply {
            color = textColor
            this.textSize = textSize
            typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            isAntiAlias = true
        }

        val maxWidth = (bmp.width - x - 20f).toInt().coerceAtLeast(100)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()

        val newBlock = PdfTextBlock(
            pageIndex = page.pageIndex,
            text = text,
            boundingBox = RectF(x, y, x + layout.width, y + layout.height),
            textColor = textColor,
            isBold = isBold,
            isEdited = true
        )
        val updated = page.textBlocks.toMutableList()
        updated.add(newBlock)
        page.textBlocks = updated
        return newBlock
    }

    fun eraseAreaOnPageBitmap(page: DocumentPage, rect: RectF) {
        val bmp = page.bitmap ?: return
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(rect, paint)
    }
}
