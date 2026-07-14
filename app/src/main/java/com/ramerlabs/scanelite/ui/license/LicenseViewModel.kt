package com.ramerlabs.scanelite.ui.license

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramerlabs.scanelite.license.LicenseConstants
import com.ramerlabs.scanelite.license.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LicenseUiState(
    val checking: Boolean = true,
    val unlocked: Boolean = false,
    val keyInput: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val buyUrl: String = LicenseConstants.BUY_URL
)

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val repository: LicenseRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LicenseUiState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = repository.publicStatus().valid
            val valid = if (cached) repository.validateStored(force = false) else false
            _state.update { it.copy(checking = false, unlocked = valid) }
        }
    }

    fun onKeyChange(value: String) = _state.update { it.copy(keyInput = value, error = null) }

    fun activate() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = repository.activate(_state.value.keyInput)
            _state.update {
                it.copy(
                    loading = false,
                    unlocked = result.success,
                    error = if (result.success) null else result.message,
                    buyUrl = result.buyUrl
                )
            }
        }
    }

    fun replaceLicense() {
        repository.clear()
        _state.update {
            it.copy(unlocked = false, keyInput = "", error = null, checking = false)
        }
    }
}
