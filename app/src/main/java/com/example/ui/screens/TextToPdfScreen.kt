package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PaperMargin
import com.example.domain.model.PaperSize
import com.example.domain.model.TextToPdfOptions
import com.example.ui.theme.LuminaPrimary

@Composable
fun TextToPdfScreen(
    options: TextToPdfOptions,
    isCompiling: Boolean,
    onOptionsChanged: (TextToPdfOptions) -> Unit,
    onCompile: () -> Unit
) {
    var showFormatSettings by remember { mutableStateOf(false) }

    val wordCount = remember(options.bodyText) {
        if (options.bodyText.isBlank()) 0
        else options.bodyText.trim().split(Regex("\\s+")).size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("text_to_pdf_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header & Presets
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Text to PDF Compiler",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Format essays, notes & contracts directly to PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showFormatSettings = !showFormatSettings },
                            modifier = Modifier.testTag("toggle_text_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Typography Settings",
                                tint = if (showFormatSettings) LuminaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Quick Templates",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            SuggestionChip(
                                onClick = {
                                    onOptionsChanged(
                                        options.copy(
                                            title = "Project Meeting Notes",
                                            author = "Team Sync",
                                            bodyText = "Date: September 14, 2026\nAttendees: Sarah, Alex, Michael\n\n1. Project Overview\nWe reviewed the mobile migration roadmap and key architectural milestones.\n\n2. Action Items\n- Finalize client-side image-to-pdf pipeline\n- Add zero-latency on-device OCR\n- Verify export fidelity\n\n3. Next Steps\nShip testing build and collect field feedback."
                                        )
                                    )
                                },
                                label = { Text("Meeting Notes") }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    onOptionsChanged(
                                        options.copy(
                                            title = "Standard Service Agreement",
                                            author = "Legal Dept",
                                            bodyText = "THIS AGREEMENT is entered into between Client and Provider.\n\n1. Scope of Work\nProvider agrees to supply digital document transformation services on-device without cloud data retention.\n\n2. Privacy & Security\nAll document parsing, bitmap rendering, and PDF compilation occurs within the local sandboxed environment.\n\nSigned,\nClient: ______________   Provider: ______________"
                                        )
                                    )
                                },
                                label = { Text("Agreement") }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    onOptionsChanged(
                                        options.copy(
                                            title = "",
                                            author = "",
                                            bodyText = ""
                                        )
                                    )
                                },
                                label = { Text("Clear") }
                            )
                        }
                    }
                }
            }
        }

        // Optional Typography Settings
        item {
            AnimatedVisibility(visible = showFormatSettings) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Page & Typography Layout",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Paper Size
                        Column {
                            Text(
                                text = "Paper Standard",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(PaperSize.A4, PaperSize.LETTER).forEach { size ->
                                    FilterChip(
                                        selected = options.paperSize == size,
                                        onClick = { onOptionsChanged(options.copy(paperSize = size)) },
                                        label = { Text(size.label, fontSize = 11.sp) },
                                        leadingIcon = if (options.paperSize == size) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Margins
                        Column {
                            Text(
                                text = "Margins",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(PaperMargin.SMALL, PaperMargin.MEDIUM).forEach { margin ->
                                    FilterChip(
                                        selected = options.margin == margin,
                                        onClick = { onOptionsChanged(options.copy(margin = margin)) },
                                        label = { Text(margin.label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Font size slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Body Font Size",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${options.fontSizeSp.toInt()} pt",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LuminaPrimary
                                )
                            }
                            Slider(
                                value = options.fontSizeSp,
                                onValueChange = { onOptionsChanged(options.copy(fontSizeSp = it)) },
                                valueRange = 10f..22f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = LuminaPrimary,
                                    activeTrackColor = LuminaPrimary
                                )
                            )
                        }

                        // Page numbers switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Show 'Page X' Footers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = options.showPageNumbers,
                                onCheckedChange = { onOptionsChanged(options.copy(showPageNumbers = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LuminaPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        // Watermark switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Watermark",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adds 'created by lumina' below each page",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = options.addWatermark,
                                onCheckedChange = { onOptionsChanged(options.copy(addWatermark = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LuminaPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Title and Author Fields
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
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = options.title,
                        onValueChange = { onOptionsChanged(options.copy(title = it)) },
                        label = { Text("Document Title") },
                        placeholder = { Text("e.g. Annual Report Summary") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("text_pdf_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = options.author,
                        onValueChange = { onOptionsChanged(options.copy(author = it)) },
                        label = { Text("Author / Organization (Optional)") },
                        placeholder = { Text("e.g. Lumina Editorial") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Main Text Editor
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
                            text = "Document Body",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$wordCount words • ${options.bodyText.length} chars",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = options.bodyText,
                        onValueChange = { onOptionsChanged(options.copy(bodyText = it)) },
                        placeholder = { Text("Type or paste your text here...\n\nParagraphs and line breaks will automatically wrap across pages with professional margins.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("text_pdf_body_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Compile Button
        item {
            Button(
                onClick = onCompile,
                enabled = !isCompiling && (options.bodyText.isNotBlank() || options.title.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("compile_text_pdf_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LuminaPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Rendering PDF Pages...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generate Multipage PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
