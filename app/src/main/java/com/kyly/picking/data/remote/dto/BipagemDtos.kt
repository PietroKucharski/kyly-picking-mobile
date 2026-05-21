package com.kyly.picking.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostBipagemRequest(
    val itemCaixaId:     String,
    val codigoSkuBipado: String,
    val enderecoId:      String,
    val quantidade:      Int,
    val statusColeta:    String,
)

@JsonClass(generateAdapter = true)
data class PostBipagemResponse(
    val coleta:          ColetaDto,
    val itemAtualizado:  ItemAtualizadoDto,
    val caixaAtualizada: CaixaAtualizadaDto,
)

@JsonClass(generateAdapter = true)
data class ColetaDto(
    val id:         String,
    val status:     String,
    val quantidade: Int,
    val criadoEm:   String,
)

@JsonClass(generateAdapter = true)
data class ItemAtualizadoDto(
    val id:                 String,
    val status:             String,
    val quantidadeColetada: Int,
)

@JsonClass(generateAdapter = true)
data class CaixaAtualizadaDto(
    val id:     String,
    val status: String,
)
