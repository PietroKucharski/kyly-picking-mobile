package com.kyly.picking.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MobileLoginRequest(
    val supervisorCodigo: String,
    val operadorCracha: String,
)

@JsonClass(generateAdapter = true)
data class MobileLoginResponse(
    val token: String,
    val operadorNome: String,
)
