# Semana 9 — Desafio Integrador
## "O Auditor da PCI"

---

## O Cenário

A FinTechBR está passando pela auditoria anual de conformidade PCI DSS. O auditor externo, após revisar os logs do switch de produção por 20 minutos, apresenta três achados críticos:

**Achado #1 — Severidade: CRÍTICA**
> "Encontrei PANs completos em texto claro nos logs de transação. Arquivo `/var/log/switch/txn-2026-03-14.log`, linha 847.291. PAN `5412789012345678` logado sem mascaramento."

**Achado #2 — Severidade: ALTA**
> "O campo DE4 (Amount) está sendo logado como inteiro sem formatação. Encontrei o valor `15000` sem indicação de que são centavos. Em auditoria anterior de outra empresa, esse tipo de erro causou um incidente onde R$ 15.000,00 foi processado como R$ 150,00."

**Achado #3 — Severidade: MÉDIA**
> "O sistema aceita mensagens com Processing Code `999999` sem validação. Enviei 50 transações de teste com Processing Code inválido e todas passaram pela validação inicial. Payload chegou no emissor."

O CTO quer um relatório completo e as correções implementadas antes do fim do dia.

---

## Sua Missão

### Parte 1 — Análise dos Achados (20 min)

Crie `relatorio-pci.md` respondendo para cada achado:

1. **Achado #1 (PAN em log):**
   - Qual é o risco exato? (não genérico — seja específico sobre o que pode acontecer)
   - Qual regra PCI DSS foi violada? (Requisito específico)
   - Quais outros campos além do PAN **nunca** devem ser logados em claro?
   - Como o incidente pode ter acontecido? (ex: log de debug esquecido em produção)

2. **Achado #2 (Amount sem formatação):**
   - Reconstrua o cenário de incidente que o auditor descreveu: como exatamente alguém processaria R$ 15.000 como R$ 150?
   - Este é um problema de log ou de processamento?
   - Qual validação poderia prevenir o processamento do valor errado?

3. **Achado #3 (Processing Code sem validação):**
   - Por que é perigoso aceitar Processing Codes inválidos?
   - O que pode acontecer no emissor ao receber um Processing Code desconhecido?
   - Quem deveria ser o responsável pela validação — switch, emissor, ou ambos?

### Parte 2 — Correções Técnicas (45 min)

Implemente as correções para os três achados:

**Correção #1: `SafeAuditLog.java`**

```java
public class SafeAuditLog implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");
        ISOMsg response = ctx.get("RESPONSE");

        // Log seguro: NUNCA loga PAN completo, PIN, CVV, track data
        // PAN: primeiros 6 + últimos 4 visíveis, resto mascarado
        // Amount: sempre formatado como valor monetário (R$ X,XX)
        // Campos sensíveis ausentes do log: DE52, DE55 (apenas presença/ausência)
    }
}
```

**Correção #2: `AmountFormatter.java`**

```java
public class AmountFormatter {

    // "000000015000" → "R$ 150,00"
    public static String format(String de4Value, String currencyCode) { ... }

    // "000000015000" → 15000L (centavos, nunca reais!)
    public static long toCents(String de4Value) { ... }

    // 15000L → "000000015000" (serializa para DE4)
    public static String fromCents(long amountInCents) { ... }

    // Validação: rejeita se amount == 0 ou negativo
    public static boolean isValid(String de4Value) { ... }
}
```

**Correção #3: Adicione ao `ValidateMessage.java`**

Adicione validação de Processing Code a partir de uma lista allowlist:

```java
private static final Set<String> VALID_PROCESSING_CODES = Set.of(
    "003000", // Compra crédito
    "012000", // Saque conta corrente
    "200030", // Estorno crédito
    "300000", // Consulta saldo
    "013010"  // Saque poupança
    // adicione os demais que você conhece
);
```

Se o Processing Code não estiver na lista → ABORTED com DE39=`"12"` (Invalid transaction).

**Para cada correção, implemente testes unitários.**

### Parte 3 — Resposta ao Auditor (15 min)

Escreva `resposta-auditor.md` com:

1. Para cada achado: **o que foi corrigido, como foi testado, e quando foi para produção**
2. Uma **análise de impacto retroativo**: as transações com PAN em log precisam de alguma ação?
3. **Compromisso de prazo**: o que você entrega hoje vs o que precisa de mais tempo?
4. **Processo de prevenção**: como garantir que esses problemas não voltem? (code review, lint, testes obrigatórios)

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Análise dos 3 achados é técnica e precisa | /20 |
| SafeAuditLog nunca loga PAN/PIN completo (testado) | /25 |
| AmountFormatter correto: centavos vs reais nunca confundidos | /20 |
| Processing Code validado com allowlist funcionando | /15 |
| Resposta ao auditor é profissional e completa | /20 |

**Meta:** 80+ pontos = Semana 9 dominada.

---

## Dicas

- PCI DSS Requisito 3.3: "Mascarar o PAN quando exibido; os primeiros seis e os últimos quatro dígitos são o máximo de dígitos do PAN que podem ser exibidos."
- O erro de Amount (centavos vs reais) é surpreendentemente comum. Em 2012, um bug similar num gateway brasileiro processou transações com valor x100 por 47 minutos antes de ser detectado.
- Allowlist é sempre melhor que blocklist para validação de campos críticos. Você não sabe quais valores inválidos existem, mas sabe exatamente quais são válidos.
- Ao responder para um auditor, **nunca minimize**. Reconheça, explique o impacto real, e apresente a correção com evidência (teste passando, commit hash).
