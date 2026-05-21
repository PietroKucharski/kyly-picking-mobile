# Mobile — Aplicativo de Picking Kyly

## Overview

Aplicativo **Native Android (Jetpack Compose + Kotlin)** para o dispositivo
Datalogic Memor 11 (Android 11, tela 5", scanner 2D integrado). Utilizado
exclusivamente pelos operadores de picking no galpão de expedição do Grupo Kyly.
É a interface principal do processo de coleta — o operador usa o app durante todo
o turno para autenticar, abrir caixas, bipar peças e registrar divergências.
O app consome a API REST do backend e suporta operação com conectividade
intermitente via Wi-Fi do galpão.

O acesso ao scanner, LEDs e buzzer é feito via **Datalogic SDK** (`.aar`)
integrado diretamente ao Gradle — sem bridge, sem runtime externo.

## Dispositivo Alvo

**Datalogic Memor 11**
- Android 11 (atualizável para Android 13)
- Tela 5" IPS HD (720×1280), capacitiva, Dragontrail glass
- RAM: 4 GB / Storage: 32 GB
- Scanner: Halogen DE2102-HP 2D area imager, depth of field 4–90 cm
- Green Spot: feedback visual de good-read no próprio código de barras
- LED indicadores: Charging LED (topo esq.) + Good Read Indicator (programável via SDK)
- 2 botões físicos de scan (laterais esquerdo e direito)
- Wi-Fi 802.11 a/b/g/n/ac + opcional 4G LTE
- IP65 (resistente a poeira e respingos)
- Drop resistance: 1,5 m sobre concreto

## User Profile

**Operador de Picking:**
- Opera com luvas em ambiente industrial
- Tela de 5" a distância variável (30–80 cm)
- Feedback tátil limitado — depende de feedback sonoro (beep) e visual (LED + tela)
- Não é usuário técnico — a UI deve ser autoexplicativa e sem ambiguidade
- Realiza a mesma sequência de ações repetidamente durante o turno

## Screens

| # | Tela                      | Classe                            | Descrição                                                         |
| - | ------------------------- | --------------------------------- | ----------------------------------------------------------------- |
| 1 | Login / Autenticação      | `LoginScreen`                     | Dupla bipagem: código do turno (supervisor) + crachá do operador  |
| 2 | Menu Principal            | `MenuScreen`                      | Botão "1. Montar Pedido" + nome do operador e turno logado        |
| 3 | Bipagem de Papeleta       | `PapeletaScreen`                  | Leitura da papeleta da caixa; exibe resumo da caixa após bipe     |
| 4 | Tela de Picking           | `PickingScreen`                   | Endereço em fonte grande, quantidade pendente, progresso, botões  |
| 5 | Bottom Sheet de Erro      | `ErrorBottomSheet`                | Fundo vermelho/modal, mensagem de erro, botão "Confirmar"         |
| 6 | Endereços Alternativos    | `EnderecosAlternativosScreen`     | Lista de localizações alternativas para o SKU em falta            |
| 7 | Finalização de Caixa      | `FinalizacaoScreen`               | Tela com verde (finalizada) ou amarelo (picking parcial)          |

## Core User Flow

1. Operador bipa o código do supervisor (placa do turno) → turno identificado.
2. Operador bipa o próprio crachá → autenticação concluída, JWT emitido pela API.
3. Menu Principal → seleciona "1. Montar Pedido".
4. Pega caixa na sequência → bipa a papeleta → tela exibe resumo da caixa.
5. `PickingScreen` exibe: endereço do primeiro SKU, referência/cor/tamanho, quantidade.
6. Operador vai ao endereço → localiza a peça → bipa o código único (etiqueta).
7. `DatalogicManager` entrega o barcode via `SharedFlow` → `PickingViewModel` processa.
8. API valida → `FeedbackManager.trigger()` aciona LED + beep → tela atualiza.
9. Repete até completar a quantidade do SKU → `PickingViewModel` avança automaticamente.
10. Ao completar todos os itens: navega para `FinalizacaoScreen` (verde).
11. Se faltam peças: `FinalizacaoScreen` com cor de warning (picking parcial).

## Alternate Flows

**FA01 — Item em Falta:** Botão "Sem Saldo" → `PickingViewModel.onSemSaldo()` →
API registra divergência → `FeedbackEvent.SEM_SALDO` → próximo SKU.

**FA02 — Endereços Alternativos:** Botão "Endereços Alt." → navega para
`EnderecosAlternativosScreen` com `skuId` como argumento.

**FA03 — SKU Incorreto:** API retorna 422 `ERRO_SKU` → `FeedbackEvent.ERRO_SKU`
(LED vermelho + beep 2s) → `ErrorBottomSheet` → operador confirma e retenta.

**FA04 — Peça Já Bipada:** API retorna 422 `SEM_SALDO` → `FeedbackEvent.SEM_SALDO`
(LED vermelho + beep 2s) → `ErrorBottomSheet` → operador busca outra peça.

**FA05 — Caixa Multi-Andar:** Botão "Salvar Parcial" → caixa salva como "Picking
Parcial" → operador vai ao próximo andar → reabre bipando a papeleta.

## Business Rules Encoded in the App

- Scanner ativado **apenas** em telas de bipagem (`PickingScreen`, `PapeletaScreen`,
  `LoginScreen`). `DatalogicManager.enable()` no `ON_RESUME`, `disable()` no `ON_PAUSE`.
- Máximo de 2 caixas simultâneas por operador — a API rejeita a terceira.
- Código EAN secundário deve ser descartado — apenas o código único longo é aceito.
  O `ColetaRepository` filtra antes de enviar à API.
- Toda validação de bipagem é realizada pela API. O app nunca aprova localmente.

## Success Criteria

1. Bipagem de peça válida retorna feedback em menos de **200ms** (LED + atualização de tela).
2. O app opera sem Wi-Fi por ao menos 5 minutos, enfileirando bipagens via Room.
3. Um operador nunca fica preso em uma tela — todos os fluxos de erro têm saída clara.
4. A tela de picking é legível a 60 cm com luvas, sem necessidade de zoom.
5. O scanner nunca fica ativo fora das telas de bipagem.
