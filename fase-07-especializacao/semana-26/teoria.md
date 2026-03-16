# Semana 26 — Fluxos Avançados de Autorização

## Por que estes fluxos são avançados?

Os fluxos das semanas 9-12 cobriram o caso mais comum: compra à vista com aprovação imediata. A realidade do mercado tem dezenas de variações — hotel, posto de gasolina, saque ATM, assinatura recorrente, aprovação parcial. Cada uma tem campos, MTIs e lógica específica. Dominar esses fluxos é o que diferencia quem implementa switches de produção de quem só conhece o happy path.

---

## 1. Pre-Authorization (Pré-Autorização)

### 1.1 Quando usar

Usada quando o valor final não é conhecido no momento da autorização:

| Caso de uso | Por quê pré-autoriza |
|-------------|---------------------|
| Check-in de hotel | Não sabe quanto o hóspede vai consumir |
| Locadora de carro | Não sabe danos, combustível, extras |
| Posto de gasolina | Autoriza antes de bombear |
| Restaurante (gorjeta) | Valor final pode incluir gorjeta |
| Marketplace (retém até enviar) | Confirma quando produto é despachado |

### 1.2 Campos ISO 8583

```
Pre-authorization request (0100):
  DE 3  = 000000  (compra normal) ou código específico
  DE 25 = 06      ← POS Condition Code: Pre-authorized request
  DE 4  = valor estimado (pode ser maior que o final)

Completion (0200 ou 0220):
  DE 25 = 12      ← Completion — confirma e captura
  DE 4  = valor real (pode ser diferente da pre-auth)
  DE 38 = código de autorização original (obrigatório)
  DE 90 = dados da mensagem original (MTI + STAN + DateTime)

Cancel pre-auth (0400):
  DE 25 = 06
  DE 90 = dados da pre-auth original
```

### 1.3 Janela de validade

```
Pre-auth tem prazo máximo antes de expirar:
  Hotel:       30 dias (Visa), 31 dias (Master)
  Locadora:    30 dias
  Restaurante: 3 dias
  Padrão:      7 dias

Após expirar, o emissor pode liberar o limite automaticamente.
O adquirente NÃO deve tentar completar uma pre-auth expirada.
```

### 1.4 Implementação

```java
public class PreAuthParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg req = ctx.get("REQUEST");

        String posCondition = req.getString(25);

        if ("06".equals(posCondition)) {
            // É uma pre-auth — marca para não enviar ao clearing automaticamente
            ctx.put("IS_PRE_AUTH", true);
            ctx.put("PRE_AUTH_EXPIRY", LocalDate.now().plusDays(7));
        } else if ("12".equals(posCondition)) {
            // É um completion — busca a pre-auth original
            String originalAuthCode = req.getString(38);
            PreAuth original = preAuthRepo.findByAuthCode(originalAuthCode)
                .orElseThrow(() -> new PreAuthNotFoundException(originalAuthCode));

            if (original.isExpired()) {
                ctx.put("RESPONSE_CODE", "69"); // Contact card issuer
                return ABORTED;
            }

            ctx.put("ORIGINAL_PRE_AUTH", original);
            ctx.put("IS_COMPLETION", true);
        }

        return PREPARED;
    }
}
```

---

## 2. Incremental Authorization

### 2.1 O que é

Permite **aumentar** o valor de uma autorização já existente sem fazer nova pre-auth:

```
t=0:  Hotel autoriza R$ 500 (check-in)
t+2d: Hóspede usa mini-bar: adiciona R$ 80 → total R$ 580
t+5d: Room service: adiciona R$ 120 → total R$ 700
t+7d: Check-out: completion por R$ 700
```

Sem incremental, o hotel teria que fazer uma nova pre-auth a cada consumo, travando o limite do portador.

### 2.2 Campos ISO 8583

```
Incremental request (0100):
  DE 25 = 10      ← POS Condition Code: Incremental
  DE 4  = valor DO INCREMENTO (não o total)
  DE 38 = auth code da pre-auth original
  DE 90 = dados da mensagem original

Incremental response (0110):
  DE 39 = 00 (aprovado)
  DE 38 = novo auth code (para o incremento)
```

### 2.3 Limite e regras

