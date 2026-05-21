package com.kyly.picking.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kyly.picking.ui.components.ScanDisplayField
import com.kyly.picking.ui.navigation.AppDestination
import com.kyly.picking.ui.theme.Background
import com.kyly.picking.ui.theme.KylyError
import com.kyly.picking.ui.theme.KylyPickingTheme
import com.kyly.picking.ui.theme.OnPrimary
import com.kyly.picking.ui.theme.OnSurfaceVariant
import com.kyly.picking.ui.theme.Primary
import com.kyly.picking.ui.theme.PrimaryContainer
import com.kyly.picking.ui.theme.SurfaceContainerHigh

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE  -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.Sucesso -> navController.navigate(AppDestination.Menu.route) {
                    popUpTo(AppDestination.Login.route) { inclusive = true }
                }
            }
        }
    }

    LoginContent(
        uiState  = uiState,
        onEntrar = viewModel::onEntrar,
        onLimpar = viewModel::onLimpar,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEntrar: () -> Unit,
    onLimpar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text          = "KYLY PICKING",
                style         = MaterialTheme.typography.titleLarge,
                color         = OnPrimary,
                letterSpacing = 2.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text  = "Sistema de Picking",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
            )
            Text(
                text  = "Faça login para iniciar",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = "1. BIPE O CÓDIGO DO SUPERVISOR",
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                )
                ScanDisplayField(
                    value       = uiState.supervisorCodigo,
                    placeholder = "Aguardando bipagem...",
                    isActive    = uiState.supervisorCodigo.isBlank(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = "2. BIPE O SEU CRACHÁ",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.supervisorCodigo.isNotBlank()) Primary else OnSurfaceVariant,
                )
                ScanDisplayField(
                    value       = uiState.operadorCracha,
                    placeholder = "Aguardando crachá...",
                    isActive    = uiState.supervisorCodigo.isNotBlank() && uiState.operadorCracha.isBlank(),
                )
            }

            if (uiState.erro != null) {
                Text(
                    text  = uiState.erro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KylyError,
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick  = onEntrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                enabled  = uiState.podeFazerLogin && !uiState.isLoading,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = PrimaryContainer,
                    contentColor           = OnPrimary,
                    disabledContainerColor = SurfaceContainerHigh,
                    disabledContentColor   = OnSurfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = OnPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text  = "Entrar",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoginEmptyPreview() {
    KylyPickingTheme {
        LoginContent(
            uiState  = LoginUiState(),
            onEntrar = {},
            onLimpar = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoginFilledPreview() {
    KylyPickingTheme {
        LoginContent(
            uiState  = LoginUiState(supervisorCodigo = "SUP001", operadorCracha = "OP42"),
            onEntrar = {},
            onLimpar = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoginErrorPreview() {
    KylyPickingTheme {
        LoginContent(
            uiState  = LoginUiState(
                supervisorCodigo = "SUP001",
                operadorCracha   = "OP42",
                erro             = "Credenciais inválidas",
            ),
            onEntrar = {},
            onLimpar = {},
        )
    }
}
