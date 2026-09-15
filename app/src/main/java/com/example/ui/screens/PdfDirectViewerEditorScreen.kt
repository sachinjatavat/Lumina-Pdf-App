package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.document.DocumentPage
import com.example.domain.document.DocumentParser
import com.example.domain.document.ParsedDocument
import com.example.domain.document.PdfTextBlock
import com.example.domain.ocr.OcrScanner
import com.example.ui.theme.LuminaEmerald
import com.example.ui.theme.LuminaPrimary
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDirectViewerEditorScreen(
    document: ParsedDocument?,
    isLoading: Boolean,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSavePdf: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isEditMode by remember { mutableStateOf(false) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var selectedBlock by remember { mutableStateOf<PdfTextBlock?>(null) }
    var isExtractingBlocks by remember { mutableStateOf(false) }

    // Transformation state for zoom & pan
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Page state refresh trigger
    var renderVersion by remember { mutableIntStateOf(0) }

    // Add Text Dialog State
    var showAddTextDialog by remember { mutableStateOf(false) }
    var newTextContent by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDocName by remember(document?.title) {
        mutableStateOf(document?.title ?: "Edited_Document")
    }

    val pages = document?.pages ?: emptyList()
    val activePage = pages.getOrNull(currentPageIndex)

    // Load blocks for current page when entering edit mode if not loaded yet
    LaunchedEffect(isEditMode, currentPageIndex, renderVersion) {
        if (isEditMode && activePage != null && !activePage.isBlocksLoaded && activePage.bitmap != null) {
            isExtractingBlocks = true
            val blocks = withContext(Dispatchers.Default) {
                OcrScanner.extractTextBlocksFromBitmap(activePage.bitmap!!, activePage.pageIndex)
            }
            activePage.textBlocks = blocks
            activePage.isBlocksLoaded = true
            isExtractingBlocks = false
            renderVersion++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = LuminaPrimary, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Opening PDF Document...",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            return@Box
        }

        if (document == null || pages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No PDF Content Loaded",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unable to preview the requested PDF file.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back")
                }
            }
            return@Box
        }

        // ==========================================
        // Main PDF Page Viewport
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = if (isEditMode) 90.dp else 70.dp)
                .pointerInput(isEditMode) {
                    if (!isEditMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                            if (zoomScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            activePage?.bitmap?.let { bmp ->
                val bmpW = bmp.width.toFloat()
                val bmpH = bmp.height.toFloat()
                val aspect = if (bmpH > 0f) bmpW / bmpH else 1f

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val availableW = maxWidth
                    val availableH = maxHeight

                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .aspectRatio(aspect)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                    ) {
                        // 1. The PDF Page Bitmap
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // 2. Interactive Overlays in Edit Mode
                        if (isEditMode) {
                            val activeBlocks = activePage.textBlocks.filter { !it.isDeleted }

                            for (block in activeBlocks) {
                                val leftRatio = (block.boundingBox.left / bmpW).coerceIn(0f, 1f)
                                val topRatio = (block.boundingBox.top / bmpH).coerceIn(0f, 1f)
                                val widthRatio = (block.boundingBox.width() / bmpW).coerceIn(0f, 1f)
                                val heightRatio = (block.boundingBox.height() / bmpH).coerceIn(0f, 1f)

                                val isSelected = selectedBlock?.id == block.id

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset {
                                            IntOffset(
                                                x = (leftRatio * this@BoxWithConstraints.constraints.maxWidth).roundToInt(),
                                                y = (topRatio * this@BoxWithConstraints.constraints.maxHeight).roundToInt()
                                            )
                                        }
                                        .size(
                                            width = (widthRatio * this@BoxWithConstraints.maxWidth.value).dp,
                                            height = (heightRatio * this@BoxWithConstraints.maxHeight.value).dp
                                        )
                                        .background(
                                            if (isSelected) Color(0x5538BDF8) else Color(0x2238BDF8),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) LuminaPrimary else Color(0x9938BDF8),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            selectedBlock = block
                                        }
                                        .testTag("pdf_block_${block.id}")
                                ) {
                                    // Corner edit hint
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .size(18.dp)
                                                .background(LuminaPrimary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editing",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Scanning spinner
                        if (isExtractingBlocks) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x77000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E293B)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = LuminaPrimary,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Analyzing text paragraphs...",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Minimal Floating Top Bar
        // (Clean, unobtrusive, shows ONLY PDF & Edit)
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color(0xCC0F172A),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (isEditMode) {
                                isEditMode = false
                                selectedBlock = null
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditMode) "Cancel Edit" else "Exit",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = if (isEditMode) "Editing PDF" else (document.title.ifBlank { "PDF Viewer" }),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${currentPageIndex + 1} of ${pages.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditMode) {
                        // Save Edited PDF Button
                        Button(
                            onClick = { showSaveDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaEmerald),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("save_edited_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // THE REQUESTED "EDIT" BUTTON
                        Button(
                            onClick = {
                                isEditMode = true
                                zoomScale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_pdf_editor_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit PDF",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Share action
                        IconButton(
                            onClick = {
                                document.sourcePath?.let { path ->
                                    val f = File(path)
                                    if (f.exists()) {
                                        FileUtils.shareFile(context, f, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "File path unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Saved copy needed to share", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Bottom Floating Bar / Controls
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xCC0F172A),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isEditMode) {
                    // Edit Mode Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Tap any paragraph on the PDF to edit or remove",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { showAddTextDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LuminaPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Text",
                                tint = LuminaPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Text", color = LuminaPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Page Navigation (if multi-page)
                if (pages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                    selectedBlock = null
                                    zoomScale = 1f
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NavigateBefore,
                                contentDescription = "Previous Page",
                                tint = if (currentPageIndex > 0) Color.White else Color.DarkGray
                            )
                        }

                        Text(
                            text = "${currentPageIndex + 1} / ${pages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        IconButton(
                            onClick = {
                                if (currentPageIndex < pages.size - 1) {
                                    currentPageIndex++
                                    selectedBlock = null
                                    zoomScale = 1f
                                }
                            },
                            enabled = currentPageIndex < pages.size - 1,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NavigateNext,
                                contentDescription = "Next Page",
                                tint = if (currentPageIndex < pages.size - 1) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Modal Bottom Sheet: Selected Paragraph Editor
        // (Click on any paragraph on the PDF to change or remove!)
        // ==========================================
        selectedBlock?.let { block ->
            ModalBottomSheet(
                onDismissRequest = { selectedBlock = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF1E293B)
            ) {
                var editedText by remember(block.id) { mutableStateOf(block.text) }
                var isBold by remember(block.id) { mutableStateOf(block.isBold) }
                var selectedColor by remember(block.id) { mutableStateOf(block.textColor) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit Paragraph on PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { selectedBlock = null }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Modify the paragraph below or click Remove to erase it from the PDF page:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("paragraph_edit_text_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = LuminaPrimary,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Formatting Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Color:", color = Color.LightGray, fontSize = 12.sp)
                            val colors = listOf(
                                AndroidColor.BLACK to Color.Black,
                                AndroidColor.DKGRAY to Color.DarkGray,
                                AndroidColor.parseColor("#1D4ED8") to Color(0xFF1D4ED8),
                                AndroidColor.parseColor("#B91C1C") to Color(0xFFB91C1C)
                            )
                            colors.forEach { (aCol, cCol) ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(cCol)
                                        .border(
                                            width = if (selectedColor == aCol) 2.5.dp else 1.dp,
                                            color = if (selectedColor == aCol) Color.White else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = aCol }
                                )
                            }
                        }

                        IconButton(
                            onClick = { isBold = !isBold },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isBold) LuminaPrimary else Color(0xFF334155),
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ACTIONS: REMOVE PARAGRAPH vs UPDATE TEXT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. REMOVE PARAGRAPH (The user's explicit example: "there are pagarah on the pdf i want to remove")
                        OutlinedButton(
                            onClick = {
                                activePage?.let { page ->
                                    DocumentParser.removeParagraphFromPageBitmap(page, block)
                                    renderVersion++
                                    selectedBlock = null
                                    Toast.makeText(context, "Paragraph removed from PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("remove_paragraph_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remove",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        // 2. UPDATE PARAGRAPH TEXT
                        Button(
                            onClick = {
                                activePage?.let { page ->
                                    DocumentParser.replaceParagraphOnPageBitmap(
                                        page = page,
                                        block = block,
                                        newText = editedText,
                                        textColor = selectedColor,
                                        isBold = isBold
                                    )
                                    renderVersion++
                                    selectedBlock = null
                                    Toast.makeText(context, "Text updated on PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("update_paragraph_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Update",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Update",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // ==========================================
        // Add Custom Text Dialog
        // ==========================================
        if (showAddTextDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showAddTextDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Add Text to PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter text to stamp on the current page:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newTextContent,
                            onValueChange = { newTextContent = it },
                            placeholder = { Text("Enter text...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LuminaPrimary,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showAddTextDialog = false },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (newTextContent.isNotBlank() && activePage != null) {
                                        DocumentParser.addTextToPageBitmap(
                                            page = activePage,
                                            text = newTextContent,
                                            x = 60f,
                                            y = 120f
                                        )
                                        renderVersion++
                                        newTextContent = ""
                                        showAddTextDialog = false
                                        Toast.makeText(context, "Text added to PDF page", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add to Page")
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Save PDF Dialog
        // ==========================================
        if (showSaveDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSaveDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Save Edited PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The PDF with your removed paragraphs and text changes will be saved to your phone's Downloads/LuminaConvert folder:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = saveDocName,
                            onValueChange = { saveDocName = it },
                            label = { Text("File Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LuminaPrimary,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showSaveDialog = false },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    showSaveDialog = false
                                    onSavePdf(saveDocName)
                                    isEditMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuminaEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Phone")
                            }
                        }
                    }
                }
            }
        }
    }
}
