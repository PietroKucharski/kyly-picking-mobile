# 09 — Mobile App: Tela de Picking (Coleta Item a Item)

## Objetivo

Implementar a tela de Picking — o loop central de trabalho do operador.
Para cada item da papeleta o operador bipa o SKU no endereço indicado.
O app valida a leitura, registra a coleta via `POST /api/mobile/bipagens`,
aciona o feedback de hardware (LED + beep) e avança para o próximo item.

Fluxos contemplados:

| Evento                  | Resultado                                                       |
|-------------------------|-----------------------------------------------------------------|
| SKU correto             | Peça registrada, contador decrementado, feedback verde          |
| SKU completo (última peça) | Item marcado `completo`, avança para próximo item            |
| SKU incorreto           | Modal de erro, feedback vermelho, confirmar registra `erro_sku` |
| Sem Saldo               | Registra `sem_saldo`, navega para Endereços Alternativos        |
| Todos os itens coletados | Navega para `FinalizacaoScreen("finalizada")`                  |
| Caixa parcial (algum `falta`) | Navega para `FinalizacaoScreen("parcial")`                |

---

## Pré-condições

- Spec 08 concluída: `ColetaStateHolder`, `ColetaRepository`, `CaixaDto` e variantes implementadas
- `AppDestination.Picking` registrado no `AppNavGraph` com argumento `caixaCodigo`
- `FinalizacaoScreen` existe como composable — placeholder aceitável
- `EnderecosAlternativosScreen` existe como composable — placeholder aceitável

---

## Layout de Referência

Tradução exata do design system (ui-context.md) para Jetpack Compose:

```
┌─────────────────────────────────────────────┐
│  Header — bg primaryContainer (#1B3A5C)     │
│  h=56dp                                     │
│  "CAIXA {caixaCodigo}"     "{atual}/{total}"│
│  (titleLarge bold, branco) (bodyLarge, branco)
├─────────────────────────────────────────────┤
│  px=16dp, pt=24dp                           │
│                                             │
│  "REF {sku.codigo} • {sku.unidade}"         │  headlineMedium, primary
│  "{sku.descricao.uppercase}"                │  labelLarge, onSurfaceVariant
│                                             │
│  "ENDEREÇO"                                 │  labelLarge, outline, letterSpacing 3sp
│  ┌───────────────────────────────────────┐  │
│  │         C37.09.6B                     │  │  JetBrainsMono, 72sp, Black, primaryContainer
│  └───────────────────────────────────────┘  │
│                                             │
│  [Corredor C37]  [Seção 09]  [Pos 6B]       │  chips, h=32dp
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │   1 / 2                               │  │  48sp, Black, primary, centralizado
│  │   PEÇAS RESTANTES                     │  │  labelLarge, onSurfaceVariant, uppercase
│  └───────────────────────────────────────┘  │
│                                             │
├─────────────────────────────────────────────┤
│  flex-row, gap=12dp, p=16dp                 │
│  [⚠ SEM SALDO]      [Endereços Alt.]        │  h=56dp cada
│  bg warehouseOrange  bg surfaceContainerHighest
└─────────────────────────────────────────────┘

── Modal de Erro (bottom sheet) ─────────────
│  Overlay escuro 60%                         │
│  ┌────────── rounded-top-24dp ────────────┐ │
│  │  [⊗ ícone vermelho, 64dp]              │ │
│  │  "SKU INCORRETO"  ← headlineMedium     │ │
│  │  "O código bipado não corresponde..."  │ │
│  │  [   CONFIRMAR   ]  ← h=56dp, bg error │ │
│  └─────────────────────────────────────────┘│
```

**Cores dos botões do rodapé:**

| Botão             | Background              | Texto     |
|-------------------|-------------------------|-----------|
| Sem Saldo         | `warehouseOrange` `#EA580C` | `#FFFFFF` |
| Endereços Alt.    | `surfaceContainerHighest` `#E0E3E6` | `onSurfaceVariant` |

---

## Checklist

### 1. DTOs de Bipagem — `data/remote/dto/BipagemDtos.kt`

