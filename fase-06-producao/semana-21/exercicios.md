# Semana 21 — Exercícios de Fixação

## Exercício 1 — Os 4 Sinais de Observabilidade (Nível: Iniciante)

No contexto de um payment switch, preencha a tabela com exemplos concretos para cada sinal:

| Sinal | Definição | Exemplo em Payment Switch | Ferramenta Comum |
|-------|-----------|--------------------------|-----------------|
| Métricas | | | |
| Logs | | | |
| Traces | | | |
| Alertas | | | |

Depois responda:
1. Por que latência P95 é mais útil que latência média para um switch de pagamentos?
2. Qual é a diferença entre uma métrica e um log?
3. Se a taxa de aprovação cai de 88% para 60%, qual é o primeiro log/métrica que você consultaria?

**Critério de sucesso:** Tabela preenchida com exemplos específicos de pagamentos, 3 perguntas respondidas.

---

## Exercício 2 — Implemente SwitchMetrics (Nível: Iniciante/Intermediário)

Implemente `SwitchMetrics` usando Micrometer com as métricas obrigatórias:

```java
public class SwitchMetrics {
    private final MeterRegistry registry;

    // LATÊNCIA: Timer por MTI + rota + resultado
    public void recordLatency(String mti, String route, boolean approved, long ms) { /* ... */ }

    // VOLUME: Counter por MTI + RC + rota
    public void recordTransaction(String mti, String responseCode, String route) { /* ... */ }

    // TIMEOUT: Counter por MTI + destino
    public void recordTimeout(String mti, String destination) { /* ... */ }

    // REVERSAL: Counter por resultado
    public void recordReversal(boolean success) { /* ... */ }

    // DUPLICATA: Counter
    public void recordDuplicate() { /* ... */ }

    // CONEXÕES ATIVAS: Gauge por destino
    public void registerConnectionGauge(String destination, AtomicInteger count) { /* ... */ }

    // TAXA DE APROVAÇÃO: Calculada a partir dos counters
    public double getApprovalRate(String route) { /* ... */ }
}
```

Escreva testes que verificam:
1. Após 100 transações aprovadas e 15 negadas: `getApprovalRate()` = 86.96%
2. Timer registrado com tag correta (`mti`, `route`, `result`)
3. Counter de timeout incrementado após timeout

**Critério de sucesso:** Métricas implementadas, testes passam, tags corretas em cada métrica.

---

## Exercício 3 — Logging Estruturado em Todos os Participants (Nível: Intermediário)

Adicione logging estruturado ao seu `payment-switch-lab` nos seguintes pontos:

**Em cada TransactionParticipant, ao iniciar o `prepare`:**
```
txn.participant.start participant=ValidateMessage mti=0200 stan=123456
```

**Ao finalizar com sucesso:**
```
txn.participant.complete participant=ValidateMessage mti=0200 stan=123456 duration_ms=2
```

**Ao falhar:**
```
txn.participant.failed participant=ValidateMessage mti=0200 stan=123456 reason=MISSING_FIELD_2
```

**No início e fim da transação completa:**
```
txn.start mti=0200 stan=123456 pan=453201******0366 amount=015000 terminal=TERM001
txn.complete mti=0200 stan=123456 rc=00 route=VISA duration_ms=45 approved=true duplicate=false
```

Regras:
- NUNCA logar PAN completo (sempre mascarado)
- NUNCA logar PIN, track data
- Sempre incluir STAN para correlacionar logs da mesma transação
- Usar MDC (Mapped Diagnostic Context) para correlação automática

Verifique com testes automatizados que: PAN mascarado, STAN presente, duração registrada.

**Critério de sucesso:** Logging em todos os participants, PAN nunca exposto, testes verificando o formato.

---

## Exercício 4 — Queries de Investigação (Nível: Intermediário)

Crie um arquivo `investigation-queries.md` com queries (simulando Elasticsearch/SQL) para investigar os seguintes cenários:

**Query 1:** Taxa de aprovação nos últimos 30 minutos, por bandeira
```
# Pseudoquery: buscar logs txn.complete dos últimos 30min,
# agrupar por route, calcular approved/total
```

