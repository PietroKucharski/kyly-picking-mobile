package com.kyly.picking.ui.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyly.picking.hardware.DatalogicManager
import com.kyly.picking.hardware.FeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PickingViewModel @Inject constructor(
    private val datalogic: DatalogicManager,
    private val feedback: FeedbackManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickingUiState())
    val uiState: StateFlow<PickingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PickingEvent>()
    val events: SharedFlow<PickingEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode -> processBipagem(barcode) }
        }
    }

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()

    private fun processBipagem(barcode: String) {
        // Lógica implementada na spec de feature de picking
    }
}
