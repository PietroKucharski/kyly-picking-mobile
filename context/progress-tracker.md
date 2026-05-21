# Mobile — Progress Tracker

## Current Phase

**Setup Inicial concluído** — estrutura do projeto Android criada, pronta para
abertura no Android Studio e compilação após adicionar arquivos binários externos.

## Current Goal

Implementar spec `02` — tela de Login.

## Completed

- **Setup Inicial (spec 01)**
  - Estrutura completa de pastas conforme `architecture.md`
  - `gradle/libs.versions.toml` — Version Catalog com todas as dependências
  - `settings.gradle.kts` e `build.gradle.kts` raiz configurados
  - `app/build.gradle.kts` — AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2025.03.00,
    Hilt 2.56.1, KSP, Room, Retrofit/Moshi, WorkManager, EncryptedSharedPreferences
  - `BuildConfig.API_BASE_URL` lendo de `local.properties`
  - `AndroidManifest.xml` com `KylyPickingApp` e `MainActivity`
  - `ui/theme/Color.kt` — paleta completa (navy, azul, verde, semânticas industriais)
  - `ui/theme/Type.kt` — PublicSans + JetBrainsMono, NumeralXl/NumeralLarge
  - `ui/theme/Theme.kt` — `KylyPickingTheme` com `lightColorScheme`
  - `hardware/DatalogicManager.kt` — singleton Hilt, SharedFlow de barcodes, LED, beep
  - `hardware/FeedbackEvent.kt` — enum com 6 eventos
  - `hardware/FeedbackManager.kt` — única porta de acesso ao hardware nas telas
  - `di/NetworkModule.kt` — OkHttp + Retrofit + Moshi + AuthInterceptor
  - `di/DatabaseModule.kt` — Room + BipagemPendenteDao
  - `data/local/` — AppDatabase, BipagemPendenteEntity, BipagemPendenteDao, SecureStorage
  - `data/remote/` — ApiService, AuthInterceptor, dto/BipagemRequestDto
  - `data/repository/` — AuthRepository, ColetaRepository (stub)
  - `worker/SyncWorker.kt` — @HiltWorker, processa fila offline a cada 15 min
  - `KylyPickingApp.kt` — @HiltAndroidApp, registra SyncWorker periódico
  - `MainActivity.kt` — @AndroidEntryPoint, KylyPickingTheme + AppNavGraph
  - `ui/navigation/AppNavGraph.kt` — 6 rotas tipadas (Login, Menu, Papeleta,
    Picking, Finalização, Endereços)
  - Telas stub: Login, Menu, Papeleta, Picking (com DisposableEffect de scanner),
    Finalização, EnderecosAlternativos
  - Componentes stub: AddressChip, ErrorBottomSheet, StatusBadge
  - ViewModels: LoginViewModel, MenuViewModel, PapeletaViewModel,
    PickingViewModel (com onResume/onPause), EnderecosViewModel

## In Progress

- Nenhum.

## Next Up

- Implementar spec `02` — tela de Login (autenticação JWT)
- Adicionar arquivos binários externos (ver Open Questions)
- Abrir projeto no Android Studio e executar `./gradlew assembleDebug`

## Open Questions

- **Fontes (ação manual necessária):** baixar e copiar para `app/src/main/res/font/`:
  - Public Sans (Regular, Medium, SemiBold, Bold, ExtraBold, Black) — Google Fonts
  - JetBrains Mono Bold — jetbrains.com/lp/mono
  - Nomear em snake_case: `publicsans_regular.ttf`, `publicsans_medium.ttf`,
    `publicsans_semibold.ttf`, `publicsans_bold.ttf`, `publicsans_extrabold.ttf`,
    `publicsans_black.ttf`, `jetbrainsmono_bold.ttf`
- **Datalogic SDK (ação manual necessária):** copiar `datalogic-sdk.aar` para
  `app/libs/` (obtido com representante Datalogic / Kyly)
- **Gradle wrapper JAR:** `gradle-wrapper.jar` (binário) não foi gerado — ao abrir
  no Android Studio ele é baixado automaticamente; ou rodar `gradle wrapper` no terminal
- **`local.properties`:** ajustar `sdk.dir` para o caminho do Android SDK local
  (atualmente aponta para `C:\Users\pietr\AppData\Local\Android\Sdk`)

## Architecture Decisions

- `DatalogicManager` usa `@Singleton` + `@Inject constructor` — resolvido pelo Hilt
  sem módulo manual.
- `FeedbackManager.trigger()` é a **única** forma de acionar LED/beep nas telas.
- Nenhuma tela importa classes do Datalogic SDK diretamente.
- `BuildConfig.API_BASE_URL` é a única fonte de URL da API — sem hardcode.
- Offline: bipagens falhas vão para Room; `SyncWorker` re-envia quando há rede.

## Session Notes

- 2026-05-21: Setup inicial implementado a partir da spec `01-mobile-setup-spec.md`.
  Projeto criado manualmente (sem Android Studio) — todos os arquivos Kotlin e Gradle
  gerados diretamente. Arquivos binários (fontes .ttf, .aar, gradle-wrapper.jar)
  precisam ser adicionados manualmente antes de compilar.
