# Semana 30 — Desafio: Construindo o Motor de Disputes

## O Cenário

Seu adquirente processa 200.000 transações/dia e recebe em média 180 chargebacks/dia. O processo atual é **100% manual**: um analista recebe um e-mail por chargeback, analisa, e envia a defesa à mão. Isso gera:

- Tempo médio de resposta: 12 dias (prazo é 30)
- Win rate: 34% (benchmark do mercado: 55-65%)
- 3 analistas sobrecarregados
- Erros por deadline perdido: ~8 CBs/mês (R$ 280.000 de prejuízo evitável)

O diretor de operações aprovou a construção de um **Motor de Automação de Disputes**. Você lidera o projeto.

---

## Sua Missão

### Parte 1 — Arquitetura do Sistema

Desenhe (Mermaid ou ASCII) a arquitetura do Dispute Engine com:

1. **Fonte de dados:** Como os chargebacks chegam? (arquivo batch, API da bandeira, portal)
2. **Engine de decisão:** Como decidir aceitar vs representar?
3. **Evidence collector:** Como buscar as evidências automaticamente?
4. **Submission:** Como enviar o representment à bandeira?
5. **Tracking:** Como monitorar prazos e escalar automaticamente?
6. **Dashboard:** Como o analista acompanha os cases manuais?

### Parte 2 — Implementação do Core

Implemente as seguintes classes:

```java
// 1. Representação de um chargeback recebido
public record ChargebackNotification(
    String chargebackId,
    String originalRrn,
    String reasonCode,
    BigDecimal chargebackAmount,
    LocalDate receivedDate,
    LocalDate representmentDeadline,
    String issuerId,
    String merchantId
) {}

// 2. Buscador de evidências (implemente para pelo menos 3 reason codes)
public interface EvidenceCollector {
    Evidence collect(ChargebackNotification cb, TransactionRecord original);
}

// 3. Submissão de representment
public interface RepresentmentSubmitter {
    RepresentmentResult submit(ChargebackNotification cb, Evidence evidence);
}

// 4. Orquestrador principal
public class DisputeOrchestrator {
    // Injete as dependências necessárias e implemente:
    public void process(ChargebackNotification cb) { ... }
    public List<ChargebackNotification> getApproachingDeadlines(int daysThreshold) { ... }
    public DisputeStats getStats(LocalDate from, LocalDate to) { ... }
}
```

### Parte 3 — Regras de Negócio

Documente em código (regras implementadas, não comentários) as seguintes decisões:

1. Threshold mínimo de valor para defender (justifique o valor escolhido)
2. Regras por reason code (quais defender automaticamente, quais manualmente, quais aceitar)
3. Escalação automática quando deadline se aproxima (quando escalar?)
4. Como lidar com chargebacks recebidos após o prazo de representment expirar?

### Parte 4 — Testes

Escreva testes de integração para os seguintes cenários:

```java
@Test void deveRepresentarAutomaticamenteCbComTresdS() { ... }
@Test void deveAceitarCbDeFraudeSemTresDS() { ... }
@Test void deveEscalarCbComPrazoMenorQue48Horas() { ... }
@Test void deveIgnorarCbRecebidoForaDoPrazo() { ... }
@Test void deveCalcularWinRateCorretamente() { ... }
@Test void deveNaoRepresentarValorAbaixoDoThreshold() { ... }
```

### Parte 5 — Estimativa de Impacto

Com base nas regras implementadas, estime:

| Métrica | Antes | Meta com automação |
|---------|-------|-------------------|
| Win rate | 34% | |
| Tempo médio de resposta | 12 dias | |
| CBs perdidos por deadline | 8/mês | |
| Horas analista/CB | 45 min | |
| Custo operacional mensal | R$ X | |

Justifique suas projeções com base nas regras de decisão que implementou.

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Arquitetura coerente e bem documentada | 20 |
| EvidenceCollector para 3+ reason codes | 20 |
| DisputeOrchestrator com lógica de decisão | 25 |
| Testes cobrindo cenários críticos | 20 |
| Estimativa de impacto justificada | 15 |

**Tempo sugerido:** 3-4 horas
**Entrega:** Código no `payment-switch-lab/src/main/java/disputes/`
