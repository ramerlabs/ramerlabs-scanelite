package com.ramerlabs.scanelite.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val pageCount: Int,
    val thumbnailPath: String?
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Insert
    suspend fun insert(doc: DocumentEntity): Long

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class ScanEliteDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
