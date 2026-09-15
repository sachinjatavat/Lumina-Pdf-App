package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LuminaBottomNavigation
import com.example.ui.components.LuminaTopBar
import com.example.ui.components.PdfResultDialog
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.DocumentViewerEditorScreen
import com.example.ui.screens.DocumentsHistoryScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.PdfDirectViewerEditorScreen
import com.example.ui.screens.PdfToImageScreen
import com.example.ui.screens.TextToPdfScreen
import com.example.ui.theme.LuminaTheme
import com.example.ui.viewmodel.LuminaTab
import com.example.ui.viewmodel.LuminaViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: LuminaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            LuminaTheme {
                LuminaApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data ?: return
            viewModel.openExternalPdf(this, uri)
        }
    }
}

@Composable
fun LuminaApp(viewModel: LuminaViewModel) {
    val context = LocalContext.current

    val isExternalPdfMode by viewModel.isExternalPdfMode.collectAsStateWithLifecycle()
    val currentDocument by viewModel.currentDocument.collectAsStateWithLifecycle()
    val isLoadingDocument by viewModel.isLoadingDocument.collectAsStateWithLifecycle()
    val isSavingDocument by viewModel.isSavingDocument.collectAsStateWithLifecycle()

    // When opened from external PDF intent (or pure reader mode):
    // Show ONLY the PDF on screen with the "Edit" option as explicitly requested by user!
    if (isExternalPdfMode) {
        PdfDirectViewerEditorScreen(
            document = currentDocument,
            isLoading = isLoadingDocument,
            isSaving = isSavingDocument,
            onClose = {
                viewModel.exitExternalPdfMode()
            },
            onSavePdf = { customName ->
                viewModel.saveVisualPdf(context, customName)
            }
        )
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()

    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val compilerOptions by viewModel.compilerOptions.collectAsStateWithLifecycle()
    val isCompiling by viewModel.isCompiling.collectAsStateWithLifecycle()
    val compilingProgress by viewModel.compilingProgress.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    val textOptions by viewModel.textOptions.collectAsStateWithLifecycle()
    val isCompilingText by viewModel.isCompilingText.collectAsStateWithLifecycle()

    val ocrImageUri by viewModel.ocrImageUri.collectAsStateWithLifecycle()
    val isScanningOcr by viewModel.isScanningOcr.collectAsStateWithLifecycle()
    val ocrResult by viewModel.ocrResult.collectAsStateWithLifecycle()
    val ocrSelectedScript by viewModel.ocrSelectedScript.collectAsStateWithLifecycle()
    val selectedOcrLanguage by viewModel.selectedOcrLanguage.collectAsStateWithLifecycle()

    val pdfUriToExtract by viewModel.pdfUriToExtract.collectAsStateWithLifecycle()
    val isRenderingPdf by viewModel.isRenderingPdf.collectAsStateWithLifecycle()
    val renderedPdfPages by viewModel.renderedPdfPages.collectAsStateWithLifecycle()
    val selectedPageToExport by viewModel.selectedPageToExport.collectAsStateWithLifecycle()

    val isOcrExtractingDocument by viewModel.isOcrExtractingDocument.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LuminaTopBar(
                currentTab = currentTab,
                historyCount = historyRecords.size,
                onHistoryClick = {
                    viewModel.switchTab(LuminaTab.HISTORY)
                }
            )
        },
        bottomBar = {
            LuminaBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    viewModel.switchTab(tab)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                LuminaTab.IMAGE_TO_PDF -> {
                    ImageToPdfScreen(
                        images = selectedImages,
                        options = compilerOptions,
                        isCompiling = isCompiling,
                        progress = compilingProgress,
                        onAddImages = { uris -> viewModel.addImages(context, uris) },
                        onRemoveImage = { id -> viewModel.removeImage(id) },
                        onMoveUp = { index -> viewModel.moveImageUp(index) },
                        onMoveDown = { index -> viewModel.moveImageDown(index) },
                        onRotate = { id -> viewModel.rotateImage(id) },
                        onClearImages = { viewModel.clearImages() },
                        onOptionsChanged = { opts -> viewModel.updateCompilerOptions(opts) },
                        onCompile = { viewModel.compileImagesToPdf(context) }
                    )
                }

                LuminaTab.VIEWER_EDITOR -> {
                    DocumentViewerEditorScreen(
                        document = currentDocument,
                        isLoading = isLoadingDocument,
                        isSaving = isSavingDocument,
                        isOcrScanning = isOcrExtractingDocument,
                        onOpenFile = { uri -> viewModel.loadDocument(context, uri) },
                        onTitleChanged = { title -> viewModel.updateDocumentTitle(title) },
                        onBodyTextChanged = { text -> viewModel.updateDocumentBodyText(text) },
                        onWatermarkChanged = { text, enabled -> viewModel.updateDocumentWatermark(text, enabled) },
                        onRotatePage = { id -> viewModel.rotateDocumentPage(id) },
                        onMovePageUp = { idx -> viewModel.moveDocumentPageUp(idx) },
                        onMovePageDown = { idx -> viewModel.moveDocumentPageDown(idx) },
                        onDeletePage = { id -> viewModel.deleteDocumentPage(id) },
                        onAddImages = { uris -> viewModel.addDocumentPageImages(context, uris) },
                        onRunOcr = { viewModel.runOcrOnCurrentDocument(context) },
                        onSaveDocument = { name, asPdf -> viewModel.saveEditedDocument(context, name, asPdf) }
                    )
                }

                LuminaTab.TEXT_TO_PDF -> {
                    TextToPdfScreen(
                        options = textOptions,
                        isCompiling = isCompilingText,
                        onOptionsChanged = { opts -> viewModel.updateTextOptions(opts) },
                        onCompile = { viewModel.compileTextToPdf(context) }
                    )
                }

                LuminaTab.OCR_EXTRACT -> {
                    OcrScreen(
                        selectedImageUri = ocrImageUri,
                        isScanning = isScanningOcr,
                        ocrResult = ocrResult,
                        selectedScript = ocrSelectedScript,
                        selectedLanguage = selectedOcrLanguage,
                        onScriptSelected = { script -> viewModel.setOcrScript(context, script) },
                        onLanguageSelected = { lang -> viewModel.setOcrLanguage(context, lang) },
                        onImageSelected = { uri -> viewModel.setOcrImage(context, uri) },
                        onRunScan = { viewModel.runOcr(context) },
                        onSendToPdf = { text -> viewModel.sendOcrTextToTextPdf(text) }
                    )
                }

                LuminaTab.PDF_TO_IMAGE -> {
                    PdfToImageScreen(
                        pdfUri = pdfUriToExtract,
                        isRendering = isRenderingPdf,
                        renderedPages = renderedPdfPages,
                        selectedPageIndex = selectedPageToExport,
                        onPdfSelected = { uri -> viewModel.loadPdfForImageExtraction(context, uri) },
                        onSelectPage = { idx -> viewModel.selectPageToExport(idx) },
                        onExportPage = { idx -> viewModel.saveSelectedPageAsImage(context, idx) },
                        onExportAll = { viewModel.saveAllPagesAsImages(context) }
                    )
                }

                LuminaTab.CONNECT -> {
                    ConnectScreen()
                }

                LuminaTab.HISTORY -> {
                    DocumentsHistoryScreen(
                        records = historyRecords,
                        onDeleteRecord = { rec -> viewModel.deleteRecord(rec) },
                        onOpenInViewer = { record ->
                            viewModel.openDocumentInViewer(
                                context = context,
                                uri = Uri.fromFile(File(record.filePath)),
                                fileNameOverride = record.fileName
                            )
                        }
                    )
                }
            }

            // Success dialog when a PDF is generated
            lastGeneratedRecord?.let { record ->
                PdfResultDialog(
                    record = record,
                    onDismiss = {
                        viewModel.dismissGeneratedResult()
                    },
                    onOpenInAppViewer = {
                        viewModel.dismissGeneratedResult()
                        viewModel.openDocumentInViewer(
                            context = context,
                            uri = Uri.fromFile(File(record.filePath)),
                            fileNameOverride = record.fileName
                        )
                    }
                )
            }
        }
    }
}
