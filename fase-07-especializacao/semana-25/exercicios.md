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

## Exercício 5 — PIX com Cartão de Crédito (Intermediário)

O PIX com cartão de crédito introduz uma perna ISO 8583 (autorização de crédito) seguida de uma perna SPI (liquidação PIX). O `endToEndId` do SPI deve ser correlacionado ao STAN ISO 8583 para reconciliação.

### Parte A — Identificação da modalidade no switch

Implemente `PixCreditDetector` que analisa uma `ISOMsg` e determina se é PIX crédito:

```java
public class PixCreditDetector {

    public record PixCreditContext(
        boolean isPixCredit,
        String pixKey,            // chave DICT do recebedor (DE48 subelemento)
        String pixKeyType,        // CHAVE, QRCODE, COPIA_COLA
        String txid,              // identificador de correlação SPI
        String pixModalidade      // PIX_CREDITO_AVISTA, PIX_PARCELADO
    ) {}

    /**
     * Analisa DE3 e DE48 para identificar PIX crédito.
     * DE3 primeiros 2 dígitos = 00 (compra) com sub-elementos PIX em DE48.
     * Retorna PixCreditContext com isPixCredit=false se não for PIX crédito.
     */
    public PixCreditContext detect(ISOMsg msg) { /* ... */ }
}
```

Escreva testes com:
- Mensagem 0200 de crédito à vista (sem PIX) → `isPixCredit=false`
- Mensagem 0200 com DE48 contendo chave PIX e txid → `isPixCredit=true`, campos extraídos corretamente
- Mensagem 0200 PIX parcelado → `pixModalidade=PIX_PARCELADO`

### Parte B — Reconciliação dual-leg

Implemente `PixCreditReconciler` que cruza o arquivo de clearing ISO 8583 com o extrato SPI:

```java
public class PixCreditReconciler {

    public record ReconciliationResult(
        List<ReconciledPair> matched,       // ISO auth + SPI liquidado
        List<IsoAuth> isoWithoutSpi,        // auth aprovado mas PIX não encontrado
        List<SpiEntry> spiWithoutIso        // PIX liquidado mas sem auth ISO
    ) {}

    /**
     * Cruza autorizações ISO 8583 (por RRN/STAN) com entradas SPI (por endToEndId/txid).
     * O vínculo é feito pelo txid presente em DE48 da mensagem ISO e no extrato SPI.
     */
    public ReconciliationResult reconcile(
        List<IsoAuth> isoAuths,
        List<SpiEntry> spiEntries
    ) { /* ... */ }
}
```

Teste com cenários de:
- Par ISO + SPI casados corretamente
- ISO aprovado mas PIX ausente (falha na Leg 2)
- PIX presente mas ISO rejeitado (situação anômala que indica problema de idempotência)

**Objetivo:** Entender a complexidade de reconciliação em sistemas dual-leg e por que o txid/endToEndId é o campo de correlação crítico.

**Dica:** Em produção, um ISO aprovado sem SPI correspondente é um alerta crítico — significa que o portador foi cobrado mas o merchant não recebeu.

---

## Exercício 6 — On-Us e Off-Us no PIX com Cartão de Crédito (Avançado)

No PIX com cartão de crédito, o on-us/off-us existe em duas dimensões independentes: a perna do cartão (ISO 8583) e a perna do PIX (SPI). O switch precisa identificar ambas para rotear corretamente e calcular os custos da transação.

Implemente `PixCreditOnUsClassifier` que determina as duas dimensões de roteamento:

```java
public class PixCreditOnUsClassifier {

    public record PixCreditRouting(
        boolean cardOnUs,          // PSP iniciador == emissor do cartão
        boolean pixOnUs,           // emissor == PSP do recebedor (via DICT)
        String cardRoute,          // "INTERNAL_ISSUER" ou "CARD_NETWORK"
        String pixRoute,           // "INTERNAL_TRANSFER" ou "SPI"
        BigDecimal interchangeFee, // zero se cardOnUs=true
        boolean strRequired        // true se pixOnUs=false (precisa do SPI/STR)
    ) {}

    /**
     * Classifica uma transação PIX Crédito nas duas dimensões on-us/off-us.
     *
     * @param cardBin        BIN do cartão (DE2, primeiros 6-8 dígitos)
     * @param initiatorPspId Identificador do PSP que iniciou a transação
     * @param pixKey         Chave PIX do recebedor (para lookup no DICT)
     */
    public PixCreditRouting classify(
        String cardBin,
        String initiatorPspId,
        String pixKey
    ) { /* ... */ }
}
```

Escreva testes cobrindo os 4 cenários da matriz:

| Teste | BIN emissor | PSP iniciador | PSP do recebedor | Resultado esperado |
|-------|------------|--------------|-----------------|-------------------|
| 1 | Itaú | Itaú | Itaú | cardOnUs=true, pixOnUs=true |
| 2 | Itaú | Itaú | Nubank | cardOnUs=true, pixOnUs=false |
| 3 | Bradesco | PicPay | Bradesco | cardOnUs=false, pixOnUs=true |
| 4 | Bradesco | PicPay | Nubank | cardOnUs=false, pixOnUs=false |

Responda também:

**a)** Em qual dos 4 cenários o interchange é zero? Por quê?

**b)** Em qual cenário o `strRequired=false`? O que isso significa em termos de custo operacional para o emissor?

**c)** Um banco grande como o Itaú tem interesse em maximizar qual cenário? Como isso influencia a estratégia de produto (e.g., oferecer PIX crédito só para clientes correntistas Itaú que recebem em conta Itaú)?

**d)** Se o PSP do recebedor estiver fora do ar mas o SPI estiver disponível, qual dimensão de on-us/off-us é afetada? O que o switch deve fazer?

**Objetivo:** Entender que o on-us/off-us no PIX crédito é bidimensional e que decisões de produto (quem pode usar, quais recebedores) são influenciadas diretamente por custos de roteamento.

**Dica:** Para resolver a dimensão PIX, o switch consulta o DICT (Diretório de Identificadores de Contas Transacionais) do BACEN via API para descobrir qual PSP está associado à chave PIX do recebedor.

---

## Exercício 7 — Revisão: Mercado Brasileiro

Responda cada questão:

**a)** Qual a diferença entre um arranjo de pagamento e um adquirente?

**b)** O Banco do Brasil emite um cartão Elo. O cliente usa em um terminal Stone (adquirente). A transação vai para a Elo (bandeira) e depois para o BB (emissor). É on-us ou off-us? Explique.

**c)** Um merchant vende R$ 600.000 em crédito parcelado 6x em novembro. Qual é o valor que ele recebe em novembro se NÃO antecipar? E se antecipar com taxa de 2,0% a.m.?

**d)** Por que o PIX acelerou a queda do MDR de débito mas não eliminou cartões de crédito?

**e)** O que é uma registradora (CIP, CERC, TAG) e por que o switch de um adquirente precisa se integrar a elas?

**Objetivo:** Consolidar os conceitos do mercado brasileiro antes de avançar para fluxos avançados.
