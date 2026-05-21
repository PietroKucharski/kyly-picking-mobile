# Mobile — UI Context

## Theme

Tema **claro corporativo industrial** (Industrial Warehouse System). Interface baseada em
Material Design 3, otimizada para scanners Android ruggedizados em ambiente de galpão com
iluminação variável. O operador usa luvas — todo elemento interativo tem mínimo 56dp de
altura. A personalidade visual é **confiabilidade e precisão operacional**: sem ornamentos,
máximo contraste, feedback imediato e inequívoco.

Fonte principal: **Public Sans** (Google Fonts) — x-height alto, leitura garantida em
displays LCD de baixa densidade a 60cm de distância.

---

## Colors

Definidas como constantes em `src/utils/colors.ts` seguindo os tokens semânticos do
Material Design 3, extraídos do design system "Industrial Warehouse System" do Stitch.

```typescript
// src/utils/colors.ts
export const colors = {
  // ── Backgrounds ──────────────────────────────────────────────
  background:                '#F7F9FC', // tela base (cool gray claro)
  surface:                   '#F7F9FC',
  surfaceDim:                '#D8DADD',
  surfaceBright:             '#F7F9FC',
  surfaceContainerLowest:    '#FFFFFF',
  surfaceContainerLow:       '#F2F4F7',
  surfaceContainer:          '#ECEEF1',
  surfaceContainerHigh:      '#E6E8EB',
  surfaceContainerHighest:   '#E0E3E6',

  // ── Text / On-surface ─────────────────────────────────────────
  onBackground:              '#191C1E', // texto principal
  onSurface:                 '#191C1E',
  onSurfaceVariant:          '#43474E', // labels, hints, texto secundário
  inverseSurface:            '#2D3133',
  inverseOnSurface:          '#EFF1F4',
  outline:                   '#73777F', // bordas padrão
  outlineVariant:            '#C3C6CF', // bordas suaves

  // ── Primary — Navy ────────────────────────────────────────────
  primary:                   '#002444', // ações primárias, títulos
  onPrimary:                 '#FFFFFF',
  primaryContainer:          '#1B3A5C', // containers ativos, endereço
  onPrimaryContainer:        '#87A4CC',
  inversePrimary:            '#ABC9F2',
  surfaceTint:               '#436084',

  // ── Secondary — Azul ──────────────────────────────────────────
  secondary:                 '#0B61A1', // botões secundários, ações auxiliares
  onSecondary:               '#FFFFFF',
  secondaryContainer:        '#7CBAFF',
  onSecondaryContainer:      '#004A7D',

  // ── Tertiary — Verde ──────────────────────────────────────────
  tertiary:                  '#002A05',
  onTertiary:                '#FFFFFF',
  tertiaryContainer:         '#00430D',
  onTertiaryContainer:       '#64B462',

  // ── Semânticas ────────────────────────────────────────────────
  successIndustrial:         '#2E7D32', // "Caixa Finalizada", "Confirmar Pick"
  warningIndustrial:         '#F59E0B', // "Caixa Parcial", alerts
  warehouseOrange:           '#EA580C', // botão "Sem Saldo" / ação de exceção
  error:                     '#BA1A1A',
  onError:                   '#FFFFFF',
  errorContainer:            '#FFDAD6',
  onErrorContainer:          '#93000A',
} as const
```

Configuração no `tailwind.config.js` para uso via NativeWind:

