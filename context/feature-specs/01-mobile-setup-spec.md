# 05 — Mobile App: Setup Inicial (KylyPicking — Native Android / Jetpack Compose)

Leia o arquivo `CLAUDE.md` e as specs anteriores antes de iniciar.

## Por que Native Android e não React Native

O app é exclusivamente Android (Datalogic Memor 11, Android 11). O Datalogic SDK é uma
biblioteca nativa `.aar` que já exige código Kotlin. Com React Native, o único ganho seria
cross-platform — que não se aplica aqui — e o custo seria a bridge JavaScript adicionando
latência ao loop crítico de scanner → feedback (< 200 ms).

Com **Jetpack Compose + Kotlin**, o SDK é chamado diretamente do ViewModel sem
`NativeEventEmitter`, sem serialização de eventos e sem o overhead do React runtime.

---

## Stack escolhida

| Responsabilidade         | Biblioteca                                   |
| ------------------------ | -------------------------------------------- |
| UI declarativa           | Jetpack Compose (BOM mais recente)            |
| Design system            | Material 3 (`androidx.compose.material3`)    |
| Navegação                | Navigation Compose                           |
| Estado global            | ViewModel + StateFlow + Hilt                 |
| HTTP / API               | Retrofit 2 + OkHttp 4 + Moshi               |
| Banco local              | Room + SQLite                                |
| Armazenamento seguro     | EncryptedSharedPreferences (Jetpack Security) |
| Variáveis de ambiente    | `local.properties` → `BuildConfig`           |
| Background sync          | WorkManager                                  |
| Scanner / LED / Beep     | Datalogic SDK (`.aar` local)                 |
| Injeção de dependências  | Hilt (Dagger)                                |

---

## Pré-condições

- Android Studio Hedgehog ou superior instalado
- JDK 17 configurado (`JAVA_HOME`)
- Android SDK 30+ instalado (target API 30 — Android 11)
- `API_BASE_URL` definida no `local.properties` (o backend não precisa estar rodando nesta etapa)
- Arquivo `datalogic-sdk.aar` disponível (obtido com a Kyly / representante Datalogic)

---

## Checklist

### 1. Criar o Projeto

- [ ] Criar projeto no Android Studio: "Empty Activity" → **"Empty Compose Activity"**
  - Name: `KylyPicking`
  - Package: `com.kyly.picking`
  - Language: `Kotlin`
  - Min SDK: `API 30 (Android 11)`
- [ ] Verificar que `MainActivity.kt` usa `setContent { }` com Compose
- [ ] Adicionar ao `.gitignore`:
  ```
  local.properties
  *.aar
  ```

---

### 2. Estrutura de Pastas