- [ ] Criar DTOs de request e response do `POST /api/mobile/bipagens`:
  ```kotlin
  @JsonClass(generateAdapter = true)
  data class PostBipagemRequest(
      val itemCaixaId:     String,
      val codigoSkuBipado: String,
      val enderecoId:      String,
      val quantidade:      Int,
      val statusColeta:    String,   // "sucesso" | "erro_sku" | "sem_saldo"
  )

  @JsonClass(generateAdapter = true)
  data class PostBipagemResponse(
      val coleta:          ColetaDto,
      val itemAtualizado:  ItemAtualizadoDto,
      val caixaAtualizada: CaixaAtualizadaDto,
  )

  @JsonClass(generateAdapter = true)
  data class ColetaDto(
      val id:         String,
      val status:     String,
      val quantidade: Int,
      val criadoEm:   String,
  )

  @JsonClass(generateAdapter = true)
  data class ItemAtualizadoDto(
      val id:                 String,
      val status:             String,
      val quantidadeColetada: Int,
  )

  @JsonClass(generateAdapter = true)
  data class CaixaAtualizadaDto(
      val id:     String,
      val status: String,
  )
  ```

---

### 2. ApiService — `data/remote/ApiService.kt`

- [ ] Adicionar endpoint de bipagem:
  ```kotlin
  interface ApiService {
      @POST("api/auth/mobile-login")
      suspend fun mobileLogin(@Body body: MobileLoginRequest): MobileLoginResponse

      @GET("api/mobile/caixas/{codigo}")
      suspend fun getCaixa(@Path("codigo") codigo: String): GetCaixaResponse

      @POST("api/mobile/bipagens")
      suspend fun postBipagem(@Body body: PostBipagemRequest): PostBipagemResponse
  }
  ```

---

### 3. BipagemRepository — `data/repository/BipagemRepository.kt`

- [ ] Criar sealed class de resultado:
  ```kotlin
  sealed class BipagemResult {
      data class Success(val response: PostBipagemResponse) : BipagemResult()
      data class ItemJaCompleto(val message: String = "Item já completamente coletado") : BipagemResult()
      data class HttpError(val code: Int, val message: String) : BipagemResult()
      data object NetworkError : BipagemResult()
  }
  ```
- [ ] Criar `BipagemRepository`:
  ```kotlin
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
          } catch (e: retrofit2.HttpException) {
              when (e.code()) {
                  422 -> BipagemResult.ItemJaCompleto()
                  else -> BipagemResult.HttpError(e.code(), e.message())
              }
          } catch (e: java.io.IOException) {
              BipagemResult.NetworkError
          }
      }
  }
  ```

---

### 4. PickingUiState, PickingError e PickingEvent — `ui/picking/PickingViewModel.kt`

- [ ] Definir enumeração de tipo de erro:
  ```kotlin
  enum class PickingErrorTipo { SKU_INCORRETO, SEM_SALDO, API_ERROR }
  ```
- [ ] Definir `PickingError`:
  ```kotlin
  data class PickingError(
      val titulo:   String,
      val mensagem: String,
      val tipo:     PickingErrorTipo,
  )
  ```
- [ ] Definir `PickingUiState`:
  ```kotlin
  data class PickingUiState(
      val caixa:          CaixaDto?      = null,
      val itemAtual:      ItemCaixaDto?  = null,
      val itensColetados: Int            = 0,
      val totalItens:     Int            = 0,
      val isLoading:      Boolean        = false,
      val errorModal:     PickingError?  = null,
  ) {
      /** Peças ainda a coletar no item atual */
      val pecasRestantes: Int
          get() = itemAtual?.let { it.quantidadeEsperada - it.quantidadeColetada } ?: 0

      /** Texto do header: "X/Y" */
      val progressoHeader: String
          get() = "$itensColetados/$totalItens"

      /** Scanner deve ignorar leituras enquanto modal ou loading estiver ativo */
      val scannerBloqueado: Boolean
          get() = isLoading || errorModal != null
  }
  ```
- [ ] Definir `PickingEvent`:
  ```kotlin
  sealed class PickingEvent {
      data object CaixaFinalizada : PickingEvent()
      data object PickingParcial  : PickingEvent()
      data class NavigateToEnderecos(val skuId: String) : PickingEvent()
  }
  ```

---

### 5. PickingViewModel — `ui/picking/PickingViewModel.kt`

