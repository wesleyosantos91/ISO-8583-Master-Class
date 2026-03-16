# Semana 9 — Exercícios de Fixação
## Campos Críticos da Autorização

---

## Exercício 1 — Flash Cards dos 19 Campos (Nível: Iniciante)

Crie um arquivo `flashcards-campos.md` com uma tabela de referência rápida para cada campo:

| DE | Nome | Formato | Exemplo de valor | Obrigatório em | O que significa se estiver ausente |
|----|------|---------|------------------|----------------|-----------------------------------|
| 2 | PAN | n ..19 LLVAR | `4532015112830366` | 0200, 0400 | |
| 3 | Processing Code | n 6 fixo | `003000` | 0200 | |
| 4 | Amount | n 12 fixo | `000000015000` | 0200 | |
| 7 | Transmission DateTime | n 10 (MMDDhhmmss) | `0314143025` | todos | |
| 11 | STAN | n 6 fixo | `000042` | todos | |
| 22 | POS Entry Mode | n 3 `[PP][C]` | `051` | 0200 | |
| 25 | POS Condition Code | n 2 fixo | `00` | 0200 | |
| 32 | Acquiring Institution ID | n ..11 LLVAR | `60004` | 0200 | |
| 37 | RRN | an 12 fixo | `123456789012` | 0200 | |
| 38 | Auth Code | an 6 fixo | `XK4729` | apenas resposta | |
| 39 | Response Code | an 2 fixo | `00` | apenas resposta | |
| 41 | Terminal ID | ans 8 fixo | `TERM0001` | todos | |
| 42 | Merchant ID | ans 15 fixo | `MERCHANT00001   ` | 0200 | |
| 49 | Currency Code | n 3 fixo | `986` | 0200 | |
| 52 | PIN Block | b 8 fixo | `[8 bytes criptografados]` | se PIN foi digitado | |
| 55 | EMV Data | b ..999 LLLVAR | `[TLV bytes]` | se chip | |

Preencha a coluna "O que significa se estiver ausente" para cada campo.

**Critério de sucesso:** Tabela completa, ausência de campo tem significado técnico correto.

---

## Exercício 2 — Decodificação de Dump (Nível: Iniciante)

Analise a seguinte mensagem ISO 8583 e responda todas as perguntas:

```
MTI=0200
DE2=4532015112830366
DE3=003000
DE4=000000150000
DE7=0314160000
DE11=000042
DE22=071
DE25=00
DE41=TERM0001
DE42=MERCHANT00001
DE49=986
DE55=[dados TLV presentes]
```

1. Que tipo de transação é esta? (crédito, débito, saque?)
2. Qual o valor exato em reais?
3. Como o cartão foi lido? (chip, tarja, NFC, digitado?)
4. O terminal aceita PIN?
5. É uma transação de e-commerce?
6. Este é um request ou response? Como você sabe?
7. Quais campos importantes **estão faltando** nesta mensagem?
8. O que o `DE22=071` significa dígito a dígito? Decodifique `07` e `1` separadamente.
9. Se o DE55 está presente mas o DE22 diz `071` (contactless), os dados EMV são esperados?
10. O DE39 está ausente. O que isso indica sobre o estado desta mensagem?

**Critério de sucesso:** Todas as 10 perguntas respondidas com precisão técnica.

---

## Exercício 3 — Decodificando o Processing Code (Nível: Intermediário)

O DE3 tem 6 dígitos com estrutura `[TT][FF][TT]`. Decodifique cada valor abaixo e descreva a transação completa:

| DE3 | Tipo transação | Conta origem | Conta destino | Descrição da operação |
|-----|----------------|--------------|---------------|----------------------|
| `003000` | | | | |
| `012000` | | | | |
| `200030` | | | | |
| `300000` | | | | |
| `013010` | | | | |
| `900000` | | | | |

**Responda também:**
1. Por que o Processing Code tem conta destino além de conta origem?
2. Em que cenário conta origem e destino são diferentes?
3. O Processing Code `003000` e `003010` são diferentes — o que muda?

**Critério de sucesso:** Todos os 6 valores decodificados corretamente.

---

## Exercício 4 — Análise de Response Codes (Nível: Intermediário)

Você é engenheiro de suporte. Para cada situação de cliente abaixo, identifique qual DE39 provavelmente gerou o problema e como resolver:

1. Cliente diz: "Tentei sacar mas o caixa disse 'transação não autorizada'." O cliente tem saldo suficiente.
2. Operador diz: "Nossas transações estão retornando code 30 para 100% dos terminais novos."
3. Cliente diz: "Meu cartão foi clonado. Tentaram fazer compras mas foram bloqueadas."
4. Merchant diz: "Cliente comprou, apareceu aprovado na maquininha, mas depois o banco cancelou."
5. Cliente diz: "Fiz uma compra e ficou parado no terminal por 2 minutos antes de dar erro."
6. Terminal retorna DE39=`76` para um reversal enviado.

