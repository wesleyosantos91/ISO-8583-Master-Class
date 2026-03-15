# Semana 12 — Desafio Integrador
## "Black Friday: O Switch Sob Pressão"

---

## O Cenário

É 00:01 da Black Friday. O volume de transações quintuplicou em relação a uma sexta normal. O switch da FinTechBR está processando 15.000 transações/minuto — o dobro do pico anterior de 7.500/minuto.

Em 8 minutos, o monitoramento dispara quatro alertas simultâneos:

**ALERTA 1 — 00:09 — Crítico**
> Taxa de timeout subiu de 0.3% para 18.7% nos últimos 2 minutos.
> Emissor afetado: todos os emissores Mastercard.

**ALERTA 2 — 00:09 — Crítico**
> Thread pool do QMUX `mastercard-mux` em 100% de capacidade.
> Aguardando: 847 requisições pendentes.

**ALERTA 3 — 00:10 — Alto**
> Fila de reconciliação: 1.420 entradas (era 12 às 23:00).
> Auto-reversals com timeout estão sendo enfileirados mais rápido que são processados.

**ALERTA 4 — 00:11 — Alto**
> Taxa de aprovação caiu de 88% para 71%.
> Top decline: DE39=68 (17.0%), DE39=51 (8.2%), DE39=00 aprovadas (71.0%)

**Dados adicionais que você coleta:**
```
mastercard-mux timeout configurado: 30s
mastercard-mux max-threads: 128

Latência da Mastercard (P99) nas últimas horas:
  22:00 → 23:59: P99 = 380ms (normal)
  00:00 → 00:05: P99 = 2.800ms (lento)
  00:06 → 00:09: P99 = 31.000ms (timeout!)

Mensagem do canal TCP: Connection alive, sem erros de rede.
A Mastercard está respondendo — mas muito devagar.
```

---

## Sua Missão

### Parte 1 — Diagnóstico Técnico (25 min)

Crie `diagnostico-blackfriday.md`:

1. **Qual é o root cause imediato?** Por que o timeout de 30s está sendo atingido se a Mastercard está viva?

2. **Efeito cascata — explique a cadeia:**
   - Volume alto → latência Mastercard aumenta → ??? → thread pool cheio → ??? → mais timeouts → ???
   - Desenhe como um diagrama de causa e efeito.

3. **Por que o thread pool ficou em 100%?**
   - Com timeout de 30s e 15.000 transações/minuto, quantas threads simultâneas são necessárias no pior caso?
   - Com 128 threads disponíveis, qual o máximo de transações/minuto que o switch aguenta? (mostre a conta)

4. **O que está enchendo a fila de reconciliação tão rápido?**
   - Auto-reversals com timeout: por que eles também têm timeout?
   - Por que estão sendo enfileirados mais rápido do que processados?

5. **Impacto financeiro:**
   - Com 15.000 transações/minuto, ticket médio R$ 130, 18.7% de timeout:
   - Quantas transações/minuto estão sendo perdidas?
   - Valor por minuto em transações perdidas?
   - Se durar 20 minutos até resolver, qual o total?

### Parte 2 — Ações de Emergência e Solução (45 min)

**Ação imediata (menos de 5 minutos para implementar):**

Implemente `TimeoutCircuitBreaker.java` que funciona como um circuit breaker para o QMUX:

```java
public class TimeoutCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long openedAt = 0;

    // Configurável:
    private final int failureThreshold = 10;    // Abre após 10 timeouts consecutivos
    private final long openDurationMs = 60_000; // Fica aberto por 60s
    private final int halfOpenMaxRequests = 5;  // Testa com 5 requests em half-open

    /**
     * Retorna true se deve tentar enviar para o emissor.
     * Retorna false se o circuit breaker está ABERTO (falha rápida).
     */
    public boolean allowRequest() { ... }

    /**
     * Registra sucesso → decrementa contadores, pode fechar o circuito
     */
    public void recordSuccess() { ... }

    /**
     * Registra falha (timeout) → incrementa contador, pode abrir o circuito
     */
    public void recordFailure() { ... }
}
```

