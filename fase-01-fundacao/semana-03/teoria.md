# Semana 3 — Encoding e Formatos de Dados

## 1. Por que encoding importa?

A maioria dos bugs em integrações ISO 8583 não é de lógica — é de **encoding**. Um campo numérico serializado como ASCII quando o receptor espera BCD, ou um campo variável com comprimento errado, gera Format Error (DE39=30) e horas de debug.

## 2. ASCII

Cada caractere ocupa 1 byte. O dígito "1" é `0x31`, "A" é `0x41`.

```
PAN "4532" em ASCII:  0x34 0x35 0x33 0x32  (4 bytes para 4 dígitos)
```

**Vantagem:** legível em hex dump. **Desvantagem:** ocupa mais espaço.

## 3. BCD (Binary Coded Decimal)

Dois dígitos por byte. O dígito "1" ocupa meio byte (nibble).

```
PAN "4532" em BCD:  0x45 0x32  (2 bytes para 4 dígitos)
Número ímpar de dígitos: "453" → 0x04 0x53 (pad 0 à esquerda)
```

**Vantagem:** metade do tamanho. **Desvantagem:** bugs com números ímpares de dígitos.

## 4. Binário puro

Usado em bitmaps e dados EMV (DE 52 PIN block, DE 55 EMV data).

```
PIN Block: 0x04 0x12 0x7E 0xED 0xCD 0x7F 0xCC 0x99  (8 bytes fixos)
```

## 5. LLVAR / LLLVAR — O detalhe que pega

O prefixo de comprimento também tem encoding!

```
Cenário A — Prefixo em ASCII:
  PAN com 16 dígitos: "16" + "4532015112830366"
  Prefixo ocupa 2 bytes ASCII: 0x31 0x36

Cenário B — Prefixo em BCD:
  PAN com 16 dígitos: 0x16 + "4532015112830366"
  Prefixo ocupa 1 byte BCD: 0x16
```

**Erro clássico:** O terminal envia prefixo BCD, o host espera ASCII. O host lê `0x16` como "22" em ASCII → tamanho errado → Format Error.

## 6. Utilitários Java — Implementação completa

### HexUtils.java

```java
public final class HexUtils {

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private HexUtils() {}

    /** byte[] → String hex uppercase. Ex: {0x4F, 0x3A} → "4F3A" */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2]     = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

    /** String hex → byte[]. Aceita upper e lowercase. Ex: "4F3a" → {0x4F, 0x3A} */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[0];
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string length must be even: " + hex);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex character at position " + i * 2);
            }
            bytes[i] = (byte) (hi << 4 | lo);
        }
        return bytes;
    }

    /** Versão que aceita hex com espaços: "4F 3A" → {0x4F, 0x3A} */
    public static byte[] hexToBytes(String hex, boolean ignoreSpaces) {
        if (ignoreSpaces) hex = hex.replace(" ", "");
        return hexToBytes(hex);
    }
}
```

### BcdUtils.java

```java
public final class BcdUtils {

    private BcdUtils() {}

    /**
     * String numérica → BCD empacotado (2 dígitos por byte).
     * Dígito ímpar: padding 0 à ESQUERDA.
     * Ex: "4532" → {0x45, 0x32}
     *     "453"  → {0x04, 0x53}  (padded)
     */
    public static byte[] stringToBcd(String digits) {
        if (digits == null || digits.isEmpty()) return new byte[0];
        // Valida que só tem dígitos
        if (!digits.matches("\\d+")) {
            throw new IllegalArgumentException("BCD input must contain only digits: " + digits);
        }
        // Pad esquerda se ímpar
        if (digits.length() % 2 != 0) {
            digits = "0" + digits;
        }
        byte[] bcd = new byte[digits.length() / 2];
        for (int i = 0; i < bcd.length; i++) {
            int hi = digits.charAt(i * 2) - '0';
            int lo = digits.charAt(i * 2 + 1) - '0';
            bcd[i] = (byte) (hi << 4 | lo);
        }
        return bcd;
    }

    /**
     * BCD → String numérica.
     * @param bcd    bytes BCD
     * @param length número de dígitos esperados (para remover padding)
     * Ex: {0x04, 0x53}, length=3 → "453"
     *     {0x45, 0x32}, length=4 → "4532"
     */
    public static String bcdToString(byte[] bcd, int length) {
        if (bcd == null) return "";
        StringBuilder sb = new StringBuilder(bcd.length * 2);
        for (byte b : bcd) {
            sb.append((b >> 4) & 0x0F);
            sb.append(b & 0x0F);
        }
        String full = sb.toString();
        // Remove padding à esquerda conforme o length pedido
        int start = full.length() - length;
        if (start < 0) throw new IllegalArgumentException(
            "BCD array too short for requested length " + length);
        return full.substring(start);
    }

    /** Versão sem length: retorna todos os dígitos (sem remover padding) */
    public static String bcdToString(byte[] bcd) {
        return bcdToString(bcd, bcd.length * 2);
    }
}
```

