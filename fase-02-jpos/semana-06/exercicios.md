# Semana 6 — Exercícios de Fixação
## ISOMsg, Packager e Serialização

---

## Exercício 1 — Anatomia do ISOMsg (Nível: Iniciante)

Dado o código abaixo, responda sem executar:

```java
ISOMsg msg = new ISOMsg();
msg.setMTI("0200");
msg.set(2, "4532015112830366");
msg.set(3, "003000");
msg.set(4, "000000015000");
msg.set(7, "0314143025");
msg.set(11, "123456");
msg.set(41, "TERM0001");
msg.set(42, "MERCHANT00001  ");
msg.set(49, "986");
```

1. Quantos campos estão preenchidos (incluindo o MTI)?
2. Qual o valor em reais representado pelo DE4?
3. O DE42 tem 15 caracteres. O valor `"MERCHANT00001  "` tem 15? Conte e justifique.
4. Qual é o Processing Code e o que ele representa?
5. O que é o DE49 e qual moeda representa `986`?
6. Quais campos obrigatórios de uma autorização estão **faltando**? (dica: pense em POS Entry Mode)

**Critério de sucesso:** Todas as 6 perguntas respondidas corretamente.

---

## Exercício 2 — Classe de Campo Certa (Nível: Iniciante)

Para cada campo, escolha a classe jPOS correta e justifique a escolha:

| DE | Nome | Tamanho | Tipo | Classe jPOS correta | Justificativa |
|----|------|---------|------|---------------------|---------------|
| 2 | PAN | ..19 | n LLVAR | | |
| 3 | Processing Code | 6 | n fixo | | |
| 4 | Amount | 12 | n fixo | | |
| 37 | RRN | 12 | an fixo | | |
| 41 | Terminal ID | 8 | ans fixo | | |
| 48 | Additional Data | ..999 | ans LLLVAR | | |
| 52 | PIN Block | 8 | b fixo | | |
| 55 | EMV Data | ..999 | b LLLVAR | | |

**Critério de sucesso:** Todas as classes corretas com justificativa técnica.

---

## Exercício 3 — Packager XML Completo (Nível: Intermediário)

Implemente o arquivo `cfg/iso87ascii.xml` com os seguintes campos mínimos para um switch de autorização:

- DE 0 (MTI), DE 1 (Bitmap), DE 2 (PAN), DE 3 (Processing Code), DE 4 (Amount)
- DE 7 (Transmission DateTime), DE 11 (STAN), DE 12 (Local Time), DE 13 (Local Date)
- DE 22 (POS Entry Mode), DE 25 (POS Condition Code), DE 32 (Acquiring Institution ID)
- DE 37 (RRN), DE 38 (Auth Code), DE 39 (Response Code)
- DE 41 (Terminal ID), DE 42 (Merchant ID), DE 48 (Additional Data), DE 49 (Currency)
- DE 52 (PIN Block), DE 55 (EMV Data)

Para cada campo, documente com comentário XML: `<!-- DE XX: nome, formato, por que esta classe -->`

**Critério de sucesso:** XML válido para jPOS, todos os 21 campos presentes com comentários.

---

## Exercício 4 — MessageFactory (Nível: Intermediário)

Implemente a classe `MessageFactory.java` com os seguintes métodos:

```java
public class MessageFactory {

    private final GenericPackager packager;

    // Constrói uma auth request 0200 com os campos essenciais
    public ISOMsg buildAuthRequest(String pan, long amountInCents, String terminalId,
                                   String merchantId, String posEntryMode);

    // Constrói a response 0210 espelhando os campos do request
    public ISOMsg buildAuthResponse(ISOMsg request, String responseCode, String authCode);

    // Constrói um echo request 0800 (network management)
    public ISOMsg buildEchoRequest();

    // Constrói um reversal 0400 baseado no auth original
    public ISOMsg buildReversalRequest(ISOMsg originalAuth);

    // Utilitário: gera STAN único (000001 a 999999, com rollover)
    private synchronized String generateSTAN();

    // Utilitário: gera RRN único (12 chars alfanumérico)
    private String generateRRN();
}
```

