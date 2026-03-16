# Exercícios — Semana 25 — Mercado Brasileiro Profundo

## Exercício 1 — Lei 12.865 e Arranjos de Pagamento (Fixação)

Antes de 2013, Cielo só aceitava Visa e Rede só aceitava Mastercard. A Lei 12.865 quebrou essa exclusividade.

Responda às perguntas abaixo com base na leitura da teoria:

1. O que a Lei 12.865/2013 proibiu que existia antes?
2. Qual órgão passou a autorizar e supervisionar arranjos de pagamento?
3. Por que o impacto técnico direto da lei é que um terminal agora precisa suportar múltiplas bandeiras?
4. Complete a tabela de arranjos autorizados no Brasil: Visa, Mastercard, Elo, _____, _____, PIX.

**Objetivo:** Compreender o marco regulatório e sua consequência na arquitetura do switch.

**Dica:** O switch precisa rotear por bandeira — a identificação vem do BIN (primeiros 6 dígitos do PAN).

---

## Exercício 2 — Diferenças técnicas do Elo no ISO 8583 (Aprofundamento)

O Elo usa especificação própria baseada em ISO 8583, com extensões nos campos DE 48, DE 60 e DE 62.

Implemente `EloBinRangeChecker` que identifica se um PAN pertence à Elo:

```java
public class EloBinRangeChecker {
    // BINs Elo representativos: 506699-506778, 509000-509099, 636368, 627780

    /**
     * Retorna true se o PAN pertence a um BIN Elo.
     * Deve suportar faixas (506699-506778) e BINs únicos (636368).
     */
    public boolean isElo(String pan) { /* ... */ }

    /**
     * Retorna o emissor Elo provável: BRADESCO, CAIXA, BB, DESCONHECIDO.
     */
    public String resolveEloBankIssuer(String pan) { /* ... */ }
}
```

Escreva testes com PANs Elo válidos e PANs Visa/Master que devem retornar `false`.

**Objetivo:** Implementar roteamento correto para a bandeira Elo.

**Dica:** A tabela de BINs Elo é mantida pela Elo Serviços S.A. e atualizada periodicamente — carregue-a de um arquivo de configuração, não hardcoded.

---

## Exercício 3 — Teto de Interchange BACEN (Intermediário)

O BACEN impôs teto de 0,5% para débito e pré-pago (Resolução BCB nº 150/2021).

Implemente `InterchangeCalculator` que calcula o interchange correto por tipo de transação:

```java
public class InterchangeCalculator {

    public record InterchangeResult(
        BigDecimal interchangeAmount,
        BigDecimal interchangeRate,
        String ruleApplied,
        boolean subjectToBacenCap
    ) {}

    /**
     * Calcula interchange para uma transação.
     * Tipos: DEBIT, CREDIT_SIGHT, CREDIT_INSTALLMENT, PREPAID
     * Para débito/pré-pago: cap 0,5%
     * Para crédito: usa tabela de bandeira (sem teto regulatório)
     */
    public InterchangeResult calculate(
        String productType,    // DEBIT, CREDIT_SIGHT, PREPAID, etc.
        BigDecimal amount,
        int installments,
        String brand           // VISA, MASTER, ELO
    ) { /* ... */ }
}
```

Teste com:
- Débito de R$ 200,00 → deve ser no máximo R$ 1,00 (0,5%)
- Crédito à vista de R$ 200,00 → usa tabela configurada (~1,5%)
- Crédito 12x de R$ 1.200,00 → usa tabela com regra de parcelamento

**Objetivo:** Entender o impacto do teto de interchange na implementação do settlement.

**Dica:** As tabelas de interchange mudam periodicamente — o switch precisa de um mecanismo de atualização sem re-deploy.

---

## Exercício 4 — Sub-adquirência e DE 42/43 (Integração)

Uma transação via iFood Pagamentos (sub-adquirente) chega ao switch do adquirente master com:
- `DE 42 = "IFOOD0000000001"` (MerchantID do sub-adquirente)
- `DE 43 = "IFOOD*SUSHI DO CHEF    SP BR"`

Implemente `SubAcquirerResolver` que identifica a estrutura:

```java
public class SubAcquirerResolver {

    public record SubAcquirerContext(
        boolean isSubAcquirer,
        String subAcquirerId,
        String realMerchantName,
        String subAcquirerName,  // extraído do DE 43
        boolean requiresExtraMonitoring
    ) {}

    /**
     * Analisa DE 42 e DE 43 para determinar se a transação
     * veio via sub-adquirente e extrai os dados relevantes.
     * Sub-adquirentes conhecidos: IFOOD*, MELI*, PGSEG*, STONE*
     */
    public SubAcquirerContext resolve(String merchantId, String merchantName) { /* ... */ }
}
```

**Objetivo:** Entender como transações de sub-adquirentes aparecem no switch master e por que o monitoramento de chargeback ratio é por sub-adquirente.

**Dica:** Para fins de fraude, o que importa é o merchant real (após o asterisco no DE 43), não o sub-adquirente.

---

## Exercício 5 — Revisão: Mercado Brasileiro

Responda cada questão:

**a)** Qual a diferença entre um arranjo de pagamento e um adquirente?

**b)** O Banco do Brasil emite um cartão Elo. O cliente usa em um terminal Stone (adquirente). A transação vai para a Elo (bandeira) e depois para o BB (emissor). É on-us ou off-us? Explique.

**c)** Um merchant vende R$ 600.000 em crédito parcelado 6x em novembro. Qual é o valor que ele recebe em novembro se NÃO antecipar? E se antecipar com taxa de 2,0% a.m.?

**d)** Por que o PIX acelerou a queda do MDR de débito mas não eliminou cartões de crédito?

**e)** O que é uma registradora (CIP, CERC, TAG) e por que o switch de um adquirente precisa se integrar a elas?

**Objetivo:** Consolidar os conceitos do mercado brasileiro antes de avançar para fluxos avançados.
