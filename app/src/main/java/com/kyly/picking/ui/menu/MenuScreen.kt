package com.kyly.picking.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.kyly.picking.ui.components.ScanDisplayField
import com.kyly.picking.ui.navigation.AppDestination
import com.kyly.picking.ui.theme.KylyPickingTheme

@Composable
fun MenuScreen(
    navController: NavController,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE  -> viewModel.onPause()
                else                      -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MenuEvent.NavigateToPapeleta ->
                    navController.navigate(AppDestination.Papeleta.withArgs(event.caixaCodigo))

                MenuEvent.NavigateToLogin ->
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
            }
        }
    }

    MenuScreenContent(
        uiState  = uiState,
        onRetry  = viewModel::onRetry,
        onLogout = viewModel::onLogout,
    )
}

@Composable
fun MenuScreenContent(
    uiState:  MenuUiState,
    onRetry:  () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = "KYLY PICKING",
                style    = MaterialTheme.typography.titleLarge.copy(
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 2.sp,
                ),
                color    = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        // ── Área Central ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text  = "BIPE O CÓDIGO DA CAIXA",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text      = "Aponte o scanner para o código de barras da caixa",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            ScanDisplayField(
                value       = uiState.codigoLido,
                placeholder = "Aguardando bipagem...",
                isActive    = !uiState.hasCode && !uiState.isLoading,
                modifier    = Modifier.fillMaxWidth(),
            )

            if (uiState.isLoading) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier    = Modifier.size(32.dp),
                    color       = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = uiState.errorMessage,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) {
                    Text(
                        text  = "Tentar novamente",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ── Rodapé ────────────────────────────────────────────────────
        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick  = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    text  = "SAIR",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MenuScreenIdlePreview() {
    KylyPickingTheme {
        MenuScreenContent(
            uiState  = MenuUiState(),
            onRetry  = {},
            onLogout = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MenuScreenLoadingPreview() {
    KylyPickingTheme {
        MenuScreenContent(
            uiState  = MenuUiState(codigoLido = "06772401", isLoading = true),
            onRetry  = {},
            onLogout = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun MenuScreenErrorPreview() {
    KylyPickingTheme {
        MenuScreenContent(
            uiState  = MenuUiState(
                codigoLido   = "06772401",
                errorMessage = "Caixa não encontrada. Verifique o código.",
            ),
            onRetry  = {},
            onLogout = {},
        )
    }
}
