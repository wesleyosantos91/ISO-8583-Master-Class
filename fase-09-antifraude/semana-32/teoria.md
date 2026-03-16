# Semana 32 — Antifraude: Operação, Monitoramento e Resposta a Incidentes

## 1. Tipos de Fraude no Ecossistema de Cartões

### 1.1 Card-Present Fraud (Fraude Presencial)

| Tipo | Como funciona | Prevenção técnica |
|------|---------------|------------------|
| Counterfeit (clonagem) | Cópia da tarja magnética | Chip EMV (ARQC único por transação) |
| Card theft (roubo) | Cartão físico roubado | PIN, biometria, bloqueio imediato |
| Skimming | Equipamento no terminal lê a tarja | EMV liability shift, monitoramento de terminais |
| Shimming | Dispositivo entre chip e terminal | Criptograma dinâmico do chip |

**EMV como solução técnica:**
O chip gera um ARQC diferente para cada transação. Mesmo que o atacante capture o ARQC de uma transação, ele não serve para outra. A clonagem de chip é praticamente inviável.

### 1.2 Card-Not-Present Fraud (Fraude em E-commerce)

| Tipo | Como funciona | Prevenção técnica |
|------|---------------|------------------|
| Card testing | Bot testa milhares de PANs com valores pequenos | Velocity rules, CAPTCHA, 3DS |
| Account takeover | Acessa conta da plataforma do portador | MFA, device binding |
| Friendly fraud | Portador contesta compra real | 3DS, evidências de entrega |
| Synthetic identity | Identidade falsa criada com dados reais misturados | KYC no onboarding |
| BIN attack | Testa variações de PAN sistematicamente | Rate limiting severo por BIN |

### 1.3 Fraud Rings (Fraude Organizada)

```
Estrutura de um fraud ring:
  1. Compromisso: dados de cartão obtidos (phishing, breach, skimmer)
  2. Validação: card testing em pequenas quantias (< R$10)
  3. Monetização: compras de itens de alto valor revendável
     (eletrônicos, passagens aéreas, gift cards)
  4. Lavagem: itens revendidos por dinheiro

Sinais no switch:
  - Pico repentino de transações de R$ 1-5 num MCC específico
  - Múltiplos PANs com mesmo prefixo BIN em sequência
  - Mesmo merchant recebendo cartões de múltiplos países
  - Horário atípico (madrugada local)
```

---

## 2. Monitoramento de Fraude em Tempo Real

### 2.1 Dashboard de fraude — métricas essenciais

```java
public class FraudDashboardMetrics {

    // Taxa de fraude (chargebacks confirmados / total)
    public double fraudRate(LocalDate date) {
        long fraudulent = chargebackRepo.countByDateAndReasonCode(date, "10.*");
        long total = txnRepo.countByDate(date);
        return (double) fraudulent / total * 100;
    }

    // Taxa de declínio por fraude (DE39=59)
    public double fraudDeclineRate(LocalDate date) {
        long fraudDeclines = txnRepo.countByDateAndResponseCode(date, "59");
        long total = txnRepo.countByDate(date);
        return (double) fraudDeclines / total * 100;
    }

    // Valor em risco (transações aprovadas com score > 70)
    public BigDecimal amountAtRisk(LocalDate date) {
        return txnRepo.sumAmountByDateAndMinScore(date, 70);
    }
}
```

### 2.2 Alertas críticos de fraude

```
Alerta 1: BIN attack detectado
  Trigger: > 100 transações com PANs no mesmo prefixo BIN em 5 minutos
  Ação: bloquear temporariamente o prefixo, alertar emissor

Alerta 2: Terminal comprometido
  Trigger: > 20% de chargebacks 10.x no mesmo terminal em 24h
  Ação: bloquear terminal, investigar hardware (skimmer?)

Alerta 3: Card testing campaign
  Trigger: > 50 transações de valor < R$ 10 no mesmo MCC em 1 hora
  Ação: elevar threshold de recusa para CNP naquele MCC

Alerta 4: Pico de fraude por país
  Trigger: taxa de fraude em transações de determinado país > 3x a média
  Ação: elevar score mínimo para aprovação, habilitar 3DS obrigatório
```

---

## 3. Negative List e Positive List

### 3.1 Negative List (Lista Negra)

```java
public class NegativeListParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg req = ctx.get("REQUEST");

        // Verifica múltiplas listas negras
        String pan = req.getString(2);
        String terminalId = req.getString(41);
        String merchantId = req.getString(42);
        String ip = extractIp(req);

        if (negativeListService.isPanBlocked(pan)) {
            ctx.put("RESPONSE_CODE", "62"); // Restricted card
            ctx.put("BLOCK_REASON", "PAN_BLACKLIST");
            return ABORTED;
        }

        if (negativeListService.isTerminalBlocked(terminalId)) {
            ctx.put("RESPONSE_CODE", "58"); // Terminal not permitted
            return ABORTED;
        }

        if (ip != null && negativeListService.isIpBlocked(ip)) {
            ctx.put("RESPONSE_CODE", "59"); // Fraud decline
            return ABORTED;
        }

        return PREPARED;
    }
}
```