- [ ] Criar `PickingViewModel` com `@HiltViewModel` e `SavedStateHandle`:
  ```kotlin
  @HiltViewModel
  class PickingViewModel @Inject constructor(
      private val coletaStateHolder: ColetaStateHolder,
      private val bipagemRepository: BipagemRepository,
      private val datalogic:         DatalogicManager,
      private val feedback:          FeedbackManager,
      savedStateHandle: SavedStateHandle,
  ) : ViewModel() {

      private val caixaCodigo: String =
          checkNotNull(savedStateHandle["caixaCodigo"])

      private val _uiState = MutableStateFlow(PickingUiState())
      val uiState: StateFlow<PickingUiState> = _uiState.asStateFlow()

      private val _events = MutableSharedFlow<PickingEvent>()
      val events: SharedFlow<PickingEvent> = _events

      init {
          inicializar()
          viewModelScope.launch {
              datalogic.barcodeEvents.collect { barcode -> onBarcodeScan(barcode) }
          }
      }

      fun onResume() = datalogic.enable()
      fun onPause()  = datalogic.disable()
  }
  ```

- [ ] Implementar `inicializar()`:
  ```kotlin
  private fun inicializar() {
      val caixa = coletaStateHolder.get() ?: return  // guard: nunca deve ser null aqui
      val proximoItem = proximoItemPendente(caixa)
      _uiState.update {
          it.copy(
              caixa          = caixa,
              itemAtual      = proximoItem,
              totalItens     = caixa.itens.size,
              itensColetados = caixa.itens.count { item -> item.status == "completo" },
          )
      }
  }
  ```

- [ ] Implementar `onBarcodeScan(barcode: String)`:
  ```kotlin
  private fun onBarcodeScan(barcode: String) {
      val state = _uiState.value
      if (state.scannerBloqueado) return
      val item = state.itemAtual ?: return

      if (barcode == item.sku.codigo) {
          // SKU correto — registra sucesso
          registrarBipagem(
              itemCaixaId     = item.id,
              codigoSkuBipado = barcode,
              enderecoId      = item.endereco.id,
              quantidade      = 1,
              statusColeta    = "sucesso",
          )
      } else {
          // SKU incorreto — exibe modal (não posta ainda)
          feedback.trigger(FeedbackEvent.ERRO_SKU)
          _uiState.update {
              it.copy(
                  errorModal = PickingError(
                      titulo   = "SKU INCORRETO",
                      mensagem = "O código bipado (${barcode}) não corresponde ao SKU esperado " +
                                 "(${item.sku.codigo}). Confirme para registrar a divergência.",
                      tipo     = PickingErrorTipo.SKU_INCORRETO,
                  )
              )
          }
      }
  }
  ```

- [ ] Implementar `onConfirmarErroModal()` — chamado quando o operador toca "CONFIRMAR" no modal:
  ```kotlin
  fun onConfirmarErroModal() {
      val state    = _uiState.value
      val item     = state.itemAtual ?: return
      val tipoErro = state.errorModal?.tipo ?: return

      _uiState.update { it.copy(errorModal = null) }

      when (tipoErro) {
          PickingErrorTipo.SKU_INCORRETO ->
              // Registra `erro_sku` — cria divergência no backend e marca item como `falta`
              registrarBipagem(
                  itemCaixaId     = item.id,
                  codigoSkuBipado = "DIVERGENCIA",   // código representativo, não o esperado
                  enderecoId      = item.endereco.id,
                  quantidade      = 0,
                  statusColeta    = "erro_sku",
              )
          PickingErrorTipo.API_ERROR ->
              Unit  // apenas fecha o modal
          PickingErrorTipo.SEM_SALDO ->
              Unit  // tratado em onSemSaldo()
      }
  }
  ```

- [ ] Implementar `onSemSaldo()`:
  ```kotlin
  fun onSemSaldo() {
      val state = _uiState.value
      if (state.scannerBloqueado) return
      val item = state.itemAtual ?: return

      registrarBipagem(
          itemCaixaId     = item.id,
          codigoSkuBipado = item.sku.codigo,
          enderecoId      = item.endereco.id,
          quantidade      = 0,
          statusColeta    = "sem_saldo",
      )
  }
  ```