```js
// tailwind.config.js
const { colors } = require('./src/utils/colors')

module.exports = {
  content: ['./src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        // Backgrounds
        'background':                   '#F7F9FC',
        'surface':                      '#F7F9FC',
        'surface-dim':                  '#D8DADD',
        'surface-container-lowest':     '#FFFFFF',
        'surface-container-low':        '#F2F4F7',
        'surface-container':            '#ECEEF1',
        'surface-container-high':       '#E6E8EB',
        'surface-container-highest':    '#E0E3E6',
        // Text
        'on-background':                '#191C1E',
        'on-surface':                   '#191C1E',
        'on-surface-variant':           '#43474E',
        'outline':                      '#73777F',
        'outline-variant':              '#C3C6CF',
        // Primary
        'primary':                      '#002444',
        'on-primary':                   '#FFFFFF',
        'primary-container':            '#1B3A5C',
        'on-primary-container':         '#87A4CC',
        // Secondary
        'secondary':                    '#0B61A1',
        'on-secondary':                 '#FFFFFF',
        'secondary-container':          '#7CBAFF',
        'on-secondary-container':       '#004A7D',
        // Tertiary
        'tertiary-container':           '#00430D',
        'on-tertiary-container':        '#64B462',
        'tertiary-fixed-dim':           '#88D982',
        // Semânticas
        'success-industrial':           '#2E7D32',
        'warning-industrial':           '#F59E0B',
        'warehouse-orange':             '#EA580C',
        'error':                        '#BA1A1A',
        'on-error':                     '#FFFFFF',
        'error-container':              '#FFDAD6',
        'on-error-container':           '#93000A',
      },
      fontFamily: {
        'public-sans': ['"Public Sans"', 'sans-serif'],
        'mono-address': ['"JetBrains Mono"', '"Roboto Mono"', 'monospace'],
      },
      fontSize: {
        'display-lg':  ['40px', { lineHeight: '48px', letterSpacing: '-0.02em', fontWeight: '700' }],
        'headline-md': ['24px', { lineHeight: '32px', fontWeight: '600' }],
        'title-lg':    ['20px', { lineHeight: '28px', fontWeight: '600' }],
        'body-lg':     ['18px', { lineHeight: '26px', fontWeight: '400' }],
        'body-md':     ['16px', { lineHeight: '24px', fontWeight: '400' }],
        'label-lg':    ['14px', { lineHeight: '20px', letterSpacing: '0.1px', fontWeight: '700' }],
        'numeral-xl':  ['48px', { lineHeight: '56px', fontWeight: '800' }],
      },
      spacing: {
        'margin-mobile':    '16px',
        'gutter':           '12px',
        'stack-gap':        '16px',
        'touch-target-min': '56px',
      },
      borderRadius: {
        'sm':   '0.125rem', //  2px — foco rings, chips
        DEFAULT: '0.25rem', //  4px — inputs, botões padrão
        'md':   '0.375rem', //  6px
        'lg':   '0.5rem',   //  8px — cards, containers
        'xl':   '0.75rem',  // 12px — modais, containers grandes
        'full': '9999px',   // pills
      },
    },
  },
  plugins: [],
}
```

---

## Typography

Fonte: **Public Sans** — carregar via `@expo-google-fonts/public-sans` ou link direto
no bundle. Fonte mono para endereços: **JetBrains Mono** (tela de picking) ou
**Roboto Mono** como fallback.

| Estilo        | fontSize | fontWeight | lineHeight | Uso principal                             |
| ------------- | -------- | ---------- | ---------- | ----------------------------------------- |
| `numeral-xl`  | `48`     | `'800'`    | `56`       | Quantidades em destaque (`0/4`, `3/16`)   |
| `display-lg`  | `40`     | `'700'`    | `48`       | Mensagens de estado (`CAIXA FINALIZADA`)  |
| `headline-md` | `24`     | `'600'`    | `32`       | Títulos de tela, IDs de caixa/pedido      |
| `title-lg`    | `20`     | `'600'`    | `28`       | Subtítulos, nome do produto               |
| `body-lg`     | `18`     | `'400'`    | `26`       | Texto de instrução, botões primários      |
| `body-md`     | `16`     | `'400'`    | `24`       | Corpo geral, labels de input              |
| `label-lg`    | `14`     | `'700'`    | `20`       | Labels em caps, chips de status, nav tabs |

