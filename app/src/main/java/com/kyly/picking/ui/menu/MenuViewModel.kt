package com.kyly.picking.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyly.picking.data.local.SecureStorage
import com.kyly.picking.data.repository.CaixaResult
import com.kyly.picking.data.repository.ColetaRepository
import com.kyly.picking.data.repository.ColetaStateHolder
import com.kyly.picking.hardware.DatalogicManager
import com.kyly.picking.hardware.FeedbackEvent
import com.kyly.picking.hardware.FeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val codigoLido:   String  = "",
    val isLoading:    Boolean = false,
    val errorMessage: String? = null,
) {
    val hasCode: Boolean get() = codigoLido.isNotBlank()
}

sealed class MenuEvent {
    data class NavigateToPapeleta(val caixaCodigo: String) : MenuEvent()
    data object NavigateToLogin : MenuEvent()
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val coletaRepository:   ColetaRepository,
    private val coletaStateHolder:  ColetaStateHolder,
    private val secureStorage:      SecureStorage,
    private val datalogic:          DatalogicManager,
    private val feedback:           FeedbackManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MenuEvent>()
    val events: SharedFlow<MenuEvent> = _events

    init {
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode -> onBarcodeScan(barcode) }
        }
    }

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()

    private fun onBarcodeScan(barcode: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(codigoLido = barcode, errorMessage = null) }
        buscarCaixa(barcode)
    }

    private fun buscarCaixa(codigo: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = coletaRepository.buscarCaixa(codigo)) {
                is CaixaResult.Success -> {
                    coletaStateHolder.set(result.caixa)
                    _events.emit(MenuEvent.NavigateToPapeleta(result.caixa.codigo))
                }
                is CaixaResult.NotFound -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Caixa não encontrada. Verifique o código.")
                    }
                }
                is CaixaResult.AlreadyFinalized -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Esta caixa já foi finalizada.")
                    }
                }
                is CaixaResult.HttpError -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Erro ${result.code}. Tente novamente.")
                    }
                }
                CaixaResult.NetworkError -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Sem conexão. Tente novamente.")
                    }
                }
            }
        }
    }

    fun onRetry() {
        _uiState.update { MenuUiState() }
    }

    fun onLogout() {
        secureStorage.clearToken()
        viewModelScope.launch {
            _events.emit(MenuEvent.NavigateToLogin)
        }
    }
}
