package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.domain.document.DocumentPage
import com.example.domain.document.DocumentType
import com.example.domain.document.ParsedDocument
import com.example.ui.theme.LuminaAccent
import com.example.ui.theme.LuminaEmerald
import com.example.ui.theme.LuminaPrimary
import com.example.util.FileUtils
import java.io.File

enum class ViewerMode {
    VIEW,
    EDIT
}

enum class EditSubTab {
    TEXT,
    PAGES_IMAGES,
    WATERMARK
}

@Composable
fun DocumentViewerEditorScreen(
    document: ParsedDocument?,
    isLoading: Boolean,
    isSaving: Boolean,
    isOcrScanning: Boolean,
    onOpenFile: (Uri) -> Unit,
    onTitleChanged: (String) -> Unit,
    onBodyTextChanged: (String) -> Unit,
    onWatermarkChanged: (String, Boolean) -> Unit,
    onRotatePage: (String) -> Unit,
    onMovePageUp: (Int) -> Unit,
    onMovePageDown: (Int) -> Unit,
    onDeletePage: (String) -> Unit,
    onAddImages: (List<Uri>) -> Unit,
    onRunOcr: () -> Unit,
    onSaveDocument: (customName: String, asPdf: Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(ViewerMode.VIEW) }
    var editTab by remember { mutableStateOf(EditSubTab.TEXT) }
    var customSaveName by remember(document?.title) { mutableStateOf(document?.title ?: "Edited_Document") }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showDirectPdfEditor by remember { mutableStateOf(false) }

    if (showDirectPdfEditor && document != null) {
        PdfDirectViewerEditorScreen(
            document = document,
            isLoading = isLoading,
            isSaving = isSaving,
            onClose = { showDirectPdfEditor = false },
            onSavePdf = { customName ->
                onSaveDocument(customName, true)
                showDirectPdfEditor = false
            }
        )
        return
    }

    // File picker launcher for all supported file types
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onOpenFile(uri)
        }
    }

    // Image picker for adding new images/pages
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddImages(uris)
        }
    }

    // Camera launcher & permission handling
    val capturedPhotoFile = remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoFile.value?.let { file ->
                if (file.exists() && file.length() > 0) {
                    onAddImages(listOf(Uri.fromFile(file)))
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val photoFile = File(photosDir, "viewer_cam_${System.currentTimeMillis()}.jpg")
                capturedPhotoFile.value = photoFile
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to snap photos for your document", Toast.LENGTH_LONG).show()
        }
    }

    fun takeCameraPhoto() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            try {
                val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val photoFile = File(photosDir, "viewer_cam_${System.currentTimeMillis()}.jpg")
                capturedPhotoFile.value = photoFile
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("document_viewer_screen")
    ) {
        // Document Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Document Title & Format
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (document?.documentType) {
                                    DocumentType.PDF -> Icons.Default.PictureAsPdf
                                    DocumentType.DOCX, DocumentType.DOC -> Icons.Default.Description
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = LuminaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = document?.fileName ?: "Document Viewer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (document != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LuminaPrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = document.documentType.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuminaPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = FileUtils.formatFileSize(document.fileSizeBytes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (document.pages.isNotEmpty()) {
                                        Text(
                                            text = " • ${document.pages.size} pages",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Open PDF, Word, or text files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Top Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/msword",
                                        "text/*",
                                        "image/*"
                                    )
                                )
                            },
                            modifier = Modifier.testTag("open_document_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Open Document",
                                tint = LuminaPrimary
                            )
                        }

                        if (document != null) {
                            IconButton(
                                onClick = {
                                    if (document.sourcePath != null) {
                                        FileUtils.sharePdf(context, document.sourcePath)
                                    } else if (document.bodyText.isNotBlank()) {
                                        FileUtils.shareText(context, document.title, document.bodyText)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (document != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode Segmented Switch: VIEW vs EDIT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (currentMode == ViewerMode.VIEW) LuminaPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { currentMode = ViewerMode.VIEW }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (currentMode == ViewerMode.VIEW) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "View Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (currentMode == ViewerMode.VIEW) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (currentMode == ViewerMode.EDIT) LuminaPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { currentMode = ViewerMode.EDIT }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (currentMode == ViewerMode.EDIT) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (currentMode == ViewerMode.EDIT) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (document.documentType == DocumentType.PDF) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showDirectPdfEditor = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("launch_direct_pdf_editor_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Visual On-PDF Editor (Click to Edit Text)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LuminaPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading document...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (document == null) {
            // Empty State
            EmptyViewerState(
                onOpenFileClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "text/*",
                            "image/*"
                        )
                    )
                }
            )
        } else if (currentMode == ViewerMode.VIEW) {
            // View Mode
            DocumentViewerContent(
                document = document,
                zoomScale = zoomScale,
                onZoomIn = { if (zoomScale < 2.5f) zoomScale += 0.25f },
                onZoomOut = { if (zoomScale > 0.75f) zoomScale -= 0.25f },
                onResetZoom = { zoomScale = 1.0f },
                onSwitchToEdit = { currentMode = ViewerMode.EDIT }
            )
        } else {
            // Edit Mode
            DocumentEditorContent(
                document = document,
                editTab = editTab,
                onTabSelected = { editTab = it },
                customSaveName = customSaveName,
                onCustomSaveNameChange = { customSaveName = it },
                isSaving = isSaving,
                isOcrScanning = isOcrScanning,
                onTitleChanged = onTitleChanged,
                onBodyTextChanged = onBodyTextChanged,
                onWatermarkChanged = onWatermarkChanged,
                onRotatePage = onRotatePage,
                onMovePageUp = onMovePageUp,
                onMovePageDown = onMovePageDown,
                onDeletePage = onDeletePage,
                onPickImages = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onCaptureCamera = { takeCameraPhoto() },
                onRunOcr = onRunOcr,
                onSaveDocument = onSaveDocument
            )
        }
    }
}

