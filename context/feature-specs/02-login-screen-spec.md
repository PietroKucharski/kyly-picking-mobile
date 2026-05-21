# 06 — Mobile App: Tela de Login

## Objetivo

Implementar a tela de Login do app KylyPicking. O operador autentica bipando
**dois barcodes em sequência** com o scanner Datalogic:

1. **Barcode do Supervisor** — código de autorização emitido pelo supervisor para
   liberar o turno. Validado localmente (deve ter 6 dígitos numéricos).
2. **Crachá do Operador** — matrícula do operador (barcode no crachá físico).

Após as duas bipagens o app chama `POST /api/auth/mobile-login` com
`{ matricula, pin }`, onde o código do supervisor é o `pin`. Em caso de sucesso,
armazena o JWT via `SecureStorage` e navega para `MenuScreen`.

---

## Pré-condições

- Setup 05 concluído: projeto compila, tema aplicado, `AppNavGraph` com `LoginScreen`
  como destino inicial
- Backend rodando com `POST /api/auth/mobile-login` funcional (spec 02)
- `API_BASE_URL` definida em `local.properties`
- `SecureStorage`, `DatalogicManager` e `FeedbackManager` injetáveis via Hilt

---

## Layout de Referência

Tradução do design system (ui-context.md) para Jetpack Compose:

```
┌─────────────────────────────────────────────┐
│  Header — bg Primary (#002444), h=56dp      │
│  "KYLY PICKING" — uppercase, white, bold    │
├─────────────────────────────────────────────┤
│  px=16dp, pt=32dp                           │
│                                             │
│  "Sistema de Picking"  ← headlineMedium     │
│  "Faça login para iniciar" ← bodyMedium     │
│                                             │
│  ── LABEL ───────────────────────────────   │
│  "1. BIPE O CÓDIGO DO SUPERVISOR"           │
│  ┌─────────────────────────────────────┐    │
│  │  Aguardando bipagem...  ← hint      │    │  h=56dp
│  │  (preenchido: código oculto ●●●●●●) │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ── LABEL ───────────────────────────────   │
  │  "2. BIPE O SEU CRACHÁ"                   │
│  ┌─────────────────────────────────────┐    │
│  │  Aguardando crachá...  ← hint       │    │  h=56dp
│  │  (preenchido: matrícula em texto)   │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  [Erro inline — vermelho, se houver]        │
│                                             │
├─────────────────────────────────────────────┤
│  p=16dp                                     │
│  ┌─────────────────────────────────────┐    │
│  │           ENTRAR                    │    │  h=60dp
│  └─────────────────────────────────────┘    │
│  (desabilitado até os dois campos serem     │
│   preenchidos; spinner quando carregando)   │
└─────────────────────────────────────────────┘
```

---

## Checklist

### 1. DTOs de Auth — `data/remote/dto/AuthDtos.kt`

- [ ] Criar data class de request:
  ```kotlin
  @JsonClass(generateAdapter = true)
  data class MobileLoginRequest(
      val matricula: String,
      val pin: String,
  )
  ```
- [ ] Criar data class de response:
  ```kotlin
  @JsonClass(generateAdapter = true)
  data class MobileLoginResponse(
      val token: String,
      val usuario: UsuarioDto,
  )

  @JsonClass(generateAdapter = true)
  data class UsuarioDto(
      val id: String,
      val nome: String,
      val matricula: String,
      val tipo: String,
  )
  ```

---

### 2. ApiService — `data/remote/ApiService.kt`

- [ ] Adicionar endpoint de login:
  ```kotlin
  interface ApiService {
      @POST("api/auth/mobile-login")
      suspend fun mobileLogin(@Body body: MobileLoginRequest): MobileLoginResponse
      // (outros endpoints serão adicionados em specs futuras)
  }
  ```

---

### 3. AuthRepository — `data/repository/AuthRepository.kt`

- [ ] Criar sealed class de resultado:
  ```kotlin
  sealed class AuthResult {
      data class Success(val response: MobileLoginResponse) : AuthResult()
      data class HttpError(val code: Int, val message: String) : AuthResult()
      data object NetworkError : AuthResult()
  }
  ```
