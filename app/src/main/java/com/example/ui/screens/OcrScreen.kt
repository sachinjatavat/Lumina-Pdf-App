package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.ocr.OcrLanguageScript
import com.example.domain.ocr.OcrLanguages
import com.example.domain.ocr.OcrResult
import com.example.domain.ocr.SupportedOcrLanguage
import com.example.ui.theme.LuminaEmerald
import com.example.ui.theme.LuminaPrimary
import com.example.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    selectedImageUri: Uri?,
    isScanning: Boolean,
    ocrResult: OcrResult?,
    selectedScript: OcrLanguageScript,
    selectedLanguage: SupportedOcrLanguage = OcrLanguages.AUTO_DETECT,
    onScriptSelected: (OcrLanguageScript) -> Unit,
    onLanguageSelected: (SupportedOcrLanguage) -> Unit = {},
    onImageSelected: (Uri) -> Unit,
    onRunScan: () -> Unit,
    onSendToPdf: (String) -> Unit
) {
    val context = LocalContext.current
    var editableText by remember(ocrResult?.fullText) {
        mutableStateOf(ocrResult?.fullText ?: "")
    }

    var showLanguageSheet by remember { mutableStateOf(false) }
    var languageSearchQuery by remember { mutableStateOf("") }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    val capturedPhotoFile = remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoFile.value?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val uri = Uri.fromFile(file)
                    onImageSelected(uri)
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
                val photoFile = File(photosDir, "ocr_${System.currentTimeMillis()}.jpg")
                capturedPhotoFile.value = photoFile
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to scan documents with camera", Toast.LENGTH_LONG).show()
        }
    }

    fun takeCameraPhoto() {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            try {
                val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val photoFile = File(photosDir, "ocr_${System.currentTimeMillis()}.jpg")
                capturedPhotoFile.value = photoFile
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = LuminaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Universal Multi-Language OCR",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Extracts every language script, special symbols & math",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OCR Recognition Language:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedLanguage.id > 0) "#${selectedLanguage.id} ${selectedLanguage.name}" else selectedLanguage.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LuminaPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { showLanguageSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("browse_70_languages_button")
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp), tint = LuminaPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All 70 Languages", fontSize = 12.sp, color = LuminaPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick popular languages from the 70
                        val quickLangs = listOf(
                            OcrLanguages.AUTO_DETECT,
                            OcrLanguages.findById(1),  // English
                            OcrLanguages.findById(2),  // Mandarin Chinese
                            OcrLanguages.findById(3),  // Hindi
                            OcrLanguages.findById(4),  // Spanish
                            OcrLanguages.findById(5),  // Arabic
                            OcrLanguages.findById(6),  // French
                            OcrLanguages.findById(7),  // Bengali
                            OcrLanguages.findById(8),  // Portuguese
                            OcrLanguages.findById(9),  // Russian
                            OcrLanguages.findById(11), // Urdu
                            OcrLanguages.findById(12), // German
                            OcrLanguages.findById(13), // Japanese
                            OcrLanguages.findById(28)  // Korean
                        )

                        quickLangs.forEach { lang ->
                            val isSelected = selectedLanguage.id == lang.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onLanguageSelected(lang) },
                                label = {
                                    Text(
                                        text = lang.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LuminaPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF8FAFC),
                                    labelColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("ocr_lang_${lang.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("ocr_pick_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gallery",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { takeCameraPhoto() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("ocr_camera_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Camera",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Image Preview & Scan Action
        if (selectedImageUri != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "OCR Input Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onRunScan,
                            enabled = !isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_ocr_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recognizing Text...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Text Extraction", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Extracted Result Card
        if (ocrResult != null || editableText.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted Text",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (ocrResult != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFEEF2FF))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = ocrResult.detectedScript,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuminaPrimary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(LuminaEmerald.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${ocrResult.lineCount} Lines",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuminaEmerald
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${editableText.length} Chars",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuminaPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editableText,
                            onValueChange = { editableText = it },
                            placeholder = { Text("Extracted text will appear here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("ocr_result_textfield"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions: Copy, Share, Convert to PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    FileUtils.copyToClipboard(context, "Lumina OCR", editableText)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ocr_copy_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }

                            OutlinedButton(
                                onClick = {
                                    FileUtils.shareText(context, "Scanned Text via Lumina", editableText)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ocr_share_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onSendToPdf(editableText) },
                            enabled = editableText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ocr_to_pdf_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert Extracted Text to PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E293B)
        ) {
            val filteredLanguages = remember(languageSearchQuery) {
                if (languageSearchQuery.isBlank()) {
                    OcrLanguages.ALL_70_LANGUAGES
                } else {
                    OcrLanguages.ALL_70_LANGUAGES.filter {
                        it.name.contains(languageSearchQuery, ignoreCase = true) ||
                        it.nativeName.contains(languageSearchQuery, ignoreCase = true) ||
                        it.id.toString() == languageSearchQuery.trim()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Language (All 70)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Optimized on-device ML Kit OCR engines",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { showLanguageSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = languageSearchQuery,
                    onValueChange = { languageSearchQuery = it },
                    placeholder = { Text("Search 70 languages...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LuminaPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LuminaPrimary,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedLanguage.id == 0) LuminaPrimary.copy(alpha = 0.25f) else Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageSelected(OcrLanguages.AUTO_DETECT)
                                    showLanguageSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("🌐 Auto Detect (Universal)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text("Detects Latin, Cyrillic, Indic, CJK & Arabic scripts", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                if (selectedLanguage.id == 0) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = LuminaPrimary)
                                }
                            }
                        }
                    }

                    items(filteredLanguages) { lang ->
                        val isSel = selectedLanguage.id == lang.id

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) LuminaPrimary.copy(alpha = 0.25f) else Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageSelected(lang)
                                    showLanguageSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF334155), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${lang.id}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(lang.name, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.5.sp)
                                        Text(lang.nativeName, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }

                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = LuminaPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