**Requisitos:**
- O STAN deve ser thread-safe (múltiplas threads podem chamar ao mesmo tempo)
- O reversal deve copiar DE2, DE3, DE4, DE7, DE11, DE41, DE42 do original
- O DE4 deve ser formatado como string de 12 dígitos com zeros à esquerda

**Critério de sucesso:** Todos os métodos implementados e funcionais, STAN thread-safe.

---

## Exercício 5 — Teste de Roundtrip (Nível: Intermediário)

Implemente `MessageRoundtripTest.java` (JUnit 5) que:

1. **Cria** uma `ISOMsg` com todos os 19 campos do seu packager
2. **Serializa** (pack) para `byte[]`
3. **Desserializa** (unpack) em uma nova `ISOMsg`
4. **Compara** todos os campos do original com o desserializado

```java
@Test
void roundtripAuthRequest() throws Exception {
    // TODO: implemente o teste
}

@Test
void roundtripWithBinaryFields() throws Exception {
    // Inclua DE52 (PIN Block) e DE55 (EMV data) com bytes aleatórios
}

@Test
void roundtripPreservesSpaces() throws Exception {
    // DE42 = "MERCHANT00001  " (2 espaços no fim) — o espaço deve sobreviver
}
```

**Critério de sucesso:** Todos os testes passam, campos binários preservados byte a byte.

---

## Exercício 6 — ASCII vs BCD (Nível: Avançado)

Implemente dois packagers para o mesmo conjunto de campos (DE2, DE3, DE4, DE11, DE39, DE41):

1. **`iso87ascii.xml`** — usa classes `IFA_*`
2. **`iso87bcd.xml`** — usa classes `IFB_*`

Depois:

1. Serialize a mesma `ISOMsg` com cada packager e imprima os bytes em hex:
   ```
   ASCII: 30323034 35333230 31353131 ...
   BCD:   02453201 511283...
   ```

2. Explique a diferença em bytes gerados para o DE4 (Amount = `000000015000`):
   - No ASCII: quantos bytes ocupa?
   - No BCD: quantos bytes ocupa?
   - Por que o BCD é mais compacto?

3. Implemente um teste que serializa com ASCII e tenta desserializar com BCD → deve lançar exceção. Isso simula um problema real de integração.

**Critério de sucesso:** Dois packagers funcionais, diferença em bytes demonstrada, teste de incompatibilidade passando.

---

## Exercício 7 — Validação de Mensagem (Nível: Avançado)

Implemente `MessageValidator.java` que valida uma `ISOMsg` antes de enviá-la:

```java
public class MessageValidator {

    // Retorna lista de erros (vazia = mensagem válida)
    public List<String> validate(ISOMsg msg) { ... }
}
```

**Regras de validação:**

| Campo | Regra |
|-------|-------|
| MTI | Deve ser um dos: 0100, 0200, 0110, 0210, 0400, 0410, 0800, 0810 |
| DE2 | Obrigatório em 0100/0200/0400, deve passar no algoritmo de Luhn |
| DE3 | Obrigatório em 0100/0200, deve ser um dos processing codes válidos |
| DE4 | Obrigatório em 0100/0200, deve ser maior que zero |
| DE11 | Obrigatório sempre, deve ser 6 dígitos numéricos |
| DE22 | Obrigatório em 0100/0200, PP deve ser um dos valores válidos |
| DE39 | Obrigatório em respostas (0110/0210/0410/0810), 2 chars alfanumérico |
| DE41 | Obrigatório sempre, exatamente 8 chars |
| DE49 | Obrigatório em 0100/0200, deve ser código ISO 4217 válido |

Implemente testes unitários para cada regra.

**Critério de sucesso:** Todas as regras implementadas e testadas, mensagem inválida retorna erros específicos (não apenas "inválido").