- [ ] Criar classe `AuthRepository` com `@Inject constructor`:
  ```kotlin
  @Singleton
  class AuthRepository @Inject constructor(
      private val apiService: ApiService,
      private val secureStorage: SecureStorage,
  ) {
      suspend fun mobileLogin(matricula: String, pin: String): AuthResult {
          return try {
              val response = apiService.mobileLogin(MobileLoginRequest(matricula, pin))
              secureStorage.saveToken(response.token)
              AuthResult.Success(response)
          } catch (e: retrofit2.HttpException) {
              AuthResult.HttpError(e.code(), e.message())
          } catch (e: java.io.IOException) {
              AuthResult.NetworkError
          }
      }
  }
  ```
- [ ] Verificar: `secureStorage.saveToken()` só é chamado em caso de sucesso da API

---

### 4. LoginUiState e LoginEvent — `ui/login/LoginViewModel.kt`

- [ ] Definir classes de estado e eventos:
  ```kotlin
  data class LoginUiState(
      val supervisorCode: String = "",      // oculto na UI (●●●●●●)
      val matricula: String = "",           // visível na UI
      val isLoading: Boolean = false,
      val errorMessage: String? = null,
  ) {
      val canSubmit: Boolean
          get() = supervisorCode.isNotBlank() && matricula.isNotBlank() && !isLoading
  }

  sealed class LoginEvent {
      data object NavigateToMenu : LoginEvent()
  }
  ```

---

### 5. LoginViewModel — `ui/login/LoginViewModel.kt`

