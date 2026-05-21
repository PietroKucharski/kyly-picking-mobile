package com.kyly.picking.data.repository

import com.kyly.picking.data.remote.ApiService
import com.kyly.picking.data.remote.dto.CaixaDto
import javax.inject.Inject
import javax.inject.Singleton

sealed class CaixaResult {
    data class Success(val caixa: CaixaDto) : CaixaResult()
    data class NotFound(val message: String = "Caixa não encontrada") : CaixaResult()
    data class AlreadyFinalized(val message: String = "Caixa já finalizada") : CaixaResult()
    data class HttpError(val code: Int, val message: String) : CaixaResult()
    data object NetworkError : CaixaResult()
}

@Singleton
class ColetaRepository @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun buscarCaixa(codigo: String): CaixaResult {
        return try {
            val response = apiService.getCaixa(codigo)
            CaixaResult.Success(response.caixa)
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                404  -> CaixaResult.NotFound()
                422  -> CaixaResult.AlreadyFinalized()
                else -> CaixaResult.HttpError(e.code(), e.message())
            }
        } catch (e: java.io.IOException) {
            CaixaResult.NetworkError
        }
    }
}
