# Semana 20 — Exercícios de Fixação

## Exercício 1 — O Ciclo de Vida Pós-Autorização (Nível: Iniciante)

Preencha a tabela com o que acontece em cada etapa após uma compra de R$ 200,00 com cartão crédito:

| Etapa | Quando | Quem age? | O que acontece? | Fluxo de dinheiro? |
|-------|--------|-----------|-----------------|-------------------|
| Autorização | D+0, tempo real | | | |
| Captura/Batch Close | D+0, fim do dia | | | |
| Clearing | D+1 | | | |
| Settlement | D+2 | | | |
| Pagamento ao merchant | D+30 | | | |

Depois responda:
1. Em qual etapa o limite do cartão é bloqueado?
2. Em qual etapa o dinheiro efetivamente sai da conta do portador?
3. Se o merchant não fizer a captura em 7 dias, o que acontece com a autorização?
4. O que é "force post" e quando é usado?

**Critério de sucesso:** Tabela correta, 4 perguntas respondidas com precisão.

---

## Exercício 2 — Mismatches: Normal ou Exceção? (Nível: Iniciante/Intermediário)

Para cada mismatch abaixo, classifique como **Normal** (esperado em alguns contextos) ou **Exceção** (indica problema). Explique e diga o que deve acontecer:

1. Auth R$ 100, Clearing R$ 115 — restaurante
2. Auth R$ 500, Clearing R$ 500 — hotel com pré-autorização exata
3. Auth R$ 200, Clearing R$ 0 — cliente cancelou no hotel antes do checkout
4. Clearing R$ 80 sem nenhuma autorização correspondente
5. Auth R$ 300, sem nenhum clearing após 30 dias
6. Auth R$ 100, Clearing R$ 110 — postos de combustível
7. Auth R$ 50, Clearing R$ 50 com PAN diferente da autorização
8. Auth R$ 200, Clearing R$ 185 — gorjeta negativa (desconto dado)

**Critério de sucesso:** 8 de 8 classificados corretamente com explicação.

---

## Exercício 3 — Implemente ReconciliationEngine (Nível: Intermediário)

Implemente `ReconciliationEngine` que detecta pelo menos 5 tipos de mismatch:

```java
public class ReconciliationEngine {

    public enum MismatchType {
        CLEARING_WITHOUT_AUTH,     // Force post
        AUTH_WITHOUT_CLEARING,     // Auth expirada (só alertar se > threshold)
        AMOUNT_MISMATCH,           // Valor diferente (verificar se está dentro da tolerância)
        PAN_MISMATCH,              // PAN diferente entre auth e clearing — sempre exceção grave
        CURRENCY_MISMATCH,         // Moeda diferente
        DUPLICATE_CLEARING         // Mesmo RRN apresentado duas vezes no clearing
    }

    public ReconciliationReport reconcile(
            List<AuthRecord> authorizations,
            List<ClearingRecord> clearings,
            ReconciliationConfig config) { /* ... */ }
}

public record ReconciliationConfig(
    int amountTolerancePercent,  // ex: 20% para restaurantes (gorjeta)
    int authExpirationDays,      // ex: 7 dias para auth sem clearing = alerta
    boolean strictPanCheck       // true = PAN mismatch sempre exceção
) {}
```

Regras:
- Amount mismatch dentro da tolerância configurada: classificar como `WARNING` (não `ERROR`)
- Amount mismatch acima da tolerância: classificar como `ERROR`
- PAN mismatch: sempre `ERROR` independente de configuração
- Duplicate clearing: sempre `ERROR`
- Auth sem clearing com menos de `authExpirationDays` dias: ignorar
- Auth sem clearing com mais de `authExpirationDays` dias: `WARNING`

Escreva testes para cada tipo de mismatch.

**Critério de sucesso:** Todos os 5+ tipos detectados, severidade correta, testes passam.

---

## Exercício 4 — Gere Dados Sintéticos com Mismatches (Nível: Intermediário)

Implemente `TestDataGenerator` que cria conjuntos de dados para testar a reconciliação:

```java
public class TestDataGenerator {
    /**
     * Gera N autorizações e um conjunto de clearings com mismatches intencionais.
     * @param totalAuths número de autorizações
     * @param clearingWithoutAuthPercent percentual de force posts
     * @param authWithoutClearingPercent percentual de auths sem clearing
     * @param amountMismatchPercent percentual de mismatches de valor
     * @param panMismatchPercent percentual de mismatches de PAN
     */
    public TestDataSet generate(int totalAuths, double... percents) { /* ... */ }
}
```

