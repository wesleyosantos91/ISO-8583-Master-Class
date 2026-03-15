# Semana 23 — Exercícios de Fixação

## Exercício 1 — Entendendo o Arquivo de Clearing (Nível: Iniciante)

Dado o seguinte arquivo de clearing simplificado (CSV):

```csv
RRN,PAN_last4,Amount,Date,MerchantID,TransactionType
000001,3366,015000,20260315,MERCHANT001,PURCHASE
000002,7890,050000,20260315,MERCHANT002,PURCHASE
000003,1234,008500,20260315,MERCHANT001,PURCHASE
000004,5678,200000,20260315,MERCHANT003,PURCHASE
000005,9012,030000,20260315,MERCHANT001,REFUND
000006,3456,015000,20260315,MERCHANT002,PURCHASE  <-- duplicata do 000001?
```

E a tabela de autorizações:

```
RRN=000001 | PAN=****3366 | Amount=15000 | Date=20260315 | Merchant=MERCHANT001 | RC=00
RRN=000002 | PAN=****7890 | Amount=45000 | Date=20260315 | Merchant=MERCHANT002 | RC=00
RRN=000003 | PAN=****1234 | Amount=8500  | Date=20260315 | Merchant=MERCHANT001 | RC=00
-- RRN=000004 não existe nas autorizações
RRN=000007 | PAN=****2222 | Amount=10000 | Date=20260315 | Merchant=MERCHANT004 | RC=00
```

Identifique e classifique cada exceção:
1. RRN=000002: Amount diff (45000 auth vs 50000 clearing) — tipo e severidade?
2. RRN=000004: clearing sem auth — tipo?
3. RRN=000006: possível duplicata — tipo e como verificar?
4. RRN=000007: auth sem clearing — tipo e severidade (transação de hoje)?

**Critério de sucesso:** Todos os 4 identificados corretamente com tipo e severidade.

---

## Exercício 2 — Implemente ClearingFileParser (Nível: Iniciante/Intermediário)

Implemente `ClearingFileParser` que lê um arquivo CSV de clearing simplificado:

```java
public class ClearingFileParser {

    /**
     * Lê arquivo CSV e retorna lista de ClearingRecord.
     * Formato: RRN,PAN_last4,Amount,Date,MerchantID,TransactionType
     */
    public List<ClearingRecord> parse(Path filePath) throws IOException { /* ... */ }

    /**
     * Valida o arquivo: header correto, campos obrigatórios presentes,
     * valores no formato correto.
     */
    public ParseResult parseWithValidation(Path filePath) { /* ... */ }
}

public record ClearingRecord(
    String rrn,
    String panLast4,
    long amountCents,
    LocalDate date,
    String merchantId,
    TransactionType type
) {}
```

Regras de validação:
- RRN não pode ser nulo ou vazio
- Amount deve ser positivo
- Date no formato YYYYMMDD
- TransactionType deve ser PURCHASE, REFUND ou REVERSAL
- PAN last 4 deve ter exatamente 4 dígitos

Escreva testes com arquivos CSV válidos e inválidos.

**Critério de sucesso:** Parser funcional, validação completa, testes passam.

---

## Exercício 3 — Implemente ReconciliationEngine Completo (Nível: Intermediário)

Implemente o `ReconciliationEngine` completo com os 5 tipos de exceção:

```java
public class ReconciliationEngine {
    public ReconciliationReport reconcile(
        List<AuthRecord> auths,
        List<ClearingRecord> clearings,
        ReconciliationConfig config
    ) { /* ... */ }
}
```

Adicione suporte a:
1. `CLEARING_WITHOUT_AUTH` — Force post (erro de negócio)
2. `AUTH_WITHOUT_CLEARING` — Auth expirada ou merchant não capturou
3. `AMOUNT_MISMATCH_WITHIN_TOLERANCE` — Normal (gorjeta/hotel) → WARNING
4. `AMOUNT_MISMATCH_ABOVE_TOLERANCE` — Suspeito → ERROR
5. `PAN_MISMATCH` — Exceção grave → ERROR
6. `DUPLICATE_CLEARING` — Duplicata no arquivo → ERROR

