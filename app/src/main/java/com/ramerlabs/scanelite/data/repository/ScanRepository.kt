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
        val file = buildExportFile(pages, format, title)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Saves cropped export into the device gallery / Downloads and returns display name.
     */
    suspend fun saveToGallery(
        pages: List<ScannedPage>,
        format: ExportFormat,
        title: String
    ): String = withContext(Dispatchers.IO) {
        val safe = sanitizeTitle(title)
        val cropped = pages.mapNotNull { page -> decodeCropped(page) }
        require(cropped.isNotEmpty()) { "No pages to export" }
        try {
            when (format) {
                ExportFormat.Jpeg -> {
                    val name = "$safe.jpg"
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                            put(
                                android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                                android.os.Environment.DIRECTORY_PICTURES + "/ScanElite"
                            )
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
                        android.provider.MediaStore.Images.Media.getContentUri(
                            android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
                        )
                    } else {
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val uri = context.contentResolver.insert(collection, values)
                        ?: error("Could not create gallery item")
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        cropped.first().compress(Bitmap.CompressFormat.JPEG, 95, out)
                    } ?: error("Could not write gallery item")
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        values.clear()
                        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                    name
                }
                ExportFormat.Pdf -> {
                    val name = "$safe.pdf"
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                            put(
                                android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                                android.os.Environment.DIRECTORY_DOWNLOADS + "/ScanElite"
                            )
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
                        android.provider.MediaStore.Downloads.getContentUri(
                            android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
                        )
                    } else {
                        android.provider.MediaStore.Files.getContentUri("external")
                    }
                    val uri = context.contentResolver.insert(collection, values)
                        ?: error("Could not create Downloads item")
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        val tmp = buildPdfFromBitmaps(cropped, safe)
                        tmp.inputStream().use { input -> input.copyTo(out) }
                        tmp.delete()
                    } ?: error("Could not write PDF")
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        values.clear()
                        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                    name
                }
            }
        } finally {
            cropped.forEach { it.recycle() }
        }
    }

    private fun sanitizeTitle(title: String): String =
        title.replace(Regex("[^a-zA-Z0-9-_ ]"), "").trim().ifBlank { "ScanElite" }
            .replace(' ', '_')

    private fun buildExportFile(
        pages: List<ScannedPage>,
        format: ExportFormat,
        title: String
    ): File {
        val safe = sanitizeTitle(title)
        val cropped = pages.mapNotNull { page -> decodeCropped(page) }
        require(cropped.isNotEmpty()) { "No pages to export" }
        return try {
            when (format) {
                ExportFormat.Pdf -> buildPdfFromBitmaps(cropped, safe)
                ExportFormat.Jpeg -> {
                    val dest = File(context.cacheDir, "$safe.jpg")
                    FileOutputStream(dest).use { out ->
                        cropped.first().compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    dest
                }
            }
        } finally {
            cropped.forEach { it.recycle() }
        }
    }

    private fun decodeCropped(page: ScannedPage): Bitmap? {
        val src = android.graphics.BitmapFactory.decodeFile(page.uriPath) ?: return null
        val crop = page.crop.clamped()
        val left = (crop.left * src.width).toInt().coerceIn(0, src.width - 1)
        val top = (crop.top * src.height).toInt().coerceIn(0, src.height - 1)
        val right = (crop.right * src.width).toInt().coerceIn(left + 1, src.width)
        val bottom = (crop.bottom * src.height).toInt().coerceIn(top + 1, src.height)
        val w = right - left
        val h = bottom - top
        val out = Bitmap.createBitmap(src, left, top, w, h)
        if (out !== src) src.recycle()
        return out
    }

    private fun buildPdfFromBitmaps(bitmaps: List<Bitmap>, name: String): File {
        val doc = PdfDocument()
        bitmaps.forEachIndexed { index, bmp ->
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
            val pdfPage = doc.startPage(pageInfo)
            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(pdfPage)
        }
        val out = File(context.cacheDir, "$name.pdf")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return out
    }
}
