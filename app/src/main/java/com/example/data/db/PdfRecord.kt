package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_records")
data class PdfRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceType: String = "IMAGE_TO_PDF" // IMAGE_TO_PDF, TEXT_TO_PDF, AI_STORY
)
