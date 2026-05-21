package com.kyly.picking.data.repository

import com.kyly.picking.data.local.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val secureStorage: SecureStorage,
) {
    fun isLoggedIn(): Boolean = secureStorage.getToken() != null
    fun saveToken(token: String) = secureStorage.saveToken(token)
    fun logout() = secureStorage.clearToken()
}
