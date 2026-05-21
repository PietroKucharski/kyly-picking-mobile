package com.kyly.picking.data.remote

import com.kyly.picking.data.remote.dto.BipagemRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("bipagens")
    suspend fun postBipagem(@Body request: BipagemRequestDto): Response<Unit>
}
