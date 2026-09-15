package com.example.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfRenderedPage(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
)

object PdfToImageExtractor {

    suspend fun renderPdfPages(
        context: Context,
        pdfUri: Uri,
        maxPages: Int = 50
    ): List<PdfRenderedPage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<PdfRenderedPage>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext emptyList()
            renderer = PdfRenderer(pfd)
            val pageCount = minOf(renderer.pageCount, maxPages)

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                // Render at 2x density for crisp clarity
                val scale = 2
                val width = page.width * scale
                val height = page.height * scale
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                pages.add(PdfRenderedPage(i, bitmap, width, height))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }

        pages
    }

    suspend fun savePageAsImage(
        context: Context,
        bitmap: Bitmap,
        pageIndex: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "extracted_images").apply { mkdirs() }
        val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val fileName = "Lumina_Page_${pageIndex + 1}_${System.currentTimeMillis()}.$ext"
        val file = File(outputDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(format, 95, out)
        }
        file
    }
}
