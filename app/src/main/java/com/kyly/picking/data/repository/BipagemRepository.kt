package com.kyly.picking.data.repository

import com.kyly.picking.data.remote.ApiService
import com.kyly.picking.data.remote.dto.PostBipagemRequest
import com.kyly.picking.data.remote.dto.PostBipagemResponse
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class BipagemResult {
    data class Success(val response: PostBipagemResponse) : BipagemResult()
    data class ItemJaCompleto(val message: String = "Item já completamente coletado") : BipagemResult()
    data class HttpError(val code: Int, val message: String) : BipagemResult()
    data object NetworkError : BipagemResult()
}

@Singleton
class BipagemRepository @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun registrar(
        itemCaixaId:     String,
        codigoSkuBipado: String,
        enderecoId:      String,
        quantidade:      Int,
        statusColeta:    String,
    ): BipagemResult {
        return try {
            val response = apiService.postBipagem(
                PostBipagemRequest(
                    itemCaixaId     = itemCaixaId,
                    codigoSkuBipado = codigoSkuBipado,
                    enderecoId      = enderecoId,
                    quantidade      = quantidade,
                    statusColeta    = statusColeta,
                )
            )
            BipagemResult.Success(response)
        } catch (e: HttpException) {
            when (e.code()) {
                422  -> BipagemResult.ItemJaCompleto()
                else -> BipagemResult.HttpError(e.code(), e.message())
            }
        } catch (e: IOException) {
            BipagemResult.NetworkError
        }
    }
}
