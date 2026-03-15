# Semana 29 — Desafio: O Pico de Chargebacks

## O Cenário

É segunda-feira, 09h. Você recebe um alerta do sistema de monitoramento:

```
[ALERTA CRÍTICO] Chargeback ratio atingiu 1.2% em novembro
Threshold: 0.9% (Standard VDMP)
Merchant afetado: ID 9876543 — "TechStore Online"
Volume nov: 12.400 transações
Chargebacks nov: 149
Prazo para representment de lote inicial: 72 horas
```

A TechStore Online é um dos seus top 10 merchants por volume. Você tem acesso aos dados de transação e ao histórico de chargebacks.

---

## Dados Disponíveis

### Breakdown dos 149 chargebacks:

| Reason Code | Quantidade | Valor Total |
|-------------|-----------|-------------|
| 10.4 (CNP Fraud) | 89 | R$ 142.400 |
| 13.1 (Não recebido) | 38 | R$ 45.600 |
| 12.6 (Duplicata) | 14 | R$ 16.800 |
| 13.2 (Recorrência cancelada) | 8 | R$ 7.200 |

### Informações adicionais:
- Os 14 chargebacks de duplicata (12.6) ocorreram todos no dia 03/11, entre 14h e 15h
- Os 89 de fraude CNP: 67 deles não usaram 3DS
- Os 38 de "não recebido": 22 são de um único produto (notebook modelo X)
- Os 8 de recorrência cancelada: o merchant migrou de plataforma em outubro

---

## Sua Missão

### Parte 1 — Diagnóstico Técnico

1. **O incidente de duplicata do dia 03/11:** Que componente técnico provavelmente falhou? Quais logs você vai analisar primeiro?

2. **Os 67 chargebacks de fraude sem 3DS:** O merchant tem 3DS configurado. Por que transações passaram sem 3DS? Liste 3 hipóteses técnicas.

3. **Os 22 chargebacks do notebook modelo X:** Qual é a hipótese mais provável (fraude organizada, problema logístico, friendly fraud)?

4. **Os 8 de recorrência:** A migração de plataforma em outubro provavelmente quebrou qual processo técnico?

### Parte 2 — Defesa Imediata (72 horas)

Para cada lote de chargebacks, decida:
- Quais vale a pena defender (representment)?
- Qual documentação o merchant precisa fornecer?
- Qual a probabilidade estimada de ganhar?

| Lote | Defender? | Documentação necessária | Probabilidade de ganho |
|------|-----------|------------------------|----------------------|
| 10.4 (com 3DS) | | | |
| 10.4 (sem 3DS) | | | |
| 13.1 (notebooks) | | | |
| 12.6 (duplicatas) | | | |
| 13.2 (recorrência) | | | |

### Parte 3 — Plano de Ação

Elabore um plano de ação com:

1. **Correções imediatas** (próximas 24h) — o que precisa ser corrigido no switch/plataforma agora?

2. **Melhorias de curto prazo** (próximos 30 dias) — o que o merchant precisa implementar?

3. **Monitoramento** — quais alertas você vai criar para detectar picos antes de virar VDMP?

4. **Comunicação** — escreva um e-mail de 10 linhas para o merchant explicando a situação sem entrar em pânico.

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Diagnóstico correto do incidente de duplicata | 20 |
| Análise de 3DS e hipóteses técnicas | 20 |
| Decisão correta de representment por lote | 25 |
| Plano de ação coerente e priorizado | 25 |
| Comunicação clara e profissional | 10 |

**Tempo sugerido:** 90 minutos
