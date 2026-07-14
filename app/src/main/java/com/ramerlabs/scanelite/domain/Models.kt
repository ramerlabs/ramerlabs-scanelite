package com.ramerlabs.scanelite.domain

enum class ScanMode { Single, Batch }

enum class ScanFilter {
    MagicColor,
    BlackWhite,
    Original
}

enum class EdgeStyle { NeonBlue, Gold }

enum class ExportFormat { Pdf, Jpeg }

data class ScannedPage(
    val id: String,
    val uriPath: String,
    val filter: ScanFilter = ScanFilter.MagicColor,
    val rotationDeg: Int = 0
)

data class ScanDocument(
    val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val pageCount: Int,
    val thumbnailPath: String?
)