```
app/
├── libs/
│   └── datalogic-sdk.aar          ← SDK nativo copiado aqui
├── src/
│   └── main/
│       ├── java/com/kyly/picking/
│       │   ├── MainActivity.kt
│       │   ├── KylyPickingApp.kt
│       │   ├── di/                ← módulos Hilt
│       │   │   ├── NetworkModule.kt
│       │   │   └── DatabaseModule.kt
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── BipagemPendenteDao.kt
│       │   │   │   ├── BipagemPendenteEntity.kt
│       │   │   │   └── SecureStorage.kt
│       │   │   ├── remote/
│       │   │   │   ├── ApiService.kt
│       │   │   │   ├── dto/       ← request / response DTOs
│       │   │   │   └── AuthInterceptor.kt
│       │   │   └── repository/
│       │   │       ├── AuthRepository.kt
│       │   │       └── ColetaRepository.kt
│       │   ├── domain/
│       │   │   └── model/         ← modelos de domínio (Caixa, Item, Coleta…)
│       │   ├── hardware/
│       │   │   ├── DatalogicManager.kt    ← wrapper do SDK .aar + SharedFlow de barcodes
│       │   │   ├── FeedbackManager.kt     ← LED + Beep por FeedbackEvent
│       │   │   └── FeedbackEvent.kt       ← enum dos eventos de hardware
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt
│       │   │   │   ├── Type.kt
│       │   │   │   └── Theme.kt
│       │   │   ├── navigation/
│       │   │   │   └── AppNavGraph.kt
│       │   │   ├── login/
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   └── LoginViewModel.kt
│       │   │   ├── menu/
│       │   │   │   ├── MenuScreen.kt
│       │   │   │   └── MenuViewModel.kt
│       │   │   ├── papeleta/
│       │   │   │   ├── PapeletaScreen.kt
│       │   │   │   └── PapeletaViewModel.kt
│       │   │   ├── picking/
│       │   │   │   ├── PickingScreen.kt
│       │   │   │   └── PickingViewModel.kt
│       │   │   ├── finalizacao/
│       │   │   │   └── FinalizacaoScreen.kt
│       │   │   ├── enderecos/
│       │   │   │   ├── EnderecosAlternativosScreen.kt
│       │   │   │   └── EnderecosViewModel.kt
│       │   │   └── components/
│       │   │       ├── AddressChip.kt
│       │   │       ├── ErrorBottomSheet.kt
│       │   │       └── StatusBadge.kt
│       │   └── worker/
│       │       └── SyncWorker.kt  ← WorkManager: envia bipagens pendentes
│       └── res/
│           └── font/
│               ├── publicsans_regular.ttf
│               ├── publicsans_medium.ttf
│               ├── publicsans_semibold.ttf
│               ├── publicsans_bold.ttf
│               ├── publicsans_extrabold.ttf
│               ├── publicsans_black.ttf
│               └── jetbrainsmono_bold.ttf
```

---

### 3. Gradle — Dependências

#### `gradle/libs.versions.toml` (Version Catalog)

```toml
[versions]
agp            = "8.7.3"
kotlin         = "2.1.0"
ksp            = "2.1.0-1.0.29"
hilt           = "2.56.1"
composeBom     = "2025.03.00"
coreKtx        = "1.15.0"
activityCompose= "1.10.1"
lifecycle      = "2.8.7"
navigation     = "2.8.9"
hiltNavCompose = "1.2.0"
retrofit       = "2.11.0"
okhttp         = "4.12.0"
moshi          = "1.15.2"
room           = "2.7.0"
securityCrypto = "1.1.0-alpha06"
workManager    = "2.10.0"
hiltWork       = "1.2.0"

[libraries]
# Compose
compose-bom              = { group = "androidx.compose", name = "compose-bom",               version.ref = "composeBom" }
compose-ui               = { group = "androidx.compose.ui",   name = "ui" }
compose-ui-tooling       = { group = "androidx.compose.ui",   name = "ui-tooling" }
compose-ui-tooling-prev  = { group = "androidx.compose.ui",   name = "ui-tooling-preview" }
compose-material3        = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons   = { group = "androidx.compose.material", name = "material-icons-extended" }
# Core
core-ktx                 = { group = "androidx.core",        name = "core-ktx",              version.ref = "coreKtx" }
activity-compose         = { group = "androidx.activity",    name = "activity-compose",       version.ref = "activityCompose" }
lifecycle-runtime-ktx    = { group = "androidx.lifecycle",   name = "lifecycle-runtime-ktx",  version.ref = "lifecycle" }
lifecycle-runtime-compose= { group = "androidx.lifecycle",   name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel      = { group = "androidx.lifecycle",   name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
# Navigation
navigation-compose       = { group = "androidx.navigation",  name = "navigation-compose",     version.ref = "navigation" }
# Hilt
hilt-android             = { group = "com.google.dagger",    name = "hilt-android",           version.ref = "hilt" }
hilt-compiler            = { group = "com.google.dagger",    name = "hilt-android-compiler",  version.ref = "hilt" }
hilt-nav-compose         = { group = "androidx.hilt",        name = "hilt-navigation-compose",version.ref = "hiltNavCompose" }
hilt-work                = { group = "androidx.hilt",        name = "hilt-work",              version.ref = "hiltWork" }
hilt-work-compiler       = { group = "androidx.hilt",        name = "hilt-compiler",          version.ref = "hiltWork" }
# Retrofit + Moshi
retrofit                 = { group = "com.squareup.retrofit2", name = "retrofit",             version.ref = "retrofit" }
retrofit-moshi           = { group = "com.squareup.retrofit2", name = "converter-moshi",      version.ref = "retrofit" }
okhttp                   = { group = "com.squareup.okhttp3",  name = "okhttp",                version.ref = "okhttp" }
okhttp-logging           = { group = "com.squareup.okhttp3",  name = "logging-interceptor",   version.ref = "okhttp" }
moshi-kotlin             = { group = "com.squareup.moshi",    name = "moshi-kotlin",          version.ref = "moshi" }
moshi-codegen            = { group = "com.squareup.moshi",    name = "moshi-kotlin-codegen",  version.ref = "moshi" }
# Room
room-runtime             = { group = "androidx.room",         name = "room-runtime",          version.ref = "room" }
room-ktx                 = { group = "androidx.room",         name = "room-ktx",              version.ref = "room" }
room-compiler            = { group = "androidx.room",         name = "room-compiler",         version.ref = "room" }
# Segurança
security-crypto          = { group = "androidx.security",     name = "security-crypto",       version.ref = "securityCrypto" }
# WorkManager
work-runtime             = { group = "androidx.work",         name = "work-runtime-ktx",      version.ref = "workManager" }

[plugins]
android-application  = { id = "com.android.application",          version.ref = "agp" }
kotlin-android       = { id = "org.jetbrains.kotlin.android",      version.ref = "kotlin" }
kotlin-compose       = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android         = { id = "com.google.dagger.hilt.android",    version.ref = "hilt" }
ksp                  = { id = "com.google.devtools.ksp",           version.ref = "ksp" }
```

