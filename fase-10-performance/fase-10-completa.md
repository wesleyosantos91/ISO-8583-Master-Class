# Fase 10 — Performance e Escala (Semana 33)

---

# Semana 33 — 5.000+ TPS em Produção

## 1. SLAs de Referência

```
Métrica         On-Us        Off-Us
────────────────────────────────────
P50 latency     < 30ms       < 150ms
P95 latency     < 80ms       < 500ms
P99 latency     < 200ms      < 1.000ms
Availability    99.99%       99.95%
TPS sustained   5.000+       2.000+
```

## 2. Decomposição de Latência

```
Total latency = Receive + Parse + Validate + Route + Forward + Issuer + Build + Send

Componente        Típico    Budget
──────────────────────────────────
TCP receive       < 1ms     1ms
Unpack (parse)    < 2ms     2ms
ValidateMessage   < 1ms     1ms
RouteByBIN        < 1ms     1ms
CheckDuplicate    < 2ms     2ms
ForwardToIssuer   10-300ms  ★ (variável — depende do emissor)
BuildResponse     < 1ms     1ms
TCP send          < 1ms     1ms
──────────────────────────────────
Total on-us       < 30ms
Total off-us      50-500ms
```

## 3. Tuning jPOS TransactionManager

```xml
<txnmgr name="txnmgr" class="org.jpos.transaction.TransactionManager">
    <property name="sessions"     value="100" />  <!-- min threads -->
    <property name="max-sessions" value="500" />  <!-- max threads -->
    <property name="queue"        value="TXNMGR" />
    <property name="max-active-sessions" value="500" />
    <property name="call-timeout" value="60000" />
</txnmgr>
```

## 4. ZGC para Baixa Latência

```bash
java -XX:+UseZGC -XX:+ZGenerational \
     -Xms4g -Xmx4g \
     -XX:MaxGCPauseMillis=1 \
     -jar payment-switch.jar
```

ZGC mantém pausas de GC < 1ms mesmo com heaps grandes.

## 5. Load Testing — 5 Cenários Obrigatórios

```
1. Steady state: 1.000 TPS por 1 hora (baseline)
2. Ramp-up: 0 → 5.000 TPS em 10 minutos (elasticidade)
3. Spike: 5.000 TPS instantâneo por 5 minutos (pico)
4. Soak: 2.000 TPS por 24 horas (memory leaks, resource exhaustion)
5. Chaos: 3.000 TPS com 1 emissor caindo a cada 5 minutos (resiliência)
```

## 6. Circuit Breaker por Emissor

```java
// Resilience4j circuit breaker — um por QMUX/emissor
CircuitBreaker cb = CircuitBreaker.of("issuer-itau", CircuitBreakerConfig.custom()
    .failureRateThreshold(50)           // abre em 50% de falha
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .slidingWindowSize(100)             // últimas 100 requests
    .permittedNumberOfCallsInHalfOpenState(10)
    .build());
```

## 7. Horizontal Scaling — Desafios

```
1. Deduplicação: cache local não funciona → Redis centralizado
2. QMUX: conexão TCP stateful → sticky sessions ou connection pool
3. SAF (Store & Forward): fila local não funciona → Kafka/SQS
4. Conexões: emissores limitam conexões → load balancer L4
```

---

## Checklist de Conclusão da Fase 10

- [ ] Conhecer os SLAs de referência (P50/P95/P99)
- [ ] Decompor latência por componente do pipeline
- [ ] Tuning do jPOS TransactionManager (sessions, max-sessions)
- [ ] Configurar ZGC para pausas < 1ms
- [ ] Implementar ISOLoadTester com RateLimiter
- [ ] Executar 5 cenários de load test
- [ ] Implementar circuit breaker com Resilience4j
- [ ] Entender os desafios de horizontal scaling
