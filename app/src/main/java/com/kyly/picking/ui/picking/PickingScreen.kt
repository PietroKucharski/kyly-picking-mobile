package com.kyly.picking.ui.picking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kyly.picking.data.remote.dto.CaixaDto
import com.kyly.picking.data.remote.dto.EnderecoDto
import com.kyly.picking.data.remote.dto.ItemCaixaDto
import com.kyly.picking.data.remote.dto.PedidoDto
import com.kyly.picking.data.remote.dto.SkuDto
import com.kyly.picking.ui.components.AddressChip
import com.kyly.picking.ui.components.PickingErrorBottomSheet
import com.kyly.picking.ui.navigation.AppDestination
import com.kyly.picking.ui.theme.KylyPickingTheme
import com.kyly.picking.ui.theme.NumeralLarge
import com.kyly.picking.ui.theme.NumeralXl
import com.kyly.picking.ui.theme.WarehouseOrange

@Composable
fun PickingScreen(
    navController: NavController,
    caixaCodigo:   String,
    viewModel:     PickingViewModel = hiltViewModel(),
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
                PickingEvent.CaixaFinalizada ->
                    navController.navigate(AppDestination.Finalizacao.withArgs("finalizada")) {
                        popUpTo(AppDestination.Picking.route) { inclusive = true }
                    }
                PickingEvent.PickingParcial ->
                    navController.navigate(AppDestination.Finalizacao.withArgs("parcial")) {
                        popUpTo(AppDestination.Picking.route) { inclusive = true }
                    }
                is PickingEvent.NavigateToEnderecos ->
                    navController.navigate(AppDestination.Enderecos.withArgs(event.skuId))
            }
        }
    }

    PickingScreenContent(
        uiState         = uiState,
        onSemSaldo      = viewModel::onSemSaldo,
        onEnderecos     = viewModel::onEnderecosAlternativos,
        onConfirmarErro = viewModel::onConfirmarErroModal,
    )
}

@Composable
fun PickingScreenContent(
    uiState:         PickingUiState,
    onSemSaldo:      () -> Unit,
    onEnderecos:     () -> Unit,
    onConfirmarErro: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {

            // ── Header ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "CAIXA ${uiState.caixa?.codigo ?: ""}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text  = uiState.progressoHeader,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            // ── Corpo ─────────────────────────────────────────────────
            val item = uiState.itemAtual
            if (item != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    Text(
                        text  = "REF ${item.sku.codigo} • ${item.sku.unidade}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text  = item.sku.descricao.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text  = "ENDEREÇO",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                        color = MaterialTheme.colorScheme.outline,
                    )

                    Text(
                        text  = item.endereco.codigo,
                        style = NumeralXl,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AddressChip("Corredor ${item.endereco.corredor}")
                        AddressChip("Seção ${item.endereco.prateleira}")
                        AddressChip("Pos ${item.endereco.posicao}")
                    }

                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text  = "${uiState.pecasRestantes} / ${item.quantidadeEsperada}",
                            style = NumeralLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text  = "PEÇAS RESTANTES",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Box(
                    modifier         = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Rodapé ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick  = onSemSaldo,
                    enabled  = !uiState.scannerBloqueado && uiState.itemAtual != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape  = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = WarehouseOrange,
                        contentColor           = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text  = "⚠ SEM SALDO",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Button(
                    onClick  = onEnderecos,
                    enabled  = !uiState.scannerBloqueado && uiState.itemAtual != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape  = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor           = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text  = "Endereços Alt.",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        // ── Loading overlay ───────────────────────────────────────────
        if (uiState.isLoading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                )
            }
        }

        // ── Modal de erro ─────────────────────────────────────────────
        val error = uiState.errorModal
        if (error != null) {
            PickingErrorBottomSheet(
                error       = error,
                onConfirmar = onConfirmarErro,
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewItem = ItemCaixaDto(
    id                 = "uuid-i1",
    status             = "pendente",
    quantidadeEsperada = 2,
    quantidadeColetada = 0,
    sku      = SkuDto("uuid-s1", "2000183", "Camiseta Manga Curta Masculina", "UN"),
    endereco = EnderecoDto("uuid-e1", "C37.09.6B", "C37", "09", "6B"),
)

private val previewCaixa = CaixaDto(
    id     = "uuid-c1",
    codigo = "06772401",
    status = "em_coleta",
    pedido = PedidoDto("uuid-p", "PED-2025-0042", "em_separacao"),
    itens  = listOf(previewItem),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PickingActivePreview() {
    KylyPickingTheme {
        PickingScreenContent(
            uiState = PickingUiState(
                caixa          = previewCaixa,
                itemAtual      = previewItem,
                itensColetados = 2,
                totalItens     = 16,
            ),
            onSemSaldo      = {},
            onEnderecos     = {},
            onConfirmarErro = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PickingLoadingPreview() {
    KylyPickingTheme {
        PickingScreenContent(
            uiState = PickingUiState(
                caixa     = previewCaixa,
                itemAtual = previewItem,
                isLoading = true,
            ),
            onSemSaldo      = {},
            onEnderecos     = {},
            onConfirmarErro = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PickingErrorModalPreview() {
    KylyPickingTheme {
        PickingScreenContent(
            uiState = PickingUiState(
                caixa     = previewCaixa,
                itemAtual = previewItem,
                errorModal = PickingError(
                    titulo   = "SKU INCORRETO",
                    mensagem = "O código bipado (3001044) não corresponde ao SKU esperado (2000183). Confirme para registrar a divergência.",
                    tipo     = PickingErrorTipo.SKU_INCORRETO,
                ),
            ),
            onSemSaldo      = {},
            onEnderecos     = {},
            onConfirmarErro = {},
        )
    }
}
