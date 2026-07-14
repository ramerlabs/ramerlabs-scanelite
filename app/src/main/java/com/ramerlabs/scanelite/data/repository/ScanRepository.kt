package com.ramerlabs.scanelite.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.ramerlabs.scanelite.data.local.DocumentDao
import com.ramerlabs.scanelite.data.local.DocumentEntity
import com.ramerlabs.scanelite.domain.ExportFormat
import com.ramerlabs.scanelite.domain.ScanDocument
import com.ramerlabs.scanelite.domain.ScanFilter
import com.ramerlabs.scanelite.domain.ScannedPage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DocumentDao
) {
    fun observeLibrary(): Flow<List<ScanDocument>> = dao.observeAll().map { list ->
        list.map {
            ScanDocument(
                id = it.id,
                title = it.title,
                createdAt = it.createdAt,
                pageCount = it.pageCount,
                thumbnailPath = it.thumbnailPath
            )
        }
    }

    suspend fun saveSession(title: String, pages: List<ScannedPage>): Long {
        val thumb = pages.firstOrNull()?.uriPath
        return dao.insert(
            DocumentEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                pageCount = pages.size,
                thumbnailPath = thumb
            )
        )
    }

    suspend fun deleteDocument(id: Long) = dao.delete(id)

    fun scansDir(): File = File(context.filesDir, "scans").also { it.mkdirs() }

    suspend fun persistBitmap(bitmap: Bitmap, filter: ScanFilter): ScannedPage =
        withContext(Dispatchers.IO) {
            val filtered = applyFilter(bitmap, filter)
            val file = File(scansDir(), "page_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                filtered.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (filtered !== bitmap) filtered.recycle()
            ScannedPage(
                id = UUID.randomUUID().toString(),
                uriPath = file.absolutePath,
                filter = filter
            )
        }

    fun applyFilter(src: Bitmap, filter: ScanFilter): Bitmap {
        if (filter == ScanFilter.Original) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint()
        val matrix = when (filter) {
            ScanFilter.MagicColor -> ColorMatrix(
                floatArrayOf(
                    1.25f, -0.05f, -0.05f, 0f, 18f,
                    -0.05f, 1.25f, -0.05f, 0f, 18f,
                    -0.05f, -0.05f, 1.25f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).also {
                // Boost contrast slightly for "clear copy"
                val contrast = ColorMatrix().apply { setScale(1.08f, 1.08f, 1.08f, 1f) }
                it.postConcat(contrast)
            }
            ScanFilter.BlackWhite -> ColorMatrix().apply {
                setSaturation(0f)
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.4f, 0f, 0f, 0f, -40f,
                        0f, 1.4f, 0f, 0f, -40f,
                        0f, 0f, 1.4f, 0f, -40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                postConcat(contrast)
            }
            ScanFilter.Original -> ColorMatrix()
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    suspend fun exportShareUri(
        pages: List<ScannedPage>,
        format: ExportFormat,
        title: String
    ): Uri = withContext(Dispatchers.IO) {
        val safe = title.replace(Regex("[^a-zA-Z0-9-_ ]"), "").ifBlank { "ScanElite" }
        val file = when (format) {
            ExportFormat.Pdf -> buildPdf(pages, safe)
            ExportFormat.Jpeg -> {
                val first = File(pages.first().uriPath)
                val dest = File(context.cacheDir, "$safe.jpg")
                first.copyTo(dest, overwrite = true)
                dest
            }
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun buildPdf(pages: List<ScannedPage>, name: String): File {
        val doc = PdfDocument()
        pages.forEachIndexed { index, page ->
            val bmp = android.graphics.BitmapFactory.decodeFile(page.uriPath) ?: return@forEachIndexed
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
            val pdfPage = doc.startPage(pageInfo)
            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(pdfPage)
            bmp.recycle()
        }
        val out = File(context.cacheDir, "$name.pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return out
    }
}
