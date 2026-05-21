package com.kyly.picking.data.remote

import com.kyly.picking.data.remote.dto.BipagemRequestDto
import com.kyly.picking.data.remote.dto.MobileLoginRequest
import com.kyly.picking.data.remote.dto.MobileLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/mobile-login")
    suspend fun mobileLogin(@Body body: MobileLoginRequest): MobileLoginResponse

    @POST("bipagens")
    suspend fun postBipagem(@Body request: BipagemRequestDto): Response<Unit>
}