- [ ] Implementar `onEnderecosAlternativos()`:
  ```kotlin
  fun onEnderecosAlternativos() {
      val skuId = _uiState.value.itemAtual?.sku?.id ?: return
      viewModelScope.launch { _events.emit(PickingEvent.NavigateToEnderecos(skuId)) }
  }
  ```

- [ ] Implementar `registrarBipagem(...)`:
  ```kotlin
  private fun registrarBipagem(
      itemCaixaId:     String,
      codigoSkuBipado: String,
      enderecoId:      String,
      quantidade:      Int,
      statusColeta:    String,
  ) {
      _uiState.update { it.copy(isLoading = true) }

      viewModelScope.launch {
          when (val result = bipagemRepository.registrar(
              itemCaixaId, codigoSkuBipado, enderecoId, quantidade, statusColeta
          )) {
              is BipagemResult.Success  -> tratarSucesso(result.response, statusColeta)
              is BipagemResult.ItemJaCompleto -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(
                          isLoading  = false,
                          errorModal = PickingError(
                              titulo   = "ITEM JÁ COLETADO",
                              mensagem = "Este item já foi completamente coletado.",
                              tipo     = PickingErrorTipo.API_ERROR,
                          )
                      )
                  }
              }
              is BipagemResult.HttpError -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(
                          isLoading  = false,
                          errorModal = PickingError(
                              titulo   = "ERRO DE CONEXÃO",
                              mensagem = "Erro ${result.code}. A bipagem não foi registrada. Tente novamente.",
                              tipo     = PickingErrorTipo.API_ERROR,
                          )
                      )
                  }
              }
              BipagemResult.NetworkError -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(
                          isLoading  = false,
                          errorModal = PickingError(
                              titulo   = "SEM CONEXÃO",
                              mensagem = "Sem conexão com o servidor. A bipagem não foi registrada.",
                              tipo     = PickingErrorTipo.API_ERROR,
                          )
                      )
                  }
              }
          }
      }
  }
  ```

- [ ] Implementar `tratarSucesso(response: PostBipagemResponse, statusColeta: String)`:
  ```kotlin
  private suspend fun tratarSucesso(response: PostBipagemResponse, statusColeta: String) {
      val caixaAtual = _uiState.value.caixa ?: return

      // Aplica o diff retornado pela API na cópia local
      val caixaAtualizada = caixaAtual.copy(
          status = response.caixaAtualizada.status,
          itens  = caixaAtual.itens.map { item ->
              if (item.id == response.itemAtualizado.id)
                  item.copy(
                      status             = response.itemAtualizado.status,
                      quantidadeColetada = response.itemAtualizado.quantidadeColetada,
                  )
              else item
          }
      )

      // Persiste no holder para que PapeletaScreen veja dados frescos ao voltar
      coletaStateHolder.set(caixaAtualizada)

      // Determina próximo item e feedback
      val itemFoiCompleto = response.itemAtualizado.status == "completo"
      val proximoItem     = proximoItemPendente(caixaAtualizada)
      val itensColetados  = caixaAtualizada.itens.count { it.status == "completo" }

      // Feedback de hardware
      when (statusColeta) {
          "sucesso"   -> if (itemFoiCompleto) feedback.trigger(FeedbackEvent.SKU_COMPLETO)
                         else                 feedback.trigger(FeedbackEvent.PECA_VALIDA)
          "erro_sku"  -> feedback.trigger(FeedbackEvent.ERRO_SKU)
          "sem_saldo" -> feedback.trigger(FeedbackEvent.SEM_SALDO)
      }

      // Atualiza estado
      _uiState.update {
          it.copy(
              caixa          = caixaAtualizada,
              itemAtual      = proximoItem,
              itensColetados = itensColetados,
              isLoading      = false,
          )
      }

      // Navegação pós-coleta
      when {
          response.caixaAtualizada.status == "finalizada" ->
              _events.emit(PickingEvent.CaixaFinalizada)

          statusColeta == "sem_saldo" && proximoItem?.id == _uiState.value.itemAtual?.id ->
              // item permanece o mesmo após sem_saldo → vai para Endereços Alt.
              _events.emit(PickingEvent.NavigateToEnderecos(
                  _uiState.value.itemAtual?.sku?.id ?: return
              ))

          proximoItem == null ->
              // Todos os itens processados, mas caixa não "finalizada" (tem `falta`)
              _events.emit(PickingEvent.PickingParcial)
      }
  }
  ```

