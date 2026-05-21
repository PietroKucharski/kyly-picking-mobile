# 08 — Mobile App: Tela de Papeleta

## Objetivo

Implementar a tela de Papeleta — a tela de revisão antes de iniciar o picking.
Após o operador bipar a caixa no Menu, o app navega para esta tela exibindo:

- Número do pedido vinculado à caixa
- Código da caixa
- Lista completa de itens a coletar com SKU, endereço, quantidades e status
- Botão **INICIAR PICKING** que leva o operador à tela de coleta item a item

A tela usa um `ColetaStateHolder` (singleton Hilt) para receber o `CaixaDto`
já buscado pelo `MenuViewModel`, evitando uma segunda chamada de rede.
Se o holder estiver vazio (ex.: navegação por deep link ou restart), a tela
re-busca os dados automaticamente via `ColetaRepository`.

---

## Pré-condições

- Spec 07 concluída: `MenuViewModel`, `ColetaRepository` e `CaixaDto` implementados
- `AppDestination.Papeleta` atualizado para `"papeleta/{caixaCodigo}"` (feito na spec 07)
- `PickingScreen` existe como composable — pode ser placeholder com `Text("Picking")`

---

## Layout de Referência

```
┌─────────────────────────────────────────────┐
│  Header — bg primaryContainer (#1B3A5C)     │
│  h=56dp                                     │
│  ← [ícone voltar]  "PEDIDO {numeroPedido}"  │
│  (headlineMedium, branco)                   │
├─────────────────────────────────────────────┤
│  px=16dp, pt=16dp                           │
│                                             │
│  "CAIXA {codigo}"  ← labelLarge, outline   │
│  "X de Y itens coletados"  ← bodyMedium    │
│                  text-onSurfaceVariant      │
│                                             │
│  ── LazyColumn (flex-1) ────────────────── │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ [PENDENTE]  ←  chip status          │   │
│  │ REF 2000183 — UN                    │   │  labelLarge, primary
│  │ Camiseta Manga Curta Masculina      │   │  bodyMedium, onSurface
│  │ 📍 C37.09.6B                        │   │  bodyMedium, primaryContainer, mono
│  │ 0 / 2 peças                         │   │  labelLarge, onSurfaceVariant
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ [PARCIAL]   SKU...  Endereço...     │   │
│  │ 1 / 3 peças                         │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ [COMPLETO]  SKU...  Endereço...     │   │
│  │ 2 / 2 peças                         │   │
│  └─────────────────────────────────────┘   │
│                                             │
├─────────────────────────────────────────────┤
│  p=16dp                                     │
│  ┌─────────────────────────────────────┐   │
│  │        INICIAR PICKING              │   │  h=60dp, bg primaryContainer
│  └─────────────────────────────────────┘   │
│  (desabilitado se todos os itens forem      │
│   "completo" ou "falta")                   │
└─────────────────────────────────────────────┘
```

**Chips de status por cor:**

| Status     | Background                  | Texto                         |
|------------|-----------------------------|-------------------------------|
| `pendente` | `surfaceContainerHigh`      | `onSurfaceVariant`            |
| `parcial`  | `warningIndustrial` (0.15 alpha) | `warningIndustrial`       |
| `completo` | `successIndustrial` (0.15 alpha) | `successIndustrial`       |
| `falta`    | `errorContainer`            | `onErrorContainer`            |

---

## Checklist

### 1. ColetaStateHolder — `data/repository/ColetaStateHolder.kt`

Singleton que armazena o `CaixaDto` mais recentemente buscado pelo `MenuViewModel`.
Evita re-fetch na transição Menu → Papeleta.

- [ ] Criar `ColetaStateHolder`:
  ```kotlin
  @Singleton
  class ColetaStateHolder @Inject constructor() {
      private val _caixa = MutableStateFlow<CaixaDto?>(null)
      val caixa: StateFlow<CaixaDto?> = _caixa.asStateFlow()

      fun set(caixa: CaixaDto) { _caixa.value = caixa }
      fun clear()              { _caixa.value = null  }
      fun get(): CaixaDto?     = _caixa.value
  }
  ```

- [ ] Atualizar `MenuViewModel` — injetar `ColetaStateHolder` e salvar a caixa ao navegar:
  ```kotlin
  // Em MenuViewModel — adicionar ao construtor:
  private val coletaStateHolder: ColetaStateHolder,

  // Em buscarCaixa(), bloco de Success, antes de emitir o evento:
  is CaixaResult.Success -> {
      coletaStateHolder.set(result.caixa)          // ← linha nova
      _events.emit(MenuEvent.NavigateToPapeleta(result.caixa.codigo))
  }
  ```

---