- [ ] Criar `LoginViewModel` anotado com `@HiltViewModel`:
  ```kotlin
  @HiltViewModel
  class LoginViewModel @Inject constructor(
      private val authRepository: AuthRepository,
      private val datalogic: DatalogicManager,
      private val feedback: FeedbackManager,
  ) : ViewModel() {

      private val _uiState = MutableStateFlow(LoginUiState())
      val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

      private val _events = MutableSharedFlow<LoginEvent>()
      val events: SharedFlow<LoginEvent> = _events

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
  - Se `supervisorCode` estiver vazio: preencher `supervisorCode` com o barcode
  - Se `supervisorCode` estiver preenchido e `matricula` estiver vazio: preencher `matricula`
  - Se os dois já estiverem preenchidos: ignorar (não re-bipa automaticamente)
  - Limpar `errorMessage` ao receber novo barcode
  ```kotlin
  private fun onBarcodeScan(barcode: String) {
      _uiState.update { state ->
          when {
              state.supervisorCode.isBlank() ->
                  state.copy(supervisorCode = barcode, errorMessage = null)
              state.matricula.isBlank() ->
                  state.copy(matricula = barcode, errorMessage = null)
              else -> state
          }
      }
  }
  ```
- [ ] Implementar `onLoginClick()`:
  ```kotlin
  fun onLoginClick() {
      val state = _uiState.value
      if (!state.canSubmit) return

      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      viewModelScope.launch {
          when (val result = authRepository.mobileLogin(state.matricula, state.supervisorCode)) {
              is AuthResult.Success -> {
                  _events.emit(LoginEvent.NavigateToMenu)
              }
              is AuthResult.HttpError -> {
                  val msg = when (result.code) {
                      401 -> "Credenciais inválidas. Verifique o código do supervisor e o crachá."
                      403 -> "Acesso negado para este tipo de usuário."
                      else -> "Erro ${result.code}. Tente novamente."
                  }
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
              }
              AuthResult.NetworkError -> {
                  feedback.trigger(FeedbackEvent.ERRO_SKU)
                  _uiState.update {
                      it.copy(isLoading = false, errorMessage = "Sem conexão com o servidor.")
                  }
              }
          }
      }
  }
  ```
- [ ] Implementar `onReset()` para limpar os dois campos (caso o operador queira re-bipa):
  ```kotlin
  fun onReset() {
      _uiState.update { LoginUiState() }
  }
  ```
- [ ] Verificar: nenhum `process.env` ou `BuildConfig` acessado no ViewModel

---

### 6. LoginScreen — `ui/login/LoginScreen.kt`

- [ ] Criar `LoginScreen` composable com `hiltViewModel()` como padrão:
  ```kotlin
  @Composable
  fun LoginScreen(
      navController: NavController,
      viewModel: LoginViewModel = hiltViewModel(),
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

      // Eventos únicos (navegação)
      LaunchedEffect(Unit) {
          viewModel.events.collect { event ->
              when (event) {
                  LoginEvent.NavigateToMenu ->
                      navController.navigate(AppDestination.Menu.route) {
                          popUpTo(AppDestination.Login.route) { inclusive = true }
                      }
              }
          }
      }

      LoginScreenContent(
          uiState  = uiState,
          onLogin  = viewModel::onLoginClick,
          onReset  = viewModel::onReset,
      )
  }
  ```
- [ ] Criar `LoginScreenContent` separado (facilita Preview e testes):
  ```kotlin
  @Composable
  fun LoginScreenContent(
      uiState: LoginUiState,
      onLogin: () -> Unit,
      onReset: () -> Unit,
  ) {
      Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

          // ── Header ──────────────────────────────────────────────────
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
          ) {
              Text(
                  text = "KYLY PICKING",
                  style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Black,
                      letterSpacing = 2.sp,
                  ),
                  color = MaterialTheme.colorScheme.onPrimary,
              )
          }

          // ── Body ─────────────────────────────────────────────────────
          Column(
              modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 16.dp)
                  .padding(top = 32.dp),
              verticalArrangement = Arrangement.spacedBy(24.dp),
          ) {
              // Títulos
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                      text = "Sistema de Picking",
                      style = MaterialTheme.typography.headlineMedium,
                      color = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                      text = "Faça login para iniciar",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
              }

              // Campo 1 — Código do Supervisor
              ScanDisplayField(
                  label     = "1. BIPE O CÓDIGO DO SUPERVISOR",
                  hint      = "Aguardando bipagem...",
                  value     = if (uiState.supervisorCode.isNotBlank()) "●".repeat(uiState.supervisorCode.length) else "",
                  isFilled  = uiState.supervisorCode.isNotBlank(),
                  labelColor = MaterialTheme.colorScheme.primary,
              )

              // Campo 2 — Crachá do Operador
              ScanDisplayField(
                  label     = "2. BIPE O SEU CRACHÁ",
                  hint      = "Aguardando crachá...",
                  value     = uiState.matricula,
                  isFilled  = uiState.matricula.isNotBlank(),
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              // Mensagem de erro
              if (uiState.errorMessage != null) {
                  Text(
                      text = uiState.errorMessage,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.error,
                  )
              }

              // Link de reset
              if (uiState.supervisorCode.isNotBlank() || uiState.matricula.isNotBlank()) {
                  TextButton(onClick = onReset) {
                      Text(
                          text = "Limpar e recomeçar",
                          style = MaterialTheme.typography.labelLarge,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                  }
              }
          }

          // ── Rodapé ────────────────────────────────────────────────────
          Box(modifier = Modifier.padding(16.dp)) {
              Button(
                  onClick  = onLogin,
                  enabled  = uiState.canSubmit,
                  modifier = Modifier.fillMaxWidth().height(60.dp),
                  shape    = RoundedCornerShape(12.dp),
                  colors   = ButtonDefaults.buttonColors(
                      containerColor     = MaterialTheme.colorScheme.primaryContainer,
                      contentColor       = MaterialTheme.colorScheme.onPrimary,
                      disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                  ),
              ) {
                  if (uiState.isLoading) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(24.dp),
                          color    = MaterialTheme.colorScheme.onPrimary,
                          strokeWidth = 2.dp,
                      )
                  } else {
                      Text(
                          text  = "ENTRAR",
                          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                      )
                  }
              }
          }
      }
  }
  ```

---

### 7. Componente ScanDisplayField — `ui/components/ScanDisplayField.kt`

Componente reutilizável para campos que recebem bipagem (não editável manualmente).

- [ ] Criar composable `ScanDisplayField`:
  ```kotlin
  @Composable
  fun ScanDisplayField(
      label: String,
      hint: String,
      value: String,
      isFilled: Boolean,
      labelColor: Color,
      modifier: Modifier = Modifier,
  ) {
      Column(
          modifier = modifier,
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
          Text(
              text = label,
              style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
              color = labelColor,
          )
          Box(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(56.dp)
                  .background(
                      color = MaterialTheme.colorScheme.surfaceContainerLow,
                      shape = RoundedCornerShape(4.dp),
                  )
                  .border(
                      width = 1.dp,
                      color = if (isFilled)
                                  MaterialTheme.colorScheme.primary
                              else
                                  MaterialTheme.colorScheme.outlineVariant,
                      shape = RoundedCornerShape(4.dp),
                  )
                  .padding(horizontal = 16.dp),
              contentAlignment = Alignment.CenterStart,
          ) {
              Text(
                  text  = if (isFilled) value else hint,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isFilled)
                              MaterialTheme.colorScheme.onSurface
                          else
                              MaterialTheme.colorScheme.outlineVariant,
              )
          }
      }
  }
  ```

---

### 8. Registro no NetworkModule — `di/NetworkModule.kt`

- [ ] Certificar que `NetworkModule` já provê `ApiService` (feito na spec 05).
  Nenhuma alteração necessária se o módulo já registra `Retrofit` e `ApiService`.

---

### 9. Navegação — `ui/navigation/AppNavGraph.kt`

- [ ] Verificar que `LoginScreen(navController)` já está registrado como
  `startDestination` em `AppNavGraph` (feito na spec 05).
- [ ] Verificar que `MenuScreen` existe como destino registrado (pode ser um
  `Composable` vazio com apenas `Text("Menu")` até a spec de menu ser implementada).

---

### 10. Preview

- [ ] Adicionar `@Preview` para `LoginScreenContent` nos dois estados:
  ```kotlin
  @Preview(showBackground = true, widthDp = 360, heightDp = 800)
  @Composable
  fun LoginScreenEmptyPreview() {
      KylyPickingTheme {
          LoginScreenContent(
              uiState = LoginUiState(),
              onLogin = {},
              onReset = {},
          )
      }
  }

  @Preview(showBackground = true, widthDp = 360, heightDp = 800)
  @Composable
  fun LoginScreenFilledPreview() {
      KylyPickingTheme {
          LoginScreenContent(
              uiState = LoginUiState(
                  supervisorCode = "123456",
                  matricula      = "OP9942",
              ),
              onLogin = {},
              onReset = {},
          )
      }
  }

  @Preview(showBackground = true, widthDp = 360, heightDp = 800)
  @Composable
  fun LoginScreenErrorPreview() {
      KylyPickingTheme {
          LoginScreenContent(
              uiState = LoginUiState(
                  supervisorCode = "999999",
                  matricula      = "OP9942",
                  errorMessage   = "Credenciais inválidas. Verifique o código do supervisor e o crachá.",
              ),
              onLogin = {},
              onReset = {},
          )
      }
  }
  ```

---

## Fluxo Completo de Login

```
App abre → LoginScreen (startDestination)
         → DatalogicManager.enable() (ON_RESUME)