- [ ] Implementar helper `proximoItemPendente(caixa: CaixaDto)`:
  ```kotlin
  private fun proximoItemPendente(caixa: CaixaDto): ItemCaixaDto? =
      caixa.itens.firstOrNull { it.status == "pendente" || it.status == "parcial" }
  ```

- [ ] Verificar: `registrarBipagem` nunca é chamado com `scannerBloqueado == true`

---

### 6. Componente AddressChip — `ui/components/AddressChip.kt`

Chip de localização usado no rodapé do corpo da PickingScreen.

- [ ] Criar `AddressChip`:
  ```kotlin
  @Composable
  fun AddressChip(
      label:    String,
      modifier: Modifier = Modifier,
  ) {
      Surface(
          modifier = modifier,
          shape    = RoundedCornerShape(999.dp),   // pill
          color    = MaterialTheme.colorScheme.surfaceContainerHigh,
          border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
          Text(
              text     = label,
              style    = MaterialTheme.typography.labelLarge,
              color    = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
      }
  }
  ```

---

### 7. Componente PickingErrorBottomSheet — `ui/components/PickingErrorBottomSheet.kt`

Bottom sheet de erro (SKU incorreto, sem conexão, item já coletado).

- [ ] Criar `PickingErrorBottomSheet`:
  ```kotlin
  @Composable
  fun PickingErrorBottomSheet(
      error:        PickingError,
      onConfirmar:  () -> Unit,
      modifier:     Modifier = Modifier,
  ) {
      Box(
          modifier = modifier.fillMaxSize(),
          contentAlignment = Alignment.BottomCenter,
      ) {
          // Overlay escuro
          Box(
              modifier = Modifier
                  .fillMaxSize()
                  .background(Color(0xFF191C1E).copy(alpha = 0.6f))
          )

          // Sheet
          Surface(
              modifier = Modifier.fillMaxWidth(),
              shape    = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
              color    = MaterialTheme.colorScheme.background,
          ) {
              Column(
                  modifier             = Modifier.padding(24.dp),
                  horizontalAlignment  = Alignment.CenterHorizontally,
                  verticalArrangement  = Arrangement.spacedBy(16.dp),
              ) {
                  // Ícone
                  Box(
                      modifier = Modifier
                          .size(64.dp)
                          .background(
                              color  = MaterialTheme.colorScheme.errorContainer,
                              shape  = CircleShape,
                          ),
                      contentAlignment = Alignment.Center,
                  ) {
                      Icon(
                          imageVector        = Icons.Filled.Close,
                          contentDescription = null,
                          modifier           = Modifier.size(40.dp),
                          tint               = MaterialTheme.colorScheme.error,
                      )
                  }

                  // Título
                  Text(
                      text      = error.titulo,
                      style     = MaterialTheme.typography.headlineMedium.copy(
                          fontWeight = FontWeight.Bold
                      ),
                      color     = MaterialTheme.colorScheme.error,
                      textAlign = TextAlign.Center,
                  )

                  // Mensagem
                  Text(
                      text      = error.mensagem,
                      style     = MaterialTheme.typography.bodyMedium,
                      color     = MaterialTheme.colorScheme.onSurface,
                      textAlign = TextAlign.Center,
                  )

                  // Botão
                  Button(
                      onClick  = onConfirmar,
                      modifier = Modifier.fillMaxWidth().height(56.dp),
                      shape    = RoundedCornerShape(12.dp),
                      colors   = ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.error,
                          contentColor   = MaterialTheme.colorScheme.onError,
                      ),
                  ) {
                      Text(
                          text  = "CONFIRMAR",
                          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                      )
                  }
              }
          }
      }
  }
  ```

---

### 8. PickingScreen — `ui/picking/PickingScreen.kt`

