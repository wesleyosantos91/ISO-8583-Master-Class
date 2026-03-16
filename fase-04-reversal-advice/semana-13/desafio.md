# Semana 13 — Desafio Integrador

## O Cenário

Você é engenheiro sênior no time de switch de um grande adquirente brasileiro. Na sexta-feira às 23h47, o sistema de monitoramento dispara um alerta crítico:

> **REVERSAL_EXHAUSTED — STAN=987654 — Terminal=TERM00312 — Merchant=PADARIA_CENTRAL_SP**
>
> "Reversal esgotou 3 tentativas. Transação original: 0200, R$ 245,00, DE39 timeout. Portador pode estar sendo cobrado indevidamente."

Ao mesmo tempo, chega outra mensagem no canal de operações:

> "Cliente do cartão final 3366 ligou reclamando que foi cobrado R$ 245,00 em uma compra que ele diz que não foi feita. O terminal exibiu 'Transação não aprovada' mas a fatura mostra a cobrança."

Esse é exatamente o cenário de reversal esgotado: o emissor aprovou a transação, mas o switch não recebeu a resposta a tempo, enviou reversal três vezes sem sucesso, e agora o portador está com a cobrança.

## Sua Missão

### Parte 1 — Diagnóstico Imediato (25 min)

Escreva `diagnostico-s13.md` respondendo:

1. **Reconstrua a linha do tempo exata** do que aconteceu entre o switch, o emissor e o terminal. Use um diagrama de sequência Mermaid com timestamps (mesmo que aproximados).

2. **Por que o reversal falhou 3 vezes?** Liste pelo menos 4 causas possíveis (técnicas e operacionais).

3. **Qual é o estado atual do sistema?**
   - O portador foi cobrado ou não? Como você sabe?
   - O limite do cartão foi consumido ou não?
   - Quem tem evidência do quê?

4. **Quais logs você precisaria consultar agora?** Liste com precisão: sistema, campo, valor esperado.

### Parte 2 — Resolução do Incidente (25 min)

Escreva `resolucao-s13.md` com:

1. **Ação imediata (próximos 15 minutos):**
   - Como confirmar junto ao emissor se a transação foi de fato processada?
   - O que dizer ao portador agora?
   - Há alguma ação técnica de emergência possível (ex: envio manual de reversal fora do sistema)?

2. **Implemente** (ou descreva detalhadamente o código de) uma operação administrativa `forceReversal(String stan, String terminalId)` que:
   - Busca a transação original no banco de dados pelo STAN + terminal
   - Constrói o reversal manualmente
   - Envia via canal de emergência (sem passar pelo engine automático)
   - Registra a tentativa manual com: operador, timestamp, resultado

3. **Critérios para considerar o incidente encerrado:**
   - O que precisa acontecer para você declarar que o portador não será cobrado?
   - Qual evidência você precisa guardar?

### Parte 3 — Prevenção (10 min)

Escreva `prevencao-s13.md` com:

1. **Por que o sistema atual não é suficiente?** O que falta para garantir zero cobranças indevidas por reversals esgotados?

2. **Proponha 3 melhorias** com estimativa de esforço (P, M, G):
   - Uma melhoria técnica no engine de reversal
   - Uma melhoria no processo operacional
   - Uma melhoria no monitoramento/alertas

3. **SLA proposto:** Após sua melhoria, qual seria a taxa esperada de cobranças indevidas por reversals esgotados? Justifique.

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Linha do tempo correta e coerente com o protocolo ISO 8583 | /20 |
| Diagnóstico cobre causas técnicas (TCP, timeout, emissor) | /20 |
| Ação imediata é realista e protege o portador | /20 |
| Código de `forceReversal` ou descrição técnica detalhada | /20 |
| Melhorias propostas são viáveis e priorizadas | /20 |

**Meta:** 80+ pontos = Semana 13 dominada.

---

## Dicas

- O portador ligou reclamando: isso é evidência de que o emissor processou a transação original (DE39=00 chegou ao emissor, mas não ao switch).
- Reversal esgotado não significa que o portador foi cobrado — mas é o cenário mais provável.
- Em produção, esse tipo de incidente exige ação em menos de 2 horas para evitar chargeback.
- Cronometre. Em produção, esse incidente acontece às 23h47 de sexta-feira mesmo.