### 2. PapeletaUiState e PapeletaEvent — `ui/papeleta/PapeletaViewModel.kt`

- [ ] Definir sealed class de conteúdo:
  ```kotlin
  sealed class PapeletaContent {
      data object Loading : PapeletaContent()
      data class Loaded(val caixa: CaixaDto) : PapeletaContent()
      data class Error(val message: String) : PapeletaContent()
  }
  ```
- [ ] Definir `PapeletaUiState`:
  ```kotlin
  data class PapeletaUiState(
      val content: PapeletaContent = PapeletaContent.Loading,
  ) {
      val canStartPicking: Boolean
          get() = content is PapeletaContent.Loaded &&
                  (content as PapeletaContent.Loaded).caixa.itens
                      .any { it.status == "pendente" || it.status == "parcial" }
  }
  ```
- [ ] Definir `PapeletaEvent`:
  ```kotlin
  sealed class PapeletaEvent {
      data class NavigateToPicking(val caixaCodigo: String) : PapeletaEvent()
      data object NavigateBack : PapeletaEvent()
  }
  ```

---

### 3. PapeletaViewModel — `ui/papeleta/PapeletaViewModel.kt`

- [ ] Criar `PapeletaViewModel` com `@HiltViewModel` e `SavedStateHandle`:
  ```kotlin
  @HiltViewModel
  class PapeletaViewModel @Inject constructor(
      private val coletaStateHolder: ColetaStateHolder,
      private val coletaRepository: ColetaRepository,
      savedStateHandle: SavedStateHandle,
  ) : ViewModel() {

      private val caixaCodigo: String =
          checkNotNull(savedStateHandle["caixaCodigo"]) { "caixaCodigo ausente nos args" }

      private val _uiState = MutableStateFlow(PapeletaUiState())
      val uiState: StateFlow<PapeletaUiState> = _uiState.asStateFlow()

      private val _events = MutableSharedFlow<PapeletaEvent>()
      val events: SharedFlow<PapeletaEvent> = _events

      init {
          carregarCaixa()
      }
  }
  ```
- [ ] Implementar `carregarCaixa()`:
  ```kotlin
  private fun carregarCaixa() {
      val cached = coletaStateHolder.get()

      // Usa o cache se o código bate — evita round-trip desnecessário
      if (cached != null && cached.codigo == caixaCodigo) {
          _uiState.update { it.copy(content = PapeletaContent.Loaded(cached)) }
          return
      }

      // Fallback: re-busca da API (deep link, app restart, etc.)
      _uiState.update { it.copy(content = PapeletaContent.Loading) }
      viewModelScope.launch {
          when (val result = coletaRepository.buscarCaixa(caixaCodigo)) {
              is CaixaResult.Success -> {
                  coletaStateHolder.set(result.caixa)
                  _uiState.update { it.copy(content = PapeletaContent.Loaded(result.caixa)) }
              }
              is CaixaResult.NotFound ->
                  _uiState.update {
                      it.copy(content = PapeletaContent.Error("Caixa não encontrada."))
                  }
              is CaixaResult.AlreadyFinalized ->
                  _uiState.update {
                      it.copy(content = PapeletaContent.Error("Esta caixa já foi finalizada."))
                  }
              is CaixaResult.HttpError ->
                  _uiState.update {
                      it.copy(content = PapeletaContent.Error("Erro ${result.code}. Tente novamente."))
                  }
              CaixaResult.NetworkError ->
                  _uiState.update {
                      it.copy(content = PapeletaContent.Error("Sem conexão. Verifique a rede."))
                  }
          }
      }
  }
  ```
- [ ] Implementar `onIniciarPicking()`:
  ```kotlin
  fun onIniciarPicking() {
      if (!_uiState.value.canStartPicking) return
      viewModelScope.launch {
          _events.emit(PapeletaEvent.NavigateToPicking(caixaCodigo))
      }
  }
  ```
- [ ] Implementar `onVoltar()`:
  ```kotlin
  fun onVoltar() {
      coletaStateHolder.clear()
      viewModelScope.launch { _events.emit(PapeletaEvent.NavigateBack) }
  }
  ```
- [ ] Verificar: nenhum acesso direto à API — sempre via `ColetaRepository`

---

### 4. Componente ItemPapeletaCard — `ui/components/ItemPapeletaCard.kt`

Card reutilizável para exibir um item da papeleta.

