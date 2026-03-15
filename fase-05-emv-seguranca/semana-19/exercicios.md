# Semana 19 — Exercícios de Fixação

## Exercício 1 — CP vs CNP: Mapeamento de Campos (Nível: Iniciante)

Preencha a tabela comparativa sem consultar a teoria:

| Campo ISO 8583 | Valor em CP (Presencial) | Valor em CNP (E-commerce) | Por quê difere? |
|----------------|--------------------------|--------------------------|-----------------|
| DE 22 (POS Entry Mode) | 051 (chip) | | |
| DE 25 (POS Condition Code) | 00 | | |
| DE 55 (EMV Data) | Presente | | |
| DE 52 (PIN) | Pode estar presente | | |
| Autenticação do portador | Chip + PIN | | |
| Liability shift (chargeback) | Emissor | | |
| Interchange rate | Menor | | |

Depois responda:
1. Por que transações CNP têm interchange maior que CP?
2. Por que o liability shift é diferente entre CP e CNP?
3. O que é 3DS e como ele transfere o liability de volta para o emissor?

**Critério de sucesso:** Tabela completa e correta, 3 perguntas respondidas com precisão.

---

## Exercício 2 — Identificar o Tipo de Transação pelos Campos (Nível: Iniciante/Intermediário)

Para cada combinação de campos abaixo, identifique: é CP ou CNP? E qual modalidade específica?

1. `DE22=051, DE25=00, DE55=presente` → ?
2. `DE22=071, DE25=00, DE55=presente (sem 9F34)` → ?
3. `DE22=012, DE25=08` → ?
4. `DE22=810, DE25=59` → ?
5. `DE22=082, DE25=59` → ? (dica: 082 = e-commerce com 3DS)
6. `DE22=100, DE25=00` → ? (dica: 100 = credential on file)
7. `DE22=801, DE25=00` → ? (dica: 801 = fallback de chip)

Para cada um: identifique o tipo, diga o risco de fraude (alto/médio/baixo) e quem absorve o chargeback em caso de fraude.

**Critério de sucesso:** 7 de 7 corretos, com análise de risco e liability.

---

## Exercício 3 — Implemente Distinção CP/CNP (Nível: Intermediário)

Implemente `ChannelClassifier` como `TransactionParticipant`:

```java
public class ChannelClassifier implements TransactionParticipant {

    public enum TransactionChannel {
        CHIP_CONTACT,
        CHIP_CONTACTLESS,
        MAGNETIC_STRIPE,
        FALLBACK,
        ECOMMERCE_3DS,
        ECOMMERCE_NO_3DS,
        MOTO,
        CREDENTIAL_ON_FILE_CIT,
        CREDENTIAL_ON_FILE_MIT,
        UNKNOWN
    }

    /**
     * Classifica a transação e armazena o canal no contexto.
     * Também valida se os campos obrigatórios para o canal estão presentes.
     */
    @Override
    public int prepare(long id, Serializable context) { /* ... */ }
}
```

Regras de classificação:
- DE22=051 → CHIP_CONTACT (DE55 obrigatório)
- DE22=071 → CHIP_CONTACTLESS (DE55 obrigatório)
- DE22=090 → MAGNETIC_STRIPE
- DE22=801 → FALLBACK (logar warning)
- DE22=082, DE25=59 → ECOMMERCE_3DS
- DE22=010, DE25=59 → ECOMMERCE_NO_3DS
- DE22=010, DE25=08 → MOTO
- DE22=100 → CREDENTIAL_ON_FILE (verificar DE48 para CIT vs MIT)

Escreva testes para cada canal.

**Critério de sucesso:** Classificação correta para todos os canais, validação de campos obrigatórios.

---

## Exercício 4 — Credential on File: Fluxo CIT → MIT (Nível: Intermediário)

Implemente o fluxo completo de Credential on File:

**Transação CIT (primeira, iniciada pelo portador):**
```java
public class CITTransaction {
    /**
     * Processa a transação inicial onde o portador autoriza
     * o armazenamento do cartão.
     * Deve armazenar o Network Transaction ID retornado pela bandeira.
     */
    public String process(ISOMsg msg) throws ISOException { /* ... */ }
}
```

**Transação MIT (recorrente, iniciada pelo merchant):**
```java
public class MITTransaction {
    /**
     * Processa cobranças recorrentes usando o token/credencial armazenado.
     * Deve incluir o Network Transaction ID da CIT original.
     */
    public void process(String networkTxId, BigDecimal amount) throws ISOException { /* ... */ }
}
```

