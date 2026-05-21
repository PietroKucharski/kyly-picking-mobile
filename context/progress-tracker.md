# Mobile — Progress Tracker

## Current Phase

**Spec 05 concluída** — tela de Picking implementada com loop de bipagem, modal de erro, feedback de hardware e navegação para Finalização/Endereços Alternativos.

## Current Goal

Compilar e testar no dispositivo físico Datalogic Memor 11.

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

- **Tela de Login (spec 02)**
  - `domain/model/Result.kt` — sealed class `Result<T>` (Success/Error) compartilhada
  - `data/remote/dto/AuthDtos.kt` — `MobileLoginRequest` e `MobileLoginResponse`
  - `data/remote/ApiService.kt` — endpoint `POST api/auth/mobile-login`
  - `data/repository/AuthRepository.kt` — método `login()` com chamada à API e salvamento de JWT;
    tratamento de `IOException` (sem rede) e `HttpException` (401/422/etc)
  - `ui/login/LoginViewModel.kt` — `LoginUiState`, `LoginEvent.Sucesso`, `LoginViewModel`
    com scanner lifecycle (`onResume`/`onPause`) e fluxo de bipagem sequencial
    (1º scan → supervisorCodigo, 2º scan → operadorCracha)
  - `ui/components/ScanDisplayField.kt` — componente reutilizável de exibição de barcode;
    borda azul quando ativo, borda navy quando preenchido, placeholder quando vazio
  - `ui/login/LoginScreen.kt` — tela completa: header navy, 2 campos scan com estado visual,
    botão "Entrar" habilitado apenas quando ambos campos preenchidos, loading spinner,
    mensagem de erro inline; extrai `LoginContent` para previews
  - 3 `@Preview` (vazio, preenchido, erro)

- **Tela de Menu (spec 03)**
  - `data/remote/dto/CaixaDtos.kt` — DTOs `GetCaixaResponse`, `CaixaDto`, `PedidoDto`,
    `ItemCaixaDto`, `SkuDto`, `EnderecoDto` com `@JsonClass(generateAdapter = true)`
  - `data/remote/ApiService.kt` — endpoint `GET api/mobile/caixas/{codigo}`
  - `data/remote/AuthInterceptor.kt` — anotação `@Singleton` adicionada
  - `data/repository/ColetaRepository.kt` — implementação completa com `CaixaResult`
    sealed class (Success, NotFound, AlreadyFinalized, HttpError, NetworkError)
    e método `buscarCaixa(codigo)`
  - `ui/navigation/AppNavGraph.kt` — `AppDestination.Papeleta` atualizado para
    `"papeleta/{caixaCodigo}"` com `withArgs()`; composable registrado com argumento
  - `ui/menu/MenuViewModel.kt` — `MenuUiState`, `MenuEvent`, `MenuViewModel` com
    lifecycle do scanner, `onBarcodeScan`, `buscarCaixa`, `onRetry`, `onLogout`
  - `ui/menu/MenuScreen.kt` — tela completa: header navy, ícone scanner, instrução,
    `ScanDisplayField`, loading spinner, mensagem de erro + "Tentar novamente",
    botão SAIR outlined; `MenuScreenContent` separado para previews
  - `ui/papeleta/PapeletaScreen.kt` — placeholder atualizado para aceitar `caixaCodigo: String`
  - 3 `@Preview` (idle, loading, erro)

