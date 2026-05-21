package com.kyly.picking.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BipagemRequestDto(
    val itemCaixaId:     String,
    val codigoSkuBipado: String,
    val enderecoId:      String,
    val quantidade:      Int,
    val statusColeta:    String,
)
