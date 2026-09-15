package com.example.domain.document

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import java.util.UUID

enum class DocumentType(val extension: String, val displayName: String) {
    PDF("pdf", "PDF Document"),
    DOCX("docx", "Word Document"),
    DOC("doc", "Word Document"),
    TEXT("txt", "Text Document"),
    MARKDOWN("md", "Markdown Document"),
    CSV("csv", "Data Document"),
    IMAGE("image", "Image Document"),
    OTHER("file", "Document")
}

data class PdfTextBlock(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    var text: String,
    val boundingBox: RectF,
    var isDeleted: Boolean = false,
    var isEdited: Boolean = false,
    var textColor: Int = Color.BLACK,
    var isBold: Boolean = false
)

data class DocumentPage(
    val id: String = UUID.randomUUID().toString(),
    var pageIndex: Int,
    var bitmap: Bitmap? = null,
    var rotation: Int = 0,
    var textContent: String = "",
    var textBlocks: List<PdfTextBlock> = emptyList(),
    var isBlocksLoaded: Boolean = false
)

data class ParsedDocument(
    val uri: Uri,
    val sourcePath: String? = null,
    val fileName: String,
    val documentType: DocumentType,
    val fileSizeBytes: Long,
    var title: String,
    var bodyText: String,
    var pages: List<DocumentPage> = emptyList(),
    var watermarkText: String = "created by lumina",
    var addWatermark: Boolean = false
)