- **Tela de Papeleta (spec 04)**
  - `data/repository/ColetaStateHolder.kt` — singleton Hilt com `MutableStateFlow<CaixaDto?>`,
    métodos `set()`, `get()`, `clear()`; evita re-fetch na transição Menu → Papeleta
  - `ui/menu/MenuViewModel.kt` — injeção de `ColetaStateHolder`; `set(result.caixa)` chamado
    antes de emitir `NavigateToPapeleta`
  - `ui/papeleta/PapeletaViewModel.kt` — `PapeletaContent` (Loading/Loaded/Error),
    `PapeletaUiState` com `canStartPicking`, `PapeletaEvent` (NavigateToPicking/NavigateBack);
    `carregarCaixa()` usa cache do holder ou faz fallback via `ColetaRepository`;
    `onIniciarPicking()` e `onVoltar()` emitem eventos únicos via `SharedFlow`
  - `ui/components/StatusChip.kt` — chip de status com 4 variantes (pendente/parcial/completo/falta);
    usa `WarningIndustrial` e `SuccessIndustrial` de `Color.kt` com alpha 0.15
  - `ui/components/ItemPapeletaCard.kt` — card com `StatusChip`, código SKU, descrição,
    endereço em fonte `JetBrainsMono`, quantidade coletada/esperada
  - `ui/papeleta/PapeletaScreen.kt` — implementação completa: header com número do pedido,
    sub-header com código da caixa e progresso, `LazyColumn` com itens ordenados
    (pendente → parcial → falta → completo), botão "INICIAR PICKING" habilitado/desabilitado
    conforme `canStartPicking`, estados Loading/Error/Loaded; `PapeletaScreenContent`
    separado para previews
  - 4 `@Preview` (Loading, Loaded, Error, AllDone)

- **Tela de Picking (spec 05)**
  - `data/remote/dto/BipagemDtos.kt` — `PostBipagemRequest`, `PostBipagemResponse`,
    `ColetaDto`, `ItemAtualizadoDto`, `CaixaAtualizadaDto`
  - `data/remote/ApiService.kt` — endpoint `POST api/mobile/bipagens` atualizado
    para usar `PostBipagemRequest`/`PostBipagemResponse`; rota corrigida de `"bipagens"`
    para `"api/mobile/bipagens"`
  - `data/local/BipagemPendenteEntity.kt` — `toRequest()` atualizado para retornar
    `PostBipagemRequest` (substituindo `BipagemRequestDto`)
  - `worker/SyncWorker.kt` — removida verificação `isSuccessful` (agora exceções
    propagam para `try/catch`); `BipagemRequestDto` substituído por `PostBipagemRequest`
  - `data/repository/BipagemRepository.kt` — `BipagemResult` sealed class
    (Success/ItemJaCompleto/HttpError/NetworkError) e `registrar()` com tratamento
    de `HttpException` 422 → `ItemJaCompleto`, outros códigos → `HttpError`,
    `IOException` → `NetworkError`
  - `ui/picking/PickingViewModel.kt` — `PickingErrorTipo`, `PickingError`,
    `PickingUiState` (com `pecasRestantes`, `progressoHeader`, `scannerBloqueado`),
    `PickingEvent` (CaixaFinalizada/PickingParcial/NavigateToEnderecos),
    `PickingViewModel` completo: `inicializar()`, `onBarcodeScan()`, `onConfirmarErroModal()`,
    `onSemSaldo()`, `onEnderecosAlternativos()`, `registrarBipagem()`, `tratarSucesso()`,
    `proximoItemPendente()`; `ColetaStateHolder` atualizado após cada bipagem bem-sucedida
  - `ui/components/AddressChip.kt` — substituído stub por chip pill com `Surface`,
    `BorderStroke` e parâmetro `label` (sem onClick)
  - `ui/components/PickingErrorBottomSheet.kt` — bottom sheet de erro com overlay escuro,
    ícone circular, título/mensagem e botão "CONFIRMAR"; aceita `PickingError`
  - `ui/picking/PickingScreen.kt` — implementação completa: header caixa + progresso,
    corpo com REF/descrição/endereço (`NumeralXl`)/chips/contador (`NumeralLarge`),
    rodapé com botões "SEM SALDO" (`WarehouseOrange`) e "Endereços Alt.", loading overlay,
    modal de erro via `PickingErrorBottomSheet`; `PickingScreenContent` separado para previews
  - 3 `@Preview` (Active, Loading, ErrorModal)

## In Progress

- Nenhum.

## Next Up

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

## Architecture Decisions

