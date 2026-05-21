# Mobile — Code Standards

## Geral

- Módulos pequenos e de responsabilidade única. Uma tela não acumula lógica de outra.
- Corrija causas raiz — não adicione `try/catch` em cima de comportamentos incorretos.
- Toda lógica de negócio de picking fica nos `repository/`. ViewModels orquestram,
  telas apenas renderizam e emitem eventos de UI.
- `DatalogicManager` é a única interface com o hardware. ViewModels nunca importam
  classes do Datalogic SDK diretamente.

---

## Kotlin

- Proibido `!!` (null assertion) fora de testes. Use `?.`, `?: return`, `requireNotNull()`.
- Proibido `var` em data classes e `state` de UI — prefira `val` e imutabilidade.
- Prefira `data class` para DTOs e modelos de domínio.
- Sealed classes para resultados de operações:

```kotlin
// Padrão de resultado nos repositórios
sealed class Result<out T> {
    data class Success<T>(val data: T)   : Result<T>()
    data class Error(val message: String): Result<Nothing>()
}
```

- Flows: use `StateFlow` para estado de UI, `SharedFlow` para eventos únicos
  (navegação, mensagens de erro).
- Sem `GlobalScope`. Sempre `viewModelScope` ou `lifecycleScope`.

---

## Jetpack Compose — Convenções de Tela

- Uma tela por arquivo. Nome do arquivo = nome do `@Composable` (`PickingScreen.kt`).
- Telas recebem `navController` e `viewModel: ViewModel = hiltViewModel()`.
- Componentes reutilizáveis (`components/`) recebem tudo via parâmetro — sem acesso
  a ViewModel ou navegação internamente.
- `collectAsStateWithLifecycle()` para consumir `StateFlow` nas telas.
- Sem lógica de negócio em `@Composable`. Use `LaunchedEffect` apenas para side-effects
  de UI (scroll, foco), não para chamadas de API.

```kotlin
// Estrutura padrão de tela
@Composable
fun PickingScreen(
    navController: NavController,
    viewModel: PickingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Gerencia ciclo de vida do scanner
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

    // Eventos únicos (navegação, erros)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PickingEvent.CaixaFinalizada ->
                    navController.navigate(AppDestination.Finalizacao.withArgs("finalizada"))
                is PickingEvent.ErroSku ->
                    { /* exibe bottom sheet */ }
            }
        }
    }

    KylyPickingTheme {
        Scaffold { padding ->
            PickingContent(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onSemSaldo  = viewModel::onSemSaldo,
                onEnderecos = { navController.navigate(AppDestination.Enderecos.withArgs(it)) },
            )
        }
    }
}
```

---

## ViewModel — Padrões

```kotlin
@HiltViewModel
class PickingViewModel @Inject constructor(
    private val datalogic: DatalogicManager,
    private val feedback: FeedbackManager,
    private val coletaRepo: ColetaRepository,
) : ViewModel() {

    // Estado imutável da tela
    private val _uiState = MutableStateFlow(PickingUiState())
    val uiState: StateFlow<PickingUiState> = _uiState.asStateFlow()

    // Eventos únicos para a tela (navegação, bottom sheet)
    private val _events = MutableSharedFlow<PickingEvent>()
    val events: SharedFlow<PickingEvent> = _events

    init {
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode ->
                processBipagem(barcode)
            }
        }
    }

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()

    private suspend fun processBipagem(barcode: String) {
        val resultado = coletaRepo.registrarBipagem(barcode, _uiState.value.itemAtual.id)
        feedback.trigger(resultado.feedbackEvent)
        when (resultado) {
            is ResultadoBipagem.CaixaFinalizada -> _events.emit(PickingEvent.CaixaFinalizada)
            is ResultadoBipagem.ErroSku         -> _events.emit(PickingEvent.ErroSku(resultado.mensagem))
            is ResultadoBipagem.PecaValida      -> _uiState.update { it.copy(/* avança estado */) }
        }
    }

    fun onSemSaldo() {
        viewModelScope.launch {
            coletaRepo.registrarSemSaldo(_uiState.value.itemAtual.id)
            feedback.trigger(FeedbackEvent.SEM_SALDO)
        }
    }
}
```

Regras:
- `_uiState` e `_events` sempre `private`. Expose apenas `val` imutável.
- `onCleared()` não precisa chamar `disable()` — o `ON_PAUSE` da tela já faz isso.
- Sem lógica de layout ou de string formatting no ViewModel — isso pertence à tela.

---

## Material 3 — Regras de Estilo

- Todas as cores via `MaterialTheme.colorScheme.*` ou constantes semânticas de
  `ui/theme/Color.kt`. **Nenhum hex hardcoded** nos `@Composable`.
- Para cores semânticas fora do `colorScheme` (ex: `SuccessIndustrial`, `WarehouseOrange`):
  importar diretamente de `Color.kt`.
- Tamanhos de toque mínimos: `Modifier.heightIn(min = 56.dp)` em qualquer elemento
  interativo. Sem exceções para uso com luvas.
- `rounded` para inputs (4dp), `rounded-lg` para cards (8dp), `rounded-xl` para
  CTAs de estado e modais (12dp).
- Fontes via `MaterialTheme.typography.*`. Para endereço: `NumeralXl` direto de `Type.kt`.
- Animações máximo 150ms. `animateContentSize()` para mudanças de tamanho.