@Composable
private fun EmptyViewerState(
    onOpenFileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = LuminaPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Built-in Document Viewer & Editor",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "View and edit PDFs, Word documents (DOCX/DOC), text files, and images directly on your phone without needing third-party viewer apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOpenFileClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_open_file_button")
                ) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Document from Phone",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentViewerContent(
    document: ParsedDocument,
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onSwitchToEdit: () -> Unit
) {
    var viewPagesOrText by remember { mutableStateOf(if (document.pages.isNotEmpty()) "pages" else "text") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Toggle between Pages view & Text view if both exist
                if (document.pages.isNotEmpty() && document.bodyText.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "Pages (${document.pages.size})",
                            fontSize = 12.sp,
                            fontWeight = if (viewPagesOrText == "pages") FontWeight.Bold else FontWeight.Normal,
                            color = if (viewPagesOrText == "pages") LuminaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { viewPagesOrText = "pages" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Text",
                            fontSize = 12.sp,
                            fontWeight = if (viewPagesOrText == "text") FontWeight.Bold else FontWeight.Normal,
                            color = if (viewPagesOrText == "text") LuminaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { viewPagesOrText = "text" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (document.pages.isNotEmpty()) {
                    Text(
                        text = "${document.pages.size} Page(s)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "${document.bodyText.length} characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Zoom Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onResetZoom() }
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onSwitchToEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        }

        // Render Pages or Text
        if (viewPagesOrText == "pages" && document.pages.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(document.pages, key = { _, item -> item.id }) { index, page ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                            modifier = Modifier
                                .fillMaxWidth(zoomScale.coerceIn(0.5f, 1.0f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                page.bitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .rotate(page.rotation.toFloat())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Page ${index + 1} of ${document.pages.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Text Document Reader View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (document.title.isNotBlank()) {
                            Text(
                                text = document.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Text(
                            text = document.bodyText.ifBlank { "No text extracted from this document yet. Switch to Edit mode and tap 'Extract Text with OCR' to scan the text from PDF pages." },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontFamily = FontFamily.Default
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentEditorContent(
    document: ParsedDocument,
    editTab: EditSubTab,
    onTabSelected: (EditSubTab) -> Unit,
    customSaveName: String,
    onCustomSaveNameChange: (String) -> Unit,
    isSaving: Boolean,
    isOcrScanning: Boolean,
    onTitleChanged: (String) -> Unit,
    onBodyTextChanged: (String) -> Unit,
    onWatermarkChanged: (String, Boolean) -> Unit,
    onRotatePage: (String) -> Unit,
    onMovePageUp: (Int) -> Unit,
    onMovePageDown: (Int) -> Unit,
    onDeletePage: (String) -> Unit,
    onPickImages: () -> Unit,
    onCaptureCamera: () -> Unit,
    onRunOcr: () -> Unit,
    onSaveDocument: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Edit Sub Tabs
        TabRow(
            selectedTabIndex = editTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = LuminaPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[editTab.ordinal]),
                    color = LuminaPrimary
                )
            }
        ) {
            Tab(
                selected = editTab == EditSubTab.TEXT,
                onClick = { onTabSelected(EditSubTab.TEXT) },
                text = { Text("Edit Text", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )

            Tab(
                selected = editTab == EditSubTab.PAGES_IMAGES,
                onClick = { onTabSelected(EditSubTab.PAGES_IMAGES) },
                text = { Text("Pages & Images (${document.pages.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )

            Tab(
                selected = editTab == EditSubTab.WATERMARK,
                onClick = { onTabSelected(EditSubTab.WATERMARK) },
                text = { Text("Watermark", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.BrandingWatermark, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Sub Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (editTab) {
                EditSubTab.TEXT -> {
                    EditTextSection(
                        document = document,
                        isOcrScanning = isOcrScanning,
                        onTitleChanged = onTitleChanged,
                        onBodyTextChanged = onBodyTextChanged,
                        onRunOcr = onRunOcr
                    )
                }

                EditSubTab.PAGES_IMAGES -> {
                    EditPagesSection(
                        pages = document.pages,
                        onRotatePage = onRotatePage,
                        onMovePageUp = onMovePageUp,
                        onMovePageDown = onMovePageDown,
                        onDeletePage = onDeletePage,
                        onPickImages = onPickImages,
                        onCaptureCamera = onCaptureCamera
                    )
                }

                EditSubTab.WATERMARK -> {
                    EditWatermarkSection(
                        watermarkText = document.watermarkText,
                        addWatermark = document.addWatermark,
                        onWatermarkChanged = onWatermarkChanged
                    )
                }
            }
        }

        // Bottom Sticky Action Bar: Save to Phone!
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Name input
                OutlinedTextField(
                    value = customSaveName,
                    onValueChange = onCustomSaveNameChange,
                    label = { Text("Save As File Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuminaPrimary,
                        cursorColor = LuminaPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onSaveDocument(customSaveName, true) },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_edited_pdf_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Phone (PDF)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // If text document, also offer saving as text
                    if (document.bodyText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onSaveDocument(customSaveName, false) },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Save as Text", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTextSection(
    document: ParsedDocument,
    isOcrScanning: Boolean,
    onTitleChanged: (String) -> Unit,
    onBodyTextChanged: (String) -> Unit,
    onRunOcr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Document Title
        OutlinedTextField(
            value = document.title,
            onValueChange = onTitleChanged,
            label = { Text("Document Title / Heading") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LuminaPrimary,
                cursorColor = LuminaPrimary
            )
        )

        // OCR Action Bar if pages exist
        if (document.pages.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Extract Text from PDF Pages",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Uses on-device OCR to read text from all ${document.pages.size} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = onRunOcr,
                        enabled = !isOcrScanning,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isOcrScanning) {
                            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Scan OCR", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Editable Body Text
        OutlinedTextField(
            value = document.bodyText,
            onValueChange = onBodyTextChanged,
            label = { Text("Document Content / Text") },
            placeholder = { Text("Type, edit or paste document text here...") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LuminaPrimary,
                cursorColor = LuminaPrimary
            )
        )

        Text(
            text = "Total characters: ${document.bodyText.length} • Words: ${document.bodyText.split(Regex("\\s+")).filter { it.isNotBlank() }.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditPagesSection(
    pages: List<DocumentPage>,
    onRotatePage: (String) -> Unit,
    onMovePageUp: (Int) -> Unit,
    onMovePageDown: (Int) -> Unit,
    onDeletePage: (String) -> Unit,
    onPickImages: () -> Unit,
    onCaptureCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Add Pages / Images Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPickImages,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Photos", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onCaptureCamera,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Snap Camera", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pages or images in this document.\nTap above to add images or take photos.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                page.bitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .rotate(page.rotation.toFloat())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Page Label
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Page ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Rotation: ${page.rotation}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onRotatePage(page.id) }) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = LuminaPrimary)
                                }
                                IconButton(
                                    onClick = { onMovePageUp(index) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                }
                                IconButton(
                                    onClick = { onMovePageDown(index) },
                                    enabled = index < pages.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                }
                                IconButton(onClick = { onDeletePage(page.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditWatermarkSection(
    watermarkText: String,
    addWatermark: Boolean,
    onWatermarkChanged: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Document Watermark",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prints subtle watermark footer on all exported pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = addWatermark,
                        onCheckedChange = { onWatermarkChanged(watermarkText, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LuminaPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { onWatermarkChanged(it, addWatermark) },
                    label = { Text("Watermark Text") },
                    placeholder = { Text("e.g. created by lumina, Confidential, Draft") },
                    enabled = addWatermark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuminaPrimary,
                        cursorColor = LuminaPrimary
                    )
                )
            }
        }
    }
}
