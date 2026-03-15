# Desafio — Semana 31 — Pico de Fraude em Black Friday

## Contexto

Você é engenheiro sênior no time de risco de uma adquirente de médio porte.
É sexta-feira, 23h30. A Black Friday está no pico. O volume está em 3.200 TPS —
o maior da história da empresa.

O sistema de alertas dispara:

```
[ALERTA CRÍTICO] Taxa de fraude: 2.1% (baseline: 0.12%)
[ALERTA] 847 transações com DE44 score > 80 aprovadas na última hora
[ALERTA] IP range 45.32.x.x: 1.240 transações em 10 minutos (12 cartões diferentes)
[ALERTA] Terminal TID-00291847: 38 cartões únicos em 5 minutos
```

Você precisa agir agora, com o switch em produção e sem parar o processamento.

## O Problema

A análise preliminar mostra dois vetores simultâneos:

**Vetor 1 — BIN attack em e-commerce:**
- IP range 45.32.0.0/16 (datacenter na Holanda) gerando transações CNP
- PANs com BIN 453977 sendo testados em sequência
- Valores entre R$ 1,00 e R$ 9,99 (card testing clássico)
- 92% das transações retornam RC `05` (Do Not Honor) — evidência de que os PANs são inválidos

**Vetor 2 — Terminal comprometido (possível skimmer):**
- Terminal TID-00291847, localizado em posto de gasolina em Guarulhos
- 38 cartões únicos em 5 minutos, todos com POS Entry Mode `02` (tarja magnética)
- 100% das transações aprovadas pelo emissor
- MCC 5541 (posto de gasolina) — coerente com o terminal

## Missão

### Parte 1 — Contenção Imediata (simule as ações no código)

Implemente as regras de contenção emergenciais no `VelocityEngine`:

```java
// Regra emergencial A: bloquear IP range de datacenter
// Regra emergencial B: BIN attack — limitar transações do BIN 453977 a 2 por minuto
// Regra emergencial C: terminal com múltiplos cartões — threshold de 5 para 2 cartões únicos/5min
public VelocityResult checkEmergency(String pan, String ip,
                                      String terminalId, BigDecimal amount) {
    // Implemente aqui as três regras emergenciais
    // Use os métodos countInWindow() e as estruturas Redis já existentes
}
```

### Parte 2 — Análise e Relatório

Responda às seguintes perguntas com base nos dados disponíveis:

1. O Vetor 2 (terminal com skimmer) é de maior ou menor urgência que o Vetor 1?
   Justifique com base no impacto financeiro esperado de cada um.

2. Para o Vetor 2, além de bloquear o terminal, qual ação o time de risco deve tomar
   em relação aos 38 cartões que foram usados naquele terminal nos últimos 5 minutos?

3. O alerta menciona "847 transações com DE44 score > 80 aprovadas". Por que o sistema
   aprovou essas transações mesmo com score alto? O que isso indica sobre a configuração
   atual do emissor?

4. Considerando que estamos em Black Friday (alto volume legítimo de viagens e compras
   atípicas), como você calibraria os thresholds de score para minimizar falsos positivos
   sem deixar o Vetor 1 passar?

### Parte 3 — Post-Mortem (documentação)

Escreva um post-mortem resumido com os campos:

```markdown
## Post-Mortem: Incidente de Fraude Black Friday [DATA]

### Resumo Executivo
[2-3 frases para a diretoria]

### Linha do Tempo
- HH:MM — Primeiro alerta disparado
- HH:MM — Contenção aplicada
- HH:MM — Vetores neutralizados

### Causa Raiz
[Vetor 1 e Vetor 2 separadamente]

### Impacto Financeiro Estimado
[Valor em risco, chargebacks esperados]

### Ações Corretivas
[O que muda no sistema para evitar recorrência]
```

## Critérios de Avaliação

- [ ] Regras emergenciais implementadas corretamente usando Redis (Parte 1)
- [ ] Diferencia os dois vetores pelo perfil de risco e impacto (Parte 2, Q1)
- [ ] Identifica a necessidade de bloquear os 38 cartões do terminal comprometido (Parte 2, Q2)
- [ ] Explica o papel do emissor na aprovação com DE44 score alto (Parte 2, Q3)
- [ ] Propõe calibração de threshold sensível ao contexto Black Friday (Parte 2, Q4)
- [ ] Post-mortem com causa raiz técnica precisa e ações corretivas acionáveis (Parte 3)

## Dicas

- O BIN attack (Vetor 1) gera volume mas baixo impacto financeiro (valores < R$ 10)
- O skimmer (Vetor 2) gera menos volume mas alto impacto por transação (compras reais)
- `NegativeListService.blockTerminal(terminalId)` persiste o bloqueio no Redis com TTL
- O DE 44 na response vem da bandeira — o emissor pode ignorar ou agir sobre ele
- Em Black Friday, priorize regras que bloqueiam padrões automatizados (bot) sem afetar humanos