---

#### `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

#### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kyly.picking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kyly.picking"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Lê local.properties e expõe como BuildConfig
        val localProps = java.util.Properties().apply {
            load(rootProject.file("local.properties").inputStream())
        }
        buildConfigField("String", "API_BASE_URL",
            "\"${localProps.getProperty("API_BASE_URL", "http://localhost:3000")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Inclui o .aar do SDK Datalogic
    repositories {
        flatDir { dirs("libs") }
    }
}

dependencies {
    // ── Compose BOM ───────────────────────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.prev)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // ── Core / Activity ───────────────────────────────────────────
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)   // collectAsStateWithLifecycle()
    implementation(libs.lifecycle.viewmodel)

    // ── Navigation ────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt ──────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // ── Retrofit + Moshi ──────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    // ── Room ──────────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── Segurança / Auth ──────────────────────────────────────────
    implementation(libs.security.crypto)

    // ── WorkManager ───────────────────────────────────────────────
    implementation(libs.work.runtime)

    // ── Datalogic SDK ─────────────────────────────────────────────
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
}
```

#### `local.properties` (não versionar)

```properties
sdk.dir=/Users/you/Library/Android/sdk
API_BASE_URL=http://192.168.1.100:3000
```

---

### 4. Datalogic SDK — Integração do .aar

- [ ] Copiar `datalogic-sdk.aar` para `app/libs/`
- [ ] Criar `hardware/DatalogicManager.kt`:

```kotlin
// hardware/DatalogicManager.kt
package com.kyly.picking.hardware

