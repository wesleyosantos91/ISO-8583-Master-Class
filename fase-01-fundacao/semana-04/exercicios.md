# Semana 4 — Exercícios de Fixação

## Exercício 1 — Matriz de Mensagens (Nível: Iniciante)

Crie uma tabela completa com todos os MTIs principais:

| MTI  | Quem envia | Quem recebe | Espera resposta? | Cenário de uso |
|------|-----------|-------------|------------------|----------------|
| 0100 |           |             |                  |                |
| 0110 |           |             |                  |                |
| 0120 |           |             |                  |                |
| 0130 |           |             |                  |                |
| 0200 |           |             |                  |                |
| 0210 |           |             |                  |                |
| 0220 |           |             |                  |                |
| 0230 |           |             |                  |                |
| 0400 |           |             |                  |                |
| 0410 |           |             |                  |                |
| 0420 |           |             |                  |                |
| 0430 |           |             |                  |                |
| 0500 |           |             |                  |                |
| 0510 |           |             |                  |                |
| 0800 |           |             |                  |                |
| 0810 |           |             |                  |                |

**Critério de sucesso:** Tabela completa e correta, sem consultar.

---

## Exercício 2 — Sequência de Mensagens (Nível: Intermediário)

Para cada cenário, liste a sequência completa de MTIs trocados, com setas indicando direção:

### Cenário A — Compra dual message com clearing
```
Compra de R$100 com crédito Visa, aprovada.
D+0: autorização
D+1: clearing

Liste todas as mensagens na ordem.
```

### Cenário B — Saque ATM (single message)
```
Saque de R$500 em ATM Bradesco, cartão Itaú (off-us).
Single message — financeiro na mesma mensagem da autorização.

Liste todas as mensagens.
```

### Cenário C — Timeout com retransmissão
```
Compra de R$200. Timeout no emissor após 30s.
Terminal retransmite. Recebe resposta. Envia reversal preventivo.

Liste todas as mensagens com timestamps.
```

### Cenário D — Hotel com pre-auth
```
Hotel: pre-auth de R$2000 no check-in.
Estadia: R$1500. Check-out com captura parcial.
30 dias depois: reversal da diferença R$500.

Liste todas as mensagens.
```

### Cenário E — Compra offline no avião
```
Compra de R$50 a bordo, sem conectividade.
Terminal decide aprovar offline (EMV offline approval).
Ao pousar, terminal envia advice para o host.

Liste todas as mensagens, indicando quais são online e quais offline.
```

---

## Exercício 3 — Response a partir de Request (Nível: Avançado)

Dado o seguinte request, escreva código Java que monta a response:

```java
ISOMsg request = new ISOMsg();
request.setMTI("0200");
request.set(2, "4532015112830366");
request.set(3, "000000");
request.set(4, "000000015000");
request.set(7, "0301150025");
request.set(11, "123456");
request.set(22, "051");
request.set(41, "TERM0001");
request.set(42, "MERCHANT0000001");
request.set(49, "986");
```

Regras:
1. MTI response = `setResponseMTI()` (0200 → 0210)
2. Copiar DEs: 2, 3, 4, 7, 11, 22, 41, 42, 49
3. Adicionar DE 38 = "A1B2C3" (authorization code)
4. Adicionar DE 39 = "00" (aprovado)
5. Não copiar campos que não devem ser ecoados

**Pergunta extra:** Quais campos do request NUNCA devem ser copiados para o response? Por quê?

---

## Exercício 4 — Classificação Request vs Advice (Nível: Intermediário)

Classifique cada situação como **Request** ou **Advice** e justifique:

1. Terminal precisa saber se pode aprovar a compra online
2. Terminal já aprovou offline e precisa informar o host
3. Adquirente precisa cancelar uma transação aprovada
4. Adquirente informa que a captura foi confirmada
5. Host precisa verificar se o link está ativo
6. Terminal informa que o cartão foi retido (capturado)

**Critério de sucesso:** 6/6 corretas com justificativa.