Scan 1 → onBarcodeScan() → supervisorCode preenchido
         → campo 1 exibe "●●●●●●"

Scan 2 → onBarcodeScan() → matricula preenchida
         → campo 2 exibe matrícula
         → botão "ENTRAR" habilitado

Tap "ENTRAR" → isLoading = true
             → POST /api/auth/mobile-login { matricula, pin: supervisorCode }
             ├─ 200 → saveToken(jwt) → NavigateToMenu
             ├─ 401 → errorMessage = "Credenciais inválidas..."
             │        feedback.trigger(ERRO_SKU)
             ├─ 403 → errorMessage = "Acesso negado..."
             │        feedback.trigger(ERRO_SKU)
             └─ IOException → errorMessage = "Sem conexão..."
                              feedback.trigger(ERRO_SKU)
```

---

## Definition of Done

- [ ] `LoginScreen` abre como tela inicial do app
- [ ] Header navy (`#002444`) com "KYLY PICKING" em branco uppercase
- [ ] Campos 1 e 2 recebem bipagem via `DatalogicManager.barcodeEvents` (sem teclado)
- [ ] Campo 1 exibe código do supervisor mascarado (`●●●●●●`)
- [ ] Campo 2 exibe matrícula em texto
- [ ] Botão "ENTRAR" desabilitado até os dois campos estarem preenchidos
- [ ] Botão exibe `CircularProgressIndicator` enquanto `isLoading = true`
- [ ] Sucesso → JWT salvo em `EncryptedSharedPreferences`, navegação para `MenuScreen`
- [ ] Erro 401/403 → mensagem em vermelho + feedback `ERRO_SKU` (LED vermelho + beep 2s)
- [ ] Erro de rede → mensagem "Sem conexão com o servidor"
- [ ] "Limpar e recomeçar" reseta os dois campos
- [ ] Scanner desativa ao sair da tela (`ON_PAUSE → disable()`)
- [ ] Nenhuma cor hardcoded — apenas `MaterialTheme.colorScheme.*`
- [ ] `./gradlew assembleDebug` compila sem erros
- [ ] `@Preview` renderiza corretamente no Android Studio
- [ ] `progress-tracker.md` atualizado
