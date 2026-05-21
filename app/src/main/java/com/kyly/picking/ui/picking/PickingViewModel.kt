package com.kyly.picking.ui.picking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyly.picking.data.remote.dto.CaixaDto
import com.kyly.picking.data.remote.dto.ItemCaixaDto
import com.kyly.picking.data.remote.dto.PostBipagemResponse
import com.kyly.picking.data.repository.BipagemRepository
import com.kyly.picking.data.repository.BipagemResult
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

enum class PickingErrorTipo { SKU_INCORRETO, SEM_SALDO, API_ERROR }

data class PickingError(
    val titulo:   String,
    val mensagem: String,
    val tipo:     PickingErrorTipo,
)

data class PickingUiState(
    val caixa:          CaixaDto?     = null,
    val itemAtual:      ItemCaixaDto? = null,
    val itensColetados: Int           = 0,
    val totalItens:     Int           = 0,
    val isLoading:      Boolean       = false,
    val errorModal:     PickingError? = null,
) {
    val pecasRestantes: Int
        get() = itemAtual?.let { it.quantidadeEsperada - it.quantidadeColetada } ?: 0

    val progressoHeader: String
        get() = "$itensColetados/$totalItens"

    val scannerBloqueado: Boolean
        get() = isLoading || errorModal != null
}

sealed class PickingEvent {
    data object CaixaFinalizada : PickingEvent()
    data object PickingParcial  : PickingEvent()
    data class NavigateToEnderecos(val skuId: String) : PickingEvent()
}

