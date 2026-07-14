package com.ramerlabs.scanelite.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramerlabs.scanelite.data.repository.ScanRepository
import com.ramerlabs.scanelite.domain.ScanDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {
    val documents: StateFlow<List<ScanDocument>> = repository.observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) = viewModelScope.launch { repository.deleteDocument(id) }
}