### BitmapUtils.java

```java
public final class BitmapUtils {

    private BitmapUtils() {}

    /**
     * Retorna os números dos DEs presentes no bitmap (1-indexed).
     * @param bitmap 8 bytes (primary) ou 16 bytes (primary + secondary)
     */
    public static List<Integer> getPresentFields(byte[] bitmap) {
        List<Integer> fields = new ArrayList<>();
        for (int byteIdx = 0; byteIdx < bitmap.length; byteIdx++) {
            for (int bitIdx = 7; bitIdx >= 0; bitIdx--) {
                if ((bitmap[byteIdx] & (1 << bitIdx)) != 0) {
                    int de = byteIdx * 8 + (8 - bitIdx);
                    fields.add(de);
                }
            }
        }
        return fields;
    }

    /**
     * Verifica se um DE específico está presente no bitmap.
     */
    public static boolean isPresent(byte[] bitmap, int de) {
        if (de < 1 || de > bitmap.length * 8) return false;
        int byteIdx = (de - 1) / 8;
        int bitIdx  = 7 - ((de - 1) % 8);
        return (bitmap[byteIdx] & (1 << bitIdx)) != 0;
    }

    /**
     * Monta bitmap a partir de lista de DEs presentes.
     * Se algum DE > 64, cria secondary bitmap automaticamente.
     */
    public static byte[] buildBitmap(List<Integer> presentDEs) {
        int maxDE = presentDEs.stream().mapToInt(Integer::intValue).max().orElse(0);
        int size = maxDE > 64 ? 16 : 8;
        byte[] bitmap = new byte[size];

        // Se tem secondary bitmap, seta bit 1 do primary (DE1)
        if (size == 16) {
            bitmap[0] |= (byte) 0x80;
        }

        for (int de : presentDEs) {
            int byteIdx = (de - 1) / 8;
            int bitIdx  = 7 - ((de - 1) % 8);
            bitmap[byteIdx] |= (byte) (1 << bitIdx);
        }
        return bitmap;
    }

    /** Converte bitmap para representação hex legível */
    public static String toHexString(byte[] bitmap) {
        return HexUtils.bytesToHex(bitmap);
    }
}
```

**Teste de roundtrip (JUnit 5):**

```java
class BcdUtilsTest {
    @Test void evenDigits() {
        assertEquals("4532", BcdUtils.bcdToString(BcdUtils.stringToBcd("4532"), 4));
    }
    @Test void oddDigits() {
        assertEquals("453", BcdUtils.bcdToString(BcdUtils.stringToBcd("453"), 3));
    }
    @Test void fullPAN() {
        String pan = "4532015112830366";
        assertArrayEquals(BcdUtils.stringToBcd(pan),
                          new byte[]{0x45, 0x32, 0x01, 0x51, 0x12, (byte)0x83, 0x03, 0x66});
        assertEquals(pan, BcdUtils.bcdToString(BcdUtils.stringToBcd(pan), pan.length()));
    }
}

class BitmapUtilsTest {
    @Test void primaryBitmap() {
        // DE2, DE3, DE4, DE11, DE22, DE25, DE41, DE49 presentes
        byte[] bitmap = BitmapUtils.buildBitmap(List.of(2, 3, 4, 11, 22, 25, 41, 49));
        assertTrue(BitmapUtils.isPresent(bitmap, 2));
        assertTrue(BitmapUtils.isPresent(bitmap, 49));
        assertFalse(BitmapUtils.isPresent(bitmap, 1));  // DE1 = secondary bitmap indicator
        assertEquals(8, bitmap.length);  // só primary
    }
    @Test void secondaryBitmap() {
        byte[] bitmap = BitmapUtils.buildBitmap(List.of(2, 55)); // DE55 > 64
        assertEquals(16, bitmap.length);
        assertTrue(BitmapUtils.isPresent(bitmap, 1));   // DE1 sinaliza secondary
        assertTrue(BitmapUtils.isPresent(bitmap, 55));
    }
}
```

## 7. Exercícios da Semana 3

### Exercício 1 — Conversão manual
Converta o PAN `5412789012345678` para: ASCII hex, BCD hex, e calcule quantos bytes cada representação ocupa.