### 3.2 Fontes de negative list

```
Internas:
  - PANs com chargebacks confirmados de fraude
  - Terminais com skimmer detectado
  - IPs de campaigns conhecidas
  - Merchants com alto fraud rate (MATCH list)

Externas:
  - Visa VMPI (Visa Merchant Purchase Inquiry) — dados de fraude da rede
  - Mastercard MATCH — merchants removidos por fraude
  - Compartilhamento entre adquirentes (via BACEN, informalmente)
  - Fornecedores especializados (Emailage, ThreatMetrix, etc.)
```

---

## 4. Resposta a Incidente de Fraude

### 4.1 Playbook de fraude em massa

```
ALERTA: taxa de fraude subiu de 0.1% para 1.5% em 2 horas

Passo 1 — Contenção (primeiros 15 minutos):
  [ ] Identificar vetor: qual BIN, merchant, país, terminal?
  [ ] Aplicar regra emergencial: bloquear o vetor identificado
  [ ] Notificar emissor(es) afetados
  [ ] Escalar para gerência de risco

Passo 2 — Investigação (primeiras 2 horas):
  [ ] Extrair amostra das transações fraudulentas
  [ ] Identificar padrão: tempo, valor, MCC, modalidade
  [ ] Verificar se há breach em andamento
  [ ] Contatar VISA/Master se fraude cross-network

Passo 3 — Remediação (primeiras 24 horas):
  [ ] Regras permanentes ou temporárias baseadas no padrão
  [ ] Comunicar portadores afetados (emissor)
  [ ] Registrar ocorrência no BACEN se volume relevante
  [ ] Ajustar modelo de ML com novos dados

Passo 4 — Post-mortem (dentro de 72 horas):
  [ ] Relatório completo: impacto financeiro, causa raiz, ações
  [ ] Apresentar para diretoria
  [ ] Atualizar playbook com aprendizados
```

### 4.2 Notificação de breach de dados

Se o switch foi comprometido e dados de cartão foram expostos:

```
Obrigações regulatórias:
  Bandeiras:
    Visa: notificar em até 72 horas após confirmação
    Mastercard: notificar em até 24 horas (!) após suspeita

  BACEN:
    Resolução BCB 85/2021: notificar em até 72 horas
    Incidentes relevantes ao sistema financeiro

  LGPD:
    Art. 48: notificar ANPD em prazo razoável (entendido como 72h)
    Notificar titulares afetados se risco ou dano relevante
```

---

## 5. Antifraude e Falso Positivo — Calibração

### 5.1 A matriz de confusão do antifraude

```
                    | Real: Fraude | Real: Legítima |
--------------------|--------------|----------------|
Decisão: Recusar    | True Positive | False Positive |
Decisão: Aprovar    | False Negative| True Negative  |

True Positive: fraude detectada (bom)
False Positive: legítima recusada (custo para o portador e merchant)
False Negative: fraude que passou (custo direto de fraude)
True Negative: legítima aprovada (o negócio funcionando)
```

### 5.2 Métricas chave

```
Precision = TP / (TP + FP)
  → De tudo que recusamos como fraude, qual % era de fato fraude?
  → Alta precision = poucos falsos positivos

Recall = TP / (TP + FN)
  → De toda a fraude real, qual % detectamos?
  → Alto recall = pouca fraude passando

F1 Score = 2 * (Precision * Recall) / (Precision + Recall)
  → Balanceamento entre os dois

Na prática:
  - Adquirentes tendem a priorizar Recall (não querem pagar chargeback)
  - Emissores tendem a priorizar Precision (não querem frustrar portadores)
```

### 5.3 Review queue — tratamento de score médio

```java
public class FraudReviewQueue {

    // Transações com score 50-75 vão para fila de revisão manual
    public void routeToReview(Transaction txn, FraudScore score) {
        ReviewCase reviewCase = ReviewCase.builder()
            .transactionId(txn.id())
            .maskedPan(PANMasker.mask(txn.pan()))
            .amount(txn.amount())
            .merchantName(txn.merchantName())
            .fraudScore(score.value())
            .scoreReasons(score.reasons())
            .reviewDeadline(Instant.now().plus(4, ChronoUnit.HOURS))
            .build();

        reviewQueue.add(reviewCase);

        // Notifica analista de fraude
        alertService.notifyFraudAnalyst(reviewCase);
    }
}
```

---

## Resumo da Fase 9

| Semana 31 | Semana 32 |
|-----------|-----------|
| Risk scoring em tempo real | Tipos de fraude e fraud rings |
| Velocity rules com Redis | Monitoramento e alertas |
| Device fingerprint | Negative list e positive list |
| ML features e false positive | Playbook de incidente de fraude |
| FraudScreeningParticipant | Breach notification |
| VAA / Safety Net | Calibração: precision vs recall |
