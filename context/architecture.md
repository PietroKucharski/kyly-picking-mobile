# Mobile — Architecture Context

## Por que Native Android

O app roda exclusivamente no **Datalogic Memor 11 (Android 11)**. O Datalogic SDK é
uma biblioteca `.aar` nativa (Java/Kotlin) que se integra diretamente ao build Gradle
sem qualquer camada intermediária. Com Native Android + Jetpack Compose:

- O `BarcodeReadListener` entrega o barcode diretamente ao ViewModel via `SharedFlow`
  — sem serialização, sem bridge JavaScript, sem latência adicional.
- O requisito crítico de **< 200ms** (scanner → feedback LED + tela) é atendido com folga.
- A equipe já escreve Kotlin — sem divisão de contexto entre JS e nativo.

---

## Stack

| Camada                | Tecnologia                                   | Papel                                              |
| --------------------- | -------------------------------------------- | -------------------------------------------------- |
| UI declarativa        | Jetpack Compose (BOM)                        | Telas e componentes                                |
| Design system         | Material 3 (`androidx.compose.material3`)   | Tema, cores, tipografia                            |
| Linguagem             | Kotlin (strict, sem `!!` fora de testes)    | Toda a camada do app                               |
| Navegação             | Navigation Compose                           | Fluxo entre telas, args tipados                    |
| Estado / ViewModel    | ViewModel + `StateFlow` + Hilt              | Estado global e local por tela                     |
| HTTP / API            | Retrofit 2 + OkHttp 4 + Moshi               | Requisições REST ao backend                        |
| Auth storage          | `EncryptedSharedPreferences`                | JWT seguro em disco (AES256)                       |
| SQLite offline        | Room                                         | Fila de bipagens pendentes                         |
| Background sync       | WorkManager                                  | Processa fila offline ao reconectar                |
| Hardware SDK          | Datalogic SDK (`.aar` local)                | Scanner, LEDs, buzzer do Memor 11                  |
| DI                    | Hilt (Dagger)                               | Injeção de dependências em todo o app              |
| Env                   | `local.properties` → `BuildConfig`          | Variáveis de build tipadas, sem `process.env`      |

---

## Estrutura de Pastas

```
app/
├── libs/
│   └── datalogic-sdk.aar              ← SDK nativo, não versionar
├── src/main/
│   ├── java/com/kyly/picking/
│   │   ├── KylyPickingApp.kt          ← @HiltAndroidApp
│   │   ├── MainActivity.kt            ← @AndroidEntryPoint, setContent { }
│   │   ├── di/
│   │   │   ├── NetworkModule.kt       ← Retrofit, OkHttp, ApiService
│   │   │   └── DatabaseModule.kt      ← Room, DAOs
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── BipagemPendenteDao.kt
│   │   │   │   ├── BipagemPendenteEntity.kt
│   │   │   │   └── SecureStorage.kt   ← EncryptedSharedPreferences
│   │   │   ├── remote/
│   │   │   │   ├── ApiService.kt      ← interface Retrofit
│   │   │   │   ├── dto/               ← @JsonClass Moshi DTOs
│   │   │   │   └── AuthInterceptor.kt ← injeta JWT no header
│   │   │   └── repository/
│   │   │       ├── AuthRepository.kt
│   │   │       └── ColetaRepository.kt
│   │   ├── domain/
│   │   │   └── model/                 ← Caixa, ItemCaixa, Sku, Coleta…
│   │   ├── hardware/
│   │   │   ├── DatalogicManager.kt    ← wrapper do SDK .aar (@Singleton Hilt)
│   │   │   ├── FeedbackManager.kt     ← LED + Beep por FeedbackEvent
│   │   │   └── FeedbackEvent.kt       ← enum dos eventos de hardware
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt           ← constantes MD3
│   │   │   │   ├── Type.kt            ← Typography + NumeralXl/NumeralLarge
│   │   │   │   └── Theme.kt           ← KylyPickingTheme
│   │   │   ├── navigation/
│   │   │   │   └── AppNavGraph.kt     ← NavHost + AppDestination sealed class
│   │   │   ├── login/
│   │   │   │   ├── LoginScreen.kt
│   │   │   │   └── LoginViewModel.kt
│   │   │   ├── menu/
│   │   │   │   ├── MenuScreen.kt
│   │   │   │   └── MenuViewModel.kt
│   │   │   ├── papeleta/
│   │   │   │   ├── PapeletaScreen.kt
│   │   │   │   └── PapeletaViewModel.kt
│   │   │   ├── picking/
│   │   │   │   ├── PickingScreen.kt
│   │   │   │   └── PickingViewModel.kt
│   │   │   ├── finalizacao/
│   │   │   │   └── FinalizacaoScreen.kt
│   │   │   ├── enderecos/
│   │   │   │   ├── EnderecosAlternativosScreen.kt
│   │   │   │   └── EnderecosViewModel.kt
│   │   │   └── components/
│   │   │       ├── AddressChip.kt
│   │   │       ├── ErrorBottomSheet.kt
│   │   │       └── StatusBadge.kt
│   │   └── worker/
│   │       └── SyncWorker.kt          ← @HiltWorker, WorkManager
│   └── res/
│       └── font/
│           ├── publicsans_regular.ttf
│           ├── publicsans_medium.ttf
│           ├── publicsans_semibold.ttf
│           ├── publicsans_bold.ttf
│           ├── publicsans_extrabold.ttf
│           ├── publicsans_black.ttf
│           └── jetbrainsmono_bold.ttf
```