import com.datalogic.decode.BarcodeManager
import com.datalogic.decode.DecodeException
import com.datalogic.decode.ReadListener
import com.datalogic.device.notification.Led
import com.datalogic.device.notification.LedIntensity
import com.datalogic.device.notification.NotificationManager
import com.datalogic.device.notification.Tone
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatalogicManager @Inject constructor() {

    private val _barcodeEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val barcodeEvents: SharedFlow<String> = _barcodeEvents

    private var barcodeManager: BarcodeManager? = null
    private var notificationManager: NotificationManager? = null

    private val listener = ReadListener { decodeResult ->
        _barcodeEvents.tryEmit(decodeResult.getText())
    }

    fun enable() {
        try {
            barcodeManager = BarcodeManager()
            barcodeManager?.addReadListener(listener)
            notificationManager = NotificationManager()
        } catch (e: DecodeException) {
            e.printStackTrace()
        }
    }

    fun disable() {
        try {
            barcodeManager?.removeReadListener(listener)
            barcodeManager = null
        } catch (e: DecodeException) { /* ignorar */ }
    }

    // ── LED ──────────────────────────────────────────────────────

    fun setLedGreen()  = setLed(Led.GREEN, LedIntensity.HIGH)
    fun setLedRed()    = setLed(Led.RED, LedIntensity.HIGH)
    fun setLedYellow() = setLed(Led.ORANGE, LedIntensity.HIGH)
    fun clearLed()     { notificationManager?.setLed(Led.GREEN, LedIntensity.OFF, 0) }

    private fun setLed(color: Led, intensity: LedIntensity) {
        notificationManager?.setLed(color, intensity, 500)
    }

    // ── Beep ─────────────────────────────────────────────────────

    fun beepShort()       = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100)
    fun beepDoubleShort() {
        notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100)
        notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100)
    }
    fun beepContinuous(ms: Int) = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 80, ms)
    fun beepSuccess()     = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 80, 500)
}
```

- [ ] Criar `hardware/FeedbackEvent.kt`:

```kotlin
// hardware/FeedbackEvent.kt
package com.kyly.picking.hardware

