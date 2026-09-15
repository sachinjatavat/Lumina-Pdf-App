package com.example.domain.ocr

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.domain.document.PdfTextBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class OcrLanguageScript(
    val title: String,
    val shortName: String,
    val description: String
) {
    AUTO("All Languages", "Auto", "Universal auto-detect across 70+ languages, Hindi, English, Chinese, Japanese, Korean & symbols"),
    DEVANAGARI("Hindi / Devanagari", "Hindi", "Hindi, Marathi, Sanskrit, Nepali, Bhojpuri & symbols"),
    LATIN("English / Latin", "English", "English, Spanish, French, German & universal characters"),
    CHINESE("Chinese", "Chinese", "Simplified & Traditional Chinese characters & symbols"),
    JAPANESE("Japanese", "Japanese", "Kanji, Hiragana & Katakana characters"),
    KOREAN("Korean", "Korean", "Hangul, Hanja & symbols")
}

data class OcrResult(
    val fullText: String,
    val lineCount: Int,
    val blockCount: Int,
    val detectedScript: String = "Universal Multi-Script"
)

object OcrScanner {

    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val devanagariRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val koreanRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    private suspend fun processImage(recognizer: TextRecognizer, image: InputImage): Text =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    suspend fun recognizeTextWithLanguage(
        context: Context,
        imageUri: Uri,
        language: SupportedOcrLanguage
    ): OcrResult {
        val image = InputImage.fromFilePath(context, imageUri)
        return recognizeInputImage(image, language)
    }

    suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        language: SupportedOcrLanguage = OcrLanguages.AUTO_DETECT
    ): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizeInputImage(image, language)
    }

    suspend fun extractTextBlocksFromBitmap(
        bitmap: Bitmap,
        pageIndex: Int,
        language: SupportedOcrLanguage = OcrLanguages.AUTO_DETECT
    ): List<PdfTextBlock> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = when (language.engineType) {
            OcrEngineType.LATIN -> runCatching { processImage(latinRecognizer, image) }.getOrNull()
            OcrEngineType.DEVANAGARI -> runCatching { processImage(devanagariRecognizer, image) }.getOrNull()
            OcrEngineType.CHINESE -> runCatching { processImage(chineseRecognizer, image) }.getOrNull()
            OcrEngineType.JAPANESE -> runCatching { processImage(japaneseRecognizer, image) }.getOrNull()
            OcrEngineType.KOREAN -> runCatching { processImage(koreanRecognizer, image) }.getOrNull()
            OcrEngineType.UNIVERSAL_MULTI -> {
                coroutineScope {
                    val lat = async { runCatching { processImage(latinRecognizer, image) }.getOrNull() }
                    val dev = async { runCatching { processImage(devanagariRecognizer, image) }.getOrNull() }
                    val chi = async { runCatching { processImage(chineseRecognizer, image) }.getOrNull() }
                    val jap = async { runCatching { processImage(japaneseRecognizer, image) }.getOrNull() }
                    val kor = async { runCatching { processImage(koreanRecognizer, image) }.getOrNull() }

                    val results = listOfNotNull(lat.await(), dev.await(), chi.await(), jap.await(), kor.await())
                    results.maxByOrNull { it.textBlocks.size }
                }
            }
        } ?: return emptyList()

        val blocks = mutableListOf<PdfTextBlock>()
        val pageW = bitmap.width.toFloat()
        val pageH = bitmap.height.toFloat()

        for (tb in visionText.textBlocks) {
            val rawRect = tb.boundingBox
            val rectF = if (rawRect != null) {
                RectF(
                    rawRect.left.toFloat().coerceAtLeast(0f),
                    rawRect.top.toFloat().coerceAtLeast(0f),
                    rawRect.right.toFloat().coerceAtMost(pageW),
                    rawRect.bottom.toFloat().coerceAtMost(pageH)
                )
            } else {
                RectF(20f, 20f, pageW - 20f, 80f)
            }

            if (tb.text.isNotBlank()) {
                blocks.add(
                    PdfTextBlock(
                        pageIndex = pageIndex,
                        text = tb.text,
                        boundingBox = rectF
                    )
                )
            }
        }
        return blocks
    }

    private suspend fun recognizeInputImage(
        image: InputImage,
        language: SupportedOcrLanguage
    ): OcrResult {
        return when (language.engineType) {
            OcrEngineType.LATIN -> {
                val text = processImage(latinRecognizer, image)
                toOcrResult(text, "${language.name} (${language.script})")
            }
            OcrEngineType.DEVANAGARI -> {
                val text = processImage(devanagariRecognizer, image)
                toOcrResult(text, "${language.name} (Devanagari)")
            }
            OcrEngineType.CHINESE -> {
                val text = processImage(chineseRecognizer, image)
                toOcrResult(text, "${language.name} (Chinese Hanzi)")
            }
            OcrEngineType.JAPANESE -> {
                val text = processImage(japaneseRecognizer, image)
                toOcrResult(text, "${language.name} (Japanese)")
            }
            OcrEngineType.KOREAN -> {
                val text = processImage(koreanRecognizer, image)
                toOcrResult(text, "${language.name} (Korean)")
            }
            OcrEngineType.UNIVERSAL_MULTI -> {
                coroutineScope {
                    val devanagariJob = async { runCatching { processImage(devanagariRecognizer, image) }.getOrNull() }
                    val latinJob = async { runCatching { processImage(latinRecognizer, image) }.getOrNull() }
                    val chineseJob = async { runCatching { processImage(chineseRecognizer, image) }.getOrNull() }
                    val japaneseJob = async { runCatching { processImage(japaneseRecognizer, image) }.getOrNull() }
                    val koreanJob = async { runCatching { processImage(koreanRecognizer, image) }.getOrNull() }

                    val devResult = devanagariJob.await()
                    val latResult = latinJob.await()
                    val chiResult = chineseJob.await()
                    val japResult = japaneseJob.await()
                    val korResult = koreanJob.await()

                    val hasDevanagari = devResult?.text?.any { it in '\u0900'..'\u097F' } == true
                    val hasChinese = chiResult?.text?.any { it in '\u4E00'..'\u9FFF' } == true
                    val hasJapanese = japResult?.text?.any { it in '\u3040'..'\u30FF' } == true
                    val hasKorean = korResult?.text?.any { it in '\uAC00'..'\uD7AF' || it in '\u1100'..'\u11FF' } == true

                    when {
                        hasDevanagari && devResult != null -> {
                            toOcrResult(devResult, "${language.name} / Devanagari")
                        }
                        hasChinese && chiResult != null -> {
                            toOcrResult(chiResult, "${language.name} / Chinese")
                        }
                        hasJapanese && japResult != null -> {
                            toOcrResult(japResult, "${language.name} / Japanese")
                        }
                        hasKorean && korResult != null -> {
                            toOcrResult(korResult, "${language.name} / Korean")
                        }
                        else -> {
                            val candidates = listOfNotNull(
                                latResult?.let { Pair(it, "${language.name} / Universal") },
                                devResult?.let { Pair(it, "${language.name} / Multi-Script") },
                                chiResult?.let { Pair(it, "${language.name} / Multi-Script") },
                                japResult?.let { Pair(it, "${language.name} / Multi-Script") },
                                korResult?.let { Pair(it, "${language.name} / Multi-Script") }
                            )

                            val best = candidates.maxByOrNull { it.first.text.trim().length }
                            if (best != null && best.first.text.isNotBlank()) {
                                toOcrResult(best.first, best.second)
                            } else {
                                latResult?.let { toOcrResult(it, language.name) } ?: OcrResult("", 0, 0, "No text detected")
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun recognizeText(
        context: Context,
        imageUri: Uri,
        script: OcrLanguageScript = OcrLanguageScript.AUTO
    ): OcrResult {
        val lang = when (script) {
            OcrLanguageScript.DEVANAGARI -> OcrLanguages.findById(3) // Hindi
            OcrLanguageScript.LATIN -> OcrLanguages.findById(1) // English
            OcrLanguageScript.CHINESE -> OcrLanguages.findById(2) // Chinese
            OcrLanguageScript.JAPANESE -> OcrLanguages.findById(13) // Japanese
            OcrLanguageScript.KOREAN -> OcrLanguages.findById(28) // Korean
            OcrLanguageScript.AUTO -> OcrLanguages.AUTO_DETECT
        }
        return recognizeTextWithLanguage(context, imageUri, lang)
    }

    private fun toOcrResult(visionText: Text, scriptName: String): OcrResult {
        var lines = 0
        for (b in visionText.textBlocks) {
            lines += b.lines.size
        }
        return OcrResult(
            fullText = visionText.text,
            lineCount = lines,
            blockCount = visionText.textBlocks.size,
            detectedScript = scriptName
        )
    }
}