Gere um dataset com:
- 1.000 autorizações
- 2% de clearing sem auth (20 force posts)
- 3% de auth sem clearing (30 expiradas)
- 1% de amount mismatch acima da tolerância (10 exceções)
- 0.5% de PAN mismatch (5 casos graves)

Execute o `ReconciliationEngine` sobre esse dataset e verifique os números.

**Critério de sucesso:** Geração funcional, números conferem após reconciliação.

---

## Exercício 5 — Relatório de Exceções (Nível: Intermediário)

Implemente `ReconciliationReporter` que gera dois formatos de relatório:

**1. Relatório técnico (para o time de TI):**
```
=== RECONCILIAÇÃO DO DIA 2026-03-15 ===
Total autorizações:  1.000
Total clearing:       970
Matched:              945
Exceptions:            55

SEVERITY ERROR (requer ação imediata):
  PAN_MISMATCH:           5 ocorrências | Valor total: R$ 2.450,00
  DUPLICATE_CLEARING:     3 ocorrências | Valor total: R$ 890,00
  AMOUNT_MISMATCH_ABOVE_TOL: 10 ocorrências | Diferença: R$ 1.230,00

SEVERITY WARNING (verificar):
  AUTH_WITHOUT_CLEARING:  30 ocorrências | Valor total: R$ 15.200,00
  CLEARING_WITHOUT_AUTH:  7 ocorrências | Valor total: R$ 3.100,00
```

**2. Relatório financeiro (para o time de finanças):**
```
=== POSIÇÃO FINANCEIRA - 15/03/2026 ===
Total autorizado:     R$ 125.000,00
Total no clearing:    R$ 121.500,00
Diferença:            R$ 3.500,00
Status: ATENÇÃO - verificar 25 exceções
```

**Critério de sucesso:** Dois formatos gerados, valores corretos, severidade clara.

---

## Exercício 6 — Ciclo Completo: post-auth-lifecycle.md (Nível: Avançado)

Escreva `post-auth-lifecycle.md` documentando o ciclo de vida completo de uma transação de R$ 200,00 em crédito à vista, do ponto de vista de cada participante:

**1. Perspectiva do Portador:**
- Quando o limite é bloqueado?
- Quando vira fatura?
- Quando é debitado efetivamente?
- Em caso de contestação: qual é o prazo e processo?

**2. Perspectiva do Merchant:**
- Quando recebe a confirmação de venda?
- Quando o dinheiro cai na conta? (D+30 padrão ou D+2 antecipado)
- Se o portador contestar: o que acontece com o valor?

**3. Perspectiva do Adquirente:**
- Qual é o fluxo de caixa? (Paga o merchant antes de receber do emissor?)
- Qual é o risco de crédito?
- Qual é a receita (MDR)?

**4. Perspectiva do Emissor:**
- Quando debita o portador?
- Quando paga o adquirente (via câmara)?
- O que acontece se o portador não pagar a fatura?

**Critério de sucesso:** Ciclo completo documentado para todos os 4 participantes, datas e fluxos de dinheiro precisos.

---

## Exercício 7 — Análise: Por Que Amounts Divergem? (Nível: Avançado)

O analista financeiro recebe o seguinte relatório: "Temos 347 transações autorizadas em R$ 200,00 que aparecem no clearing com valores diferentes."

Escreva `amount-divergence-analysis.md` explicando (para um analista financeiro não-técnico) **3 cenários legítimos** onde um amount de autorização de R$ 200,00 pode chegar ao clearing com valor diferente:

**Cenário 1:** Restaurante
- Explique o processo de gorjeta
- Por que é permitido um valor maior no clearing?
- Qual é o limite de variação aceitável?

**Cenário 2:** Hotel
- Explique pré-autorização vs valor real do checkout
- Por que o clearing pode ser menor que a autorização?
- Como o hotel processa o checkout com valor diferente?

**Cenário 3:** Posto de combustível
- Explique como funciona a autorização em postos (valor estimado vs abastecido)
- Por que a variação pode ser significativa?

Para cada cenário: explique o fluxo ISO 8583, quais campos mudam e qual é o tratamento correto na reconciliação.

**Critério de sucesso:** 3 cenários explicados de forma clara para não-técnico, com fundamento técnico ISO 8583 correto.