- [ ] Criar `ItemPapeletaCard`:
  ```kotlin
  @Composable
  fun ItemPapeletaCard(
      item: ItemCaixaDto,
      modifier: Modifier = Modifier,
  ) {
      Card(
          modifier = modifier.fillMaxWidth(),
          shape    = RoundedCornerShape(8.dp),
          colors   = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
          Column(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
              // Linha 1: chip de status + código do SKU
              Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment     = Alignment.CenterVertically,
              ) {
                  StatusChip(status = item.status)
                  Text(
                      text  = "${item.sku.codigo} — ${item.sku.unidade}",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary,
                  )
              }

              // Linha 2: descrição do SKU
              Text(
                  text  = item.sku.descricao,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
              )

              // Linha 3: endereço
              Row(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment     = Alignment.CenterVertically,
              ) {
                  Icon(
                      imageVector        = Icons.Outlined.LocationOn,
                      contentDescription = null,
                      modifier           = Modifier.size(16.dp),
                      tint               = MaterialTheme.colorScheme.primaryContainer,
                  )
                  Text(
                      text     = item.endereco.codigo,
                      style    = MaterialTheme.typography.bodyMedium.copy(
                          fontFamily = JetBrainsMono, // fonte mono para endereço
                      ),
                      color    = MaterialTheme.colorScheme.primaryContainer,
                  )
              }

              // Linha 4: progresso de quantidade
              Text(
                  text  = "${item.quantidadeColetada} / ${item.quantidadeEsperada} peças",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
          }
      }
  }
  ```

---

### 5. Componente StatusChip — `ui/components/StatusChip.kt`

- [ ] Criar `StatusChip` baseado no status do item:
  ```kotlin
  @Composable
  fun StatusChip(
      status: String,
      modifier: Modifier = Modifier,
  ) {
      val (bgColor, textColor, label) = when (status) {
          "pendente" -> Triple(
              MaterialTheme.colorScheme.surfaceContainerHigh,
              MaterialTheme.colorScheme.onSurfaceVariant,
              "PENDENTE",
          )
          "parcial"  -> Triple(
              Color(0xFFF59E0B).copy(alpha = 0.15f),
              Color(0xFFF59E0B),
              "PARCIAL",
          )
          "completo" -> Triple(
              Color(0xFF2E7D32).copy(alpha = 0.15f),
              Color(0xFF2E7D32),
              "COMPLETO",
          )
          "falta"    -> Triple(
              MaterialTheme.colorScheme.errorContainer,
              MaterialTheme.colorScheme.onErrorContainer,
              "FALTA",
          )
          else       -> Triple(
              MaterialTheme.colorScheme.surfaceContainerHigh,
              MaterialTheme.colorScheme.onSurfaceVariant,
              status.uppercase(),
          )
      }

      Surface(
          modifier = modifier,
          shape    = RoundedCornerShape(4.dp),
          color    = bgColor,
      ) {
          Text(
              text     = label,
              style    = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
              color    = textColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
      }
  }
  ```

---

### 6. PapeletaScreen — `ui/papeleta/PapeletaScreen.kt`

- [ ] Substituir o placeholder criado na spec 07 pela implementação real:
  ```kotlin
  @Composable
  fun PapeletaScreen(
      navController: NavController,
      caixaCodigo: String,
      viewModel: PapeletaViewModel = hiltViewModel(),
  ) {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      // Eventos únicos
      LaunchedEffect(Unit) {
          viewModel.events.collect { event ->
              when (event) {
                  is PapeletaEvent.NavigateToPicking ->
                      navController.navigate(
                          AppDestination.Picking.withArgs(event.caixaCodigo)
                      )
                  PapeletaEvent.NavigateBack ->
                      navController.popBackStack()
              }
          }
      }

      PapeletaScreenContent(
          uiState         = uiState,
          onIniciarPicking = viewModel::onIniciarPicking,
          onVoltar        = viewModel::onVoltar,
      )
  }
  ```
- [ ] Criar `PapeletaScreenContent`:
  ```kotlin
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

          // ── Header ────────────────────────────────────────────────
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
                  text      = headerTitle,
                  style     = MaterialTheme.typography.headlineMedium,
                  color     = MaterialTheme.colorScheme.onPrimary,
                  modifier  = Modifier.align(Alignment.Center),
              )
          }

          // ── Conteúdo ──────────────────────────────────────────────
          when (val content = uiState.content) {

              PapeletaContent.Loading -> {
                  Box(
                      modifier            = Modifier.weight(1f).fillMaxWidth(),
                      contentAlignment    = Alignment.Center,
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
                  val caixa       = content.caixa
                  val coletados   = caixa.itens.count { it.status == "completo" }
                  val total       = caixa.itens.size
                  // Ordem de exibição: pendente → parcial → falta → completo
                  val itensOrdenados = caixa.itens.sortedBy { item ->
                      when (item.status) {
                          "pendente" -> 0
                          "parcial"  -> 1
                          "falta"    -> 2
                          "completo" -> 3
                          else       -> 4
                      }
                  }

                  // Sub-header com resumo
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

                  // Lista de itens
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

          // ── Rodapé ────────────────────────────────────────────────
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
  ```

