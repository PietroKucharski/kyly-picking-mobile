package com.kyly.picking.data.repository

import com.kyly.picking.data.remote.dto.CaixaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColetaStateHolder @Inject constructor() {
    private val _caixa = MutableStateFlow<CaixaDto?>(null)
    val caixa: StateFlow<CaixaDto?> = _caixa.asStateFlow()

    fun set(caixa: CaixaDto) { _caixa.value = caixa }
    fun clear()              { _caixa.value = null  }
    fun get(): CaixaDto?     = _caixa.value
}
