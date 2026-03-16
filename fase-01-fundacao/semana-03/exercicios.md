# Semana 3 — Exercícios de Fixação

## Exercício 1 — Conversão Manual (Nível: Iniciante)

Converta o PAN `5412789012345678` para cada formato e calcule o tamanho em bytes:

| Formato | Representação Hex | Tamanho (bytes) |
|---------|------------------|-----------------|
| ASCII   |                  |                 |
| BCD     |                  |                 |

Agora faça o mesmo para um PAN de 13 dígitos: `4000001234567`. O que muda no BCD com número ímpar de dígitos?

---

## Exercício 2 — Debug de Encoding (Nível: Intermediário)

O host recebeu o seguinte dump de um campo DE 2 (PAN) com prefixo LLVAR em ASCII:

```
0x31 0x36 0x34 0x35 0x33 0x32 0x30 0x31 0x35 0x31 0x31 0x32 0x38 0x33 0x30 0x33 0x36 0x36
```

1. Decodifique o prefixo LLVAR — qual é o comprimento indicado?
2. Decodifique o PAN completo
3. Valide: o comprimento indicado bate com o PAN extraído?

Agora analise este segundo dump onde o terminal enviou o prefixo em **BCD** em vez de ASCII:

```
0x16 0x34 0x35 0x33 0x32 0x30 0x31 0x35 0x31 0x31 0x32 0x38 0x33 0x30 0x33 0x36 0x36
```

4. Se o host interpreta o prefixo como ASCII, o que ele lê como comprimento?
5. O que acontece com o parsing do resto da mensagem?

---

## Exercício 3 — Implementação Completa (Nível: Avançado)

Implemente as seguintes classes no módulo `iso-core` com testes unitários:

### HexUtils
```java
public class HexUtils {
    public static String bytesToHex(byte[] bytes);  // {0x4A, 0x3B} → "4A3B"
    public static byte[] hexToBytes(String hex);    // "4A3B" → {0x4A, 0x3B}
    public static String toBinaryString(byte[] b);  // {0xF2} → "11110010"
}
```

Testes obrigatórios:
- Array vazio → string vazia
- Byte único → 2 caracteres hex
- Hex string com letras maiúsculas e minúsculas
- Hex string com comprimento ímpar → exceção
- Round-trip: `hexToBytes(bytesToHex(x)) == x`

### BcdUtils
```java
public class BcdUtils {
    public static byte[] stringToBcd(String digits);    // "4532" → {0x45, 0x32}
    public static String bcdToString(byte[] bcd, int len); // {0x45, 0x32}, 4 → "4532"
}
```

Testes obrigatórios:
- Número par de dígitos
- Número ímpar de dígitos (padding à esquerda)
- String com caracteres não numéricos → exceção
- Round-trip: `bcdToString(stringToBcd(x), x.length()) == x`

---

## Exercício 4 — Encoding Errado Proposital (Nível: Avançado)

1. Crie um teste que monta uma mensagem ISO 8583 com DE 2 (PAN) serializado em **BCD** usando um packager que espera **ASCII**
2. Prove que o `unpack` falha ou retorna dados incorretos
3. Corrija o encoding e prove que funciona

Documente: qual foi a mensagem de erro? Como você identificaria isso em produção?

**Critério de sucesso:** Cobertura de testes > 90% nos utilitários implementados.