**Quando o circuit breaker estiver ABERTO:**
- Retornar DE39=`91` (Issuer unavailable) imediatamente, sem esperar 30s
- Logar: `[CIRCUIT-BREAKER] OPEN — failing fast for mastercard-mux`
- **Não gerar reversal** (não houve envio, não há o que reverter)

Implemente testes para: circuit abre após threshold, falha rápida quando aberto, half-open testa o emissor.

**Solução de médio prazo — timeout adaptativo:**

Implemente `AdaptiveTimeout.java`:

```java
public class AdaptiveTimeout {

    private final long minTimeoutMs = 5_000;  // mínimo 5s
    private final long maxTimeoutMs = 30_000; // máximo 30s
    private final LatencyTracker tracker;

    /**
     * Retorna o timeout recomendado baseado na latência recente do emissor.
     * Formula: max(minTimeout, min(maxTimeout, P95_latency * 1.5))
     */
    public long getRecommendedTimeout(String muxName) { ... }
}
```

Explique: por que durante a Black Friday o timeout fixo de 30s é um problema? Como o timeout adaptativo resolveria?

**Relatório de dimensionamento:**

Escreva `dimensionamento-blackfriday.md` respondendo:

1. Para suportar 15.000 transações/minuto com timeout de 30s e P99 de latência de 2s:
   - Quantas threads o QMUX precisaria no mínimo?
   - Como calcular: `min_threads = TPS × timeout_seconds`?

2. Se o P99 de latência subir para 10s (emissores sobrecarregados):
   - Quantas threads seriam necessárias?
   - Como o circuit breaker mitiga esse crescimento?

3. Qual a capacidade máxima do switch em transações/minuto, dado:
   - 128 threads por QMUX
   - 3 QMUXes (Visa, Mastercard, on-us)
   - Timeout de 30s
   - P99 latência normal: 500ms

### Parte 3 — Post-Mortem e Capacidade Planejada (10 min)

Escreva `post-mortem-blackfriday.md` com:

1. **5 Ws:** What, When, Who, Where, Why

2. **Contributing factors** (fatores que contribuíram, não causa única):
   - Thread pool subdimensionado
   - Timeout fixo
   - Sem circuit breaker
   - Fila de reconciliação sem rate limit

3. **Action items com responsável e prazo:**
   - Imediato (até amanhã)
   - Curto prazo (até o próximo final de semana)
   - Médio prazo (antes da Cyber Monday)

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Diagnóstico: cálculo correto de threads necessárias | /20 |
| Diagnóstico: explica o efeito cascata completamente | /15 |
| Circuit breaker: estados CLOSED/OPEN/HALF_OPEN corretos | /25 |
| Circuit breaker: testes cobrindo os 3 estados | /15 |
| Relatório de dimensionamento com contas corretas | /15 |
| Post-mortem focado em processo, não em culpa | /10 |

**Meta:** 80+ pontos = Semana 12 dominada. Fase 3 concluída.

---

## Dicas

- A fórmula `threads = TPS × timeout` é uma simplificação, mas é válida para o pior caso. Se você tem 250 transações/segundo e cada uma pode ficar pendente por 30s, no pior caso você tem 7.500 transações simultaneamente aguardando resposta — e precisa de 7.500 threads disponíveis.
- O circuit breaker é a diferença entre um sistema que degrada graciosamente e um sistema que cai. Sem circuit breaker, um emissor lento derruba o seu switch. Com circuit breaker, o seu switch continua funcionando para os outros emissores enquanto o lento se recupera.
- **Fail fast is sometimes more humane than waiting.** Um DE39=`91` em 10ms é melhor para o cliente do que um DE39=`68` após 30 segundos. O portador sabe imediatamente que precisa tentar outro cartão.
- Black Friday é o evento de maior risco do ano para pagamentos. Empresas sérias fazem simulações de carga (load testing) com 3x o pico esperado e revisam todos os timeouts e pools antes. Se esse exercício foi difícil, o sistema real vai ser ainda mais desafiador.