Endereço de picking (campo central da tela de coleta):
```tsx
// Fonte mono, tamanho 72, peso black — máxima legibilidade a 60cm
<Text style={{ fontFamily: 'JetBrainsMono-Bold', fontSize: 72, color: '#1B3A5C' }}>
  C37.09.6B
</Text>
```

NativeWind equivalentes rápidos: `text-[48px]` (numeral-xl), `text-[40px]` (display-lg),
`text-2xl` (headline-md ≈24), `text-xl` (title-lg ≈20), `text-lg` (body-lg ≈18),
`text-base` (body-md ≈16), `text-sm` (label-lg ≈14).

---

## Layout — Tela de Picking em Andamento (Principal)

Estrutura em 3 zonas com `flex: 1`. Fundo claro `bg-background`.

```
┌─────────────────────────────────────┐
│ Header (h-14, bg-primary-container) │  ← "Caixa 06772401 | 3/16" — texto branco
├─────────────────────────────────────┤
│ flex-1 px-margin-mobile             │
│                                     │
│  REF 2000183 • 8532 • TAM 4         │  ← headline-md, text-primary
│  Camiseta Manga Curta Masculina      │  ← label-lg, text-on-surface-variant, uppercase
│                                     │
│  ENDEREÇO                           │  ← label-lg, outline, tracking-widest
│  ┌───────────────────────────────┐  │
│  │         C37.09.6B             │  │  ← JetBrains Mono, 72px, font-black, primary-container
│  └───────────────────────────────┘  │
│                                     │
│  [Corredor C37] [Seção 09] [Pos 6B] │  ← chips bg-surface-container-high, label-lg
│                                     │
│  1 / 2  Peças Restantes             │  ← numeral-xl (48px) + label-lg
│                                     │
├─────────────────────────────────────┤
│ flex-row gap-3 p-4                  │
│ [⚠ Sem Saldo] [Endereços Alt.]      │  ← h-14, rounded-lg
└─────────────────────────────────────┘
```

NativeWind na `PickingScreen`:
```tsx
<View className="flex-1 bg-background">
  {/* Header */}
  <View className="h-14 bg-primary-container flex-row justify-between items-center px-4">
    <Text className="font-bold text-lg uppercase text-white font-public-sans">
      Caixa {caixaCodigo}
    </Text>
    <Text className="font-bold text-lg text-white font-public-sans">
      {atual}/{total}
    </Text>
  </View>

  {/* Corpo */}
  <View className="flex-1 px-margin-mobile pt-6 gap-4">
    <Text className="text-headline-md font-semibold text-primary tracking-tight">
      {referencia}
    </Text>
    <Text className="text-label-lg font-bold text-on-surface-variant uppercase tracking-wider">
      {descricao}
    </Text>

    {/* Campo de endereço */}
    <Text className="text-label-lg text-outline uppercase tracking-[0.2em]">
      ENDEREÇO
    </Text>
    <Text className="text-[72px] font-black text-primary-container font-mono-address leading-none">
      {endereco}
    </Text>

    {/* Chips de localização */}
    <View className="flex-row gap-2 flex-wrap">
      {chips.map((chip) => (
        <View key={chip} className="bg-surface-container-high px-4 py-2 rounded-full border border-outline-variant">
          <Text className="text-label-lg text-primary">{chip}</Text>
        </View>
      ))}
    </View>

    {/* Contador */}
    <View className="items-center gap-1">
      <Text className="text-[48px] font-black text-primary leading-none">
        {pecasRestantes} / {pecasEsperadas}
      </Text>
      <Text className="text-label-lg text-on-surface-variant uppercase">
        Peças Restantes
      </Text>
    </View>
  </View>

  {/* Rodapé */}
  <View className="flex-row gap-3 p-4">
    <TouchableOpacity className="flex-1 h-14 bg-warehouse-orange rounded-lg items-center justify-center gap-2 flex-row">
      <Text className="text-label-lg text-white font-bold uppercase">Sem Saldo</Text>
    </TouchableOpacity>
    <TouchableOpacity className="flex-1 h-14 bg-surface-container-highest rounded-lg items-center justify-center">
      <Text className="text-label-lg text-on-surface-variant font-bold">Endereços Alt.</Text>
    </TouchableOpacity>
  </View>
</View>
```

