package com.kyly.picking.ui.papeleta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kyly.picking.data.remote.dto.CaixaDto
import com.kyly.picking.data.remote.dto.EnderecoDto
import com.kyly.picking.data.remote.dto.ItemCaixaDto
import com.kyly.picking.data.remote.dto.PedidoDto
import com.kyly.picking.data.remote.dto.SkuDto
import com.kyly.picking.ui.components.ItemPapeletaCard
import com.kyly.picking.ui.navigation.AppDestination
import com.kyly.picking.ui.theme.KylyPickingTheme

@Composable
fun PapeletaScreen(
    navController: NavController,
    caixaCodigo: String,
    viewModel: PapeletaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PapeletaEvent.NavigateToPicking ->
                    navController.navigate(AppDestination.Picking.withArgs(event.caixaCodigo))
                PapeletaEvent.NavigateBack ->
                    navController.popBackStack()
            }
        }
    }

    PapeletaScreenContent(
        uiState          = uiState,
        onIniciarPicking = viewModel::onIniciarPicking,
        onVoltar         = viewModel::onVoltar,
    )
}

@Composable
fun PapeletaScreenContent(
    uiState:          PapeletaUiState,
    onIniciarPicking: () -> Unit,
    onVoltar:         () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val headerTitle = when (val c = uiState.content) {
            is PapeletaContent.Loaded -> "PEDIDO ${c.caixa.pedido.numeroPedido}"
            else                      -> "PAPELETA"
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text     = headerTitle,
                style    = MaterialTheme.typography.headlineMedium,
                color    = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        when (val content = uiState.content) {

            PapeletaContent.Loading -> {
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is PapeletaContent.Error -> {
                Box(
                    modifier         = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text      = content.message,
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is PapeletaContent.Loaded -> {
                val caixa          = content.caixa
                val coletados      = caixa.itens.count { it.status == "completo" }
                val total          = caixa.itens.size
                val itensOrdenados = caixa.itens.sortedBy { item ->
                    when (item.status) {
                        "pendente" -> 0
                        "parcial"  -> 1
                        "falta"    -> 2
                        "completo" -> 3
                        else       -> 4
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text  = "CAIXA ${caixa.codigo}",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text  = "$coletados de $total itens coletados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyColumn(
                    modifier            = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = itensOrdenados,
                        key   = { it.id },
                    ) { item ->
                        ItemPapeletaCard(item = item)
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick  = onIniciarPicking,
                enabled  = uiState.canStartPicking,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primaryContainer,
                    contentColor           = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text  = "INICIAR PICKING",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

private val previewCaixa = CaixaDto(
    id     = "uuid-1",
    codigo = "06772401",
    status = "em_coleta",
    pedido = PedidoDto("uuid-p", "PED-2025-0042", "em_separacao"),
    itens  = listOf(
        ItemCaixaDto(
            id = "uuid-i1", status = "pendente",
            quantidadeEsperada = 2, quantidadeColetada = 0,
            sku      = SkuDto("uuid-s1", "2000183", "Camiseta Manga Curta Masculina", "UN"),
            endereco = EnderecoDto("uuid-e1", "C37.09.6B", "C37", "09", "6B"),
        ),
        ItemCaixaDto(
            id = "uuid-i2", status = "parcial",
            quantidadeEsperada = 3, quantidadeColetada = 1,
            sku      = SkuDto("uuid-s2", "3001044", "Bermuda Jeans Feminina", "UN"),
            endereco = EnderecoDto("uuid-e2", "A12.03.1C", "A12", "03", "1C"),
        ),
        ItemCaixaDto(
            id = "uuid-i3", status = "completo",
            quantidadeEsperada = 2, quantidadeColetada = 2,
            sku      = SkuDto("uuid-s3", "1500277", "Blusa Regata Feminina", "UN"),
            endereco = EnderecoDto("uuid-e3", "B05.11.4A", "B05", "11", "4A"),
        ),
    ),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PapeletaLoadingPreview() {
    KylyPickingTheme {
        PapeletaScreenContent(
            uiState          = PapeletaUiState(PapeletaContent.Loading),
            onIniciarPicking = {},
            onVoltar         = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PapeletaLoadedPreview() {
    KylyPickingTheme {
        PapeletaScreenContent(
            uiState          = PapeletaUiState(PapeletaContent.Loaded(previewCaixa)),
            onIniciarPicking = {},
            onVoltar         = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PapeletaErrorPreview() {
    KylyPickingTheme {
        PapeletaScreenContent(
            uiState          = PapeletaUiState(PapeletaContent.Error("Caixa não encontrada.")),
            onIniciarPicking = {},
            onVoltar         = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PapeletaAllDonePreview() {
    KylyPickingTheme {
        val allDone = previewCaixa.copy(
            itens = previewCaixa.itens.map { it.copy(status = "completo") }
        )
        PapeletaScreenContent(
            uiState          = PapeletaUiState(PapeletaContent.Loaded(allDone)),
            onIniciarPicking = {},
            onVoltar         = {},
        )
    }
}
