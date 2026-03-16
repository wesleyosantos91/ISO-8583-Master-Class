# Semana 10 — Exercícios de Fixação
## Fluxo 0200/0210 Ponta a Ponta

---

## Exercício 1 — Fluxo de Mensagens (Nível: Iniciante)

Trace o fluxo completo de uma compra de R$ 89,90 com cartão Mastercard Nubank na maquininha Stone. Preencha a tabela:

| Etapa | Quem envia | Quem recebe | MTI | DE39 | O que muda em relação à etapa anterior |
|-------|-----------|-------------|-----|------|---------------------------------------|
| 1 | Terminal Stone | Switch Stone | 0200 | (vazio) | Mensagem inicial |
| 2 | Switch Stone | Switch Mastercard | | | |
| 3 | Switch Mastercard | Switch Nubank | | | |
| 4 | Switch Nubank | Switch Mastercard | 0210 | 00 | |
| 5 | Switch Mastercard | Switch Stone | | | |
| 6 | Switch Stone | Terminal Stone | | | |

**Responda também:**
1. Em qual etapa o MTI muda de `0200` para `0210`?
2. O DE39 existe em todas as mensagens? Por quê?
3. O DE38 (Auth Code) é gerado por quem e em qual etapa?

**Critério de sucesso:** Tabela preenchida corretamente, perguntas respondidas.

---

## Exercício 2 — Convertendo MTI (Nível: Iniciante)

Para cada MTI de request, qual é o MTI de response correspondente?

| Request MTI | Response MTI | Tipo de transação |
|-------------|-------------|-------------------|
| `0100` | | |
| `0200` | | |
| `0400` | | |
| `0420` | | |
| `0800` | | |

**Explique também:**
1. Qual a diferença entre `0100` e `0200`?
2. Qual a diferença entre `0400` e `0420`?
3. Quando você usaria `0100` em vez de `0200`?

**Critério de sucesso:** Todos os MTIs corretos, diferenças explicadas.

---

## Exercício 3 — ForwardToIssuer Completo (Nível: Intermediário)

Implemente `ForwardToIssuer.java` completo com base no exemplo da teoria, adicionando:

1. **Conversão de MTI:** se o request recebido é `0200` mas o emissor espera `0100`, converter antes de enviar
2. **Log estruturado** antes e depois da chamada ao emissor:
   ```
   [FORWARD] → stan=001234 terminal=TERM0042 amount=8990 destination=nubank-mux
   [FORWARD] ← stan=001234 response_code=00 auth_code=XK4729 duration_ms=134
   ```
3. **Timeout configurável** via propriedade XML (default 30000ms)
4. **Em caso de timeout:** coloca `NEEDS_REVERSAL=true` no Context
5. **Em caso de DE39=96 (system malfunction):** retry uma vez após 500ms

Implemente testes para: aprovação, decline, timeout, system malfunction com retry.

**Critério de sucesso:** Todos os cenários implementados e testados.

---

## Exercício 4 — IssuerSimulator (Nível: Intermediário)

Implemente `IssuerSimulator.java` que simula um emissor real com pelo menos **10 regras de decisão**:

```java
public class IssuerSimulator implements ISORequestListener {

    private String decide(String pan, long amountCents, ISOMsg msg) throws ISOException {
        // Implemente 10+ regras baseadas em PAN prefix e amount
    }
}
```

**Regras obrigatórias:**
| PAN prefix | Cenário simulado | DE39 retornado |
|------------|-----------------|----------------|
| `4000` | PAN inválido | `14` |
| `4100` | Sem saldo | `51` |
| `4200` | Cartão expirado | `54` |
| `4300` | PIN incorreto | `55` |
| `4400` | Cartão bloqueado | `05` |
| `4500` | Cartão furtado | `43` |
| `4600` | Limite excedido | `61` |
| `4700` | Emissor indisponível | `91` |
| `4800` | Transação suspeita de fraude | `59` |
| Qualquer outro | Aprovado | `00` |

**Requisitos adicionais:**
- Quando aprovado: gerar DE38 (Auth Code) com 6 chars alfanuméricos aleatórios
- Simular latência realista: responder em 50-200ms (aleatório, não constante)
- Logar cada decisão: `[ISSUER-SIM] pan=4100****0001 amount=150.00 decision=51 latency=87ms`

**Critério de sucesso:** Todas as 10 regras funcionando, latência simulada.

---

## Exercício 5 — Teste de Cada Response Code (Nível: Intermediário)

Implemente `IssuerSimulatorTest.java` com um teste para cada response code do `IssuerSimulator`:

```java
@ParameterizedTest
@MethodSource("responseCodeScenarios")
void issuerReturnsCorrectResponseCode(String pan, long amount, String expectedCode) {
    // TODO
}

static Stream<Arguments> responseCodeScenarios() {
    return Stream.of(
        Arguments.of("4000000000000000", 10000L, "14"),
        Arguments.of("4100000000000000", 10000L, "51"),
        // ... complete todos os 10 casos
    );
}
```

**Bonus:** Adicione um teste que verifica que PANs aprovados (`5000` prefix) com amount > R$ 10.000 retornam `61`.

**Critério de sucesso:** Todos os 10+ testes passando.

---

## Exercício 6 — Teste End-to-End (Nível: Avançado)

Implemente `AuthorizationE2ETest.java` que simula o fluxo completo sem mocks:

```
Terminal (test) → Switch (real) → IssuerSimulator (real)
```

O teste deve:
1. Iniciar o Q2 em memória (sem porta TCP)
2. Colocar uma mensagem 0200 no Space do TransactionManager
3. Aguardar a response no Space (timeout 5s)
4. Verificar DE39 da response
5. Verificar que o AuditLog foi gerado com PAN mascarado

**Cenários a testar:**
- Happy path: PAN aprovado → DE39=`00`, DE38 presente
- Cartão sem saldo: DE39=`51`, DE38 ausente
- Timeout do emissor: DE39=`68`, `NEEDS_REVERSAL=true` no Context

**Critério de sucesso:** 3 cenários passando end-to-end sem mocks externos.

---

## Exercício 7 — Métricas em Tempo Real (Nível: Avançado)

Implemente `MetricsParticipant.java` que usa `NO_JOIN` e coleta:

```java
public class MetricsParticipant implements TransactionParticipant {

    private final AtomicLong totalTransactions = new AtomicLong();
    private final AtomicLong approvedTransactions = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> responseCodeCount = new ConcurrentHashMap<>();
    private final LatencyHistogram latencyHistogram = new LatencyHistogram(100); // últimos 100

    // prepare(): coleta métricas do Context e atualiza contadores
    // Métricas: total, aprovadas, taxa aprovação, top 5 response codes, P50/P95/P99
}
```

Implemente `MetricsEndpoint.java` que expõe as métricas via `toString()`:

```
=== Métricas ISO 8583 — últimos 60s ===
Total transações:      1.234
Taxa de aprovação:     87.3%
Latência P50:          47ms
Latência P95:          312ms
Latência P99:          891ms

Top 5 Response Codes:
  00 (Approved):            1.076 (87.2%)
  51 (Insuf. Funds):           89  (7.2%)
  05 (Do not honor):           38  (3.1%)
  54 (Expired card):           18  (1.5%)
  91 (Issuer unavailable):     13  (1.1%)
```

**Critério de sucesso:** Métricas corretas, formatação legível, thread-safe.