```
Visa: incremental authorization program — requer participação
Mastercard: permitido por padrão para merchant category codes específicos

Limites:
  Visa Hotel:    até 15% acima do valor da pre-auth
  Visa Car:      até 15%
  Se ultrapassar: nova pre-auth necessária
```

---

## 3. Partial Approval (Aprovação Parcial)

### 3.1 O que é e quando ocorre

Ocorre quando o emissor **aprova menos que o valor solicitado**:

```
Portador solicita: R$ 300 (saldo disponível no cartão pré-pago: R$ 180)

Sem partial approval:
  Emissor → DE39=51 (insufficient funds) → transação negada

Com partial approval:
  Emissor → DE39=10 (partial approval) → DE4=000000018000 (R$ 180 aprovado)
```

Muito comum em:
- Cartões pré-pagos (gift cards, cartões de benefício)
- Cartões de débito com saldo parcial
- Cartões com limite parcialmente disponível

### 3.2 Campos ISO 8583

```
Request (0200):
  DE 3  = Código de processamento normal
  Sem campo específico — o merchant habilita via DE 25 = 59 (partial approval capable)

Response com partial approval (0210):
  DE 39 = 10      ← Response Code: Partial Approval
  DE 4  = valor APROVADO (menor que o solicitado)
  DE 44 = valor original solicitado (em alguns arranjos)

O terminal DEVE:
  1. Verificar se DE39=10
  2. Exibir ao portador: "Aprovado parcialmente: R$ 180"
  3. Perguntar se quer completar com outro meio de pagamento (split tender)
  4. Se portador recusar: enviar reversal pelo valor aprovado
```

### 3.3 Split Tender (Pagamento Dividido)

```
Compra: R$ 300
  Passo 1: Débito pré-pago parcial: R$ 180 (DE39=10)
  Passo 2: Portador escolhe completar com crédito
  Passo 3: Novo 0200 por R$ 120 no cartão de crédito (DE39=00)
  Resultado: Compra de R$ 300 completada com 2 transações
```

### 3.4 Implementação no switch

```java
public class PartialApprovalHandler implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg response = ctx.get("RESPONSE");

        if (response != null && "10".equals(response.getString(39))) {
            BigDecimal requestedAmount = new BigDecimal(ctx.get("REQUEST_AMOUNT").toString())
                .movePointLeft(2);
            BigDecimal approvedAmount = new BigDecimal(response.getString(4))
                .movePointLeft(2);

            ctx.put("IS_PARTIAL_APPROVAL", true);
            ctx.put("APPROVED_AMOUNT", approvedAmount);
            ctx.put("REMAINING_AMOUNT", requestedAmount.subtract(approvedAmount));

            // Logar para monitoramento
            log.info("Partial approval: requested={} approved={} remaining={}",
                requestedAmount, approvedAmount,
                requestedAmount.subtract(approvedAmount));
        }
        return PREPARED;
    }
}
```

---

## 4. Balance Inquiry

### 4.1 O que é

Consulta de saldo sem movimentação financeira. Usado em ATMs e POS para verificar saldo antes de sacar/comprar.

### 4.2 Campos ISO 8583

```
Balance Inquiry Request (0100 ou 0200):
  DE 3  = 312000  ← Processing Code: Balance Inquiry
  DE 4  = 000000000000  (zero — sem valor)
  DE 25 = 00 (normal) ou 01 (unattended)

Balance Inquiry Response (0110 ou 0210):
  DE 39 = 00 (sucesso)
  DE 54 = campo de saldos (Additional Amounts)
```

### 4.3 DE 54 — Additional Amounts

DE 54 é um campo LLLVAR com múltiplos sub-registros de 20 chars cada:

```
Formato de cada sub-registro (20 chars):
  Posição 1-2:   Account Type (00=default, 10=savings, 20=checking, 30=credit)
  Posição 3-4:   Amount Type  (01=ledger balance, 02=available balance, ...)
  Posição 5-7:   Currency Code (986=BRL)
  Posição 8:     Sign (C=credit/positive, D=debit/negative)
  Posição 9-20:  Amount (12 dígitos, centavos)

Exemplo:
  DE 54 = "402001986C000000050000"
           ↑↑ ↑↑ ↑↑↑ ↑ ↑↑↑↑↑↑↑↑↑↑↑↑
           40 20 986 C 000000050000
           = Conta crédito, saldo disponível, BRL, +R$500,00
```

### 4.4 Implementação

