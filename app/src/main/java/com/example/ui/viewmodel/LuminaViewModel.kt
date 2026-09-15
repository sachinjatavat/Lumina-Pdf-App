package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.LuminaDatabase
import com.example.data.db.PdfRecord
import com.example.data.repository.PdfRepository
import com.example.domain.document.DocumentPage
import com.example.domain.document.DocumentParser
import com.example.domain.document.DocumentType
import com.example.domain.document.ParsedDocument
import com.example.domain.model.PdfCompilerOptions
import com.example.domain.model.SelectedImage
import com.example.domain.model.TextToPdfOptions
import com.example.domain.ocr.OcrLanguageScript
import com.example.domain.ocr.OcrLanguages
import com.example.domain.ocr.OcrResult
import com.example.domain.ocr.OcrScanner
import com.example.domain.ocr.SupportedOcrLanguage
import com.example.domain.pdf.PdfCompiler
import com.example.domain.pdf.PdfRenderedPage
import com.example.domain.pdf.PdfToImageExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

enum class LuminaTab(val title: String) {
    IMAGE_TO_PDF("Images to PDF"),
    VIEWER_EDITOR("View & Edit"),
    TEXT_TO_PDF("Text to PDF"),
    OCR_EXTRACT("Scan OCR"),
    PDF_TO_IMAGE("PDF to Image"),
    CONNECT("Connect"),
    HISTORY("Documents")
}

class LuminaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository

    init {
        val db = LuminaDatabase.getDatabase(application)
        repository = PdfRepository(db.pdfRecordDao())
    }

    val historyRecords: StateFlow<List<PdfRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(LuminaTab.IMAGE_TO_PDF)
    val currentTab: StateFlow<LuminaTab> = _currentTab.asStateFlow()

    fun switchTab(tab: LuminaTab) {
        _currentTab.value = tab
    }

    // ----------------------------------------------------
    // Image to PDF State & Actions
    // ----------------------------------------------------
    private val _selectedImages = MutableStateFlow<List<SelectedImage>>(emptyList())
    val selectedImages: StateFlow<List<SelectedImage>> = _selectedImages.asStateFlow()

    private val _compilerOptions = MutableStateFlow(PdfCompilerOptions())
    val compilerOptions: StateFlow<PdfCompilerOptions> = _compilerOptions.asStateFlow()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    private val _compilingProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val compilingProgress: StateFlow<Pair<Int, Int>?> = _compilingProgress.asStateFlow()

    private val _lastGeneratedRecord = MutableStateFlow<PdfRecord?>(null)
    val lastGeneratedRecord: StateFlow<PdfRecord?> = _lastGeneratedRecord.asStateFlow()

    fun addImages(context: Context, uris: List<Uri>) {
        val current = _selectedImages.value.toMutableList()
        for (uri in uris) {
            var name = "Image_${current.size + 1}.jpg"
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
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {}

            current.add(SelectedImage(uri = uri, displayName = name, fileSizeBytes = size))
        }
        _selectedImages.value = current
    }

    fun removeImage(id: String) {
        _selectedImages.value = _selectedImages.value.filter { it.id != id }
    }

    fun moveImageUp(index: Int) {
        if (index <= 0) return
        val list = _selectedImages.value.toMutableList()
        Collections.swap(list, index, index - 1)
        _selectedImages.value = list
    }

    fun moveImageDown(index: Int) {
        val list = _selectedImages.value.toMutableList()
        if (index >= list.size - 1) return
        Collections.swap(list, index, index + 1)
        _selectedImages.value = list
    }

    fun rotateImage(id: String) {
        _selectedImages.value = _selectedImages.value.map {
            if (it.id == id) it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360)
            else it
        }
    }

    fun clearImages() {
        _selectedImages.value = emptyList()
    }

    fun updateCompilerOptions(newOptions: PdfCompilerOptions) {
        _compilerOptions.value = newOptions
    }

    fun dismissGeneratedResult() {
        _lastGeneratedRecord.value = null
    }

    fun compileImagesToPdf(context: Context) {
        val images = _selectedImages.value
        if (images.isEmpty()) {
            Toast.makeText(context, "Please select at least one image", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isCompiling.value = true
            _compilingProgress.value = Pair(1, images.size)
            try {
                val file = PdfCompiler.compileImagesToPdf(
                    context = context,
                    images = images,
                    options = _compilerOptions.value,
                    onProgress = { cur, tot ->
                        _compilingProgress.value = Pair(cur, tot)
                    }
                )

                val record = PdfRecord(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    pageCount = images.size,
                    sourceType = "IMAGE_TO_PDF"
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to compile PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isCompiling.value = false
                _compilingProgress.value = null
            }
        }
    }

    // ----------------------------------------------------
    // Text to PDF State & Actions
    // ----------------------------------------------------
    private val _textOptions = MutableStateFlow(
        TextToPdfOptions(
            title = "Document Notes",
            author = "Lumina Convert",
            bodyText = "Welcome to Lumina Convert.\n\nYou can format and generate professional PDFs from simple notes, essays, or scanned OCR text instantly.\n\nAll documents are processed privately on-device without cloud servers."
        )
    )
    val textOptions: StateFlow<TextToPdfOptions> = _textOptions.asStateFlow()

    private val _isCompilingText = MutableStateFlow(false)
    val isCompilingText: StateFlow<Boolean> = _isCompilingText.asStateFlow()

    fun updateTextOptions(options: TextToPdfOptions) {
        _textOptions.value = options
    }

    fun compileTextToPdf(context: Context) {
        val opts = _textOptions.value
        if (opts.bodyText.isBlank() && opts.title.isBlank()) {
            Toast.makeText(context, "Please enter some text or title", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isCompilingText.value = true
            try {
                val file = PdfCompiler.compileTextToPdf(context, opts)
                val record = PdfRecord(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    pageCount = maxOf(1, opts.bodyText.length / 1500 + 1),
                    sourceType = "TEXT_TO_PDF"
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                Toast.makeText(context, "Text PDF created successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isCompilingText.value = false
            }
        }
    }

    // ----------------------------------------------------
    // OCR Scanner State & Actions
    // ----------------------------------------------------
    private val _ocrImageUri = MutableStateFlow<Uri?>(null)
    val ocrImageUri: StateFlow<Uri?> = _ocrImageUri.asStateFlow()

    private val _isScanningOcr = MutableStateFlow(false)
    val isScanningOcr: StateFlow<Boolean> = _isScanningOcr.asStateFlow()

    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult: StateFlow<OcrResult?> = _ocrResult.asStateFlow()

    private val _ocrSelectedScript = MutableStateFlow(OcrLanguageScript.AUTO)
    val ocrSelectedScript: StateFlow<OcrLanguageScript> = _ocrSelectedScript.asStateFlow()

    private val _selectedOcrLanguage = MutableStateFlow(OcrLanguages.AUTO_DETECT)
    val selectedOcrLanguage: StateFlow<SupportedOcrLanguage> = _selectedOcrLanguage.asStateFlow()

    fun setOcrScript(context: Context, script: OcrLanguageScript) {
        _ocrSelectedScript.value = script
        val mappedLang = when (script) {
            OcrLanguageScript.DEVANAGARI -> OcrLanguages.findById(3)
            OcrLanguageScript.LATIN -> OcrLanguages.findById(1)
            OcrLanguageScript.CHINESE -> OcrLanguages.findById(2)
            OcrLanguageScript.JAPANESE -> OcrLanguages.findById(13)
            OcrLanguageScript.KOREAN -> OcrLanguages.findById(28)
            OcrLanguageScript.AUTO -> OcrLanguages.AUTO_DETECT
        }
        _selectedOcrLanguage.value = mappedLang
        if (_ocrImageUri.value != null) {
            runOcrWithLanguage(context, _ocrImageUri.value, mappedLang)
        }
    }

    fun setOcrLanguage(context: Context, language: SupportedOcrLanguage) {
        _selectedOcrLanguage.value = language
        val mappedScript = when (language.engineType) {
            com.example.domain.ocr.OcrEngineType.DEVANAGARI -> OcrLanguageScript.DEVANAGARI
            com.example.domain.ocr.OcrEngineType.CHINESE -> OcrLanguageScript.CHINESE
            com.example.domain.ocr.OcrEngineType.JAPANESE -> OcrLanguageScript.JAPANESE
            com.example.domain.ocr.OcrEngineType.KOREAN -> OcrLanguageScript.KOREAN
            com.example.domain.ocr.OcrEngineType.LATIN -> OcrLanguageScript.LATIN
            else -> OcrLanguageScript.AUTO
        }
        _ocrSelectedScript.value = mappedScript
        if (_ocrImageUri.value != null) {
            runOcrWithLanguage(context, _ocrImageUri.value, language)
        }
    }

    fun setOcrImage(context: Context, uri: Uri) {
        _ocrImageUri.value = uri
        runOcrWithLanguage(context, uri, _selectedOcrLanguage.value)
    }

    fun runOcr(context: Context, uri: Uri? = _ocrImageUri.value, script: OcrLanguageScript = _ocrSelectedScript.value) {
        runOcrWithLanguage(context, uri, _selectedOcrLanguage.value)
    }

    fun runOcrWithLanguage(
        context: Context,
        uri: Uri? = _ocrImageUri.value,
        language: SupportedOcrLanguage = _selectedOcrLanguage.value
    ) {
        if (uri == null) return
        viewModelScope.launch {
            _isScanningOcr.value = true
            _ocrResult.value = null
            try {
                val result = OcrScanner.recognizeTextWithLanguage(context, uri, language)
                _ocrResult.value = result
                if (result.fullText.isBlank()) {
                    Toast.makeText(context, "No text recognized in image for ${language.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Extracted text via ${result.detectedScript}!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "OCR Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isScanningOcr.value = false
            }
        }
    }

    fun sendOcrTextToTextPdf(text: String) {
        _textOptions.value = _textOptions.value.copy(
            title = "Scanned Document",
            bodyText = text
        )
        _currentTab.value = LuminaTab.TEXT_TO_PDF
    }

    // ----------------------------------------------------
    // PDF to Image State & Actions
    // ----------------------------------------------------
    private val _pdfUriToExtract = MutableStateFlow<Uri?>(null)
    val pdfUriToExtract: StateFlow<Uri?> = _pdfUriToExtract.asStateFlow()

    private val _isRenderingPdf = MutableStateFlow(false)
    val isRenderingPdf: StateFlow<Boolean> = _isRenderingPdf.asStateFlow()

    private val _renderedPdfPages = MutableStateFlow<List<PdfRenderedPage>>(emptyList())
    val renderedPdfPages: StateFlow<List<PdfRenderedPage>> = _renderedPdfPages.asStateFlow()

    private val _selectedPageToExport = MutableStateFlow<Int?>(null)
    val selectedPageToExport: StateFlow<Int?> = _selectedPageToExport.asStateFlow()

    fun loadPdfForImageExtraction(context: Context, uri: Uri) {
        _pdfUriToExtract.value = uri
        viewModelScope.launch {
            _isRenderingPdf.value = true
            _renderedPdfPages.value = emptyList()
            try {
                val pages = PdfToImageExtractor.renderPdfPages(context, uri)
                _renderedPdfPages.value = pages
                if (pages.isEmpty()) {
                    Toast.makeText(context, "Could not extract pages from PDF", Toast.LENGTH_SHORT).show()
                } else {
                    _selectedPageToExport.value = 0
                    Toast.makeText(context, "Loaded ${pages.size} pages", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isRenderingPdf.value = false
            }
        }
    }

    fun selectPageToExport(index: Int) {
        _selectedPageToExport.value = index
    }

    fun saveSelectedPageAsImage(context: Context, pageIndex: Int) {
        val page = _renderedPdfPages.value.getOrNull(pageIndex) ?: return
        viewModelScope.launch {
            try {
                val file = PdfToImageExtractor.savePageAsImage(context, page.bitmap, pageIndex)
                Toast.makeText(context, "Page ${pageIndex + 1} saved to ${file.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveAllPagesAsImages(context: Context) {
        val pages = _renderedPdfPages.value
        if (pages.isEmpty()) return
        viewModelScope.launch {
            try {
                for (p in pages) {
                    PdfToImageExtractor.savePageAsImage(context, p.bitmap, p.pageIndex)
                }
                Toast.makeText(context, "All ${pages.size} pages saved!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Save error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ----------------------------------------------------
    // Documents History Actions
    // ----------------------------------------------------
    fun deleteRecord(record: PdfRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    // ----------------------------------------------------
    // Document Viewer & Editor State & Actions
    // ----------------------------------------------------
    private val _currentDocument = MutableStateFlow<ParsedDocument?>(null)
    val currentDocument: StateFlow<ParsedDocument?> = _currentDocument.asStateFlow()

    private val _isLoadingDocument = MutableStateFlow(false)
    val isLoadingDocument: StateFlow<Boolean> = _isLoadingDocument.asStateFlow()

    private val _isSavingDocument = MutableStateFlow(false)
    val isSavingDocument: StateFlow<Boolean> = _isSavingDocument.asStateFlow()

    private val _isOcrExtractingDocument = MutableStateFlow(false)
    val isOcrExtractingDocument: StateFlow<Boolean> = _isOcrExtractingDocument.asStateFlow()

    fun loadDocument(context: Context, uri: Uri, fileNameOverride: String? = null) {
        viewModelScope.launch {
            _isLoadingDocument.value = true
            try {
                val doc = DocumentParser.parseDocument(context, uri, fileNameOverride)
                _currentDocument.value = doc
                Toast.makeText(context, "Opened ${doc.fileName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load document: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoadingDocument.value = false
            }
        }
    }

    fun openDocumentInViewer(context: Context, uri: Uri, fileNameOverride: String? = null) {
        loadDocument(context, uri, fileNameOverride)
        _currentTab.value = LuminaTab.VIEWER_EDITOR
    }

    fun updateDocumentTitle(newTitle: String) {
        _currentDocument.value = _currentDocument.value?.copy(title = newTitle)
    }

    fun updateDocumentBodyText(newText: String) {
        _currentDocument.value = _currentDocument.value?.copy(bodyText = newText)
    }

    fun updateDocumentWatermark(text: String, enabled: Boolean) {
        _currentDocument.value = _currentDocument.value?.copy(
            watermarkText = text,
            addWatermark = enabled
        )
    }

    fun rotateDocumentPage(pageId: String) {
        val doc = _currentDocument.value ?: return
        val updatedPages = doc.pages.map { page ->
            if (page.id == pageId) {
                page.copy(rotation = (page.rotation + 90) % 360)
            } else page
        }
        _currentDocument.value = doc.copy(pages = updatedPages)
    }

    fun moveDocumentPageUp(index: Int) {
        val doc = _currentDocument.value ?: return
        if (index <= 0) return
        val updatedPages = doc.pages.toMutableList()
        Collections.swap(updatedPages, index, index - 1)
        _currentDocument.value = doc.copy(pages = updatedPages)
    }

    fun moveDocumentPageDown(index: Int) {
        val doc = _currentDocument.value ?: return
        if (index >= doc.pages.size - 1) return
        val updatedPages = doc.pages.toMutableList()
        Collections.swap(updatedPages, index, index + 1)
        _currentDocument.value = doc.copy(pages = updatedPages)
    }

    fun deleteDocumentPage(pageId: String) {
        val doc = _currentDocument.value ?: return
        val updatedPages = doc.pages.filter { it.id != pageId }
        _currentDocument.value = doc.copy(pages = updatedPages)
    }

    fun addDocumentPageImages(context: Context, uris: List<Uri>) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            val updatedPages = doc.pages.toMutableList()
            for (uri in uris) {
                try {
                    val stream = if (uri.scheme == "file") {
                        val f = File(uri.path ?: "")
                        if (f.exists()) f.inputStream() else context.contentResolver.openInputStream(uri)
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bmp != null) {
                        updatedPages.add(DocumentPage(pageIndex = updatedPages.size, bitmap = bmp))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _currentDocument.value = doc.copy(pages = updatedPages)
            Toast.makeText(context, "Added ${uris.size} image page(s)", Toast.LENGTH_SHORT).show()
        }
    }

    fun runOcrOnCurrentDocument(context: Context) {
        val doc = _currentDocument.value ?: return
        if (doc.pages.isEmpty()) {
            Toast.makeText(context, "No pages or images found to scan", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isOcrExtractingDocument.value = true
            try {
                val extractedText = DocumentParser.runOcrOnDocument(context, doc)
                if (extractedText.isNotBlank()) {
                    val combined = if (doc.bodyText.isBlank()) extractedText else "${doc.bodyText}\n\n$extractedText"
                    _currentDocument.value = doc.copy(bodyText = combined)
                    Toast.makeText(context, "Extracted text from PDF pages!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No text found in pages", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "OCR Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _isOcrExtractingDocument.value = false
            }
        }
    }

    fun saveEditedDocument(context: Context, customName: String, asPdf: Boolean = true) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            _isSavingDocument.value = true
            try {
                if (asPdf) {
                    val file = DocumentParser.compileEditedDocumentToPdf(context, doc, customName)
                    val record = PdfRecord(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileSizeBytes = file.length(),
                        pageCount = maxOf(1, doc.pages.size),
                        sourceType = "EDITED_DOCUMENT"
                    )
                    repository.insertRecord(record)
                    _lastGeneratedRecord.value = record
                    Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
                } else {
                    val file = DocumentParser.saveEditedDocumentAsText(context, doc, customName)
                    Toast.makeText(context, "Saved text to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save document: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isSavingDocument.value = false
            }
        }
    }

    // ----------------------------------------------------
    // External Pure PDF Viewer/Editor Mode
    // ----------------------------------------------------
    private val _isExternalPdfMode = MutableStateFlow(false)
    val isExternalPdfMode: StateFlow<Boolean> = _isExternalPdfMode.asStateFlow()

    fun openExternalPdf(context: Context, uri: Uri) {
        _isExternalPdfMode.value = true
        loadDocument(context, uri)
    }

    fun exitExternalPdfMode() {
        _isExternalPdfMode.value = false
    }

    fun saveVisualPdf(context: Context, customName: String) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch {
            _isSavingDocument.value = true
            try {
                val file = DocumentParser.compileEditedDocumentToPdf(context, doc, customName)
                val record = PdfRecord(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    pageCount = maxOf(1, doc.pages.size),
                    sourceType = "EDITED_PDF"
                )
                repository.insertRecord(record)
                _lastGeneratedRecord.value = record
                Toast.makeText(context, "Saved to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isSavingDocument.value = false
            }
        }
    }
}