### Exercício 2 — Debug de encoding
O host recebeu: `0x31 0x36 0x34 0x35 0x33 0x32 0x30 0x31 0x35 0x31 0x31 0x32 0x38 0x33 0x30 0x33 0x36 0x36`
Decodifique como LLVAR ASCII. Qual é o PAN?

### Exercício 3 — Implementação completa
Implemente `HexUtils`, `BcdUtils` e `PaddingUtils` com testes unitários cobrindo:
- Número par/ímpar de dígitos em BCD
- Padding left/right para campos fixos
- Conversão roundtrip (encode → decode → igual ao original)

### Exercício 4 — Erro proposital
Crie uma mensagem com encoding errado no DE 2 (BCD em vez de ASCII) e prove com teste que o unpack falha. Depois corrija.

### Desafio — Parser from scratch
Implemente um mini-parser ISO 8583 **sem usar jPOS** que:
1. Recebe `byte[]` raw
2. Extrai MTI (4 bytes ASCII)
3. Extrai bitmap (8 ou 16 bytes binary)
4. Para cada DE presente, extrai o valor baseado em uma spec simplificada (tabela de {DE → tipo, tamanho, encoding})
5. Retorna um `Map<Integer, String>` com os campos

Isso força você a entender o protocolo no nível de bytes, sem magia de framework.

---

# Semana 4 — MTIs e Lifecycle de Mensagens

## 1. O ciclo de vida de uma transação em mensagens

Uma transação simples de compra gera no mínimo 2 mensagens (request + response). Mas cenários reais geram muito mais:

```
Cenário: Compra com timeout e retransmissão

t=0   Terminal → Host:     0200  (Financial Request)
t=30  [TIMEOUT — sem resposta]
t=31  Terminal → Host:     0201  (Financial Request Repeat)
t=32  Host → Terminal:     0210  (Financial Response — pode ser da 1ª ou 2ª)
t=33  Terminal → Host:     0400  (Reversal Request — por segurança)
t=34  Host → Terminal:     0410  (Reversal Response)
```

Total: 5 mensagens para 1 transação que deu problema.

```
Cenário: Compra dual message com advice

t=0   Adquirente → Bandeira:  0100  (Auth Request)
t=1   Bandeira → Adquirente:  0110  (Auth Response, DE39=00)
t=D1  Adquirente → Bandeira:  0220  (Financial Advice — confirma captura)
t=D1  Bandeira → Adquirente:  0230  (Financial Advice Response)
```

Total: 4 mensagens, espalhadas em 2 dias.

## 2. Request vs Advice — Quando usar?

| Cenário | Tipo | Por quê? |
|---------|------|----------|
| Compra normal (online) | Request (0100/0200) | Emissor decide (aprova/nega) |
| Confirmação de captura | Advice (0220) | Adquirente informa, emissor registra |
| Offline (SAF) | Advice (0120/0220) | Terminal já decidiu localmente |
| Reversal por timeout | Request (0400) | Precisa que emissor confirme o estorno |
| Reversal offline | Advice (0420) | Adquirente já reverteu localmente |

**Regra de ouro:**
- Se você **precisa da decisão** do outro lado → Request
- Se você está **informando** o outro lado → Advice

## 3. Exercises da Semana 4

### Exercício 1 — Matriz de mensagens
Crie uma tabela completa: para cada MTI (0100 a 0810), liste quem envia, quem recebe, se espera resposta, e um exemplo de cenário.

### Exercício 2 — Sequência de mensagens
Para cada cenário, liste a sequência completa de MTIs trocados:

1. Compra de R$100 com crédito, aprovada (dual message, com clearing)
2. Saque de R$500 no ATM (single message)
3. Compra de R$200 que dá timeout no emissor
4. Hotel: pre-auth de R$2000, estadia de R$1500, checkout
5. Compra offline no avião (sem conectividade)

### Exercício 3 — Response a partir de Request
Dado um `ISOMsg` com MTI=0200, escreva código Java que monta a response correta (MTI=0210) copiando os campos que devem ser ecoados e adicionando DE 38 e DE 39.

### Desafio — Simulador de conversa
Implemente um programa Java que simula a troca de mensagens entre terminal e host:
1. Terminal envia 0800 (echo test) → Host responde 0810
2. Terminal envia 0200 (compra) → Host responde 0210 (aprovado)
3. Terminal envia 0200 (compra) → Host NÃO responde (simula timeout) → Terminal envia 0400 (reversal)

Use `System.out` para imprimir cada mensagem trocada com timestamp, MTI, STAN e campos principais. Formato sugerido:

```
[14:30:25.001] → 0200 STAN=123456 PAN=4532****0366 AMT=000000015000
[14:30:25.150] ← 0210 STAN=123456 DE39=00 AUTH=A1B2C3
```
