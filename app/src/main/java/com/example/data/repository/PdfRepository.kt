package com.example.data.repository

import com.example.data.db.PdfRecord
import com.example.data.db.PdfRecordDao
import kotlinx.coroutines.flow.Flow
import java.io.File

class PdfRepository(private val dao: PdfRecordDao) {
    val allRecords: Flow<List<PdfRecord>> = dao.getAllRecords()

    suspend fun insertRecord(record: PdfRecord): Long = dao.insertRecord(record)

    suspend fun deleteRecord(record: PdfRecord) {
        try {
            val file = File(record.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.deleteRecord(record)
    }

    suspend fun deleteRecordById(id: Int, filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.deleteRecordById(id)
    }
}
