# Mobile — AI Workflow Rules

## Approach

Construa o app tela por tela, na ordem do fluxo operacional do operador.
Cada iteração entrega uma tela funcional: `@Composable` de tela, `@HiltViewModel`,
repositório necessário, chamada à API e feedback de hardware.
O `DatalogicManager` (mesmo que como stub sem o `.aar` real) deve existir antes
de qualquer tela de bipagem ser implementada.

## Ordem de Implementação Recomendada

1. **Setup do projeto** (ver spec `05-mobile-setup-spec.md`):
   Android Studio → "Empty Compose Activity", Gradle com Compose BOM + Hilt + Retrofit +
   Room + WorkManager + Datalogic `.aar`. Configurar `ui/theme/` (Color.kt, Type.kt,
   Theme.kt) com tokens de `context/ui-context.md`. Baixar fontes Public Sans e
   JetBrains Mono em `res/font/`.

2. **`DatalogicManager` + `FeedbackManager`** (stub):
   Sem o `.aar` real: implementar com logs (`Log.d`). Com o `.aar`: usar
   `BarcodeManager`, `NotificationManager` conforme `context/architecture.md`.
   Criar `FeedbackEvent` enum. Testar no dispositivo físico antes de avançar.

3. **Módulos Hilt**: `NetworkModule` (Retrofit, OkHttp, `AuthInterceptor`),
   `DatabaseModule` (Room, `BipagemPendenteDao`). Verificar que a injeção
   compila sem erros (`./gradlew hiltJavaCompile`).

4. **`SecureStorage`** + **`AppNavGraph`**: navegação com todas as rotas registradas.

5. **Login** (`LoginScreen` + `LoginViewModel` + `AuthRepository`):
   dupla bipagem (turno → operador), chamada ao `ApiService.mobileLogin()`,
   armazenamento de JWT via `SecureStorage`, navegação para `MenuScreen`.

6. **Menu Principal** (`MenuScreen` + `MenuViewModel`): botão "Montar Pedido" + nome/turno
   vindos de `AuthRepository`.

7. **Bipagem de Papeleta** (`PapeletaScreen` + `PapeletaViewModel`):
   scanner ativo, `DatalogicManager.barcodeEvents.collect`, chamada a
   `ApiService.getCaixa()`, armazenamento no `ColetaRepository`, navegação para
   `PickingScreen`.

8. **Tela de Picking** (`PickingScreen` + `PickingViewModel`) — **iteração mais crítica**:
   layout completo conforme `ui-context.md`, scanner via `DisposableEffect`, coleta de
   eventos do `DatalogicManager.barcodeEvents`, `FeedbackManager.trigger()` para todos
   os cenários, `ErrorBottomSheet`, avanço automático de endereço, navegação para
   `FinalizacaoScreen`.

9. **Finalização** (`FinalizacaoScreen`): telas verde (finalizada) e warning (parcial).

10. **Item em Falta** + **Endereços Alternativos** (`EnderecosAlternativosScreen`).

11. **Offline/Sync**: `SyncWorker` (`@HiltWorker`) + agendamento via `WorkManager` em
    `KylyPickingApp.onCreate()`. Habilitar fallback offline no `ColetaRepository`.

## Scoping Rules

- Uma tela por iteração.
- `DatalogicManager` (mesmo como stub) deve existir antes de qualquer tela de bipagem.
- `AppNavGraph` deve existir antes de qualquer tela — não construa telas sem navegação.
- `PickingScreen` é o coração do app — dedique uma iteração inteira. Não combine com
  outras telas.
- Implemente o modo offline apenas após a `PickingScreen` funcionar no modo conectado.

## When to Split Work

Divida uma implementação se ela combinar:

- Mudanças no Datalogic SDK (`.aar` ou `DatalogicManager.kt`) + lógica de tela
- Mais de uma tela por iteração
- Lógica offline (`SyncWorker`) + qualquer tela nova ao mesmo tempo
- Qualquer comportamento de hardware não documentado em `context/ui-context.md`

## New Screen Checklist

Para cada nova tela, antes de marcar como concluída:

- [ ] Layout usa `MaterialTheme.colorScheme.*` e constantes de `Color.kt` (sem hex hardcoded)
- [ ] Fontes via `MaterialTheme.typography.*` ou `NumeralXl`/`NumeralLarge` de `Type.kt`
- [ ] `DisposableEffect` com `LifecycleEventObserver` para `ON_RESUME`/`ON_PAUSE` do scanner
      (apenas em telas de bipagem: Login, Papeleta, Picking)
- [ ] `FeedbackManager.trigger()` chamado para todos os cenários de feedback
- [ ] Todos os estados de erro com mensagem clara ao operador (`ErrorBottomSheet`)
- [ ] Navegação de entrada e saída via `AppDestination` (sem strings literais)
- [ ] Sem `!!` introduzido. `./gradlew build` sem warnings de Kotlin
- [ ] Testado no emulador para fluxo feliz + pelo menos um fluxo de erro
- [ ] Testado no Datalogic Memor 11 para todos os cenários de hardware (LED, beep, scanner)

## DatalogicManager Checklist

Ao modificar `DatalogicManager.kt` ou `FeedbackManager.kt`:

- [ ] `FeedbackEvent` enum atualizado se novo evento adicionado
- [ ] `progress-tracker.md` registra a mudança
- [ ] Rebuild completo: `./gradlew assembleDebug`
- [ ] Testado no dispositivo físico Datalogic Memor 11 (não apenas emulador)

## Handling Missing Requirements

- Se um comportamento do SDK não estiver documentado em `context/ui-context.md`
  (ex: padrão de LED para evento novo), registre como open question no
  `context/progress-tracker.md`.
- Não invente validações de bipagem — toda validação vem da API.
- Se a resposta da API para um cenário offline não estiver definida, registre como
  open question antes de implementar o `SyncWorker`.

## Protected Files

Não modifique sem instrução explícita:

- `hardware/DatalogicManager.kt` — qualquer erro afeta hardware real.
  Mudanças exigem teste no dispositivo físico.
- `app/libs/datalogic-sdk.aar` — nunca substituir sem alinhar com a Kyly.
- `app/build.gradle.kts` (seção `flatDir` e dependência do `.aar`).

## Before Moving to the Next Screen

1. A tela funciona no emulador para o fluxo feliz e para o principal fluxo de erro.
2. O feedback de LED/beep está correto conforme a tabela de `context/ui-context.md`.
3. O scanner é desabilitado quando a tela perde o foco (cleanup do `DisposableEffect`).
4. Nenhum `!!` introduzido. `./gradlew build` sem erros.
5. `context/progress-tracker.md` reflete a tela concluída.