---

### 7. Previews — `ui/papeleta/PapeletaScreen.kt`

- [ ] Adicionar `@Preview` nos quatro estados:
  ```kotlin
  private val previewCaixa = CaixaDto(
      id     = "uuid-1",
      codigo = "06772401",
      status = "em_coleta",
      pedido = PedidoDto("uuid-p", "PED-2025-0042", "em_separacao"),
      itens  = listOf(
          ItemCaixaDto(
              id = "uuid-i1", status = "pendente",
              quantidadeEsperada = 2, quantidadeColetada = 0,
              sku     = SkuDto("uuid-s1", "2000183", "Camiseta Manga Curta Masculina", "UN"),
              endereco = EnderecoDto("uuid-e1", "C37.09.6B", "C37", "09", "6B"),
          ),
          ItemCaixaDto(
              id = "uuid-i2", status = "parcial",
              quantidadeEsperada = 3, quantidadeColetada = 1,
              sku     = SkuDto("uuid-s2", "3001044", "Bermuda Jeans Feminina", "UN"),
              endereco = EnderecoDto("uuid-e2", "A12.03.1C", "A12", "03", "1C"),
          ),
          ItemCaixaDto(
              id = "uuid-i3", status = "completo",
              quantidadeEsperada = 2, quantidadeColetada = 2,
              sku     = SkuDto("uuid-s3", "1500277", "Blusa Regata Feminina", "UN"),
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
  ```

---

## Fluxo Completo da Tela de Papeleta

```
MenuScreen bipa caixa
  → MenuViewModel.buscarCaixa() retorna CaixaResult.Success
  → coletaStateHolder.set(caixa)                 ← armazena
  → navega para "papeleta/{caixaCodigo}"

PapeletaScreen abre
  → PapeletaViewModel.carregarCaixa()
  │
  ├─ coletaStateHolder.get().codigo == caixaCodigo?
  │   └─ Sim → content = Loaded(caixa)           ← sem chamada de rede
  │
  └─ Não (deep link / restart)
      → GET /api/mobile/caixas/{caixaCodigo}
      ├─ 200 → coletaStateHolder.set() + content = Loaded
      ├─ 404 → content = Error("Caixa não encontrada")
      ├─ 422 → content = Error("Caixa já finalizada")
      └─ IOException → content = Error("Sem conexão")

Tela exibe lista de itens ordenada: pendente → parcial → falta → completo

Tap "INICIAR PICKING"
  → habilitado só se algum item for "pendente" ou "parcial"
  → navega para "picking/{caixaCodigo}"

Tap "← Voltar"
  → coletaStateHolder.clear()
  → navController.popBackStack() → volta para MenuScreen
```

---

## Definition of Done

- [ ] `ColetaStateHolder` criado e injetável via Hilt
- [ ] `MenuViewModel` armazena `CaixaDto` no `ColetaStateHolder` antes de navegar
- [ ] `PapeletaScreen` usa cache do `ColetaStateHolder` — zero chamadas de rede na transição normal
- [ ] `PapeletaScreen` re-busca automaticamente se o holder estiver vazio (fallback)
- [ ] Header exibe número do pedido em branco sobre `primaryContainer`
- [ ] Sub-header exibe código da caixa e progresso (`X de Y itens coletados`)
- [ ] Lista ordenada: itens `pendente` e `parcial` aparecem antes de `completo` e `falta`
- [ ] `StatusChip` exibe cor correta para cada status
- [ ] `ItemPapeletaCard` exibe SKU, descrição, endereço (fonte mono) e quantidade
- [ ] Botão "INICIAR PICKING" habilitado enquanto existir item `pendente` ou `parcial`
- [ ] Botão "INICIAR PICKING" desabilitado (visualmente) quando todos estão `completo`/`falta`
- [ ] Tap "INICIAR PICKING" → navega para `PickingScreen/{caixaCodigo}`
- [ ] Tap "← Voltar" → limpa o state holder e volta para MenuScreen
- [ ] Estado `Loading` exibe `CircularProgressIndicator` centralizado
- [ ] Estado `Error` exibe mensagem em vermelho centralizada
- [ ] Nenhuma cor hardcoded fora de `StatusChip` (necessário para `warningIndustrial` e `successIndustrial` com alpha)
- [ ] `./gradlew assembleDebug` compila sem erros
- [ ] `@Preview` renderiza corretamente nos quatro estados
- [ ] `progress-tracker.md` atualizado
