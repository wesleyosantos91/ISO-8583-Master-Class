# Semana 30 — Exercícios de Fixação

## Exercício 1 — Decisão de Representment (Nível: Intermediário)

Para cada chargeback abaixo, decida: **representar** ou **aceitar**? Justifique.

| # | Valor | Reason Code | Evidências disponíveis | Custo operacional estimado |
|---|-------|-------------|----------------------|---------------------------|
| 1 | R$ 45 | 13.1 | Tracking code mostra entrega | R$ 30 (15 min analista) |
| 2 | R$ 3.200 | 10.4 | 3DS ECI 05 com CAVV válido | R$ 50 |
| 3 | R$ 890 | 10.4 | Sem 3DS, IP match histórico | R$ 50 |
| 4 | R$ 1.500 | 13.2 | E-mail de cancelamento do cliente 45 dias antes | R$ 50 |
| 5 | R$ 250 | 12.6 | Logs mostram bug no POS (enviou 2x) | R$ 30 |
| 6 | R$ 8.000 | 13.1 | Produto digital (acesso ao sistema confirmado) | R$ 50 |

---

## Exercício 2 — Liability Shift e 3DS (Nível: Intermediário)

Complete a tabela indicando quem é responsável pelo prejuízo:

| Cenário | Terminal | Cartão | 3DS | Liability |
|---------|----------|--------|-----|-----------|
| Clonagem de cartão de tarja em terminal com chip | Chip | Tarja | N/A | |
| Clonagem de cartão com chip em terminal sem chip | Tarja | Chip | N/A | |
| Fraude CNP — 3DS autenticado (ECI 05) | N/A | N/A | Sim | |
| Fraude CNP — sem 3DS | N/A | N/A | Não | |
| Fraude CNP — 3DS tentado, emissor não suporta (ECI 06) | N/A | N/A | Attempted | |
| Cartão roubado usado com chip + PIN correto | Chip | Chip | N/A | |

---

## Exercício 3 — Automação de Disputes (Nível: Avançado)

Implemente em Java o método `evaluateDisputeDecision` abaixo:

```java
public enum DisputeDecision { ACCEPT, REPRESENT_AUTO, REPRESENT_MANUAL, ESCALATE }

public class DisputeEvaluator {

    private static final BigDecimal MIN_DEFENSE_VALUE = new BigDecimal("100.00");

    /**
     * Regras:
     * - Valor < R$ 100: aceitar (não compensa defender)
     * - Reason code 10.4:
     *   - Com 3DS ECI 05 ou 02: representar automaticamente
     *   - Com 3DS ECI 06 ou 01: representar manual (revisar evidências)
     *   - Sem 3DS: aceitar
     * - Reason code 12.6 (duplicata):
     *   - Se temos logs de deduplicação provando que foi bug: representar automático
     *   - Caso contrário: representar manual
     * - Reason code 13.1 (não recebido):
     *   - Se tracking confirmado com assinatura: representar automático
     *   - Se tracking entregue sem assinatura: representar manual
     *   - Se tracking não encontrado: aceitar
     * - Reason code 13.2 (recorrência cancelada):
     *   - Se cobrança após pedido de cancelamento: aceitar
     *   - Se não há cancelamento registrado: representar automático
     * - Outros reason codes: escalate para análise manual
     */
    public DisputeDecision evaluate(
            String reasonCode,
            BigDecimal amount,
            String eci,           // null se não e-commerce
            boolean hasDeliveryConfirmation,
            boolean hasSignedDelivery,
            boolean hasDuplicationLogs,
            boolean hasCancellationRecord
    ) {
        // TODO: implementar
        throw new UnsupportedOperationException("Implementar");
    }
}
```

Escreva também testes unitários cobrindo pelo menos 8 cenários diferentes.

---

## Exercício 4 — Métricas de Chargeback (Nível: Avançado)

Você recebeu os dados de dois meses de um merchant:

**Outubro:**
- Transações: 15.000
- Chargebacks: 95
- Representments enviados: 60
- Representments ganhos: 38

**Novembro:**
- Transações: 18.200
- Chargebacks: 164
- Representments enviados: 110
- Representments ganhos: 52

Calcule para cada mês:
1. Chargeback ratio
2. Win rate do representment
3. % de chargebacks defendidos
4. Volume financeiro em risco (assuma ticket médio R$ 420)

Responda:
5. Em qual programa de monitoramento Visa o merchant se enquadra em novembro?
6. O merchant está melhorando ou piorando? Use os dados para justificar.
7. Quais 3 ações recomendaria com base nos números?

---

## Exercício 5 — CVV2 e AVS (Nível: Intermediário)

Um e-commerce recebe as seguintes respostas de autorização:

| Transação | CVV2 Result | AVS Result | Valor | Decisão sugerida |
|-----------|-------------|------------|-------|-----------------|
| 1 | M (Match) | Y (Address + ZIP) | R$ 150 | |
| 2 | N (No Match) | Y | R$ 89 | |
| 3 | M | N (Neither) | R$ 2.800 | |
| 4 | U (Unavailable) | U | R$ 320 | |
| 5 | M | A (Address only) | R$ 95 | |
| 6 | N | N | R$ 4.500 | |

Para cada transação:
- Qual deve ser a decisão do merchant (aprovar, declinar, revisar)?
- Qual é o risco de chargeback?
- Se decidir aprovar mesmo com indicadores ruins, o que documentar?

---

## Exercício 6 — Modelo de Dados (Nível: Avançado)

Com base na teoria da semana, estenda a classe `TransactionRecord` para suportar o ciclo completo de disputes. Requisitos:

1. Campos para rastreio de chargeback (RRN do CB, reason code, data de recebimento)
2. Campos para representment (data de envio, evidências, decisão)
3. Campos para pre-arbitration (se aplicável)
4. Status enum com todos os estados possíveis do ciclo de vida
5. Método `isEligibleForRepresentment()` com as regras básicas
6. Método `getDaysUntilDeadline()` que retorna quantos dias restam para defender
7. Integração com a classe `DisputeEvaluator` do Exercício 3

```java
public enum TransactionLifecycleStatus {
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    REVERSED,
    CHARGEBACK_RECEIVED,
    REPRESENTMENT_SENT,
    REPRESENTMENT_WON,
    REPRESENTMENT_LOST,
    PRE_ARBITRATION,
    ARBITRATION,
    DISPUTE_CLOSED
}
```

Implemente a classe completa com os campos e métodos listados.