Para cada caso:
- Qual DE39 foi gerado?
- Qual a causa provável?
- Quem é responsável por resolver (emissor, adquirente, switch, merchant)?
- O que o terminal deve exibir ao cliente?

**Critério de sucesso:** Response codes corretos, responsabilidade de resolução correta para todos os casos.

---

## Exercício 5 — Implementando o Algoritmo de Luhn (Nível: Intermediário)

Implemente `LuhnValidator.java`:

```java
public class LuhnValidator {

    public static boolean isValid(String pan) {
        // Implemente o algoritmo de Luhn
    }

    public static String generateCheckDigit(String panWithout) {
        // Dado um PAN sem o último dígito, calcula o dígito verificador
    }
}
```

Valide os seguintes PANs (sem consultar online):
1. `4532015112830366` — válido ou inválido?
2. `4532015112830367` — válido ou inválido?
3. `5412789012345678` — válido ou inválido?
4. `4111111111111111` — válido ou inválido? (PAN de teste Visa)
5. `1234567890123456` — válido ou inválido?

Implemente testes JUnit para cada caso.

**Critério de sucesso:** Algoritmo correto, todos os 5 PANs com resultado correto.

---

## Exercício 6 — Mascaramento de PAN (Nível: Intermediário)

Implemente `PANMasker.java` que segue as regras PCI DSS:

```java
public class PANMasker {

    // Exibe primeiros 6 e últimos 4 dígitos, asteriscos no meio
    // Ex: "4532015112830366" → "453201******0366"
    public static String mask(String pan) { ... }

    // Para logs: exibe apenas últimos 4 dígitos
    // Ex: "4532015112830366" → "****0366"
    public static String maskForLog(String pan) { ... }

    // Para comprovante: exibe apenas últimos 4 dígitos com prefixo "xxxx xxxx xxxx"
    // Ex: "4532015112830366" → "xxxx xxxx xxxx 0366"
    public static String maskForReceipt(String pan) { ... }
}
```

**Regras adicionais:**
- PANs curtos (< 13 dígitos): retornar `"***"` (inválido)
- PAN null ou vazio: retornar `"***"`
- Nunca lançar exceção, sempre retornar string segura

Implemente testes para PANs de 13, 16 e 19 dígitos.

**Critério de sucesso:** Todos os formatos corretos, casos extremos tratados sem exceção.

---

## Exercício 7 — Validador Completo de Mensagem (Nível: Avançado)

Implemente `MessageValidator.java` completo conforme descrito na teoria:

```java
public class MessageValidator {

    public ValidationResult validate(ISOMsg msg) throws ISOException {
        // Retorna lista de erros e o response code apropriado
    }
}

public record ValidationResult(boolean valid, String responseCode, List<String> errors) {}
```

**Regras a implementar:**
1. Validação de MTI (deve ser reconhecido)
2. Validação de campos obrigatórios por MTI
3. Luhn no PAN
4. Amount > 0
5. Processing Code válido (de uma lista conhecida)
6. POS Entry Mode válido (código PP deve ser um dos reconhecidos)
7. Currency Code válido (deve ser um código ISO 4217)
8. Terminal ID exatamente 8 caracteres
9. Response Code presente em respostas

Implemente no mínimo 15 testes unitários cobrindo cenários de erro e sucesso.

**Critério de sucesso:** 15+ testes passando, cada erro gera o response code correto.

---

## Exercício 8 — Análise de Incidente (Nível: Avançado)

**Contexto:** O time de operações reporta:

> "Taxa de decline subiu de 3% para 25% nos últimos 30 minutos. Apenas terminais novos TERM0100 a TERM0150. Os terminais antigos estão normais."

Os logs dos terminais afetados mostram:
```
DE22=012  (PAN Entry Mode: tarja magnética, PIN capability: não aceita)
DE55=[presente com 150 bytes de dados TLV]
```

Os terminais antigos (funcionando) mostram:
```
DE22=051  (PAN Entry Mode: chip contact, PIN capability: terminal aceita)
DE55=[presente com 150 bytes de dados TLV]
```

Responda:
1. O que está tecnicamente errado nos terminais novos?
2. Por que isso causa declines? Quem está recusando (switch, emissor, bandeira)?
3. Qual o impacto para o merchant? E para o portador?
4. Como corrigir? (hint: quem precisa fazer o quê?)
5. Enquanto a correção não chega, existe alguma mitigação temporária?
6. Se você fosse o emissor e recebesse DE22=012 mas DE55 presente, qual response code você usaria?

**Critério de sucesso:** Diagnóstico correto, impacto quantificado, solução viável.