- [ ] Substituir o placeholder pela implementação real:
  ```kotlin
  @Composable
  fun PickingScreen(
      navController: NavController,
      caixaCodigo:   String,
      viewModel:     PickingViewModel = hiltViewModel(),
  ) {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      // Ciclo de vida do scanner
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

      // Eventos únicos
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
          uiState        = uiState,
          onSemSaldo     = viewModel::onSemSaldo,
          onEnderecos    = viewModel::onEnderecosAlternativos,
          onConfirmarErro = viewModel::onConfirmarErroModal,
      )
  }
  ```

- [ ] Criar `PickingScreenContent`:
  ```kotlin
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

              // ── Header ────────────────────────────────────────────
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

              // ── Corpo ─────────────────────────────────────────────
              val item = uiState.itemAtual
              if (item != null) {
                  Column(
                      modifier = Modifier
                          .weight(1f)
                          .padding(horizontal = 16.dp)
                          .padding(top = 24.dp),
                      verticalArrangement = Arrangement.spacedBy(16.dp),
                  ) {

                      // Referência + descrição
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

                      // Label ENDEREÇO
                      Text(
                          text  = "ENDEREÇO",
                          style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                          color = MaterialTheme.colorScheme.outline,
                      )

                      // Endereço — fonte mono, 72sp
                      Text(
                          text  = item.endereco.codigo,
                          style = MaterialTheme.typography.displayLarge.copy(
                              fontFamily = JetBrainsMono,
                              fontWeight = FontWeight.Black,
                              fontSize   = 72.sp,
                              lineHeight = 80.sp,
                          ),
                          color = MaterialTheme.colorScheme.primaryContainer,
                      )

                      // Chips de localização
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          AddressChip("Corredor ${item.endereco.corredor}")
                          AddressChip("Seção ${item.endereco.prateleira}")
                          AddressChip("Pos ${item.endereco.posicao}")
                      }

                      // Contador de peças
                      Column(
                          modifier            = Modifier.fillMaxWidth(),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                          Text(
                              text  = "${uiState.pecasRestantes} / ${item.quantidadeEsperada}",
                              style = MaterialTheme.typography.displayLarge.copy(
                                  fontWeight = FontWeight.Black,
                                  fontSize   = 48.sp,
                              ),
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
                  // Estado de transição: todos os itens processados, aguardando navegação
                  Box(
                      modifier         = Modifier.weight(1f).fillMaxWidth(),
                      contentAlignment = Alignment.Center,
                  ) {
                      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                  }
              }

              // ── Rodapé ────────────────────────────────────────────
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                  // Sem Saldo — warehouseOrange
                  Button(
                      onClick   = onSemSaldo,
                      enabled   = !uiState.scannerBloqueado && uiState.itemAtual != null,
                      modifier  = Modifier.weight(1f).height(56.dp),
                      shape     = RoundedCornerShape(8.dp),
                      colors    = ButtonDefaults.buttonColors(
                          containerColor         = Color(0xFFEA580C),  // warehouseOrange
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

                  // Endereços Alternativos
                  Button(
                      onClick   = onEnderecos,
                      enabled   = !uiState.scannerBloqueado && uiState.itemAtual != null,
                      modifier  = Modifier.weight(1f).height(56.dp),
                      shape     = RoundedCornerShape(8.dp),
                      colors    = ButtonDefaults.buttonColors(
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

          // ── Loading overlay ───────────────────────────────────────
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

          // ── Modal de erro ─────────────────────────────────────────
          val error = uiState.errorModal
          if (error != null) {
              PickingErrorBottomSheet(
                  error       = error,
                  onConfirmar = onConfirmarErro,
              )
          }
      }
  }
  ```

---

### 9. Previews — `ui/picking/PickingScreen.kt`

- [ ] Adicionar `@Preview` nos estados relevantes:
  ```kotlin
  private val previewItem = ItemCaixaDto(
      id = "uuid-i1", status = "pendente",
      quantidadeEsperada = 2, quantidadeColetada = 0,
      sku     = SkuDto("uuid-s1", "2000183", "Camiseta Manga Curta Masculina", "UN"),
      endereco = EnderecoDto("uuid-e1", "C37.09.6B", "C37", "09", "6B"),
  )

  private val previewCaixa = CaixaDto(
      id = "uuid-c1", codigo = "06772401", status = "em_coleta",
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
                  caixa      = previewCaixa,
                  itemAtual  = previewItem,
                  isLoading  = true,
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
  ```

