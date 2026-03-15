# Semana 33 — Performance e Escala: 5.000+ TPS em Produção

## Por que performance é um tema de especialista?

Um switch que processa 100 TPS é muito diferente de um que processa 5.000 TPS. Nessa escala, decisões que pareciam triviais — tamanho do pool de conexões, timeout de socket, GC do JVM — se tornam críticas. Especialistas de referência sabem onde o gargalo está **antes** de entrar em produção.

---

## 1. Baseline de Performance para um Switch

### 1.1 SLAs de referência de mercado

| Métrica | Básico | Bom | Excelente |
|---------|--------|-----|-----------|
| Latência P50 (on-us) | < 50ms | < 20ms | < 10ms |
| Latência P95 (on-us) | < 150ms | < 80ms | < 50ms |
| Latência P99 (on-us) | < 500ms | < 200ms | < 100ms |
| Latência P95 (off-us) | < 500ms | < 300ms | < 200ms |
| Throughput | 500 TPS | 2.000 TPS | 10.000 TPS |
| Disponibilidade | 99.9% | 99.99% | 99.999% |

### 1.2 Onde está o tempo numa transação

```
Decomposição de latência de uma transação off-us (P95 = 350ms):

  5ms   Deserialização ISO 8583 (parse da mensagem)
  2ms   Validação de campos obrigatórios
  3ms   Velocity check / fraud screening (Redis)
  2ms   Lookup de BIN table (cache em memória)
  1ms   Enfileiramento no QMUX
280ms   Round-trip até emissor (rede + processamento remoto)  ← DOMINANTE
  3ms   Deserialização da response
  2ms   Logging + métricas
  2ms   Serialização da response
────────
300ms  Total
```

A latência do emissor domina. O switch bem otimizado faz tudo o que pode em < 20ms.

---

## 2. jPOS Performance Tuning

### 2.1 Pool de conexões — configuração crítica

```xml
<!-- deploy/10_channel.xml -->
<server name="main-server" class="org.jpos.q2.iso.QServer">
  <attr name="port">8000</attr>
  <attr name="minSessions">50</attr>    <!-- conexões mínimas prontas -->
  <attr name="maxSessions">500</attr>   <!-- máximo de conexões simultâneas -->
  <attr name="timeout">30000</attr>     <!-- timeout de socket: 30s -->
</server>

<!-- Para conexão outbound (adquirente → bandeira) -->
<channel name="visa-channel" class="org.jpos.iso.channel.NACChannel">
  <attr name="host">visa-host.com</attr>
  <attr name="port">8000</attr>
  <attr name="timeout">30000</attr>
  <attr name="connect-timeout">5000</attr>  <!-- timeout de conexão TCP -->
</channel>
```

### 2.2 QMUX — configuração para alto volume

```xml
<qmux name="visa-mux">
  <attr name="channel">visa-channel</attr>
  <attr name="ready-timeout">10000</attr>   <!-- aguarda sign-on por 10s -->
  <attr name="max-outstanding">5000</attr>  <!-- máximo de requests pendentes -->
</qmux>
```

### 2.3 TransactionManager — thread pool

```xml
<txnmgr name="txnmgr" logger="Q2">
  <attr name="queue">TXNMGR</attr>
  <attr name="sessions">200</attr>    <!-- threads processando transações -->
  <attr name="max-sessions">500</attr> <!-- pode escalar até 500 -->
  <attr name="debug">false</attr>     <!-- NUNCA true em produção -->
</txnmgr>
```

### 2.4 Impacto do GC no P99

```
Problema: GC pause de 200ms causa spike no P99

Java 21+ options para baixa latência:
  -XX:+UseZGC              ← ZGC: pauses < 1ms na maioria dos casos
  -Xmx8g -Xms8g           ← Heap fixo: evita resize dinâmico
  -XX:MaxGCPauseMillis=5   ← Target de pausa máxima

Monitoramento de GC:
  -Xlog:gc*:file=/var/log/jpos/gc.log:time,level,tags:filecount=5,filesize=20m

  Métrica no Micrometer:
  jvm.gc.pause (histogram) — alertar se P99 > 50ms
```

---

## 3. Load Testing — Como medir antes de ir para produção

### 3.1 Ferramentas para load test de ISO 8583

