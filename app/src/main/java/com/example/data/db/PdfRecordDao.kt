package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfRecordDao {
    @Query("SELECT * FROM pdf_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<PdfRecord>>

    @Query("SELECT * FROM pdf_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Int): PdfRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PdfRecord): Long

    @Delete
    suspend fun deleteRecord(record: PdfRecord)

    @Query("DELETE FROM pdf_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM pdf_records")
    suspend fun clearAll()
}