---

## Datalogic SDK — Integração Direta

O SDK é linked via `flatDir` no Gradle:

```kotlin
// app/build.gradle.kts
android {
    repositories { flatDir { dirs("libs") } }
}
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
}
```

### `hardware/DatalogicManager.kt` — Singleton Hilt

```kotlin
@Singleton
class DatalogicManager @Inject constructor() {

    // SharedFlow consumido pelos ViewModels de bipagem
    private val _barcodeEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val barcodeEvents: SharedFlow<String> = _barcodeEvents

    private var barcodeManager: BarcodeManager? = null
    private var notificationManager: NotificationManager? = null

    private val listener = ReadListener { result ->
        _barcodeEvents.tryEmit(result.getText())
    }

    fun enable() {
        barcodeManager = BarcodeManager().also { it.addReadListener(listener) }
        notificationManager = NotificationManager()
    }

    fun disable() {
        barcodeManager?.removeReadListener(listener)
        barcodeManager = null
    }

    // LED
    fun setLedGreen()  = notificationManager?.setLed(Led.GREEN,  LedIntensity.HIGH, 500)
    fun setLedRed()    = notificationManager?.setLed(Led.RED,    LedIntensity.HIGH, 500)
    fun setLedYellow() = notificationManager?.setLed(Led.ORANGE, LedIntensity.HIGH, 500)
    fun clearLed()     = notificationManager?.setLed(Led.GREEN,  LedIntensity.OFF,  0)

    // Beep
    fun beepShort()            = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100)
    fun beepDoubleShort()      { repeat(2) { notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100) } }
    fun beepContinuous(ms: Int)= notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 80, ms)
    fun beepSuccess()          = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 80, 500)
}
```

### `hardware/FeedbackManager.kt`

```kotlin
enum class FeedbackEvent {
    PECA_VALIDA, SKU_COMPLETO, ERRO_SKU, SEM_SALDO, CAIXA_FINALIZADA, PICKING_PARCIAL
}

@Singleton
class FeedbackManager @Inject constructor(private val datalogic: DatalogicManager) {
    fun trigger(event: FeedbackEvent) {
        datalogic.clearLed()
        when (event) {
            FeedbackEvent.PECA_VALIDA      -> { datalogic.setLedGreen();  datalogic.beepShort() }
            FeedbackEvent.SKU_COMPLETO     -> { datalogic.setLedGreen();  datalogic.beepDoubleShort() }
            FeedbackEvent.ERRO_SKU,
            FeedbackEvent.SEM_SALDO        -> { datalogic.setLedRed();    datalogic.beepContinuous(2000) }
            FeedbackEvent.CAIXA_FINALIZADA -> { datalogic.setLedGreen();  datalogic.beepSuccess() }
            FeedbackEvent.PICKING_PARCIAL  -> { datalogic.setLedYellow(); datalogic.beepDoubleShort() }
        }
    }
}
```

---

## Ciclo de Vida do Scanner

O scanner é habilitado **apenas** enquanto a tela de bipagem está na foreground.
O `PickingViewModel` (e `PapeletaViewModel`) gerenciam o ciclo via `Lifecycle`:

```kotlin
// PickingViewModel.kt
class PickingViewModel @Inject constructor(
    private val datalogic: DatalogicManager,
    private val feedback: FeedbackManager,
    private val coletaRepo: ColetaRepository,
) : ViewModel() {

    val uiState: StateFlow<PickingUiState> = /* ... */

    init {
        // Coleta barcodes enquanto o ViewModel estiver ativo
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode ->
                processBipagem(barcode)
            }
        }
    }

    fun onResume()  = datalogic.enable()
    fun onPause()   = datalogic.disable()

    private suspend fun processBipagem(barcode: String) {
        val resultado = coletaRepo.registrarBipagem(barcode, uiState.value.itemAtual.id)
        feedback.trigger(resultado.feedbackEvent)
        // Atualiza uiState conforme o resultado
    }
}
```

Na tela, conectado via `LifecycleEventObserver`:

```kotlin
// PickingScreen.kt
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
```

---

## Navegação — AppDestination

```kotlin
// ui/navigation/AppNavGraph.kt
sealed class AppDestination(val route: String) {
    object Login       : AppDestination("login")
    object Menu        : AppDestination("menu")
    object Papeleta    : AppDestination("papeleta")
    object Picking     : AppDestination("picking/{caixaCodigo}") {
        fun withArgs(codigo: String) = "picking/$codigo"
    }
    object Finalizacao : AppDestination("finalizacao/{tipo}") {
        fun withArgs(tipo: String) = "finalizacao/$tipo"
    }
    object Enderecos   : AppDestination("enderecos/{skuId}") {
        fun withArgs(skuId: String) = "enderecos/$skuId"
    }
}
```

---

## Estado — ViewModel + StateFlow

Cada tela tem seu próprio ViewModel injetado via Hilt. Não há store global —
o estado compartilhado (sessão autenticada, caixa ativa) é passado via repositórios
ou argumentos de navegação.

```kotlin
// AuthRepository guarda a sessão (lida de SecureStorage)
// ColetaRepository gerencia a caixa ativa em memória (StateFlow interno)
```

---

## Estratégia Offline / Sync

1. **Modo conectado**: `ColetaRepository` chama `ApiService` via Retrofit diretamente.
2. **Modo offline** (`IOException` / `HttpException` de rede): a bipagem é inserida em
   `BipagemPendenteEntity` (Room) e o operador recebe feedback otimista na tela.
3. **Sincronização**: `SyncWorker` (`@HiltWorker`) é agendado como `PeriodicWorkRequest`
   a cada 15 minutos, com constraint `NetworkType.CONNECTED`. Processa a fila em ordem
   de `criadoEm` e deleta cada registro após confirmação da API.
4. **Conflito**: API rejeita → erro logado localmente, supervisor notificado no painel web.

---

## Auth Storage

JWT armazenado via `EncryptedSharedPreferences` em `SecureStorage`:

```kotlin
@Singleton
class SecureStorage @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "kyly_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    fun saveToken(token: String) = prefs.edit().putString("jwt", token).apply()
    fun getToken(): String?      = prefs.getString("jwt", null)
    fun clearToken()             = prefs.edit().remove("jwt").apply()
}
```

---

## Variáveis de Ambiente — BuildConfig

```kotlin
// app/build.gradle.kts
val localProps = java.util.Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}
buildConfigField("String", "API_BASE_URL",
    "\"${localProps.getProperty("API_BASE_URL", "http://localhost:3000")}\"")
```

```properties
# local.properties (não versionar)
API_BASE_URL=http://192.168.1.100:3000
```

Uso: `BuildConfig.API_BASE_URL` — tipado, validado em compile time, sem acesso direto
a `local.properties` fora do `build.gradle.kts`.

---

## Invariants

1. **Scanner apenas em telas de bipagem**: `DatalogicManager.enable()` é chamado no
   `ON_RESUME` da tela, `disable()` no `ON_PAUSE`. Nunca habilitado globalmente.
2. **Validação pela API**: o app nunca aprova uma bipagem localmente. No máximo enfileira
   para sync offline com feedback otimista.
3. **SDK via `DatalogicManager`**: nenhuma tela ou repositório importa classes do
   Datalogic SDK diretamente. Sempre via `DatalogicManager` injetado pelo Hilt.
4. **Feedback via `FeedbackManager.trigger()`**: nenhuma tela chama métodos de LED
   ou beep diretamente. Sempre via `feedback.trigger(FeedbackEvent.*)`.
5. **URL via `BuildConfig`**: nenhum URL hardcoded no código. Sempre via
   `BuildConfig.API_BASE_URL`.