```java
// Usando jPOS como cliente de teste
public class ISOLoadTester {

    private final int targetTps;
    private final int durationSeconds;
    private final MUX mux;

    public LoadTestResult run() throws Exception {
        int totalMessages = targetTps * durationSeconds;
        CountDownLatch latch = new CountDownLatch(totalMessages);
        AtomicLong successCount = new AtomicLong();
        AtomicLong timeoutCount = new AtomicLong();
        LongAdder totalLatencyMs = new LongAdder();

        // Rate limiter para controlar TPS
        RateLimiter rateLimiter = RateLimiter.create(targetTps);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(50);

        for (int i = 0; i < totalMessages; i++) {
            rateLimiter.acquire();
            executor.submit(() -> {
                try {
                    ISOMsg request = buildTestAuth();
                    long start = System.currentTimeMillis();
                    ISOMsg response = mux.request(request, 30000);
                    long latency = System.currentTimeMillis() - start;

                    if (response != null && "00".equals(response.getString(39))) {
                        successCount.incrementAndGet();
                    } else if (response == null) {
                        timeoutCount.incrementAndGet();
                    }
                    totalLatencyMs.add(latency);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        return new LoadTestResult(
            successCount.get(), timeoutCount.get(),
            totalLatencyMs.longValue() / totalMessages
        );
    }
}
```

### 3.2 Cenários de load test obrigatórios

```
1. Baseline (TPS nominal):
   - Verificar latência P95/P99 em carga normal
   - Duração: 30 minutos

2. Stress test (150% do TPS nominal):
   - Verificar degradação graceful, sem crash
   - Duração: 15 minutos

3. Spike test (500% por 30 segundos):
   - Verificar recovery após pico
   - Duração: 5 minutos no pico, 10 minutos recovery

4. Soak test (100% TPS por 24h):
   - Memory leak, thread leak, conexão leak
   - Monitorar heap, threads, conexões abertas

5. Emissor lento (simular 1s de latência):
   - Verificar que threads não ficam presas
   - Verificar que timeout e reversal funcionam na carga
```

---

## 4. Circuit Breaker — Proteção contra Cascata de Falhas

### 4.1 O problema

```
Emissor começa a responder em 28s (quase no timeout de 30s)
Switch cria 500 threads aguardando resposta do emissor
Novas transações chegam mas não há threads disponíveis
Switch fica indisponível para todos os emissores
→ Falha em cascata
```

### 4.2 Implementação com Resilience4j

```java
@Component
public class IssuerCircuitBreaker {

    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker getBreaker(String issuerId) {
        return breakers.computeIfAbsent(issuerId, id -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)          // abrir se 50% falham
                .slowCallRateThreshold(30)          // abrir se 30% são lentos
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(20)              // janela de 20 transações
                .minimumNumberOfCalls(10)           // mínimo antes de avaliar
                .build();
            return CircuitBreaker.of(id, config);
        });
    }

    public ISOMsg execute(String issuerId, Supplier<ISOMsg> call) {
        CircuitBreaker breaker = getBreaker(issuerId);

        return switch (breaker.getState()) {
            case OPEN -> {
                // Stand-in: aprovar com score baixo ou recusar conservadoramente
                metrics.increment("circuit_breaker.open", "issuer", issuerId);
                yield standInService.process(issuerId);
            }
            case HALF_OPEN, CLOSED -> breaker.executeSupplier(call);
        };
    }
}
```

---

## 5. Horizontal Scaling — Múltiplas Instâncias do Switch

### 5.1 Desafios ao escalar

```
Problema 1: Deduplicação distribuída
  - STAN + Terminal deve ser único globalmente
  - Solução: Redis centralizado para cache de deduplicação

Problema 2: QMUX correlation
  - Cada instância tem seu próprio QMUX
  - Instância A envia request, instância B pode receber a response?
  - Solução: sticky routing (mesmo cliente sempre vai para mesma instância)
           OU: response routing via header com instância ID

Problema 3: SAF (Store-and-Forward)
  - Fila de SAF deve ser persistente e não duplicada entre instâncias
  - Solução: fila em banco de dados ou Redis Streams com consumer groups

Problema 4: Conexões TCP com emissores
  - Cada instância tem suas próprias conexões
  - 3 instâncias × 20 conexões = 60 conexões no emissor
  - Verificar limite de conexões do emissor antes de escalar
```

### 5.2 Arquitetura de referência para alto volume

```
                    Load Balancer (L4 — TCP)
                         │
           ┌─────────────┼─────────────┐
           ▼             ▼             ▼
      Switch-01     Switch-02     Switch-03
           │             │             │
           └─────────────┼─────────────┘
                         │
                  ┌──────┴──────┐
                  │             │
               Redis         PostgreSQL
           (velocity,       (transactions,
           dedup cache)      audit, SAF)
                         │
           ┌─────────────┼─────────────┐
           ▼             ▼             ▼
       Visa Host    Master Host    Elo Host
```

---

## Resumo da Semana

| Tópico | Meta |
|--------|------|
| SLAs | P95 on-us < 150ms, P99 < 500ms, 99.99% disponibilidade |
| Decomposição de latência | Saber onde vai cada milissegundo |
| jPOS tuning | Sessions, max-outstanding, ZGC |
| Load test | 5 cenários obrigatórios antes de produção |
| Circuit breaker | Resilience4j protegendo contra emissor lento |
| Scaling | Deduplicação distribuída com Redis, sticky routing |
