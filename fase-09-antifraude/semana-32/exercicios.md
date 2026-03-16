# Exercícios — Semana 32 — Antifraude: Operação e Incidentes

## Exercício 1 — Tipos de Fraude e Prevenção Técnica

Para cada cenário abaixo, identifique o tipo de fraude e explique qual mecanismo técnico
do ecossistema ISO 8583/EMV torna a prevenção possível:

1. Um atacante captura a tarja magnética de um cartão num posto de gasolina e tenta usá-la
   em outro terminal no dia seguinte.
2. Um bot faz 3.000 requisições em 2 minutos, variando os últimos 4 dígitos de um PAN com
   BIN 516220, usando valores de R$ 1,00 em um e-commerce sem CAPTCHA.
3. Um portador faz uma compra legítima de R$ 800 numa loja, recebe o produto, e depois
   contesta o débito alegando que não reconhece a compra.
4. Uma quadrilha cria identidades compostas por CPFs reais (vazados) combinados com
   nomes e endereços fictícios para abrir contas e solicitar cartões de crédito.

**Objetivo:** distinguir os tipos de fraude e conectá-los às contramedidas técnicas.

---

## Exercício 2 — Métricas do Dashboard de Fraude

Dado o seguinte conjunto de dados de um dia de operação:

```
Total de transações: 480.000
Transações com RC "59" (fraud decline): 2.160
Chargebacks confirmados (reason codes 10.*): 144
Transações aprovadas com fraud score > 70: 9.600
```

Calcule:
1. A taxa de declínio por fraude (fraud decline rate)
2. A taxa de fraude real (chargeback ratio por fraude)
3. O valor em risco (assumindo ticket médio de R$ 250)
4. Se os 144 chargebacks vieram de transações com score > 70, qual é a **Precision**
   do modelo para score > 70?

**Objetivo:** calcular e interpretar as métricas operacionais de um sistema antifraude.

---

## Exercício 3 — Negative List e Response Codes

Analise o `NegativeListParticipant` da teoria e responda:

1. Por que o bloqueio de PAN retorna RC `62` (Restricted card) enquanto o bloqueio de
   IP retorna RC `59` (Fraud decline)? Qual a diferença semântica?
2. Um merchant com alto chargeback ratio deve ser adicionado à negative list com
   qual consequência: bloquear todas as transações futuras ou elevar o score mínimo
   para aprovação? Argumente.
3. A teoria menciona a MATCH list da Mastercard. O que é e quem tem obrigação de consultá-la?
4. Quais são os riscos de uma negative list desatualizada? Dê um exemplo de dano
   ao portador legítimo e um exemplo de dano à adquirente.

**Objetivo:** compreender o papel e os limites das listas de controle de acesso.

---

## Exercício 4 — FraudReviewQueue

Implemente o método `shouldRoute` que decide se uma transação deve ir para a fila
de revisão manual, considerando score e contexto:

```java
public class FraudReviewQueue {

    // Transações com score 50-75 E valor > R$ 500 vão para revisão
    // Transações com score 50-75 E valor <= R$ 500 são aprovadas diretamente
    // Transações com score > 75 são recusadas (não vão para fila)
    // Transações com score < 50 são aprovadas diretamente
    public ReviewDecision shouldRoute(Transaction txn, FraudScore score) {
        // TODO: implemente a lógica acima
        // ReviewDecision pode ser: APPROVE, DECLINE, REVIEW
        return null;
    }

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
        alertService.notifyFraudAnalyst(reviewCase);
    }
}
```

Depois, responda: qual é o risco de um `reviewDeadline` de 4 horas para uma transação
de e-commerce que precisa de liberação imediata para entrega?

**Objetivo:** implementar lógica de roteamento e analisar impactos operacionais.

---

## Exercício 5 — Precision, Recall e F1: Questões de Múltipla Escolha

Marque a alternativa correta para cada questão:

**Q1.** Um emissor que prefere maximizar o **Recall** do seu modelo antifraude está priorizando:
- a) Reduzir o número de transações legítimas negadas incorretamente
- b) Detectar a maior proporção possível de fraudes reais
- c) Equilibrar falsos positivos e falsos negativos
- d) Reduzir o custo operacional da fila de revisão

**Q2.** Uma adquirente que paga os chargebacks de fraude tem incentivo financeiro direto para:
- a) Maximizar Precision
- b) Maximizar Recall
- c) Maximizar F1 Score
- d) Ignorar falsos positivos

**Q3.** O F1 Score é a métrica mais adequada quando:
- a) Fraude tem custo muito maior que falso positivo
- b) Falso positivo tem custo muito maior que fraude
- c) Falso positivo e fraude têm custos equivalentes
- d) Não há dados históricos disponíveis

**Q4.** Na matriz de confusão de antifraude, um "True Negative" representa:
- a) Uma fraude detectada corretamente
- b) Uma fraude que passou pelo sistema sem ser detectada
- c) Uma transação legítima recusada por engano
- d) Uma transação legítima aprovada corretamente

**Objetivo:** consolidar o vocabulário de métricas de classificação binária aplicado ao antifraude.