```kotlin
// ✅ Correto
Text(
    text = endereco,
    style = NumeralXl,              // de ui/theme/Type.kt
    color = MaterialTheme.colorScheme.onPrimaryContainer,
)

Button(
    onClick = onSemSaldo,
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = WarehouseOrange,  // de ui/theme/Color.kt
        contentColor   = Color.White,
    ),
    shape = RoundedCornerShape(8.dp),
) {
    Text("Sem Saldo", style = MaterialTheme.typography.labelLarge)
}

// ❌ Errado
Text(text = endereco, color = Color(0xFF1B3A5C))  // hex hardcoded
```

---

## Retrofit + Moshi — HTTP Client

```kotlin
// data/remote/ApiService.kt
interface ApiService {
    @POST("api/auth/mobile-login")
    suspend fun mobileLogin(@Body body: MobileLoginRequest): MobileLoginResponse

    @GET("api/mobile/caixas/{codigo}")
    suspend fun getCaixa(@Path("codigo") codigo: String): CaixaResponse

    @POST("api/mobile/bipagens")
    suspend fun postBipagem(@Body body: PostBipagemRequest): PostBipagemResponse
}

// data/remote/AuthInterceptor.kt
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
        } else chain.request()
        val response = chain.proceed(request)
        if (response.code == 401) secureStorage.clearToken()
        return response
    }
}
```

---

## Room — Fila Offline

```kotlin
// data/local/BipagemPendenteEntity.kt
@Entity(tableName = "bipagens_pendentes")
data class BipagemPendenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCaixaId:     String,
    val codigoSkuBipado: String,
    val enderecoId:      String,
    val quantidade:      Int,
    val statusColeta:    String,   // "sucesso" | "erro_sku" | "sem_saldo"
    val criadoEm:        Long = System.currentTimeMillis(),
)

// data/local/BipagemPendenteDao.kt
@Dao
interface BipagemPendenteDao {
    @Query("SELECT * FROM bipagens_pendentes ORDER BY criadoEm ASC")
    suspend fun listarTodas(): List<BipagemPendenteEntity>

    @Insert
    suspend fun inserir(bipagem: BipagemPendenteEntity)

    @Delete
    suspend fun deletar(bipagem: BipagemPendenteEntity)
}
```

---

## WorkManager — Sync Worker

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: BipagemPendenteDao,
    private val api: ApiService,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendentes = dao.listarTodas()
        if (pendentes.isEmpty()) return Result.success()
        return try {
            pendentes.forEach { bipagem ->
                api.postBipagem(bipagem.toRequest())
                dao.deletar(bipagem)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## Datalogic SDK — Regras

- Nenhum arquivo fora de `hardware/DatalogicManager.kt` importa classes do Datalogic SDK.
- Nenhum ViewModel ou tela chama `setLed*()` ou `beep*()` diretamente. Sempre via
  `FeedbackManager.trigger(FeedbackEvent.*)`.
- `DatalogicManager.enable()` chamado **apenas** no `ON_RESUME` da tela de bipagem.
  `disable()` **obrigatório** no `ON_PAUSE` — o laser nunca deve ficar ativo fora do scan.
- Mudanças no SDK `.aar` exigem sync do Gradle e rebuild completo.
  Documente em `progress-tracker.md`.
- Teste obrigatório no dispositivo físico Datalogic Memor 11 — o emulador não tem
  o SDK instalado.

---

## Error Handling

- Erros de rede nos repositórios. ViewModels recebem `Result<T>` tipado.
- `IOException`: enfileirar bipagem localmente e retornar feedback otimista.
- `HttpException` 401: `AuthInterceptor` limpa o token; ViewModel navega para Login.
- `HttpException` 422: negócio rejeitou a bipagem — mapear para `FeedbackEvent.ERRO_SKU`
  ou `FeedbackEvent.SEM_SALDO` conforme o código de erro.

```kotlin
// data/repository/ColetaRepository.kt
suspend fun registrarBipagem(barcode: String, itemId: String): ResultadoBipagem {
    return try {
        val response = apiService.postBipagem(PostBipagemRequest(barcode, itemId))
        mapToResultado(response)
    } catch (e: IOException) {
        // Offline: enfileirar e retornar otimista
        bipagemDao.inserir(BipagemPendenteEntity(/* ... */))
        ResultadoBipagem.PecaValida(otimista = true)
    } catch (e: HttpException) {
        mapHttpError(e)
    }
}
```

---

## Navegação — Padrões

- `navController.navigate(AppDestination.X.withArgs(...))` — sem strings literais.
- Use `popUpTo + inclusive = true` para transições sem retorno:
  `navController.navigate(route) { popUpTo(AppDestination.Picking.route) { inclusive = true } }`
- Nunca passe objetos grandes como args de navegação — passe apenas IDs.
  O ViewModel da tela destino carrega os dados via repositório.

---

## Organização dos Arquivos

- `ui/[nome-tela]/[Nome]Screen.kt` — `@Composable` raiz da tela
- `ui/[nome-tela]/[Nome]ViewModel.kt` — `@HiltViewModel`, `StateFlow`, eventos
- `ui/components/` — componentes reutilizáveis sem referência a ViewModel ou nav
- `hardware/` — **apenas** `DatalogicManager`, `FeedbackManager`, `FeedbackEvent`
- `data/remote/dto/` — DTOs Moshi (`@JsonClass(generateAdapter = true)`)
- `data/repository/` — lógica de acesso a dados; retorna tipos de domínio, não DTOs
- `domain/model/` — modelos de domínio puros (sem anotações de framework)
