package com.kyly.picking.data.remote

import com.kyly.picking.data.remote.dto.GetCaixaResponse
import com.kyly.picking.data.remote.dto.MobileLoginRequest
import com.kyly.picking.data.remote.dto.MobileLoginResponse
import com.kyly.picking.data.remote.dto.PostBipagemRequest
import com.kyly.picking.data.remote.dto.PostBipagemResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/mobile-login")
    suspend fun mobileLogin(@Body body: MobileLoginRequest): MobileLoginResponse

    @GET("api/mobile/caixas/{codigo}")
    suspend fun getCaixa(@Path("codigo") codigo: String): GetCaixaResponse

    @POST("api/mobile/bipagens")
    suspend fun postBipagem(@Body body: PostBipagemRequest): PostBipagemResponse
}
