package com.kyly.picking.ui.login

import androidx.lifecycle.ViewModel
import com.kyly.picking.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel()
