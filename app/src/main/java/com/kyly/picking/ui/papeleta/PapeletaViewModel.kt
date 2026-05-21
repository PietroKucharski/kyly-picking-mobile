package com.kyly.picking.ui.papeleta

import androidx.lifecycle.ViewModel
import com.kyly.picking.hardware.DatalogicManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PapeletaViewModel @Inject constructor(
    private val datalogic: DatalogicManager,
) : ViewModel() {

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()
}