enum class FeedbackEvent {
    PECA_VALIDA,
    SKU_COMPLETO,
    ERRO_SKU,
    SEM_SALDO,
    CAIXA_FINALIZADA,
    PICKING_PARCIAL,
}
```

- [ ] Criar `hardware/FeedbackManager.kt`:

```kotlin
// hardware/FeedbackManager.kt
package com.kyly.picking.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    private val datalogic: DatalogicManager,
) {
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

- [ ] Verificar: nenhum arquivo fora de `hardware/DatalogicManager.kt` importa classes
  do Datalogic SDK. O restante da app usa apenas `FeedbackManager.trigger()` e o
  `SharedFlow<String>` de barcodes.

---

### 5. Fontes — Public Sans + JetBrains Mono

- [ ] Baixar variantes e colocar em `app/src/main/res/font/`:
  - **Public Sans**: Regular (400), Medium (500), SemiBold (600), Bold (700), ExtraBold (800), Black (900)
    - Google Fonts: `https://fonts.google.com/specimen/Public+Sans`
  - **JetBrains Mono**: Bold (700)
    - `https://www.jetbrains.com/lp/mono/` → Download → `JetBrainsMono-Bold.ttf`
- [ ] Nomear os arquivos em `snake_case` conforme listado na estrutura de pastas
- [ ] Verificar: Android Studio valida os `.ttf` ao compilar — sem erros em "res/font"

---

### 6. Tema Material 3 — Cores, Tipografia e Theme

#### `ui/theme/Color.kt`

```kotlin
package com.kyly.picking.ui.theme

import androidx.compose.ui.graphics.Color

// ── Backgrounds / Surface ─────────────────────────────────────────────────
val Background                = Color(0xFFF7F9FC)
val Surface                   = Color(0xFFF7F9FC)
val SurfaceDim                = Color(0xFFD8DADD)
val SurfaceContainerLowest    = Color(0xFFFFFFFF)
val SurfaceContainerLow       = Color(0xFFF2F4F7)
val SurfaceContainer          = Color(0xFFECEEF1)
val SurfaceContainerHigh      = Color(0xFFE6E8EB)
val SurfaceContainerHighest   = Color(0xFFE0E3E6)

// ── Text ─────────────────────────────────────────────────────────────────
val OnBackground              = Color(0xFF191C1E)
val OnSurface                 = Color(0xFF191C1E)
val OnSurfaceVariant          = Color(0xFF43474E)
val Outline                   = Color(0xFF73777F)
val OutlineVariant            = Color(0xFFC3C6CF)

// ── Primary — Navy ────────────────────────────────────────────────────────
val Primary                   = Color(0xFF002444)
val OnPrimary                 = Color(0xFFFFFFFF)
val PrimaryContainer          = Color(0xFF1B3A5C)
val OnPrimaryContainer        = Color(0xFF87A4CC)

// ── Secondary — Azul ─────────────────────────────────────────────────────
val Secondary                 = Color(0xFF0B61A1)
val OnSecondary               = Color(0xFFFFFFFF)
val SecondaryContainer        = Color(0xFF7CBAFF)
val OnSecondaryContainer      = Color(0xFF004A7D)

// ── Tertiary — Verde ──────────────────────────────────────────────────────
val TertiaryContainer         = Color(0xFF00430D)
val OnTertiaryContainer       = Color(0xFF64B462)

// ── Semânticas Industriais ────────────────────────────────────────────────
val SuccessIndustrial         = Color(0xFF2E7D32)
val WarningIndustrial         = Color(0xFFF59E0B)
val WarehouseOrange           = Color(0xFFEA580C)
val KylyError                 = Color(0xFFBA1A1A)
val OnKylyError               = Color(0xFFFFFFFF)
val ErrorContainer            = Color(0xFFFFDAD6)
val OnErrorContainer          = Color(0xFF93000A)
```

#### `ui/theme/Type.kt`

```kotlin
package com.kyly.picking.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kyly.picking.R

val PublicSans = FontFamily(
    Font(R.font.publicsans_regular,   FontWeight.Normal),
    Font(R.font.publicsans_medium,    FontWeight.Medium),
    Font(R.font.publicsans_semibold,  FontWeight.SemiBold),
    Font(R.font.publicsans_bold,      FontWeight.Bold),
    Font(R.font.publicsans_extrabold, FontWeight.ExtraBold),
    Font(R.font.publicsans_black,     FontWeight.Black),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

// Estilo especial para endereços de picking — usado diretamente nas telas
val NumeralXl = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Bold,
    fontSize = 72.sp,
    lineHeight = 80.sp,
)

// Estilo para quantidades em destaque (ex.: "1 / 2")
val NumeralLarge = TextStyle(
    fontFamily = PublicSans,
    fontWeight = FontWeight.Black,
    fontSize = 48.sp,
    lineHeight = 56.sp,
)

val KylyTypography = Typography(
    // displayLarge → mensagens de estado ("CAIXA FINALIZADA")
    displayLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    // headlineMedium → títulos de tela, IDs de caixa/pedido
    headlineMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    // titleLarge → subtítulos, nome do produto
    titleLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    // bodyLarge → texto de instrução, botões primários
    bodyLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    // bodyMedium → corpo geral, labels de input
    bodyMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // labelLarge → labels em caps, chips de status, nav tabs
    labelLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)
```

#### `ui/theme/Theme.kt`

```kotlin
package com.kyly.picking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KylyColorScheme = lightColorScheme(
    primary                 = Primary,
    onPrimary               = OnPrimary,
    primaryContainer        = PrimaryContainer,
    onPrimaryContainer      = OnPrimaryContainer,
    secondary               = Secondary,
    onSecondary             = OnSecondary,
    secondaryContainer      = SecondaryContainer,
    onSecondaryContainer    = OnSecondaryContainer,
    tertiaryContainer       = TertiaryContainer,
    onTertiaryContainer     = OnTertiaryContainer,
    background              = Background,
    onBackground            = OnBackground,
    surface                 = Surface,
    onSurface               = OnSurface,
    surfaceVariant          = SurfaceContainerHigh,
    onSurfaceVariant        = OnSurfaceVariant,
    outline                 = Outline,
    outlineVariant          = OutlineVariant,
    error                   = KylyError,
    onError                 = OnKylyError,
    errorContainer          = ErrorContainer,
    onErrorContainer        = OnErrorContainer,
    surfaceContainerLowest  = SurfaceContainerLowest,
    surfaceContainerLow     = SurfaceContainerLow,
    surfaceContainer        = SurfaceContainer,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun KylyPickingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KylyColorScheme,
        typography  = KylyTypography,
        content     = content,
    )
}
```

- [ ] Verificar: nenhuma cor hardcoded nas telas — usar `MaterialTheme.colorScheme.*`
  e as constantes semânticas de `Color.kt` apenas no `lightColorScheme`

---

### 7. Hilt — Módulos de Injeção

> `DatalogicManager` e `FeedbackManager` são `@Singleton` com `@Inject constructor` —
> o Hilt os resolve automaticamente sem módulo manual. Apenas `NetworkModule` e
> `DatabaseModule` são necessários.

#### `di/NetworkModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

#### `di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kyly_picking.db")
            .build()

    @Provides
    fun provideBipagemDao(db: AppDatabase): BipagemPendenteDao = db.bipagemPendenteDao()
}
```

---

### 8. Armazenamento Seguro — EncryptedSharedPreferences

```kotlin
// data/local/SecureStorage.kt
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "kyly_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveToken(token: String) = prefs.edit().putString("jwt_token", token).apply()
    fun getToken(): String?      = prefs.getString("jwt_token", null)
    fun clearToken()             = prefs.edit().remove("jwt_token").apply()
}
```

---

### 9. Banco Local — Room (fila offline)

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

// data/local/AppDatabase.kt
@Database(entities = [BipagemPendenteEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bipagemPendenteDao(): BipagemPendenteDao
}
```