---

## Fluxo Completo da Tela de Picking

```
PickingScreen abre com caixaCodigo
  → inicializar(): lê CaixaDto do ColetaStateHolder
  → itemAtual = primeiro item "pendente" ou "parcial"
  → DatalogicManager.enable() (ON_RESUME)

── Loop de Coleta ──────────────────────────────────

Scan barcode
  ├─ barcode == itemAtual.sku.codigo?
  │   └─ Sim → registrarBipagem(statusColeta = "sucesso")
  │       ├─ API 200 → tratarSucesso():
  │       │   ├─ item.status == "parcial" → feedback PECA_VALIDA (verde + beep curto)
  │       │   │   → itemAtual.quantidadeColetada++ → mantém item atual
  │       │   ├─ item.status == "completo" → feedback SKU_COMPLETO (verde + 2 beeps)
  │       │   │   → itemAtual = próximo pendente/parcial
  │       │   ├─ caixa.status == "finalizada" → CaixaFinalizada → FinalizacaoScreen("finalizada")
  │       │   └─ sem próximo item → PickingParcial → FinalizacaoScreen("parcial")
  │       └─ API error → modal API_ERROR + feedback ERRO_SKU
  │
  └─ Não → feedback ERRO_SKU (LED vermelho + beep 2s)
       → errorModal = "SKU INCORRETO"
       → operador toca "CONFIRMAR"
           → registrarBipagem(statusColeta = "erro_sku")
           → item.status = "falta" → avança para próximo

Tap "SEM SALDO"
  → registrarBipagem(statusColeta = "sem_saldo")
  → API 200 → feedback SEM_SALDO
  → navega para EnderecosAlternativosScreen(skuId)

Tap "Endereços Alt."
  → navega para EnderecosAlternativosScreen(skuId) direto
```

---

## Definition of Done

- [ ] `PickingScreen` recebe `caixaCodigo` e inicializa a partir do `ColetaStateHolder`
- [ ] Header exibe código da caixa e progresso `X/Y` em branco sobre `primaryContainer`
- [ ] Corpo exibe `sku.codigo`, `sku.descricao` uppercase, `endereco.codigo` em JetBrains Mono 72sp
- [ ] Chips de localização (Corredor, Seção, Posição) exibidos como pills
- [ ] Contador de peças (`pecasRestantes / quantidadeEsperada`) em 48sp Black
- [ ] Scanner ativo em `ON_RESUME`, inativo em `ON_PAUSE`
- [ ] Scan correto → `POST /api/mobile/bipagens` com `sucesso` → contador atualizado → feedback verde
- [ ] Scan de peça que completa o item → feedback SKU_COMPLETO + avança para próximo item
- [ ] Scan incorreto → modal "SKU INCORRETO" + feedback ERRO_SKU (nenhuma chamada de rede ainda)
- [ ] Confirmar erro modal → `POST` com `erro_sku` → item marcado `falta` → avança
- [ ] Tap "SEM SALDO" → `POST` com `sem_saldo` → feedback SEM_SALDO → navega para Endereços Alt.
- [ ] Tap "Endereços Alt." → navega para `EnderecosAlternativosScreen(skuId)` sem postar bipagem
- [ ] Caixa `finalizada` → navega para `FinalizacaoScreen("finalizada")` com back stack limpa até `Picking`
- [ ] Sem próximo item (algum `falta`) → navega para `FinalizacaoScreen("parcial")`
- [ ] `ColetaStateHolder` atualizado com dados frescos após cada bipagem bem-sucedida
- [ ] Loading overlay bloqueia interações durante chamada de rede
- [ ] Modal bloqueia scanner (scans ignorados enquanto modal visível)
- [ ] Botões "SEM SALDO" e "Endereços Alt." desabilitados durante `isLoading`
- [ ] Nenhuma cor hardcoded exceto `warehouseOrange` (`#EA580C`) — não existe token no `colorScheme` do MD3
- [ ] `./gradlew assembleDebug` compila sem erros
- [ ] `@Preview` renderiza corretamente nos três estados
- [ ] `progress-tracker.md` atualizado