Para cada exceção: incluir no report o RRN, os valores de cada lado, a diferença e a severidade.

Escreva testes para cada tipo de exceção.

**Critério de sucesso:** 6 tipos detectados, severidade correta, relatório gerado.

---

## Exercício 4 — Gerar Relatório de Exceções (Nível: Intermediário)

Implemente `ReconciliationReporter` com saída em 3 formatos:

**Formato 1 — Console/Log:**
```
=== RECONCILIAÇÃO 2026-03-15 ===
Autorizações: 1000 | Total: R$ 125.000,00
Clearing:      985  | Total: R$ 121.200,00
Matched:       960  | Total: R$ 118.500,00
Exceções:       55

CRÍTICO (ação imediata):
  PAN_MISMATCH           : 5 casos | R$ 2.450,00
  DUPLICATE_CLEARING     : 3 casos | R$ 1.200,00
  AMOUNT_MISMATCH_ABOVE  : 10 casos | Diferença: R$ 1.800,00

ATENÇÃO (verificar):
  CLEARING_WITHOUT_AUTH  : 12 casos | R$ 6.300,00
  AUTH_WITHOUT_CLEARING  : 25 casos | R$ 13.200,00
```

**Formato 2 — CSV para planilha (equipe financeira)**

**Formato 3 — JSON para sistema downstream**

**Critério de sucesso:** 3 formatos gerados corretamente, totais conferem.

---

## Exercício 5 — Calcule os Totais de Conferência (Nível: Intermediário)

Implemente `ReconciliationSummary` que calcula:

```java
public record ReconciliationSummary(
    long totalAuthCount,
    BigDecimal totalAuthAmount,
    long totalClearingCount,
    BigDecimal totalClearingAmount,
    long matchedCount,
    BigDecimal matchedAmount,
    long exceptionCount,
    BigDecimal exceptionAmount,
    BigDecimal netDifference,  // clearing - auth (pode ser negativo)
    double matchRate            // % matched/total
) {}
```

Regra de negócio: a `netDifference` deve ser próxima de zero para uma operação saudável. Diferença > 1% do total de authorizations deve gerar alerta.

Escreva um teste com dados onde:
- 1.000 auths, R$ 125.000
- 985 clearings, R$ 121.200
- Esperado: netDifference = -R$ 3.800, matchRate = 96%

**Critério de sucesso:** Cálculos corretos, alerta gerado quando diferença > 1%.

---

## Exercício 6 — Processamento em Escala (Nível: Avançado)

Implemente `BatchReconciliationProcessor` que processa 10.000 autorizações e 9.800 registros de clearing de forma eficiente:

```java
public class BatchReconciliationProcessor {
    /**
     * Processa em chunks para evitar OOM com volumes grandes.
     * Reporta progresso a cada 1.000 registros processados.
     */
    public ReconciliationReport processBatch(
        Path authFile,
        Path clearingFile,
        int chunkSize
    ) { /* ... */ }
}
```

Requisitos de performance:
- Não carregar todos os dados em memória simultaneamente
- Usar streaming/paginação
- Tempo de processamento < 30 segundos para 10.000 registros

Escreva um teste de performance que verifica o tempo de execução.

**Critério de sucesso:** Processa 10.000 registros em < 30s sem OOM.

---

## Exercício 7 — Relatório para o Time Financeiro (Nível: Avançado)

Dado os dados da reconciliação do exercício 6 (10.000 auths, 9.800 clearings), escreva `financial-reconciliation-report.md` simulando o relatório que você entregaria ao time financeiro.

O relatório deve incluir:
1. **Resumo executivo** (5 linhas): O que aconteceu, está tudo bem?
2. **Posição financeira**: Total autorizado vs clearing vs diferença
3. **Classificação das exceções**: Quantas são normais vs problemáticas?
4. **Ações requeridas**: O que precisa ser feito para cada categoria?
5. **Prazo**: Quando cada item deve ser resolvido?

**Critério de sucesso:** Relatório claro para não-técnico, ações específicas, prazos realistas.