---

## Botões

| Tipo              | Background              | Texto               | Uso                              |
| ----------------- | ----------------------- | ------------------- | -------------------------------- |
| Primary           | `bg-primary-container`  | `text-on-primary`   | Confirmar pick, iniciar separação|
| Success           | `bg-success-industrial` | `text-white`        | "Caixa Finalizada", confirmação  |
| Secondary         | `bg-secondary`          | `text-on-secondary` | Ações auxiliares, "Ver no Mapa"  |
| Warning/Exception | `bg-warning-industrial` | `text-white`        | "Confirmar" em caixa parcial     |
| Danger            | `bg-error`              | `text-on-error`     | Dismiss de erro, "Sem Saldo"     |
| Orange/Exception  | `bg-warehouse-orange`   | `text-white`        | "Sem Saldo" na tela de picking   |
| Ghost             | `border-2 border-outline` | `text-outline`    | Ações secundárias, "Cancelar"    |

Regras:
- Altura mínima: `h-14` (56dp) — obrigatório para uso com luvas
- `flex-1` quando em par no rodapé
- `rounded-lg` (8px) para botões de ação padrão; `rounded-xl` (12px) para CTAs de estado
- `text-body-lg font-bold` ou `text-label-lg font-bold uppercase tracking-widest`
- Sem ícones em botões de ação primária isolados; ícone + texto permitido em CTAs de estado

---

## Tela de Login

```tsx
<View className="flex-1 bg-background">
  {/* Header da empresa */}
  <View className="h-14 bg-primary items-center justify-center">
    <Text className="text-lg font-black text-white uppercase tracking-wider font-public-sans">
      Kyly Picking
    </Text>
  </View>

  <View className="flex-1 px-margin-mobile pt-8 gap-6">
    <Text className="text-headline-md font-semibold text-primary">Sistema de Picking</Text>
    <Text className="text-body-md text-on-surface-variant">Faça login para iniciar</Text>

    {/* Campo 1 — supervisor */}
    <Text className="text-label-lg text-primary uppercase tracking-tight font-bold">
      1. Bipe o código do Supervisor
    </Text>
    <View className="h-14 bg-surface-container-low border border-outline-variant rounded items-center justify-center">
      <Text className="text-body-md text-on-surface-variant">Aguardando bipagem...</Text>
    </View>

    {/* Campo 2 — crachá operador */}
    <Text className="text-label-lg text-on-surface-variant uppercase tracking-tight font-bold">
      2. Bipe o seu crachá
    </Text>
    <View className="h-14 bg-surface-container-low border border-outline-variant rounded items-center justify-center">
      <Text className="text-body-md text-outline-variant">Aguardando crachá...</Text>
    </View>
  </View>

  <View className="p-4">
    <TouchableOpacity className="w-full h-[60px] bg-primary-container rounded-xl items-center justify-center">
      <Text className="text-title-lg font-semibold text-on-primary">Entrar</Text>
    </TouchableOpacity>
  </View>
</View>
```

---

## Modais de Feedback de Estado

Implementados como telas (`FinalizacaoScreen`) ou `Modal` absoluto fullscreen.

| Evento              | Background               | Ícone                | Cor do título          | Botão CTA                    |
| ------------------- | ------------------------ | -------------------- | ---------------------- | ---------------------------- |
| Caixa finalizada    | `bg-background`          | `check_circle` verde | `text-success-industrial` | `bg-success-industrial`   |
| Picking parcial     | `bg-background`          | `warning` amarelo    | `text-warning-industrial` | `bg-warning-industrial`   |
| SKU incorreto       | `bg-background` + overlay| `close` vermelho     | `text-error`           | `bg-error`                   |
| Peça sem saldo      | `bg-background` + overlay| `close` vermelho     | `text-error`           | `bg-error`                   |