```java
public class BalanceInquiryParser {

    public record AccountBalance(
        String accountType,
        String amountType,
        String currency,
        boolean isCredit,
        BigDecimal amount
    ) {}

    public List<AccountBalance> parse(String de54) {
        List<AccountBalance> balances = new ArrayList<>();
        int offset = 0;

        while (offset + 20 <= de54.length()) {
            String record = de54.substring(offset, offset + 20);
            balances.add(new AccountBalance(
                record.substring(0, 2),          // Account Type
                record.substring(2, 4),          // Amount Type
                record.substring(4, 7),          // Currency
                "C".equals(record.substring(7, 8)), // Sign
                new BigDecimal(record.substring(8)).movePointLeft(2) // Amount
            ));
            offset += 20;
        }
        return balances;
    }
}
```

---

## 5. Recurring Transactions / Credential on File Avançado

### 5.1 CIT vs MIT — Revisão aprofundada

```
CIT (Cardholder Initiated Transaction):
  Portador está presente (fisicamente ou online)
  Portador inicia a transação
  DE 22 = 0x1 (e-com) ou 051 (chip)
  Exempos: primeira compra, primeira assinatura

MIT (Merchant Initiated Transaction):
  Portador NÃO está presente
  Merchant dispara baseado em acordo prévio
  DE 22 = 100 (stored credential)
  Exemplos: mensalidade, parcela, renovação automática
```

### 5.2 Campos obrigatórios MIT

```
MIT Request (0100 ou 0200):
  DE 22 = 100     ← Stored Credential — MIT
  DE 48, subelem. = Tipo MIT:
    01 = Recurring (recorrência)
    02 = Installment (parcelamento — portador iniciou)
    03 = Unscheduled (data não fixa, ex: recarga de saldo)
    04 = Incremental
    05 = Resubmission (nova tentativa após decline)
    06 = Reauthorization (nova auth após pre-auth expirar)
    07 = Delayed Charge (cobrança posterior — ex: hotel check-out)
    08 = No-show (penalidade por não comparecimento)

  DE 48, subelem. Network Transaction ID:
    = Transaction ID da CIT original (obrigatório!)
    Prova que houve acordo inicial com o portador
```

### 5.3 No-Show e Penalidades

```
No-show: portador reservou hotel mas não compareceu.
  Hotel pode cobrar penalidade mesmo sem o portador presente.

  Request (0100):
    DE 25 = 71      ← POS Condition Code: No-show / penalty
    DE 22 = 100     ← Stored Credential
    DE 48           ← MIT type 08 + original Network Transaction ID
    DE 4  = valor da penalidade
```

---

## 6. Cash Advance e Saque em Caixa

### 6.1 Diferença entre cash advance e saque ATM

| Aspecto | Saque ATM | Cash Advance (POS) |
|---------|-----------|-------------------|
| DE 3 | 01x000 | 01x000 |
| DE 25 | 01 | 00 |
| Terminal | ATM | POS com função saque |
| Teclado PIN | Sempre | Sempre |
| Interchange | Específico ATM | Específico POS |
| Limite | Saldo / limite saque | Limite cash advance |

### 6.2 Processing Code para cash

```
DE 3 — Processing Code (6 dígitos):
  Posição 1-2: Tipo de transação
    00 = Purchase (compra)
    01 = Cash withdrawal (saque)
    09 = Purchase with cashback
    20 = Refund
    31 = Balance inquiry

  Posição 3-4: From account (conta débito)
    00 = Default
    10 = Savings (poupança)
    20 = Checking (corrente)
    30 = Credit (crédito)

  Posição 5-6: To account
    00 = Default
    (para saque: sempre 00)
```

---

## Resumo da Semana

| Fluxo | Campo chave | Caso de uso principal |
|-------|-------------|----------------------|
| Pre-auth | DE 25=06 / DE 25=12 | Hotel, locadora, posto |
| Incremental | DE 25=10 | Hotel (consumos adicionais) |
| Partial approval | DE 39=10 | Pré-pago, gift card |
| Balance inquiry | DE 3=312000, DE 54 | ATM, POS |
| MIT/Recurring | DE 22=100, Network TX ID | Assinaturas, parcelamento |
| No-show | DE 25=71 | Hotel, companhia aérea |
| Cash advance | DE 3=01x000 | Saque no caixa |
