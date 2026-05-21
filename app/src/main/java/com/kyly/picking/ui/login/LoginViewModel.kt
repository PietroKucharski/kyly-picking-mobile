package com.kyly.picking.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyly.picking.data.repository.AuthRepository
import com.kyly.picking.domain.model.Result
import com.kyly.picking.hardware.DatalogicManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val supervisorCodigo: String = "",
    val operadorCracha: String = "",
    val isLoading: Boolean = false,
    val erro: String? = null,
) {
    val podeFazerLogin: Boolean
        get() = supervisorCodigo.isNotBlank() && operadorCracha.isNotBlank()
}

sealed class LoginEvent {
    object Sucesso : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val datalogic: DatalogicManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode ->
                handleBarcode(barcode)
            }
        }
    }

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()

    private fun handleBarcode(barcode: String) {
        _uiState.update { state ->
            when {
                state.supervisorCodigo.isBlank() -> state.copy(supervisorCodigo = barcode, erro = null)
                state.operadorCracha.isBlank()   -> state.copy(operadorCracha = barcode, erro = null)
                else -> state
            }
        }
    }

    fun onEntrar() {
        val state = _uiState.value
        if (!state.podeFazerLogin || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, erro = null) }
            when (val result = authRepository.login(state.supervisorCodigo, state.operadorCracha)) {
                is Result.Success -> _events.emit(LoginEvent.Sucesso)
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, erro = result.message) }
            }
        }
    }

    fun onLimpar() {
        _uiState.update { LoginUiState() }
    }
}