```tsx
// components/ErrorModal.tsx — SKU incorreto / sem saldo
<Modal visible={visible} animationType="fade" transparent>
  <View className="flex-1 bg-on-background/60 justify-end">
    <View className="bg-background rounded-t-3xl p-6 gap-6">
      {/* Ícone */}
      <View className="w-16 h-16 bg-error-container rounded-full items-center justify-center self-center">
        <MaterialSymbol name="close" size={40} color="#BA1A1A" weight={700} />
      </View>
      {/* Título */}
      <Text className="text-headline-md font-bold text-error text-center uppercase">
        {titulo}
      </Text>
      {/* Detalhe */}
      <Text className="text-body-md text-on-surface text-center">{mensagem}</Text>
      {/* CTA */}
      <TouchableOpacity
        className="w-full h-14 bg-error rounded-xl items-center justify-center"
        onPress={onConfirmar}
      >
        <Text className="text-title-lg font-bold text-white">Confirmar</Text>
      </TouchableOpacity>
    </View>
  </View>
</Modal>
```

```tsx
// screens/FinalizacaoScreen.tsx — caixa finalizada
<View className="flex-1 bg-background items-center justify-center px-margin-mobile gap-8">
  <View className="w-20 h-20 bg-success-industrial rounded-full items-center justify-center">
    <MaterialSymbol name="check_circle" fill size={56} color="white" />
  </View>
  <Text className="text-[32px] font-bold text-success-industrial leading-tight text-center">
    CAIXA FINALIZADA
  </Text>
  <Text className="text-body-lg text-on-surface-variant text-center">
    Todas as peças foram coletadas
  </Text>
  {/* Resumo */}
  <View className="w-full bg-surface-container-low rounded-lg p-4 gap-2 border border-outline-variant">
    <Text className="text-label-lg text-on-surface-variant uppercase">Caixa</Text>
    <Text className="text-headline-md font-semibold text-primary">{caixaCodigo}</Text>
    <Text className="text-title-lg text-success-industrial">{total} de {total} peças ✓</Text>
  </View>
  {/* CTA */}
  <TouchableOpacity className="w-full h-14 bg-success-industrial rounded-xl items-center justify-center">
    <Text className="text-headline-md font-semibold text-white">Confirmar</Text>
  </TouchableOpacity>
</View>
```

---

## Tela de Endereços Alternativos

Exibida quando o SKU está em falta no endereço primário. Lista de cards com endereços
alternativos, cada um com estoque disponível.

```tsx
<View className="w-full bg-white border-2 border-slate-200 rounded-lg p-4">
  <Text className="font-mono text-3xl font-bold text-primary-container tracking-wider">
    B15.03.2A
  </Text>
  <Text className="text-xs text-label-lg text-on-surface-variant uppercase mt-1">
    Corredor B15 • Seção 03 • Posição 2A
  </Text>
  <Text className="text-label-lg text-xs mt-2">12 peças disponíveis</Text>
  <TouchableOpacity className="w-full h-14 bg-secondary items-center justify-center rounded font-bold uppercase tracking-widest mt-3">
    <Text className="text-label-lg text-white font-bold uppercase">Usar Este Endereço</Text>
  </TouchableOpacity>
</View>
```

---

## Input Fields

- Estilo: **Outlined** com borda `border border-outline-variant` padrão, `border-2 border-primary` quando focado
- Altura mínima: `h-14` (56dp)
- Label sempre visível acima do campo (`text-label-lg text-primary uppercase`)
- `rounded` (4px) — mais estruturado, não arredondado como consumer apps
- Background: `bg-surface-container-low`

