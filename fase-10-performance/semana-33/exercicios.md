# Exercícios — Semana 33 — Performance e Escala: 5000+ TPS

## Exercício 1 — Leitura de SLAs e Decomposição de Latência

Dado o perfil de latência de uma transação off-us descrito na teoria (P95 = 350ms),
responda:

1. Qual componente domina a latência total? Que porcentagem do tempo total ele representa?
2. O switch tem controle sobre esse componente dominante? O que ele pode fazer para
   mitigar seu impacto na experiência do portador?
3. Um cliente exige SLA de P95 < 200ms para transações off-us. O que você diria a ele?
   Quais condições precisariam ser verdadeiras para atingir esse número?
4. Em qual métrica (P50, P95, P99) o impacto do GC pause é mais visível? Por quê?

**Objetivo:** interpretar SLAs e identificar o gargalo real numa decomposição de latência.

---

## Exercício 2 — Configuração do jPOS para Alto Volume

Analise as configurações XML da teoria e responda:

1. O `maxSessions` do QServer está em 500 e o `sessions` do TransactionManager também
   em 500. O que acontece se chegarem 600 conexões simultâneas?
2. Por que o atributo `debug` do `txnmgr` deve ser `false` em produção?
   Qual é o impacto de deixá-lo `true` com 3.000 TPS?
3. O `max-outstanding` do QMUX está em 5.000. O que acontece se esse limite for atingido?
   O que o QMUX faz com a 5.001ª requisição?
4. O `connect-timeout` do canal para a bandeira é 5.000ms. O que acontece se a
   bandeira não responder ao TCP handshake em 5 segundos?

**Objetivo:** compreender as implicações de cada parâmetro de configuração do jPOS.

---

## Exercício 3 — Circuit Breaker com Resilience4j

Usando a implementação da `IssuerCircuitBreaker` da teoria, simule o comportamento
do circuit breaker para o seguinte cenário:

```
Janela de 20 transações do emissor BANCO-X:
  Transações 1-8:   success (200ms cada)
  Transações 9-12:  timeout (>10s cada) — emissor ficou lento
  Transações 13-16: timeout (>10s cada)
  Transação 17:     ?
```

1. Após a transação 16, qual é o estado do circuit breaker? (Assumindo
   `failureRateThreshold=50`, `slowCallRateThreshold=30`, `minimumNumberOfCalls=10`)
   Calcule as taxas para justificar sua resposta.
2. O que acontece com a transação 17?
3. Quanto tempo o breaker fica no estado OPEN antes de tentar HALF_OPEN?
4. No estado OPEN, o código retorna `standInService.process(issuerId)`. O que deve
   retornar esse stand-in para uma transação de autorização? RC `91` ou RC `00`?
   Qual é a implicação financeira de cada escolha?

**Objetivo:** simular o comportamento do circuit breaker e entender suas implicações de negócio.

---

## Exercício 4 — ISOLoadTester

Complete o método `buildTestAuth()` que constrói uma mensagem de autorização válida
para uso no load test:

```java
public class ISOLoadTester {

    private final int targetTps;
    private final int durationSeconds;
    private final MUX mux;
    private final ISOPackager packager;

    // Construa uma 0200 de autorização válida para testes de carga.
    // Use PANs fictícios de teste (ex: 4111111111111111 — Visa test PAN)
    // Varie o STAN a cada chamada para evitar falha por duplicidade
    private ISOMsg buildTestAuth() throws ISOException {
        ISOMsg msg = new ISOMsg();
        // TODO: setar MTI correto para autorização
        // TODO: DE 2 — PAN de teste
        // TODO: DE 3 — Processing Code (compra = 000000)
        // TODO: DE 4 — Amount (use valor fixo de R$ 10,00 = "000000001000")
        // TODO: DE 7 — Transmission date/time
        // TODO: DE 11 — STAN (deve ser único por sessão — use AtomicInteger)
        // TODO: DE 22 — POS Entry Mode (digitado = "01")
        // TODO: DE 41 — Terminal ID de teste
        // TODO: DE 42 — Merchant ID de teste
        return msg;
    }

    // Método auxiliar para gerar STAN único
    private static final AtomicInteger stanCounter = new AtomicInteger(1);
    private String nextStan() {
        return String.format("%06d", stanCounter.getAndIncrement() % 1_000_000);
    }
}
```

**Objetivo:** praticar a construção de mensagens ISO 8583 para cenários de load test.

---

## Exercício 5 — Análise: Scaling Horizontal

A teoria descreve 4 problemas ao escalar horizontalmente. Para cada problema,
avalie a solução proposta e aponte uma limitação ou risco residual:

| Problema | Solução proposta | Sua análise: limitação ou risco residual |
|----------|-----------------|------------------------------------------|
| Deduplicação distribuída | Redis centralizado | ? |
| QMUX correlation | Sticky routing | ? |
| SAF distribuído | Redis Streams com consumer groups | ? |
| Conexões TCP com emissores | Verificar limite antes de escalar | ? |

Depois, responda: se o Redis centralizado cair, quais dos 4 problemas se tornam
críticos imediatamente? Qual é a estratégia de fallback?

**Objetivo:** analisar criticamente as soluções de scaling e identificar pontos únicos de falha.