- `Result<T>` sealed class definida em `domain/model/Result.kt` — usada por todos
  os repositórios para retornar sucesso/erro tipado sem lançar exceções no ViewModel.
- `AuthRepository` agora depende de `ApiService` além de `SecureStorage` — sem
  circular dependency pois `ApiService` não depende de `AuthRepository`.
- `LoginViewModel` habilita/desabilita scanner via `onResume`/`onPause` igual às
  telas de bipagem — login também usa scanner para capturar credenciais.
- Fluxo de bipagem sequencial: 1º barcode → `supervisorCodigo`, 2º → `operadorCracha`;
  barcodes extras ignorados enquanto ambos estiverem preenchidos.
- `LoginContent` separado de `LoginScreen` para viabilizar `@Preview` sem Hilt.
- `CaixaResult` sealed class em `ColetaRepository.kt` — resultado tipado de `buscarCaixa`,
  evita lançar exceções no ViewModel.
- `AppDestination.Papeleta` alterado para `"papeleta/{caixaCodigo}"` — argumento obrigatório
  de navegação; `withArgs()` constrói a rota com o código real.
- `MenuScreenContent` separado de `MenuScreen` (igual ao padrão `LoginContent/LoginScreen`)
  para viabilizar `@Preview` sem Hilt.
- `ColetaStateHolder` é o único ponto de passagem de `CaixaDto` entre Menu e Papeleta —
  evita serialização do DTO como argumento de navegação (violaria a regra "passe só IDs").
- `PapeletaViewModel` lê `caixaCodigo` de `SavedStateHandle` — garante sobrevivência
  ao process death sem precisar re-navegar.
- `StatusChip` é o único arquivo com cores alpha calculadas em runtime; fora dele,
  todas as cores vêm de `MaterialTheme.colorScheme` ou constantes de `Color.kt`.
- `PapeletaScreenContent` separado de `PapeletaScreen` para viabilizar `@Preview` sem Hilt.

## Session Notes

- 2026-05-21: Setup inicial implementado a partir da spec `01-mobile-setup-spec.md`.
  Projeto criado manualmente (sem Android Studio) — todos os arquivos Kotlin e Gradle
  gerados diretamente. Arquivos binários (fontes .ttf, .aar, gradle-wrapper.jar)
  precisam ser adicionados manualmente antes de compilar.
- 2026-05-21: Tela de Login implementada a partir dos context files (spec `02` estava
  vazia). Implementação seguiu ui-context.md, architecture.md e code-standards.md.
- 2026-05-21: Tela de Menu implementada a partir da spec `03-menu-screen-spec.md`.
  `ScanDisplayField` reutilizado com assinatura existente (`value`, `placeholder`, `isActive`).
  `AuthInterceptor` recebeu `@Singleton` que estava faltando.
- 2026-05-21: Tela de Papeleta implementada a partir da spec `04-papeleta-screen-spec.md`.
  6 arquivos criados/modificados: `ColetaStateHolder`, `MenuViewModel` (injeção do holder),
  `PapeletaViewModel`, `StatusChip`, `ItemPapeletaCard`, `PapeletaScreen`.
  `WarningIndustrial`/`SuccessIndustrial` de `Color.kt` usados no `StatusChip` em vez de hex hardcoded.
- 2026-05-21: Tela de Picking implementada a partir da spec `05-picking-screen-spec.md`.
  10 arquivos criados/modificados: `BipagemDtos.kt` (criado), `BipagemRepository.kt` (criado),
  `PickingErrorBottomSheet.kt` (criado), `ApiService.kt`, `BipagemPendenteEntity.kt`,
  `SyncWorker.kt`, `PickingViewModel.kt`, `PickingScreen.kt`, `AddressChip.kt` (substituído stub),
  `BipagemRequestDto.kt` (esvaziado — substituído por `PostBipagemRequest` em `BipagemDtos.kt`).
  Cores via `WarehouseOrange` de `Color.kt`; endereço via `NumeralXl`; contador via `NumeralLarge`.