---

## Feedback de Hardware — Datalogic SDK

Tabela de referência para `src/utils/feedback.ts`.

| Evento                       | LED      | Som                    | Tela                                 |
| ---------------------------- | -------- | ---------------------- | ------------------------------------ |
| Peça válida (qtde pendente)  | Verde    | `beepShort()`          | Mantém endereço atual                |
| SKU completo (avança)        | Verde    | `beepDoubleShort()`    | Avança para próximo endereço         |
| SKU incorreto                | Vermelho | `beepContinuous(2000)` | Exibe `ErrorModal` (SKU incorreto)   |
| Peça sem saldo               | Vermelho | `beepContinuous(2000)` | Exibe `ErrorModal` (sem saldo)       |
| Caixa finalizada             | Verde    | `beepSuccess()`        | Navega para `FinalizacaoScreen`      |
| Picking parcial              | Amarelo  | `beepDoubleShort()`    | Navega para `FinalizacaoScreen`      |

```typescript
// src/utils/feedback.ts — inalterado da versão anterior
import { DatalogicSDK } from '@/native/DatalogicSDK'

type FeedbackEvent =
  | 'peca_valida' | 'sku_completo' | 'erro_sku'
  | 'sem_saldo'   | 'caixa_finalizada' | 'picking_parcial'

export function triggerFeedback(event: FeedbackEvent) {
  DatalogicSDK.clearLed()
  switch (event) {
    case 'peca_valida':
      DatalogicSDK.setLedGreen(); DatalogicSDK.beepShort(); break
    case 'sku_completo':
      DatalogicSDK.setLedGreen(); DatalogicSDK.beepDoubleShort(); break
    case 'erro_sku':
    case 'sem_saldo':
      DatalogicSDK.setLedRed(); DatalogicSDK.beepContinuous(2000); break
    case 'caixa_finalizada':
      DatalogicSDK.setLedGreen(); DatalogicSDK.beepSuccess(); break
    case 'picking_parcial':
      DatalogicSDK.setLedYellow(); DatalogicSDK.beepDoubleShort(); break
  }
}
```

---

## ScanField — Campo de Bipagem

Inalterado — o `TextInput` invisível captura leituras do scanner Datalogic Memor 11.

```tsx
// components/ScanField.tsx
import { TextInput } from 'react-native'
import { useDatalogic } from '@/hooks/useDatalogic'

interface ScanFieldProps { onScan: (barcode: string) => void }

export function ScanField({ onScan }: ScanFieldProps) {
  useDatalogic(onScan)
  return (
    <TextInput
      className="absolute opacity-0 w-0 h-0"
      autoFocus
      onSubmitEditing={(e) => onScan(e.nativeEvent.text)}
    />
  )
}
```

---

## Accessibility (Industrial Context)

- Contraste mínimo 4.5:1 entre texto e fundo (WCAG AA) — paleta MD3 já garante.
- Nenhum elemento interativo menor que `h-14` (56dp).
- Animações máximo 150ms. `active:scale-[0.98]` ou `active:scale-95` como feedback tátil.
- `accessibilityLabel` em todos os botões de ação.
- Labels sempre visíveis (nunca floating) para que o operador nunca perca o contexto.

---

## Spacing & Elevation

Grid de 8px. Margens externas de 16px (`margin-mobile`).

Profundidade via **Tonal Layers** (MD3) — sem sombras pesadas:
- **Nível 0 — Surface:** `#F7F9FC` — tela base
- **Nível 1 — Cards:** `bg-white border border-outline-variant rounded-lg` — itens de lista, SKU details
- **Nível 2 — Botões/Floating:** `shadow-md` + `bg-primary-container` ou cor semântica

Separação de cards e inputs: borda `1px #C3C6CF` (`outline-variant`) em vez de sombra,
garantindo visibilidade mesmo com brilho reduzido do scanner.
