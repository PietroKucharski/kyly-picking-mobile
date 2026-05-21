# 07 — Mobile App: Tela de Menu (Seleção de Papeleta)

## Objetivo

Implementar a tela de Menu — a tela principal do operador após o login.
O operador bipa o código de barras de uma **caixa** para iniciar sua papeleta de picking.
O app valida a caixa via `GET /api/mobile/caixas/:codigo` e, em caso de sucesso,
navega para a `PapeletaScreen` passando o `caixaCodigo` como argumento de rota.

O operador também pode fazer logout nessa tela (JWT removido + retorno ao Login).

---

## Pré-condições

- Spec 06 concluída: Login funcional, JWT armazenado em `SecureStorage`
- `AuthInterceptor` injetando o Bearer token em todas as requisições
- Backend com `GET /api/mobile/caixas/:codigo` funcional (spec 03)
- `AppNavGraph` com `MenuScreen` registrado como destino após o login
- `MenuScreen` navegando para `AppDestination.Papeleta` (atualizar para aceitar argumento `caixaCodigo`)

---

## Layout de Referência

```
┌─────────────────────────────────────────────┐
│  Header — bg Primary (#002444), h=56dp      │
│  "KYLY PICKING" | "Turno: {nomeOperador}"   │
│  (nome à direita em bodyMedium, branco)     │
├─────────────────────────────────────────────┤
│                                             │
│  Área central — flex-1, centralizada        │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │   🔲  [Ícone scan — 64dp]           │    │  Ícone scanner, cor primary
│  │                                     │    │
│  │  "BIPE O CÓDIGO DA CAIXA"           │    │  labelLarge, primary, uppercase
│  │  "Aponte o scanner para o código    │    │  bodyMedium, onSurfaceVariant
│  │   de barras da caixa"               │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │  Aguardando bipagem...  ← hint      │    │  ScanDisplayField, h=56dp
│  │  (preenchido: exibe o código)       │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  [Mensagem de erro — vermelho]              │
│                                             │
│  [CircularProgressIndicator] ← se loading   │
│                                             │
├─────────────────────────────────────────────┤
│  p=16dp                                     │
│  ┌─────────────────────────────────────┐    │
│  │           SAIR                      │    │  h=56dp, ghost button (outlined)
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

---

## Checklist

### 1. DTOs de Caixa — `data/remote/dto/CaixaDtos.kt`

- [ ] Criar os DTOs de resposta do `GET /api/mobile/caixas/:codigo`:
  ```kotlin
  @JsonClass(generateAdapter = true)
  data class GetCaixaResponse(
      val caixa: CaixaDto,
  )

  @JsonClass(generateAdapter = true)
  data class CaixaDto(
      val id:     String,
      val codigo: String,
      val status: String,
      val pedido: PedidoDto,
      val itens:  List<ItemCaixaDto>,
  )

  @JsonClass(generateAdapter = true)
  data class PedidoDto(
      val id:           String,
      val numeroPedido: String,
      val status:       String,
  )

  @JsonClass(generateAdapter = true)
  data class ItemCaixaDto(
      val id:                  String,
      val status:              String,
      val quantidadeEsperada:  Int,
      val quantidadeColetada:  Int,
      val sku:                 SkuDto,
      val endereco:            EnderecoDto,
  )

  @JsonClass(generateAdapter = true)
  data class SkuDto(
      val id:        String,
      val codigo:    String,
      val descricao: String,
      val unidade:   String,
  )

  @JsonClass(generateAdapter = true)
  data class EnderecoDto(
      val id:         String,
      val codigo:     String,
      val corredor:   String,
      val prateleira: String,
      val posicao:    String,
  )
  ```

---

### 2. ApiService — `data/remote/ApiService.kt`

- [ ] Adicionar endpoint de consulta de caixa:
  ```kotlin
  interface ApiService {
      @POST("api/auth/mobile-login")
      suspend fun mobileLogin(@Body body: MobileLoginRequest): MobileLoginResponse

      @GET("api/mobile/caixas/{codigo}")
      suspend fun getCaixa(@Path("codigo") codigo: String): GetCaixaResponse
  }
  ```

---

### 3. AuthInterceptor — `data/remote/AuthInterceptor.kt`

- [ ] Criar o interceptor que injeta o Bearer token em todas as requisições:
  ```kotlin
  @Singleton
  class AuthInterceptor @Inject constructor(
      private val secureStorage: SecureStorage,
  ) : Interceptor {
      override fun intercept(chain: Interceptor.Chain): Response {
          val token = secureStorage.getToken()
          val request = if (token != null) {
              chain.request().newBuilder()
                  .addHeader("Authorization", "Bearer $token")
                  .build()
          } else {
              chain.request()
          }
          return chain.proceed(request)
      }
  }
  ```
- [ ] Verificar: `NetworkModule.provideOkHttp()` já recebe `AuthInterceptor` como parâmetro
  (definido na spec 05). Nenhuma alteração adicional necessária se o módulo já o injeta.

---

### 4. ColetaRepository — `data/repository/ColetaRepository.kt`

- [ ] Criar sealed class de resultado:
  ```kotlin
  sealed class CaixaResult {
      data class Success(val caixa: CaixaDto) : CaixaResult()
      data class NotFound(val message: String = "Caixa não encontrada") : CaixaResult()
      data class AlreadyFinalized(val message: String = "Caixa já finalizada") : CaixaResult()
      data class HttpError(val code: Int, val message: String) : CaixaResult()
      data object NetworkError : CaixaResult()
  }
  ```
- [ ] Criar classe `ColetaRepository` com `@Inject constructor`:
  ```kotlin
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
                  404 -> CaixaResult.NotFound()
                  422 -> CaixaResult.AlreadyFinalized()
                  else -> CaixaResult.HttpError(e.code(), e.message())
              }
          } catch (e: java.io.IOException) {
              CaixaResult.NetworkError
          }
      }
  }
  ```

---

### 5. Atualizar AppNavGraph — `ui/navigation/AppNavGraph.kt`

- [ ] Atualizar `AppDestination.Papeleta` para aceitar `caixaCodigo` como argumento:
  ```kotlin
  object Papeleta : AppDestination("papeleta/{caixaCodigo}") {
      fun withArgs(caixaCodigo: String) = "papeleta/$caixaCodigo"
  }
  ```
- [ ] Atualizar o composable da `PapeletaScreen` no `NavHost`:
  ```kotlin
  composable(
      AppDestination.Papeleta.route,
      arguments = listOf(navArgument("caixaCodigo") { type = NavType.StringType })
  ) { backStackEntry ->
      PapeletaScreen(
          navController = navController,
          caixaCodigo   = backStackEntry.arguments?.getString("caixaCodigo") ?: "",
      )
  }
  ```
- [ ] Verificar: `PapeletaScreen` (composable placeholder) aceita `caixaCodigo: String`
  como parâmetro (pode exibir `Text("Papeleta: $caixaCodigo")` por ora)

---

### 6. MenuUiState e MenuEvent — `ui/menu/MenuViewModel.kt`

- [ ] Definir classes de estado e eventos:
  ```kotlin
  data class MenuUiState(
      val codigoLido:   String = "",
      val isLoading:    Boolean = false,
      val errorMessage: String? = null,
  ) {
      val hasCode: Boolean get() = codigoLido.isNotBlank()
  }

  sealed class MenuEvent {
      data class NavigateToPapeleta(val caixaCodigo: String) : MenuEvent()
      data object NavigateToLogin : MenuEvent()
  }
  ```

---

### 7. MenuViewModel — `ui/menu/MenuViewModel.kt`

- [ ] Criar `MenuViewModel` anotado com `@HiltViewModel`:
  ```kotlin
  @HiltViewModel
  class MenuViewModel @Inject constructor(
      private val coletaRepository: ColetaRepository,
      private val secureStorage: SecureStorage,
      private val datalogic: DatalogicManager,
      private val feedback: FeedbackManager,
  ) : ViewModel() {

      private val _uiState = MutableStateFlow(MenuUiState())
      val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

      private val _events = MutableSharedFlow<MenuEvent>()
      val events: SharedFlow<MenuEvent> = _events

      init {
          viewModelScope.launch {
              datalogic.barcodeEvents.collect { barcode -> onBarcodeScan(barcode) }
          }
      }

      fun onResume() = datalogic.enable()
      fun onPause()  = datalogic.disable()
  }
  ```
- [ ] Implementar `onBarcodeScan(barcode: String)`:
  - Ignorar se já está carregando
  - Preencher `codigoLido` e disparar `buscarCaixa` imediatamente
  ```kotlin
  private fun onBarcodeScan(barcode: String) {
      if (_uiState.value.isLoading) return
      _uiState.update { it.copy(codigoLido = barcode, errorMessage = null) }
      buscarCaixa(barcode)
  }
  ```
- [ ] Implementar `buscarCaixa(codigo: String)`:
  ```kotlin
  private fun buscarCaixa(codigo: String) {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      viewModelScope.launch {
          when (val result = coletaRepository.buscarCaixa(codigo)) {
              is CaixaResult.Success -> {
                  _events.emit(MenuEvent.NavigateToPapeleta(result.caixa.codigo))
              }
              is CaixaResult.NotFound -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(isLoading = false, errorMessage = "Caixa não encontrada. Verifique o código.")
                  }
              }
              is CaixaResult.AlreadyFinalized -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(isLoading = false, errorMessage = "Esta caixa já foi finalizada.")
                  }
              }
              is CaixaResult.HttpError -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(isLoading = false, errorMessage = "Erro ${result.code}. Tente novamente.")
                  }
              }
              CaixaResult.NetworkError -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(isLoading = false, errorMessage = "Sem conexão. Tente novamente.")
                  }
              }
          }
      }
  }
  ```
- [ ] Implementar `onRetry()` — limpa o código lido para nova bipagem:
  ```kotlin
  fun onRetry() {
      _uiState.update { MenuUiState() }
  }
  ```
- [ ] Implementar `onLogout()` — limpa JWT e navega para Login:
  ```kotlin
  fun onLogout() {
      secureStorage.clearToken()
      viewModelScope.launch {
          _events.emit(MenuEvent.NavigateToLogin)
      }
  }
  ```

---

### 8. MenuScreen — `ui/menu/MenuScreen.kt`

- [ ] Criar `MenuScreen` composable com `hiltViewModel()`:
  ```kotlin
  @Composable
  fun MenuScreen(
      navController: NavController,
      viewModel: MenuViewModel = hiltViewModel(),
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
                  is MenuEvent.NavigateToPapeleta ->
                      navController.navigate(AppDestination.Papeleta.withArgs(event.caixaCodigo))

                  MenuEvent.NavigateToLogin ->
                      navController.navigate(AppDestination.Login.route) {
                          popUpTo(0) { inclusive = true } // limpa toda a back stack
                      }
              }
          }
      }

      MenuScreenContent(
          uiState   = uiState,
          onRetry   = viewModel::onRetry,
          onLogout  = viewModel::onLogout,
      )
  }
  ```
- [ ] Criar `MenuScreenContent` separado (facilita Preview e testes):
  ```kotlin
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

          // ── Header ────────────────────────────────────────────────
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

          // ── Área Central ─────────────────────────────────────────
          Column(
              modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
          ) {

              // Ícone scanner
              Icon(
                  imageVector         = Icons.Outlined.QrCodeScanner,
                  contentDescription  = null,
                  modifier            = Modifier.size(64.dp),
                  tint                = MaterialTheme.colorScheme.primary,
              )

              Spacer(Modifier.height(24.dp))

              // Instrução principal
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

              // Campo de exibição da bipagem
              ScanDisplayField(
                  label      = "CÓDIGO DA CAIXA",
                  hint       = "Aguardando bipagem...",
                  value      = uiState.codigoLido,
                  isFilled   = uiState.hasCode,
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier   = Modifier.fillMaxWidth(),
              )

              // Loading
              if (uiState.isLoading) {
                  Spacer(Modifier.height(24.dp))
                  CircularProgressIndicator(
                      modifier    = Modifier.size(32.dp),
                      color       = MaterialTheme.colorScheme.primary,
                      strokeWidth = 3.dp,
                  )
              }

              // Erro
              if (uiState.errorMessage != null) {
                  Spacer(Modifier.height(16.dp))
                  Text(
                      text  = uiState.errorMessage,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.error,
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

          // ── Rodapé ────────────────────────────────────────────────
          Box(modifier = Modifier.padding(16.dp)) {
              OutlinedButton(
                  onClick   = onLogout,
                  modifier  = Modifier.fillMaxWidth().height(56.dp),
                  shape     = RoundedCornerShape(8.dp),
                  border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
  ```

---

### 9. Placeholder de PapeletaScreen — `ui/papeleta/PapeletaScreen.kt`

A spec completa da `PapeletaScreen` é entregue separadamente. Crie aqui
um placeholder funcional para que a navegação compile:

- [ ] Criar `PapeletaScreen.kt` com composable mínimo:
  ```kotlin
  @Composable
  fun PapeletaScreen(
      navController: NavController,
      caixaCodigo: String,
  ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(16.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
          Text(
              text  = "Papeleta",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.height(8.dp))
          Text(
              text  = "Caixa: $caixaCodigo",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
      }
  }
  ```

---

### 10. Previews — `ui/menu/MenuScreen.kt`

- [ ] Adicionar `@Preview` nos três estados:
  ```kotlin
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
  ```

---

## Fluxo Completo da Tela de Menu

```
MenuScreen abre após login
  → DatalogicManager.enable() (ON_RESUME)
  → Estado inicial: aguardando bipagem

Scan barcode
  → onBarcodeScan(barcode)
  → codigoLido = barcode, isLoading = true
  → GET /api/mobile/caixas/{barcode} (com JWT via AuthInterceptor)
  ├─ 200 OK
  │   → NavigateToPapeleta(caixaCodigo)
  │   → navController.navigate("papeleta/{caixaCodigo}")
  ├─ 404 Not Found
  │   → feedback ERRO_SKU (LED vermelho + beep 2s)
  │   → errorMessage = "Caixa não encontrada..."
  ├─ 422 Already Finalized
  │   → feedback ERRO_SKU
  │   → errorMessage = "Esta caixa já foi finalizada."
  ├─ 401 Unauthorized
  │   → (token expirado) → onLogout() → volta para Login
  └─ IOException
      → feedback ERRO_SKU
      → errorMessage = "Sem conexão..."

Tap "Tentar novamente"
  → onRetry() → codigoLido = "", errorMessage = null
  → aguarda nova bipagem

Tap "SAIR"
  → onLogout()
  → secureStorage.clearToken()
  → navController.navigate(Login) { popUpTo(0) inclusive = true }
```

---

## Definition of Done

- [ ] `MenuScreen` abre após login bem-sucedido
- [ ] Header navy com "KYLY PICKING" em branco
- [ ] Ícone de scanner exibido na área central
- [ ] Scanner habilitado em `ON_RESUME` e desabilitado em `ON_PAUSE`
- [ ] Bipagem dispara `GET /api/mobile/caixas/:codigo` com Bearer token automaticamente
- [ ] Sucesso → navega para `PapeletaScreen` passando `caixaCodigo`
- [ ] 404 → mensagem de erro vermelha + feedback `ERRO_SKU`
- [ ] 422 → mensagem "caixa já finalizada" + feedback `ERRO_SKU`
- [ ] Erro de rede → mensagem "Sem conexão" + feedback `ERRO_SKU`
- [ ] "Tentar novamente" limpa o estado e aguarda nova bipagem
- [ ] "SAIR" limpa o JWT (`SecureStorage.clearToken()`) e volta para Login com back stack zerada
- [ ] `AppDestination.Papeleta` atualizado para `"papeleta/{caixaCodigo}"`
- [ ] `PapeletaScreen` placeholder recebe `caixaCodigo` sem crash
- [ ] Nenhuma cor hardcoded — apenas `MaterialTheme.colorScheme.*`
- [ ] `./gradlew assembleDebug` compila sem erros
- [ ] `@Preview` renderiza corretamente nos três estados
- [ ] `progress-tracker.md` atualizado
