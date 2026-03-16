# Exercícios — Semana 31 — Antifraude: Risk Scoring em Tempo Real

## Exercício 1 — Campos ISO no Fluxo de Risco

Liste quais campos ISO 8583 são consumidos pelo sistema antifraude do adquirente e explique
o sinal de risco que cada um carrega. Para cada campo, indique se ele está presente em
transações presenciais (card-present), não-presenciais (CNP) ou em ambas.

**Objetivo:** fixar a relação entre a estrutura da mensagem ISO e as decisões de risco.

---

## Exercício 2 — Visa Advanced Authorization (VAA)

A response `0110` de uma transação retorna o seguinte conteúdo no DE 44:

```
Score: 73
Reason codes: 02, 04
```

1. O que significa um score de 73 segundo o modelo VAA?
2. O que indicam os reason codes 02 e 04?
3. Como o emissor deveria reagir a esses valores? Descreva a lógica de decisão.
4. Em que cenário o emissor poderia aprovar mesmo com score 73?

**Objetivo:** interpretar a saída do Visa Advanced Authorization e traduzi-la em decisão de negócio.

---

## Exercício 3 — Velocity Rules com Redis Sorted Sets

Analise o trecho de código da `VelocityEngine` da teoria e responda:

1. Por que é usada uma sorted set (ZSet) do Redis em vez de um simples contador (`INCR`)?
2. Qual é a diferença entre janela fixa e janela deslizante? Qual o código usa?
3. A regra de terminal usa `opsForSet` em vez de `opsForZSet`. Por que essa escolha é adequada
   para o caso de "cartões únicos por terminal"? Qual é sua limitação?
4. O que acontece com as chaves Redis quando a janela de tempo expira?
   Explique o papel do `redis.expire(key, Duration.ofSeconds(windowSeconds * 2))`.

**Objetivo:** compreender a modelagem de dados em Redis para velocity rules.

---

## Exercício 4 — FraudScreeningParticipant

Complete o esqueleto abaixo implementando a integração do `FraudScreeningParticipant`
com o fluxo de risco. O participant deve:
- Checar a negative list antes do velocity check
- Adicionar o score de fraude ao contexto para log posterior
- Nunca lançar exceção para cima (fail open)

```java
public class FraudScreeningParticipant implements TransactionParticipant {

    private final VelocityEngine velocityEngine;
    private final FraudScoreService fraudScoreService;
    private final NegativeListService negativeListService;

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg req = ctx.get("REQUEST");

        try {
            String pan = req.getString(2);
            // TODO: 1. Checar se o PAN está na negative list
            //          Se estiver, setar RC "62" e retornar ABORTED

            BigDecimal amount = new BigDecimal(req.getString(4)).movePointLeft(2);
            String ip = extractIp(req);
            String terminalId = req.getString(41);

            // TODO: 2. Checar velocity
            //          Se CRITICAL, setar RC "59" e retornar ABORTED

            // TODO: 3. Calcular score de fraude
            //          Salvar score no contexto com chave "FRAUD_SCORE"
            //          Se score > 85, setar RC "59" e retornar ABORTED
            //          Se score entre 60 e 85, setar flag "RISK_FLAG" = "REVIEW"

            return PREPARED;

        } catch (Exception e) {
            // TODO: 4. Log do erro sem lançar exceção (fail open)
            return PREPARED;
        }
    }

    private String extractIp(ISOMsg msg) throws ISOException {
        // DE 48 contém IP em implementação proprietária
        String privateData = msg.getString(48);
        if (privateData == null) return null;
        // Parsing simplificado — retorne o IP extraído ou null
        return null;
    }
}
```

**Objetivo:** praticar a implementação de um `TransactionParticipant` com múltiplas etapas de risco.

---

## Exercício 5 — Análise: Tradeoff False Positive vs False Negative

Considere dois emissores com os seguintes parâmetros de antifraude:

| Emissor | Threshold de recusa | Taxa de fraude (FN%) | Taxa de falso positivo (FP%) |
|---------|---------------------|----------------------|------------------------------|
| Banco A | Score > 60          | 0,05%                | 1,20%                        |
| Banco B | Score > 80          | 0,40%                | 0,15%                        |

1. Qual emissor tem maior **Precision**? E maior **Recall**?
2. Para um emissor de cartão de crédito premium (portador de alto valor), qual estratégia é mais adequada? Por quê?
3. Para um emissor de cartão pré-pago com público jovem e muitas transações de baixo valor, qual estratégia faz mais sentido?
4. Como o Visa Advanced Authorization poderia ajudar o Banco B a reduzir sua taxa de fraude sem aumentar os falsos positivos?

**Objetivo:** raciocinar sobre o tradeoff precision/recall em contextos reais de mercado.
