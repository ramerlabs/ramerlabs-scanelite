package com.ramerlabs.scanelite.ui.session

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramerlabs.scanelite.data.repository.ScanRepository
import com.ramerlabs.scanelite.domain.CropNorm
import com.ramerlabs.scanelite.domain.EdgeStyle
import com.ramerlabs.scanelite.domain.ExportFormat
import com.ramerlabs.scanelite.domain.ScanFilter
import com.ramerlabs.scanelite.domain.ScanMode
import com.ramerlabs.scanelite.domain.ScannedPage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val mode: ScanMode = ScanMode.Single,
    val autoCapture: Boolean = true,
    val edgeStyle: EdgeStyle = EdgeStyle.NeonBlue,
    val edgeLocked: Boolean = false,
    val capturing: Boolean = false,
    val processing: Boolean = false,
    val pages: List<ScannedPage> = emptyList(),
    val editingPageId: String? = null,
    val cropPageIndex: Int = 0,
    val activeFilter: ScanFilter = ScanFilter.MagicColor,
    val documentTitle: String = nextAutoName(),
    val exportFormat: ExportFormat = ExportFormat.Jpeg,
    val message: String? = null
)

fun nextAutoName(): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
    return "ScanElite_$stamp"
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    fun setMode(mode: ScanMode) = _state.update { it.copy(mode = mode) }
    fun setAutoCapture(enabled: Boolean) = _state.update { it.copy(autoCapture = enabled) }
    fun setEdgeStyle(style: EdgeStyle) = _state.update { it.copy(edgeStyle = style) }
    fun setEdgeLocked(locked: Boolean) = _state.update { it.copy(edgeLocked = locked) }
    fun setTitle(title: String) = _state.update { it.copy(documentTitle = title) }
    fun setExportFormat(format: ExportFormat) = _state.update { it.copy(exportFormat = format) }
    fun clearMessage() = _state.update { it.copy(message = null) }
    fun setCropPageIndex(index: Int) = _state.update {
        it.copy(cropPageIndex = index.coerceIn(0, (it.pages.size - 1).coerceAtLeast(0)))
    }

    fun ensureAutoName() {
        _state.update { s ->
            if (s.documentTitle.isBlank() || s.documentTitle == "Untitled scan") {
                s.copy(documentTitle = nextAutoName())
            } else s
        }
    }

    fun updateCropForPage(pageId: String, crop: CropNorm) {
        _state.update { s ->
            s.copy(
                pages = s.pages.map {
                    if (it.id == pageId) it.copy(crop = crop.clamped()) else it
                }
            )
        }
    }

    fun setFilter(filter: ScanFilter) {
        _state.update { s ->
            val pages = s.pages.map { p ->
                if (p.id == s.editingPageId || s.editingPageId == null && p.id == s.pages.lastOrNull()?.id) {
                    p.copy(filter = filter)
                } else p
            }
            s.copy(activeFilter = filter, pages = pages)
        }
    }

    fun rotateSelected() {
        _state.update { s ->
            val id = s.editingPageId ?: s.pages.lastOrNull()?.id ?: return@update s
            s.copy(
                pages = s.pages.map {
                    if (it.id == id) it.copy(rotationDeg = (it.rotationDeg + 90) % 360) else it
                }
            )
        }
    }

    fun removePage(id: String) {
        _state.update { s -> s.copy(pages = s.pages.filterNot { it.id == id }) }
    }

    fun movePage(from: Int, to: Int) {
        _state.update { s ->
            if (from !in s.pages.indices || to !in s.pages.indices) return@update s
            val mutable = s.pages.toMutableList()
            val item = mutable.removeAt(from)
            mutable.add(to, item)
            s.copy(pages = mutable)
        }
    }

    fun selectPage(id: String?) = _state.update { it.copy(editingPageId = id) }

    fun onCaptureBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(capturing = true, processing = true, edgeLocked = false) }
            val page = repository.persistBitmap(bitmap, _state.value.activeFilter)
            _state.update {
                val title = if (it.pages.isEmpty()) nextAutoName() else it.documentTitle
                it.copy(
                    capturing = false,
                    processing = false,
                    pages = it.pages + page,
                    editingPageId = page.id,
                    documentTitle = title,
                    message = if (it.mode == ScanMode.Batch) "Page ${it.pages.size + 1} added" else null
                )
            }
        }
    }

    fun resetSession() {
        _state.value = SessionUiState(
            mode = _state.value.mode,
            autoCapture = _state.value.autoCapture,
            edgeStyle = _state.value.edgeStyle,
            documentTitle = nextAutoName(),
            exportFormat = ExportFormat.Jpeg
        )
    }

    suspend fun finalizeDocument(): Long {
        ensureAutoName()
        val s = _state.value
        return repository.saveSession(s.documentTitle, s.pages)
    }

    suspend fun createShareUri() =
        repository.exportShareUri(_state.value.pages, _state.value.exportFormat, _state.value.documentTitle)
}