**Query 2:** Transações com latência > 1000ms nas últimas 1 hora, agrupadas por destino

**Query 3:** STANs que aparecem mais de 1 vez nos logs (detectar duplicatas ou retransmissões)

**Query 4:** Transações que geraram reversal, com: STAN original, motivo, resultado do reversal

**Query 5:** Merchants com maior taxa de recusa (DE39 != 00) nas últimas 24h

Para cada query:
- Escreva em pseudocódigo SQL ou Elasticsearch DSL
- Indique quais campos de log são usados
- Descreva o que a resposta deve mostrar

**Critério de sucesso:** 5 queries escritas, campos de log usados coerentes com o logging implementado.

---

## Exercício 5 — Implemente runbook-observability.md (Nível: Intermediário)

Escreva `runbook-observability.md` com:

**Seção 1: Métricas monitoradas**
| Métrica | Threshold Normal | Threshold Alerta | Threshold Crítico | Ação |
|---------|-----------------|------------------|-------------------|------|
| Latência P95 on-us | < 150ms | 150-300ms | > 300ms | |
| Taxa de aprovação | > 85% | 75-85% | < 75% | |
| Taxa de timeout | < 0.1% | 0.1-0.5% | > 0.5% | |
| Taxa de reversal | < 0.5% | 0.5-2% | > 2% | |

**Seção 2: Investigação de incidentes**
Para cada sintoma, defina o processo de investigação em 5 passos:
- "Taxa de aprovação caiu abruptamente"
- "Latência P95 subiu para 2000ms"
- "Canal off-us Visa com 100% timeout"

**Seção 3: Alertas configurados**
Lista de todos os alertas com: condição, severidade, destinatário, ação esperada.

**Critério de sucesso:** Runbook completo, thresholds baseados nos SLAs da teoria, ações específicas para cada cenário.

---

## Exercício 6 — Dashboard: Detectar a Queda de Aprovação (Nível: Avançado)

**Cenário simulado:** A taxa de aprovação caiu de 87% para 62% em 10 minutos.

Construa um "dashboad em log" — uma classe que a cada 60 segundos imprime um snapshot do estado:

```
=== SWITCH HEALTH SNAPSHOT 2026-03-15T14:30:00Z ===
Aprovações (últimos 5min):   247/284 = 87.0% [OK]
Latência P95 (on-us):        45ms [OK]
Latência P95 (off-us/Visa):  1420ms [ALERTA]
Timeouts (últimos 5min):     12/284 = 4.2% [CRITICO]
Reversals pendentes:         8 [ALERTA]
Canal VISA:                  DEGRADED (echo latency 820ms)
Canal MASTERCARD:            UP
Canal ELO:                   UP
```

Implemente `HealthSnapshot` que:
1. Agrega métricas dos últimos 5 minutos
2. Classifica cada métrica como OK/ALERTA/CRÍTICO
3. Identifica a causa mais provável da degradação (ex: se timeout alto + canal degradado = provavelmente o canal é a causa)

Escreva um teste que simula 10 minutos de dados com a degradação descrita e verifica que o snapshot final mostra o status correto.

**Critério de sucesso:** Dashboard implementado, causa raiz da degradação identificada automaticamente.

---

## Exercício 7 — Análise de SLA: Quanto Downtime é Permitido? (Nível: Avançado)

O SLA contratual exige disponibilidade de 99.99% ao ano.

Calcule:
1. Quantos minutos de downtime são permitidos por ano com 99.99%?
2. Quantos minutos por mês?
3. Quantos minutos por semana?
4. Se ocorrer um incidente de 45 minutos, qual é o percentual de SLA consumido no mês?
5. O payment switch processa 500 TPS no pico. Se ficar offline por 45 minutos no pico: quantas transações são perdidas? Qual o valor estimado (ticket médio R$ 120)?

Documente em `sla-calculations.md` com os cálculos e uma análise de: vale a pena ter redundância ativa-ativa (99.999%) vs ativa-passiva (99.99%)?

**Critério de sucesso:** Cálculos corretos, análise de trade-off entre disponibilidade e custo.
