package com.ramerlabs.scanelite.domain

enum class ScanMode { Single, Batch }

enum class ScanFilter {
    MagicColor,
    BlackWhite,
    Original
}

enum class EdgeStyle { NeonBlue, Gold }

enum class ExportFormat { Pdf, Jpeg }

/** Normalized crop rectangle in 0f..1f image space. */
data class CropNorm(
    val left: Float = 0.08f,
    val top: Float = 0.08f,
    val right: Float = 0.92f,
    val bottom: Float = 0.92f
) {
    fun clamped(): CropNorm {
        val l = left.coerceIn(0f, 0.95f)
        val t = top.coerceIn(0f, 0.95f)
        val r = right.coerceIn(l + 0.05f, 1f)
        val b = bottom.coerceIn(t + 0.05f, 1f)
        return CropNorm(l, t, r, b)
    }
}

data class ScannedPage(
    val id: String,
    val uriPath: String,
    val filter: ScanFilter = ScanFilter.MagicColor,
    val rotationDeg: Int = 0,
    val crop: CropNorm = CropNorm()
)

data class ScanDocument(
    val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val pageCount: Int,
    val thumbnailPath: String?
)
