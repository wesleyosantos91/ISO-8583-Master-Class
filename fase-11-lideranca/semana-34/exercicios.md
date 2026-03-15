# Exercícios — Semana 34 — Comunicação Técnica e Liderança

## Exercício 1 — Quando Escrever (ou Não) uma RFC

Para cada situação abaixo, decida se uma RFC é necessária e justifique em 2-3 frases:

1. Você quer mudar o timeout de socket de 30s para 25s na conexão com a Mastercard.
2. Você propõe migrar o storage de SAF de PostgreSQL para Redis Streams, afetando
   o processo de reconciliação do time financeiro.
3. Você corrigiu um bug onde o DE 90 não era populado corretamente nos reversals.
4. Você quer implementar suporte a parcelamento em 12x, o que exige coordenação com
   3 emissores e mudança no formato do DE 48 proprietário.
5. Você quer renomear uma variável interna de `txnAmt` para `transactionAmount`
   em um único arquivo Java.

**Objetivo:** desenvolver o julgamento sobre quando documentar formalmente uma decisão.

---

## Exercício 2 — Escrever um ADR

Escreva um ADR completo para a seguinte decisão já tomada no seu switch:

> **Decisão:** Usar chave de correlação composta por `STAN + TerminalID` para deduplicação,
> em vez de usar apenas o `STAN`.

Use o template da teoria (Contexto, Decisão, Consequências positivas/negativas/neutras).
O ADR deve ter no mínimo:
- Contexto com o problema técnico que forçou a decisão (por que STAN sozinho não basta?)
- Decisão com a justificativa
- Pelo menos 2 consequências positivas e 2 negativas

**Objetivo:** praticar a documentação de decisões arquiteturais com o formato ADR.

---

## Exercício 3 — Apresentação para Diferentes Audiências

O seu switch teve um incidente: o emissor Banco X ficou indisponível por 22 minutos,
causando 1.847 transações recusadas com RC `91`. O circuit breaker ativou o stand-in
após 4 minutos e as transações seguintes foram processadas via stand-in.

Escreva a comunicação do incidente para cada audiência:

**3a. Para o CTO (máximo 5 frases):**
Use o template da teoria. Inclua: o que aconteceu, impacto em números, como foi
tratado e qual ação de follow-up.

**3b. Para o time de produto (máximo 10 linhas):**
O produto quer saber se os clientes foram impactados e o que muda na experiência
do usuário final. Explique sem jargão de protocolo.

**3c. Para o time de operações (máximo 8 linhas):**
Explique o que o alerta `CIRCUIT_BREAKER_OPEN` significa, como identificar no Grafana
e qual é o passo-a-passo quando ele dispara novamente.

**Objetivo:** adaptar a mesma informação técnica para três audiências com necessidades distintas.

---

## Exercício 4 — Code Review em Sistemas de Pagamento

Revise o seguinte trecho de código e aponte **todos** os problemas encontrados.
Para cada problema, use o formato da teoria: explique o problema e sugira a correção.

```java
@Service
public class AuthorizationService {

    public AuthResult authorize(String pan, double amount, String terminalId) {
        // Verifica se o valor é válido
        if (amount <= 0) {
            log.info("Transação negada. PAN: " + pan + ", amount: " + amount);
            return AuthResult.declined("13");
        }

        // Busca limite do cartão
        CardLimit limit = cardLimitRepo.findByPan(pan);
        if (limit == null) {
            return AuthResult.declined("14");
        }

        // Verifica se tem saldo
        double available = limit.getLimit() - limit.getUsed();
        if (amount > available) {
            return AuthResult.declined("51");
        }

        // Atualiza o saldo usado
        limit.setUsed(limit.getUsed() + amount);
        cardLimitRepo.save(limit);

        // Aprova
        return AuthResult.approved("00");
    }
}
```

Dica: há pelo menos 5 problemas distintos. Alguns envolvem segurança, outros
envolvem aritmética financeira e outros envolvem idempotência.

**Objetivo:** aplicar o checklist de code review de sistemas de pagamento.

---

## Exercício 5 — Análise: Boas Práticas de RFC

Leia o trecho de RFC da teoria (RFC-042 sobre 3DS 2.0) e responda:

1. A seção "Impacto de Performance" menciona "+50ms (frictionless) ou +3s (step-up)".
   Por que é importante quantificar esse impacto na RFC? O que acontece se essa estimativa
   estiver errada em 5x?
2. A RFC menciona "queda de 2% na taxa de conversão (step-up force)". Quem deve
   avaliar se esse tradeoff vale a pena: o autor da RFC ou o time de produto? Como
   a RFC deve lidar com essa decisão?
3. O que está faltando na RFC de exemplo que o template completo exige?
   Liste pelo menos 3 seções ausentes e explique por que cada uma importa.

**Objetivo:** avaliar criticamente uma RFC e identificar o que a torna completa ou incompleta.
