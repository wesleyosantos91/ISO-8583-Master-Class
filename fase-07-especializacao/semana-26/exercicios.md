# Exercícios — Semana 26 — Fluxos Avançados de Autorização

## Exercício 1 — Pre-Authorization: Campos e Janela de Validade (Fixação)

Dado o seguinte cenário de check-in de hotel:

```
t=0:   Hóspede faz check-in. Hotel pré-autoriza R$ 800,00.
t+3d:  Hóspede adiciona jantar no restaurante (R$ 120,00).
t+5d:  Check-out. Valor final: R$ 720,00 (não consumiu o café da manhã).
```

Responda:
1. Qual o valor e `DE 25` da mensagem 0100 no check-in?
2. O completion (0200) deve ter `DE 25` igual a que valor?
3. O que acontece com a diferença de R$ 80,00 (800 - 720)?
4. Se o hóspede não fizer check-out após 30 dias, o que deve acontecer com a pré-autorização?
5. O campo `DE 90` no completion deve conter o quê?

**Objetivo:** Fixar os campos e regras de validade da pre-authorization.

**Dica:** A pré-auth expira — o adquirente não deve tentar completar uma pré-auth expirada; o emissor retornará DE39=69.

---

## Exercício 2 — Implemente PreAuthParticipant Completo (Intermediário)

Implemente o `PreAuthParticipant` com suporte a pre-auth, completion e cancelamento:

```java
public class PreAuthParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg req = ctx.get("REQUEST");
        String posCondition = req.getString(25);

        // DE25=06 → pre-auth: salvar com expiração de 7 dias
        // DE25=12 → completion: buscar pré-auth pelo DE38 (auth code original)
        //           verificar se não expirou
        //           verificar se valor do completion <= valor original + 15%
        // DE25=06 em MTI 0400 → cancelamento da pre-auth
        /* ... */
    }
}
```

Requisitos:
- Persiste `PreAuth` com: authCode, panLast4, amount, expiryDate, status (OPEN, COMPLETED, CANCELLED, EXPIRED)
- Completion com valor acima do original + 15% deve retornar DE39=61
- Pre-auth não encontrada retorna DE39=25

**Objetivo:** Implementar o ciclo completo de pre-authorization de forma defensiva.

---

## Exercício 3 — Partial Approval e Split Tender (Intermediário)

Um portador tem cartão pré-pago com saldo de R$ 75,00 e tenta comprar R$ 120,00.

Implemente `PartialApprovalHandler` e o fluxo de split tender:

```java
public class PartialApprovalHandler implements TransactionParticipant {
    @Override
    public int prepare(long id, Serializable context) {
        // Se DE39=10 na resposta do emissor:
        //   1. Registrar o valor aprovado (DE4 da response)
        //   2. Calcular o valor remanescente
        //   3. Marcar a transação para exibição ao portador
        //   4. Se portador recusar o valor parcial → enviar 0400 reversal
        /* ... */
    }
}
```

Responda também:
- Qual campo habilita o terminal a receber aprovação parcial? (`DE 25 = ?`)
- O que acontece se o terminal não suportar partial approval e receber `DE39=10`?
- Como o terminal deve exibir a mensagem ao portador?

**Objetivo:** Implementar o handler de aprovação parcial com tratamento correto do split tender.

---

## Exercício 4 — Balance Inquiry e Parsing do DE 54 (Intermediário/Avançado)

Implemente `BalanceInquiryHandler` e `DE54Parser`:

```java
public class DE54Parser {
    // Cada sub-registro tem 20 chars:
    // Pos 1-2: Account Type (10=savings, 20=checking, 30=credit, 40=universal)
    // Pos 3-4: Amount Type  (01=ledger, 02=available)
    // Pos 5-7: Currency     (986=BRL)
    // Pos 8:   Sign         (C=crédito/positivo, D=débito/negativo)
    // Pos 9-20: Amount (12 dígitos, centavos)

    public List<AccountBalance> parse(String de54) { /* ... */ }

    /**
     * Formata DE54 para exibição:
     * "Conta Corrente: R$ 1.230,00 disponível"
     */
    public String format(List<AccountBalance> balances) { /* ... */ }
}
```

Teste com:
- `"201001986C000000123000"` → Conta corrente, saldo contábil, BRL, R$ 1.230,00
- `"302001986C000000050000"` → Conta crédito, saldo disponível, BRL, R$ 500,00
- Múltiplos sub-registros concatenados (40 chars)

**Objetivo:** Dominar o campo DE 54 que carrega saldos em balance inquiry.

**Dica:** DE 3 = 312000 identifica balance inquiry no processing code.

---

## Exercício 5 — Revisão: Fluxos Avançados

**a)** Um hotel registra uma pre-auth de R$ 1.000,00 com `DE25=06`. No checkout, o valor final é R$ 1.130,00 (hóspede consumiu mais que o estimado). O completion de R$ 1.130,00 deve ser aceito? Por quê? Qual o campo que define o limite?

**b)** Qual a diferença entre MIT tipo `01` (Recurring) e tipo `03` (Unscheduled)? Dê um exemplo real de cada.

**c)** Uma recarga de celular automática (quando o saldo cai abaixo de R$ 10) é CIT ou MIT? Qual `DE 22` usar? O que é obrigatório incluir para provar o acordo inicial do portador?

**d)** Um ATM retorna `DE39=10` (partial approval) para um saque de R$ 500 com apenas R$ 300 disponível. O que o ATM deve fazer? Isso é diferente de um POS — por quê?

**e)** Cash advance via POS tem `DE 3 = 01x000`. Se a transação for de débito em conta corrente, qual o valor completo do DE 3?

**Objetivo:** Consolidar os fluxos avançados antes do PCI-DSS e certificação.
