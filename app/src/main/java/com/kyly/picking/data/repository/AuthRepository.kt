package com.kyly.picking.data.repository

import com.kyly.picking.data.local.SecureStorage
import com.kyly.picking.data.remote.ApiService
import com.kyly.picking.data.remote.dto.MobileLoginRequest
import com.kyly.picking.domain.model.Result
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val secureStorage: SecureStorage,
    private val apiService: ApiService,
) {
    fun isLoggedIn(): Boolean = secureStorage.getToken() != null
    fun logout() = secureStorage.clearToken()

    suspend fun login(supervisorCodigo: String, operadorCracha: String): Result<Unit> {
        return try {
            val response = apiService.mobileLogin(
                MobileLoginRequest(supervisorCodigo, operadorCracha)
            )
            secureStorage.saveToken(response.token)
            Result.Success(Unit)
        } catch (e: IOException) {
            Result.Error("Sem conexão com o servidor")
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> Result.Error("Credenciais inválidas")
                422      -> Result.Error("Dados inválidos")
                else     -> Result.Error("Erro no servidor (${e.code()})")
            }
        }
    }
}
