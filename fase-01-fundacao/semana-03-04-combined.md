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

## 6. Utilitários Java para implementar

```java
// HexUtils.java — converte entre byte[] e String hex
public class HexUtils {
    public static String bytesToHex(byte[] bytes) { /* ... */ }
    public static byte[] hexToBytes(String hex) { /* ... */ }
}

// BcdUtils.java — converte entre BCD e String numérica
public class BcdUtils {
    public static byte[] stringToBcd(String digits) { /* ... */ }
    public static String bcdToString(byte[] bcd, int length) { /* ... */ }
}

// BitmapUtils.java — já implementado na semana 2
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
