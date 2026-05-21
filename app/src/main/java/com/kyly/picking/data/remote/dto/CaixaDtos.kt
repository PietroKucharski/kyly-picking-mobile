package com.kyly.picking.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetCaixaResponse(
    val caixa: CaixaDto,
)

@JsonClass(generateAdapter = true)
data class CaixaDto(
    val id:     String,
    val codigo: String,
    val status: String,
    val pedido: PedidoDto,
    val itens:  List<ItemCaixaDto>,
)

@JsonClass(generateAdapter = true)
data class PedidoDto(
    val id:           String,
    val numeroPedido: String,
    val status:       String,
)

@JsonClass(generateAdapter = true)
data class ItemCaixaDto(
    val id:                 String,
    val status:             String,
    val quantidadeEsperada: Int,
    val quantidadeColetada: Int,
    val sku:                SkuDto,
    val endereco:           EnderecoDto,
)

@JsonClass(generateAdapter = true)
data class SkuDto(
    val id:        String,
    val codigo:    String,
    val descricao: String,
    val unidade:   String,
)

@JsonClass(generateAdapter = true)
data class EnderecoDto(
    val id:         String,
    val codigo:     String,
    val corredor:   String,
    val prateleira: String,
    val posicao:    String,
)
