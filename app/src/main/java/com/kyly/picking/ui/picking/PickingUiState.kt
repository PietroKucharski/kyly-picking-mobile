package com.kyly.picking.ui.picking

data class PickingUiState(
    val isLoading: Boolean = false,
    val error: String?     = null,
)

sealed class PickingEvent {
    object CaixaFinalizada : PickingEvent()
    object PickingParcial  : PickingEvent()
}
