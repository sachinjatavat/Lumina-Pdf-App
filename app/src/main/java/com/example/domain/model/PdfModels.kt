package com.example.domain.model

import android.net.Uri
import java.util.UUID

data class SelectedImage(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val rotationDegrees: Int = 0,
    val displayName: String = "Image",
    val fileSizeBytes: Long = 0L
)

enum class PaperSize(val label: String, val widthPt: Int, val heightPt: Int) {
    FIT_IMAGE("Fit Image", 0, 0),
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("US Letter (8.5 × 11 in)", 612, 792)
}

enum class PaperOrientation(val label: String) {
    AUTO("Auto Detect"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

enum class PaperMargin(val label: String, val paddingPt: Int) {
    NONE("None (0 pt)", 0),
    SMALL("Small (16 pt)", 16),
    MEDIUM("Medium (32 pt)", 32)
}

enum class ImageFitMode(val label: String) {
    CONTAIN("Contain (Keep Ratio)"),
    COVER("Cover (Fill Page)")
}

enum class ImageQuality(val label: String, val qualityPercent: Int) {
    HIGH("Original / High (100%)", 100),
    BALANCED("Balanced (85%)", 85),
    COMPACT("Small File / Compact (65%)", 65)
}

data class PdfCompilerOptions(
    val paperSize: PaperSize = PaperSize.A4,
    val orientation: PaperOrientation = PaperOrientation.PORTRAIT,
    val margin: PaperMargin = PaperMargin.SMALL,
    val fitMode: ImageFitMode = ImageFitMode.CONTAIN,
    val quality: ImageQuality = ImageQuality.BALANCED,
    val outputFileName: String = "",
    val showPageNumbers: Boolean = true,
    val headerTitle: String = "",
    val addWatermark: Boolean = true,
    val watermarkText: String = "created by lumina"
)

data class TextToPdfOptions(
    val title: String = "",
    val author: String = "",
    val bodyText: String = "",
    val paperSize: PaperSize = PaperSize.A4,
    val margin: PaperMargin = PaperMargin.MEDIUM,
    val fontSizeSp: Float = 14f,
    val lineSpacingMultiplier: Float = 1.3f,
    val outputFileName: String = "",
    val showPageNumbers: Boolean = true,
    val addWatermark: Boolean = true,
    val watermarkText: String = "created by lumina"
)