@HiltViewModel
class PickingViewModel @Inject constructor(
    private val coletaStateHolder: ColetaStateHolder,
    private val bipagemRepository: BipagemRepository,
    private val datalogic:         DatalogicManager,
    private val feedback:          FeedbackManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val caixaCodigo: String =
        checkNotNull(savedStateHandle["caixaCodigo"])

    private val _uiState = MutableStateFlow(PickingUiState())
    val uiState: StateFlow<PickingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PickingEvent>()
    val events: SharedFlow<PickingEvent> = _events

    init {
        inicializar()
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode -> onBarcodeScan(barcode) }
        }
    }

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()

    private fun inicializar() {
        val caixa = coletaStateHolder.get() ?: return
        val proximoItem = proximoItemPendente(caixa)
        _uiState.update {
            it.copy(
                caixa          = caixa,
                itemAtual      = proximoItem,
                totalItens     = caixa.itens.size,
                itensColetados = caixa.itens.count { item -> item.status == "completo" },
            )
        }
    }

    private fun onBarcodeScan(barcode: String) {
        val state = _uiState.value
        if (state.scannerBloqueado) return
        val item = state.itemAtual ?: return

        if (barcode == item.sku.codigo) {
            registrarBipagem(
                itemCaixaId     = item.id,
                codigoSkuBipado = barcode,
                enderecoId      = item.endereco.id,
                quantidade      = 1,
                statusColeta    = "sucesso",
            )
        } else {
            feedback.trigger(FeedbackEvent.ERRO_SKU)
            _uiState.update {
                it.copy(
                    errorModal = PickingError(
                        titulo   = "SKU INCORRETO",
                        mensagem = "O código bipado ($barcode) não corresponde ao SKU esperado " +
                                   "(${item.sku.codigo}). Confirme para registrar a divergência.",
                        tipo     = PickingErrorTipo.SKU_INCORRETO,
                    )
                )
            }
        }
    }

    fun onConfirmarErroModal() {
        val state    = _uiState.value
        val item     = state.itemAtual ?: return
        val tipoErro = state.errorModal?.tipo ?: return

        _uiState.update { it.copy(errorModal = null) }

        when (tipoErro) {
            PickingErrorTipo.SKU_INCORRETO ->
                registrarBipagem(
                    itemCaixaId     = item.id,
                    codigoSkuBipado = "DIVERGENCIA",
                    enderecoId      = item.endereco.id,
                    quantidade      = 0,
                    statusColeta    = "erro_sku",
                )
            PickingErrorTipo.API_ERROR ->
                Unit
            PickingErrorTipo.SEM_SALDO ->
                Unit
        }
    }

    fun onSemSaldo() {
        val state = _uiState.value
        if (state.scannerBloqueado) return
        val item = state.itemAtual ?: return

        registrarBipagem(
            itemCaixaId     = item.id,
            codigoSkuBipado = item.sku.codigo,
            enderecoId      = item.endereco.id,
            quantidade      = 0,
            statusColeta    = "sem_saldo",
        )
    }

    fun onEnderecosAlternativos() {
        val skuId = _uiState.value.itemAtual?.sku?.id ?: return
        viewModelScope.launch { _events.emit(PickingEvent.NavigateToEnderecos(skuId)) }
    }

    private fun registrarBipagem(
        itemCaixaId:     String,
        codigoSkuBipado: String,
        enderecoId:      String,
        quantidade:      Int,
        statusColeta:    String,
    ) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            when (val result = bipagemRepository.registrar(
                itemCaixaId, codigoSkuBipado, enderecoId, quantidade, statusColeta
            )) {
                is BipagemResult.Success ->
                    tratarSucesso(result.response, statusColeta)

                is BipagemResult.ItemJaCompleto -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(
                            isLoading  = false,
                            errorModal = PickingError(
                                titulo   = "ITEM JÁ COLETADO",
                                mensagem = "Este item já foi completamente coletado.",
                                tipo     = PickingErrorTipo.API_ERROR,
                            )
                        )
                    }
                }

                is BipagemResult.HttpError -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(
                            isLoading  = false,
                            errorModal = PickingError(
                                titulo   = "ERRO DE CONEXÃO",
                                mensagem = "Erro ${result.code}. A bipagem não foi registrada. Tente novamente.",
                                tipo     = PickingErrorTipo.API_ERROR,
                            )
                        )
                    }
                }

                BipagemResult.NetworkError -> {
                    feedback.trigger(FeedbackEvent.ERRO_SKU)
                    _uiState.update {
                        it.copy(
                            isLoading  = false,
                            errorModal = PickingError(
                                titulo   = "SEM CONEXÃO",
                                mensagem = "Sem conexão com o servidor. A bipagem não foi registrada.",
                                tipo     = PickingErrorTipo.API_ERROR,
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun tratarSucesso(response: PostBipagemResponse, statusColeta: String) {
        val caixaAtual = _uiState.value.caixa ?: return

        val caixaAtualizada = caixaAtual.copy(
            status = response.caixaAtualizada.status,
            itens  = caixaAtual.itens.map { item ->
                if (item.id == response.itemAtualizado.id)
                    item.copy(
                        status             = response.itemAtualizado.status,
                        quantidadeColetada = response.itemAtualizado.quantidadeColetada,
                    )
                else item
            }
        )

        coletaStateHolder.set(caixaAtualizada)

        val itemFoiCompleto = response.itemAtualizado.status == "completo"
        val proximoItem     = proximoItemPendente(caixaAtualizada)
        val itensColetados  = caixaAtualizada.itens.count { it.status == "completo" }

        when (statusColeta) {
            "sucesso"   -> if (itemFoiCompleto) feedback.trigger(FeedbackEvent.SKU_COMPLETO)
                           else                 feedback.trigger(FeedbackEvent.PECA_VALIDA)
            "erro_sku"  -> feedback.trigger(FeedbackEvent.ERRO_SKU)
            "sem_saldo" -> feedback.trigger(FeedbackEvent.SEM_SALDO)
        }

        _uiState.update {
            it.copy(
                caixa          = caixaAtualizada,
                itemAtual      = proximoItem,
                itensColetados = itensColetados,
                isLoading      = false,
            )
        }

        when {
            response.caixaAtualizada.status == "finalizada" ->
                _events.emit(PickingEvent.CaixaFinalizada)

            statusColeta == "sem_saldo" && proximoItem?.id == _uiState.value.itemAtual?.id ->
                _events.emit(PickingEvent.NavigateToEnderecos(
                    _uiState.value.itemAtual?.sku?.id ?: return
                ))

            proximoItem == null ->
                _events.emit(PickingEvent.PickingParcial)
        }
    }

    private fun proximoItemPendente(caixa: CaixaDto): ItemCaixaDto? =
        caixa.itens.firstOrNull { it.status == "pendente" || it.status == "parcial" }
}
