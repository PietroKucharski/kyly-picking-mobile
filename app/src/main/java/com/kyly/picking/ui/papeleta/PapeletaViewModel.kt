package com.kyly.picking.ui.papeleta

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyly.picking.data.remote.dto.CaixaDto
import com.kyly.picking.data.repository.CaixaResult
import com.kyly.picking.data.repository.ColetaRepository
import com.kyly.picking.data.repository.ColetaStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PapeletaContent {
    data object Loading : PapeletaContent()
    data class Loaded(val caixa: CaixaDto) : PapeletaContent()
    data class Error(val message: String) : PapeletaContent()
}

data class PapeletaUiState(
    val content: PapeletaContent = PapeletaContent.Loading,
) {
    val canStartPicking: Boolean
        get() = content is PapeletaContent.Loaded &&
                (content as PapeletaContent.Loaded).caixa.itens
                    .any { it.status == "pendente" || it.status == "parcial" }
}

sealed class PapeletaEvent {
    data class NavigateToPicking(val caixaCodigo: String) : PapeletaEvent()
    data object NavigateBack : PapeletaEvent()
}

@HiltViewModel
class PapeletaViewModel @Inject constructor(
    private val coletaStateHolder: ColetaStateHolder,
    private val coletaRepository:  ColetaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val caixaCodigo: String =
        checkNotNull(savedStateHandle["caixaCodigo"]) { "caixaCodigo ausente nos args" }

    private val _uiState = MutableStateFlow(PapeletaUiState())
    val uiState: StateFlow<PapeletaUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PapeletaEvent>()
    val events: SharedFlow<PapeletaEvent> = _events

    init {
        carregarCaixa()
    }

    private fun carregarCaixa() {
        val cached = coletaStateHolder.get()

        if (cached != null && cached.codigo == caixaCodigo) {
            _uiState.update { it.copy(content = PapeletaContent.Loaded(cached)) }
            return
        }

        _uiState.update { it.copy(content = PapeletaContent.Loading) }
        viewModelScope.launch {
            when (val result = coletaRepository.buscarCaixa(caixaCodigo)) {
                is CaixaResult.Success -> {
                    coletaStateHolder.set(result.caixa)
                    _uiState.update { it.copy(content = PapeletaContent.Loaded(result.caixa)) }
                }
                is CaixaResult.NotFound ->
                    _uiState.update {
                        it.copy(content = PapeletaContent.Error("Caixa não encontrada."))
                    }
                is CaixaResult.AlreadyFinalized ->
                    _uiState.update {
                        it.copy(content = PapeletaContent.Error("Esta caixa já foi finalizada."))
                    }
                is CaixaResult.HttpError ->
                    _uiState.update {
                        it.copy(content = PapeletaContent.Error("Erro ${result.code}. Tente novamente."))
                    }
                CaixaResult.NetworkError ->
                    _uiState.update {
                        it.copy(content = PapeletaContent.Error("Sem conexão. Verifique a rede."))
                    }
            }
        }
    }

    fun onIniciarPicking() {
        if (!_uiState.value.canStartPicking) return
        viewModelScope.launch {
            _events.emit(PapeletaEvent.NavigateToPicking(caixaCodigo))
        }
    }

    fun onVoltar() {
        coletaStateHolder.clear()
        viewModelScope.launch { _events.emit(PapeletaEvent.NavigateBack) }
    }
}
