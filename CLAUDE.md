## Mobile — Application Building Context

Este é o aplicativo mobile do Sistema de Picking Kyly.
**Native Android (Jetpack Compose + Kotlin)** rodando no Datalogic Memor 11
(Android 11, tela 5", scanner 2D integrado).
Utilizado exclusivamente pelos operadores de picking no galpão de expedição.

O acesso ao hardware do Datalogic (scanner, LEDs, buzzer) é feito diretamente
via **Datalogic SDK** (`datalogic-sdk.aar`) integrado ao Gradle — sem bridge,
sem overhead de runtime externo. O scanner emite eventos via `BarcodeReadListener`
capturados por `DatalogicManager`, um singleton Hilt que expõe um `SharedFlow<String>`
consumido pelos ViewModels das telas de bipagem.

Read the following files in order before implementing
or making any architectural decision:

1. `context/project-overview.md` — papel do app mobile no sistema,
   telas, fluxos operacionais e perfil do usuário (operador)
2. `context/architecture.md` — stack Kotlin/Compose, estrutura de pastas,
   integração Datalogic SDK e estratégia offline/sync
3. `context/ui-context.md` — tema Material 3 claro, tipografia Public Sans,
   feedback LED/som e padrões de layout para tela de 5"
4. `context/code-standards.md` — padrões Kotlin, Compose, Hilt, Room,
   Retrofit e uso do Datalogic SDK
5. `context/ai-workflow-rules.md` — workflow de desenvolvimento,
   regras de escopo e critérios de entrega
6. `context/progress-tracker.md` — fase atual, trabalho concluído,
   próximos passos e decisões arquiteturais

Update `context/progress-tracker.md` after each
meaningful implementation change.

If implementation changes the architecture, scope, or
standards documented in the context files, update the
relevant file before continuing.