---

### 10. Background Sync — WorkManager

```kotlin
// worker/SyncWorker.kt
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bipagemDao: BipagemPendenteDao,
    private val apiService: ApiService,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendentes = bipagemDao.listarTodas()
        if (pendentes.isEmpty()) return Result.success()

        return try {
            for (bipagem in pendentes) {
                apiService.postBipagem(bipagem.toRequest())
                bipagemDao.deletar(bipagem)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

- [ ] Registrar o worker periódico no `KylyPickingApp.onCreate()`:

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(this)
    .enqueueUniquePeriodicWork("sync_bipagens", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
```

---

### 11. Navegação — NavGraph

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

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppDestination.Login.route) {
        composable(AppDestination.Login.route)    { LoginScreen(navController) }
        composable(AppDestination.Menu.route)     { MenuScreen(navController) }
        composable(AppDestination.Papeleta.route) { PapeletaScreen(navController) }
        composable(
            AppDestination.Picking.route,
            arguments = listOf(navArgument("caixaCodigo") { type = NavType.StringType })
        ) { backStackEntry ->
            PickingScreen(
                navController = navController,
                caixaCodigo   = backStackEntry.arguments?.getString("caixaCodigo") ?: "",
            )
        }
        composable(
            AppDestination.Finalizacao.route,
            arguments = listOf(navArgument("tipo") { type = NavType.StringType })
        ) { backStackEntry ->
            FinalizacaoScreen(
                navController = navController,
                tipo          = backStackEntry.arguments?.getString("tipo") ?: "finalizada",
            )
        }
        composable(
            AppDestination.Enderecos.route,
            arguments = listOf(navArgument("skuId") { type = NavType.StringType })
        ) { backStackEntry ->
            EnderecosAlternativosScreen(
                navController = navController,
                skuId         = backStackEntry.arguments?.getString("skuId") ?: "",
            )
        }
    }
}
```

---

### 12. Ciclo de Vida do Scanner — Padrão `DisposableEffect`

O scanner deve estar ativo **apenas** enquanto a tela de bipagem está em foreground.
Este padrão é obrigatório em `LoginScreen`, `PapeletaScreen` e `PickingScreen`.

```kotlin
// Exemplo em PickingScreen.kt
@Composable
fun PickingScreen(
    navController: NavController,
    caixaCodigo: String,
    viewModel: PickingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Habilita/desabilita scanner conforme ciclo de vida da tela
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

    // Consome eventos únicos (navegação, bottom sheet de erro)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PickingEvent.CaixaFinalizada ->
                    navController.navigate(AppDestination.Finalizacao.withArgs("finalizada")) {
                        popUpTo(AppDestination.Picking.route) { inclusive = true }
                    }
                is PickingEvent.PickingParcial ->
                    navController.navigate(AppDestination.Finalizacao.withArgs("parcial")) {
                        popUpTo(AppDestination.Picking.route) { inclusive = true }
                    }
            }
        }
    }

    // ... conteúdo da tela
}
```

No ViewModel:

```kotlin
@HiltViewModel
class PickingViewModel @Inject constructor(
    private val datalogic: DatalogicManager,
    private val feedback: FeedbackManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickingUiState())
    val uiState: StateFlow<PickingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PickingEvent>()
    val events: SharedFlow<PickingEvent> = _events

    init {
        viewModelScope.launch {
            datalogic.barcodeEvents.collect { barcode -> processBipagem(barcode) }
        }
    }

    fun onResume() = datalogic.enable()
    fun onPause()  = datalogic.disable()
}
```

- [ ] Verificar: ao navegar para outra tela, o scanner desabilita automaticamente
  (`ON_PAUSE` → `datalogic.disable()`)
- [ ] Verificar: ao voltar para a tela, o scanner reabilita (`ON_RESUME` →
  `datalogic.enable()`)

---

### 13. Application + MainActivity

```kotlin
// KylyPickingApp.kt
@HiltAndroidApp
class KylyPickingApp : Application()

// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KylyPickingTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
```

- [ ] Registrar em `AndroidManifest.xml`:

```xml
<application
    android:name=".KylyPickingApp"
    android:theme="@style/Theme.KylyPicking"
    ... >
    <activity android:name=".MainActivity" ... />
</application>
```

---

### 14. Verificação Final

- [ ] `./gradlew assembleDebug` compila sem erros
- [ ] App instala no Datalogic Memor 11 via `adb install`
- [ ] `MainActivity` abre `LoginScreen` com tema correto (fundo `#F7F9FC`, header navy)
- [ ] `DatalogicManager.enable()` inicializa sem crash no dispositivo real
- [ ] Fontes renderizam: Public Sans no corpo, JetBrains Mono no campo de endereço
- [ ] WorkManager agendado: confirmar via Android Studio App Inspection → WorkManager
- [ ] EncryptedSharedPreferences: salvar e recuperar token sem erro
- [ ] Room: inserir e deletar `BipagemPendenteEntity` sem erro
- [ ] `./gradlew lint` sem erros críticos
- [ ] `progress-tracker.md` atualizado

---

## Definition of Done

- [ ] Projeto compila e instala no Datalogic Memor 11 (Android 11)
- [ ] Tema MD3 aplicado: `KylyPickingTheme` envolve toda a app
- [ ] Nenhuma cor hardcoded fora de `Color.kt`
- [ ] Fontes Public Sans e JetBrains Mono renderizando corretamente
- [ ] `DatalogicManager` inicializa, recebe eventos do scanner via `SharedFlow` e aciona LED/beep
- [ ] `FeedbackManager.trigger()` é a **única** forma de acionar feedback de hardware nas telas
- [ ] `SecureStorage` persiste JWT entre sessões com EncryptedSharedPreferences
- [ ] `AppDatabase` (Room) armazena bipagens pendentes corretamente
- [ ] `SyncWorker` registrado e executando sincronização periódica em background
- [ ] `AppNavGraph` com todas as 6 rotas definidas e tipadas
- [ ] `EnderecosAlternativosScreen` registrada e navegável a partir da `PickingScreen`
- [ ] `BuildConfig.API_BASE_URL` lendo de `local.properties` (nenhum URL hardcoded)
- [ ] Hilt injetando todas as dependências sem erro de compilação
- [ ] `progress-tracker.md` atualizado