Implemente um teste de integração que:
1. Processa CIT (portador cadastra cartão em serviço de streaming)
2. Armazena o Network Transaction ID
3. 30 dias depois: processa MIT (cobrança automática)
4. Verifica que a MIT tem o Network Transaction ID correto no campo adequado
5. Verifica que a MIT não tem DE52 (PIN) — portador não está presente

**Critério de sucesso:** Fluxo CIT→MIT funcional, Network Transaction ID persistido e reutilizado.

---

## Exercício 5 — Tokenização: FPAN vs DPAN (Nível: Intermediário/Avançado)

Implemente um mock simplificado do Token Service Provider (TSP):

```java
public class MockTokenServiceProvider {
    // Vault: DPAN → FPAN
    private final Map<String, String> tokenVault = new HashMap<>();

    /**
     * Cria um token (DPAN) para um FPAN.
     * Retorna o DPAN gerado.
     */
    public String provision(String fpan, String deviceId) { /* ... */ }

    /**
     * Converte DPAN de volta para FPAN (de-tokenização).
     * Retorna null se token inválido/expirado.
     */
    public String detokenize(String dpan) { /* ... */ }

    /**
     * Invalida um token específico sem cancelar o FPAN.
     */
    public void invalidate(String dpan) { /* ... */ }
}
```

Depois implemente um `TokenAwareParticipant` que:
1. Detecta se o DE 2 é um DPAN (baseado em algum prefixo ou flag — defina sua própria convenção)
2. Chama `detokenize` para obter o FPAN
3. Substitui o DE 2 na mensagem pelo FPAN antes de rotear para o emissor
4. Loga que a de-tokenização foi realizada (sem logar FPAN completo)

Escreva testes para: tokenização bem-sucedida, de-tokenização, invalidação de token, token inválido.

**Critério de sucesso:** TSP mock funcional, de-tokenização transparente para o emissor.

---

## Exercício 6 — Account Updater: O Problema do Cartão Expirado (Nível: Avançado)

Um merchant de streaming tem 50.000 credenciais armazenadas (COF). A cada mês, cerca de 2% dos cartões expiram ou são reemitidos.

Implemente `AccountUpdaterProcessor`:

```java
public class AccountUpdaterProcessor {
    /**
     * Simula o processo de Account Updater.
     * Para cada credencial armazenada:
     * - Se cartão expirado: buscar novo PAN/data via mock do emissor
     * - Se PAN mudou: atualizar no vault
     * - Se conta encerrada: marcar como inativa
     */
    public AccountUpdaterReport process(List<StoredCredential> credentials) { /* ... */ }
}

public record AccountUpdaterReport(
    int updated,    // credenciais atualizadas
    int closed,     // contas encerradas
    int unchanged,  // sem mudanças
    int errors,     // falhas
    List<String> updatedPans  // PANs mascarados que foram atualizados
) {}
```

Escreva um teste que processa 1.000 credenciais com:
- 2% com data de validade expirada (PAN mesmo, nova data)
- 1% com cartão reemitido (PAN diferente)
- 0.5% com conta encerrada

**Critério de sucesso:** Relatório correto, PANs mascarados no relatório (nunca expostos).

---

## Exercício 7 — Análise: Recurring Payment Failures (Nível: Avançado)

Um serviço de streaming com 100.000 assinantes processa cobranças mensais. Analise:

1. **Taxa de falha esperada:** Se 2% dos cartões expiram por mês, 1% têm problema de saldo, 0.5% foram bloqueados por suspeita de fraude: qual a taxa total de falha na cobrança mensal?

2. **Estratégia de retry:** Quantas vezes deve tentar? Com qual intervalo? Se o portador não atualiza o cartão em 30 dias: suspender ou cancelar a conta?

3. **Impacto financeiro:** Com 100.000 assinantes a R$ 39,90/mês: quanto em receita está em risco por falhas de cobrança? Qual o ROI de implementar Account Updater?

4. **Campos ISO 8583 que mudam:** Compare os campos de uma CIT (mês 1) vs MIT (mês 2) vs MIT após Account Updater (mês 7 após renovação do cartão).

Documente em `recurring-payment-analysis.md`.

**Critério de sucesso:** Análise numérica correta, estratégia de retry justificada, campos ISO corretos.
