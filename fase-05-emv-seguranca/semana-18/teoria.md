# Fase 5 — EMV, Segurança, Clearing e Settlement (Semanas 17-20)

---

# Semana 17 — EMV, Chip, Contactless e DE 55

## 1. TLV (Tag-Length-Value)

O DE 55 carrega dados do chip no formato TLV. Cada dado é uma "tag":

```
[Tag 9F26][Length 08][Value AABBCCDDEE112233]
[Tag 9F27][Length 01][Value 80]
[Tag 9F10][Length 07][Value 06010A03A4A000]

Concatenado:
9F2608AABBCCDDEE1122339F2701809F100706010A03A4A000
```

**Tag:** 1-3 bytes. Se os 5 bits inferiores do primeiro byte são todos 1 (0x1F), a tag é multi-byte.
**Length:** 1-3 bytes. Se bit 7 = 1, indica tamanho multi-byte (0x81 XX ou 0x82 XX XX).
**Value:** Os dados propriamente ditos.

## 2. Tags que você DEVE conhecer

| Tag | Nome | Bytes | O que faz |
|-----|------|-------|-----------|
| `9F26` | Application Cryptogram | 8 | Criptograma do chip (ARQC/TC/AAC) |
| `9F27` | Cryptogram Information Data | 1 | Tipo: 80=ARQC, 40=TC, 00=AAC |
| `9F10` | Issuer Application Data | var | Dados proprietários do emissor |
| `9F37` | Unpredictable Number | 4 | Anti-replay |
| `9F36` | Application Transaction Counter | 2 | Contador incremental |
| `95` | Terminal Verification Results | 5 | O que o terminal verificou |
| `9A` | Transaction Date | 3 | YYMMDD |
| `9C` | Transaction Type | 1 | 00=compra, 01=saque |
| `9F02` | Amount Authorized | 6 | Valor autorizado (BCD) |
| `9F1A` | Terminal Country Code | 2 | 076 = Brasil |
| `5F2A` | Transaction Currency Code | 2 | 0986 = BRL |
| `82` | Application Interchange Profile | 2 | Capacidades do app |
| `84` | Dedicated File Name (AID) | var | Application ID selecionado |
| `9F33` | Terminal Capabilities | 3 | O que o terminal suporta |
| `9F34` | CVM Results | 3 | Como portador foi verificado |

## 3. Decode Profundo das Tags Críticas

Essas três tags aparecem em **toda transação chip** e são fundamentais para debugging, investigação de fraude e análise de chargeback.

### TVR — Terminal Verification Results (tag `95`, 5 bytes)

Cada bit indica que o terminal **detectou** aquela condição durante o processamento. Bit `1` = condição presente.

```
Byte 1 — Offline Data Authentication
  Bit 8 (0x80): Offline data authentication was not performed
  Bit 7 (0x40): SDA failed
  Bit 6 (0x20): ICC data missing
  Bit 5 (0x10): Card appears on terminal exception file
  Bit 4 (0x08): DDA failed
  Bit 3 (0x04): CDA failed
  Bits 2-1:     RFU

Byte 2 — Application Version / Expiration
  Bit 8 (0x80): ICC and terminal have different application versions
  Bit 7 (0x40): Expired application
  Bit 6 (0x20): Application not yet effective
  Bit 5 (0x10): Requested service not allowed for card product
  Bit 4 (0x08): New card
  Bits 3-1:     RFU

Byte 3 — Cardholder Verification
  Bit 8 (0x80): Cardholder verification was not successful
  Bit 7 (0x40): Unrecognised CVM
  Bit 6 (0x20): PIN Try Limit exceeded
  Bit 5 (0x10): PIN entry required and PIN pad not present or not working
  Bit 4 (0x08): PIN entry required, PIN pad present, but PIN was not entered
  Bit 3 (0x04): Online PIN entered
  Bits 2-1:     RFU

Byte 4 — Terminal Risk Management
  Bit 8 (0x80): Transaction exceeds floor limit
  Bit 7 (0x40): Lower consecutive offline limit exceeded
  Bit 6 (0x20): Upper consecutive offline limit exceeded
  Bit 5 (0x10): Transaction selected randomly for online processing
  Bit 4 (0x08): Merchant forced transaction online
  Bits 3-1:     RFU

Byte 5 — Script / Issuer Authentication
  Bit 8 (0x80): Default TDOL used
  Bit 7 (0x40): Issuer authentication failed
  Bit 6 (0x20): Script processing failed before final GENERATE AC
  Bit 5 (0x10): Script processing failed after final GENERATE AC
  Bits 4-1:     RFU
```

**Exemplo de decode:** TVR = `00 80 04 00 00`
```
Byte 1 (00): sem falhas na autenticação offline
Byte 2 (80): bit 8 → versões diferentes entre terminal e chip — comum, geralmente inócuo
Byte 3 (04): bit 3 → CDA failed — chip tentou Combined DDA mas terminal não suportou
Byte 4 (00): sem questões de risco
Byte 5 (00): sem falhas de script
```

**Sinais de alerta em investigações:**
- `xx 40 xx xx xx` (byte 2, bit 7): aplicação expirada — portador com cartão vencido ou fallback
- `xx xx 20 xx xx` (byte 3, bit 6): PIN Try Limit exceeded — possível tentativa de força bruta
- `xx xx 80 xx xx` (byte 3, bit 8): CVM failed — PIN digitado errado mas transação aprovada offline

---

### AIP — Application Interchange Profile (tag `82`, 2 bytes)

Declara o que o **chip suporta** (capacidades do cartão, não do terminal):

```
Byte 1:
  Bit 8 (0x80): RFU
  Bit 7 (0x40): SDA supported
  Bit 6 (0x20): DDA supported
  Bit 5 (0x10): Cardholder verification supported
  Bit 4 (0x08): Terminal risk management to be performed
  Bit 3 (0x04): Issuer authentication supported
  Bit 2 (0x02): On-device cardholder verification supported (contactless)
  Bit 1 (0x01): CDA supported

Byte 2: RFU (geralmente 0x00)
```

**Exemplo — cartão de débito padrão:** AIP = `5C 00`
```
5C = 0101 1100
  Bit 7 (0x40): SDA supported ✓
  Bit 5 (0x10): Cardholder verification supported ✓
  Bit 4 (0x08): Terminal risk management ✓
  Bit 3 (0x04): Issuer authentication supported ✓
→ Suporta SDA, PIN e auth online. Não suporta DDA/CDA.
```

**Exemplo — cartão premium com contactless:** AIP = `7E 00`
```
7E = 0111 1110
  Adiciona bit 6 (0x20): DDA supported ✓
  Adiciona bit 2 (0x02): On-device CVM supported ✓ (ex: biometria no celular)
→ Cartão mais seguro; DDA impede clonagem, on-device CVM habilita biometria.
```

---

### CVM Results — Cardholder Verification Method Results (tag `9F34`, 3 bytes)

Indica **como** o portador foi verificado e qual foi o resultado:

```
Byte 1 — Método usado (CVM Code):
  0x00: Fail / No CVM
  0x01: Plaintext PIN verificado pelo chip (offline)
  0x02: Online Enciphered PIN (PIN enviado criptografado ao emissor)
  0x03: Plaintext PIN offline + Signature
  0x04: Enciphered PIN verificado pelo chip (offline)
  0x1E: Signature (papel)
  0x1F: No CVM required (valor abaixo do floor limit)
  0x3F: No CVM performed

Byte 2 — Condição de aplicação (CVM Condition):
  0x00: Always
  0x03: If terminal supports the CVM
  0x04: If manual cash
  0x06: If not unattended cash and not manual cash

Byte 3 — Resultado:
  0x00: Unknown
  0x01: Failed
  0x02: Successful
```

| Cenário | CVM Results | Significado |
|---------|-------------|-------------|
| PIN online aprovado | `02 00 02` | Online PIN, sempre, sucesso |
| PIN offline aprovado | `04 00 02` | Enciphered PIN no chip, sucesso |
| Assinatura | `1E 00 02` | Signature, sempre, sucesso |
| Contactless valor baixo | `1F 03 02` | No CVM required, se terminal suportar, sucesso |
| PIN errado (fallback assinatura) | `02 00 01` + `1E 00 02` | PIN falhou, usou assinatura |

---

### Exemplo Completo: Parse de um DE 55 Real

```
DE 55 (compra chip Visa, R$ 50,00 — hex bruto):
9F2608A1B2C3D4E5F607189F2701808202
5C009F100706010A03A0B8009F37041234
5678 9F360200A5950500800400009A0326
03159C01009F02060000000050009F1A02
00765F2A020986 8407A0000000031010
9F3303E0F8C89F34030200 02

Parse tag a tag:
  Tag 9F26 (08): A1B2C3D4E5F60718  → ARQC: criptograma de autenticação do chip
  Tag 9F27 (01): 80                 → CID: 80 = ARQC (solicitando auth online)
  Tag 82   (02): 5C00               → AIP: SDA + CV + TermRisk + IssuerAuth
  Tag 9F10 (07): 06010A03A0B800     → IAD: dados proprietários do emissor
  Tag 9F37 (04): 12345678           → Unpredictable Number (anti-replay)
  Tag 9F36 (02): 00A5               → ATC: 165 (165ª transação deste cartão)
  Tag 95   (05): 0080040000         → TVR: byte2=80 (diff version), byte3=04 (CDA failed)
  Tag 9A   (03): 260315             → Data: 2026-03-15
  Tag 9C   (01): 00                 → Tipo: 00 = compra
  Tag 9F02 (06): 000000005000       → Valor: R$ 50,00 (BCD, em centavos)
  Tag 9F1A (02): 0076               → País do terminal: 076 = Brasil
  Tag 5F2A (02): 0986               → Moeda: 0986 = BRL
  Tag 84   (07): A0000000031010     → AID: Visa Credit
  Tag 9F33 (03): E0F8C8             → Terminal Capabilities
  Tag 9F34 (03): 020002             → CVM: Online PIN, sempre, sucesso ✓

O que esse DE 55 revela:
  - ATC 165: cartão ativo, histórico razoável de uso
  - TVR byte 3 = 04 (CDA failed): chip usou SDA como fallback → risco levemente elevado
  - CVM Online PIN Successful: PIN foi validado pelo emissor, não pelo chip
  - ARQC presente: autenticação legítima do chip para esta transação específica
```

---

## 4. Fluxo ARQC → ARPC

```
1. Chip calcula ARQC (criptograma de request) usando:
   - Chave do chip (derivada da Master Key do emissor)
   - Dados da transação (valor, data, ATC, unpredictable number)
   
2. Terminal envia ARQC no DE 55 (tag 9F26)

3. Emissor (com HSM):
   - Deriva a mesma chave do chip
   - Recalcula o ARQC esperado
   - Se match → cartão é autêntico
   - Gera ARPC (resposta criptográfica)
   
4. ARPC volta no DE 55 da response

5. Chip valida ARPC:
   - Se OK → gera TC (Transaction Certificate) = transação confirmada
   - Se falha → gera AAC = transação rejeitada pelo chip
```

## 5. Parser TLV em Java

```java
public class TLVParser {
    
    public static Map<String, byte[]> parse(byte[] data) {
        Map<String, byte[]> tags = new LinkedHashMap<>();
        int offset = 0;
        
        while (offset < data.length) {
            // Tag
            int tagStart = offset;
            if ((data[offset] & 0x1F) == 0x1F) { // Multi-byte tag
                offset++;
                while (offset < data.length && (data[offset] & 0x80) != 0) offset++;
                offset++;
            } else {
                offset++;
            }
            String tag = bytesToHex(data, tagStart, offset - tagStart);
            
            // Length
            int length = 0;
            if (offset < data.length) {
                if ((data[offset] & 0x80) == 0) {
                    length = data[offset++] & 0x7F;
                } else {
                    int numBytes = data[offset++] & 0x7F;
                    for (int i = 0; i < numBytes; i++) {
                        length = (length << 8) | (data[offset++] & 0xFF);
                    }
                }
            }
            
            // Value
            if (offset + length <= data.length) {
                byte[] value = Arrays.copyOfRange(data, offset, offset + length);
                tags.put(tag.toUpperCase(), value);
                offset += length;
            } else break;
        }
        return tags;
    }
}
```

## 6. Exercícios Semana 17

1. **Implemente TLVParser** completo com testes
2. **Parse um DE 55 real** (use dados de exemplo) e identifique cada tag
3. **Implemente `DE55Analyzer`** que extrai e explica: tipo de criptograma, ATC, data, CVM usado
4. **Diferencie chip de fallback:** quando DE22=051 vs DE22=801, o que muda no DE55?

### DE55Analyzer — Implementação completa

```java
public class DE55Analyzer {

    private final TLVParser parser = new TLVParser();

    /** Resultado da análise do DE55 */
    public record DE55Analysis(
        String cryptogramType,   // ARQC, TC, AAC
        String atc,              // Application Transaction Counter
        String txnDate,          // Data da transação (tag 9A)
        String cvmUsed,          // Método de verificação do portador
        String tvr,              // Terminal Verification Results (hex)
        String aip,              // Application Interchange Profile (hex)
        boolean sdaFailed,       // True se SDA/DDA falhou
        boolean cvmFailed,       // True se CVM falhou
        boolean isContactless,   // Inferido pelo AIP
        List<String> warnings    // Avisos de segurança
    ) {}

    public DE55Analysis analyze(byte[] de55Data, String de22) {
        Map<String, byte[]> tags = parser.parse(de55Data);
        List<String> warnings = new ArrayList<>();

        // ── Tipo de criptograma (tag 9F27) ──────────────────────────────────
        byte[] cryptoInfo = tags.get("9F27");
        String cryptogramType = "UNKNOWN";
        if (cryptoInfo != null && cryptoInfo.length > 0) {
            int ci = cryptoInfo[0] & 0xC0; // bits 7-6
            cryptogramType = switch (ci) {
                case 0x00 -> "AAC";   // Authorization rejected — offline declined
                case 0x40 -> "TC";    // Transaction Certificate — offline approved
                case 0x80 -> "ARQC";  // Authorization Request Cryptogram — online
                default   -> "RFU";
            };
        }
        if ("AAC".equals(cryptogramType)) {
            warnings.add("CHIP_DECLINED_OFFLINE: cartão recusou a transação offline (AAC)");
        }

        // ── ATC (tag 9F36) ──────────────────────────────────────────────────
        byte[] atcBytes = tags.get("9F36");
        String atc = atcBytes != null ? HexUtils.bytesToHex(atcBytes) : "N/A";

        // ── Data da transação (tag 9A) ───────────────────────────────────────
        byte[] dateBytes = tags.get("9A");
        String txnDate = dateBytes != null
            ? BcdUtils.bcdToString(dateBytes, dateBytes.length * 2)
            : "N/A";

        // ── TVR (tag 95) ─────────────────────────────────────────────────────
        byte[] tvrBytes = tags.get("95");
        String tvr = tvrBytes != null ? HexUtils.bytesToHex(tvrBytes) : "N/A";
        boolean sdaFailed = false;
        boolean cvmFailed = false;
        if (tvrBytes != null && tvrBytes.length >= 1) {
            sdaFailed = (tvrBytes[0] & 0x01) != 0; // byte1 bit1: Offline data auth failed
            if (tvrBytes.length >= 3) {
                cvmFailed = (tvrBytes[2] & 0x08) != 0; // byte3 bit4: CVM failed
            }
        }
        if (sdaFailed) warnings.add("OFFLINE_DATA_AUTH_FAILED: risco elevado, possível clone");
        if (cvmFailed) warnings.add("CVM_FAILED: método de verificação falhou");

        // ── AIP (tag 82) ─────────────────────────────────────────────────────
        byte[] aipBytes = tags.get("82");
        String aip = aipBytes != null ? HexUtils.bytesToHex(aipBytes) : "N/A";
        boolean isContactless = false;
        if (aipBytes != null && aipBytes.length >= 1) {
            isContactless = (aipBytes[0] & 0x20) != 0; // bit6: on-device CVM supported
        }
        // Também inferir pelo DE22
        if (de22 != null && (de22.startsWith("07") || de22.startsWith("91"))) {
            isContactless = true;
        }

        // ── CVM Results (tag 9F34) ────────────────────────────────────────────
        byte[] cvmBytes = tags.get("9F34");
        String cvmUsed = "UNKNOWN";
        if (cvmBytes != null && cvmBytes.length >= 1) {
            int method = cvmBytes[0] & 0x3F;
            cvmUsed = switch (method) {
                case 0x00 -> "Fail/No CVM";
                case 0x01 -> "Offline Plaintext PIN";
                case 0x02 -> "Online Encrypted PIN";
                case 0x03 -> "Online Encrypted PIN + Signature";
                case 0x04 -> "Offline Encrypted PIN";
                case 0x05 -> "Offline Encrypted PIN + Signature";
                case 0x1E -> "Signature";
                case 0x1F -> "No CVM required";
                case 0x3F -> "No CVM performed";
                default   -> String.format("Unknown(0x%02X)", method);
            };
            if (cvmBytes.length >= 3) {
                int result = cvmBytes[2] & 0xFF;
                if (result != 0x02) {
                    warnings.add("CVM_RESULT_NOT_SUCCESSFUL: byte3=" +
                                 String.format("%02X", result));
                }
            }
        }

        // ── Verificações cruzadas ─────────────────────────────────────────────
        if ("ARQC".equals(cryptogramType) && "No CVM required".equals(cvmUsed)) {
            // OK para contactless abaixo do floor limit
        }
        if ("TC".equals(cryptogramType)) {
            warnings.add("OFFLINE_TC: aprovação offline — não há garantia do emissor online");
        }

        return new DE55Analysis(cryptogramType, atc, txnDate, cvmUsed,
                                tvr, aip, sdaFailed, cvmFailed,
                                isContactless, Collections.unmodifiableList(warnings));
    }

    /** Resumo legível para logs/debugging */
    public String summarize(DE55Analysis a) {
        return String.format(
            "DE55[crypto=%s atc=%s date=%s cvm=%s tvr=%s aip=%s contactless=%b sdaFail=%b cvmFail=%b warnings=%s]",
            a.cryptogramType(), a.atc(), a.txnDate(), a.cvmUsed(),
            a.tvr(), a.aip(), a.isContactless(),
            a.sdaFailed(), a.cvmFailed(), a.warnings()
        );
    }
}
```

**Uso no participant:**

```java
// Dentro do prepare() do ValidateEMV participant
if (msg.hasField(55)) {
    byte[] de55 = msg.getBytes(55);
    DE55Analyzer.DE55Analysis emv = analyzer.analyze(de55, msg.getString(22));

    if ("AAC".equals(emv.cryptogramType())) {
        // Chip recusou offline — não deve autorizar
        ctx.put("RESPONSE_CODE", "05");
        return ABORTED;
    }
    if (emv.sdaFailed()) {
        // Risco de clone — acionar regras anti-fraude extras
        ctx.put("EMV_RISK_FLAG", "SDA_FAILED");
    }
    emv.warnings().forEach(w -> log.warn("EMV_WARNING {} STAN={}", w, stan));
    ctx.put("EMV_ANALYSIS", emv);
}
```

### Desafio
Receba dois dumps de DE 55 — um de transação chip e um de contactless. Compare tag a tag e documente as diferenças.

---

# Semana 18 — HSM, PIN, DUKPT e Segurança

## 1. Hierarquia de Chaves

```
HSM (Hardware Security Module)
  └── LMK (Local Master Key) — NUNCA sai do HSM
      └── ZMK (Zone Master Key) — compartilhada entre instituições
          ├── ZPK (Zone PIN Key) — criptografa PIN blocks
          ├── ZAK (Zone Auth Key) — calcula MAC
          └── ZEK (Zone Encryption Key) — criptografa dados
```

## 2. DUKPT (Derived Unique Key Per Transaction)

### 2.1 Hierarquia de Chaves

```
BDK (Base Derivation Key, 128 bits) — fica NO HSM do adquirente. Nunca sai.
  └── IPEK (Initial PIN Encryption Key, 128 bits)
        = 3DES( BDK, KSN_inicial[bits 0..63] XOR C0C0C0C000000000 )
        injetada no terminal durante key injection ceremony
      └── Para cada transação:
            KSN = Terminal ID (59 bits) + Contador de transação (21 bits)
            Session Key = Future_Key_Register derivado do IPEK + KSN
            EncryptedPINBlock = 3DES( Session Key, ClearPINBlock )
```

### 2.2 Estrutura do KSN

```
┌──────────────────────────┬──────────────────────┐
│  Key Set ID (59 bits)    │  Counter (21 bits)    │
│  = Terminal ID + BDK ID  │  0 → 2.097.151 máx   │
└──────────────────────────┴──────────────────────┘
Total: 80 bits = 10 bytes
```

O contador incrementa a cada transação. Quando atinge o máximo (2^21 - 1), o terminal precisa ser re-injetado com nova IPEK.

### 2.3 Derivação da Session Key (simplificado)

O algoritmo DUKPT usa um processo de "future key register" baseado em ANSI X9.24-1:

```
1. Começa com IPEK no "Current Key Register"
2. Para cada bit '1' do contador (da esquerda para a direita):
     a. XOR o KSN com a máscara correspondente ao bit
     b. Current Key = 3DES( Current Key, KSN XOR mask )
3. Session Key = Current Key XOR derivation constant (00000000000000FF...)
```

Pseudocódigo didático:
```
IPEK = deriveIPEK(BDK, KSN_initial)

function deriveSessionKey(IPEK, KSN):
    registers = [IPEK]  # array de chaves intermediárias
    counter = KSN & 0x1FFFFF  # 21 bits menos significativos

    for each bit i (0 to 20, high to low):
        if bit i of counter == 1:
            mask = shiftRegisterMask(i)
            prevKey = registers[-1]
            newKey = TDES_EDE( prevKey, (KSN XOR mask)[0:8] )
            registers.append(newKey)

    sessionKey = registers[-1] XOR PIN_ENCRYPTION_VARIANT
    return sessionKey
```

**Vantagem:** Mesmo se um atacante capturar e quebrar uma session key, ele não consegue derivar chaves de outras transações — o processo só avança para frente (forward secrecy).

### 2.4 O que o adquirente recebe e como decripta

Junto com DE52 (PIN block criptografado), o terminal envia o **KSN** (geralmente em campo proprietário ou DE 53):

```
Terminal → Switch:
  DE 52 = 3DES(SessionKey, PINBlock)        8 bytes
  KSN   = Terminal ID + Counter             10 bytes (campo proprietário)

Switch → HSM do adquirente:
  "Decripta DE52 usando BDK com este KSN"

HSM:
  1. Reconstrói IPEK = deriveIPEK(BDK, KSN)
  2. Reconstrói SessionKey usando a lógica do counter
  3. Decripta PIN Block
  4. Re-criptografa com ZPK da rede destino
  5. Retorna EncPINBlock' para o switch
```

O BDK fica **permanentemente no HSM** do adquirente. O switch nunca vê chaves em claro.

**Vantagem:** Se uma chave de sessão vazar, só compromete aquela transação. Não afeta as demais.

## 3. PIN Translation

Quando a mensagem passa por um nó intermediário (processadora/bandeira):

```
Terminal → Adquirente:  PIN criptografado com ZPK-A
Adquirente → HSM:       "Traduza de ZPK-A para ZPK-B"
HSM:                     Descriptografa → re-criptografa
Adquirente → Bandeira:  PIN criptografado com ZPK-B
```

O PIN em claro NUNCA existe fora do HSM.

## 4. Como Construir um PIN Block (ISO 9564 Format 0)

O **PIN Block** é a representação criptografável do PIN do portador. O Format 0 (mais usado) é construído em 3 passos:

### Passo 1 — PIN Block (8 bytes)

```
Nibble 0:   '0'              (identificador de formato)
Nibble 1:   tamanho do PIN   (ex: '4' para PIN de 4 dígitos)
Nibbles 2-N: dígitos do PIN  (ex: '1','2','3','4')
Nibbles N+1 a 15: 'F'       (padding)
```

Para PIN=`1234`: `04 12 34 FF FF FF FF FF`

### Passo 2 — PAN Block (8 bytes)

```
Nibbles 0-3:  '0000'         (zeros fixos)
Nibbles 4-15: 12 dígitos centrais do PAN (excluindo check digit)
```

Para PAN=`4532015112830366`:
- Remove check digit → `453201511283036`
- Pega os 12 dígitos da direita → `532015112830`
- PAN Block: `00 00 53 20 15 11 28 30`

### Passo 3 — XOR

```
PIN Block XOR PAN Block = Cleartext PIN Block
```

```
04 12 34 FF FF FF FF FF
XOR
00 00 53 20 15 11 28 30
=
04 12 67 DF EA EE D7 CF
```

Este resultado é enviado ao HSM para criptografia com 3DES usando a ZPK.

### Implementação Java

```java
public class PINBlockBuilder {

    /**
     * Constrói PIN Block Format 0 (ISO 9564-1).
     * @param pin  PIN do portador (4-12 dígitos)
     * @param pan  PAN completo (13-19 dígitos)
     * @return     8 bytes do cleartext PIN block (deve ser criptografado imediatamente)
     */
    public static byte[] buildFormat0(String pin, String pan) {
        if (pin == null || pin.length() < 4 || pin.length() > 12)
            throw new IllegalArgumentException("PIN deve ter 4-12 dígitos");
        if (pan == null || pan.length() < 13)
            throw new IllegalArgumentException("PAN inválido");

        // ── Passo 1: PIN Block ──────────────────────────────────────────
        // Nibbles: 0 | len | digit... | F...F (16 nibbles = 8 bytes)
        char[] pinNibbles = new char[16];
        pinNibbles[0] = '0';
        pinNibbles[1] = (char) ('0' + pin.length());
        for (int i = 0; i < pin.length(); i++)
            pinNibbles[2 + i] = pin.charAt(i);
        for (int i = 2 + pin.length(); i < 16; i++)
            pinNibbles[i] = 'F';

        byte[] pinBlock = new byte[8];
        for (int i = 0; i < 8; i++)
            pinBlock[i] = (byte) ((hexVal(pinNibbles[i * 2]) << 4)
                                 | hexVal(pinNibbles[i * 2 + 1]));

        // ── Passo 2: PAN Block ──────────────────────────────────────────
        // Remove check digit → pega 12 dígitos mais à direita
        String panStripped = pan.replaceAll("\\D", "");
        String panDigits = panStripped.substring(panStripped.length() - 13,
                                                  panStripped.length() - 1);
        String panHex = "0000" + panDigits;   // 16 nibbles

        byte[] panBlock = new byte[8];
        for (int i = 0; i < 8; i++)
            panBlock[i] = (byte) ((hexVal(panHex.charAt(i * 2)) << 4)
                                 | hexVal(panHex.charAt(i * 2 + 1)));

        // ── Passo 3: XOR ────────────────────────────────────────────────
        byte[] result = new byte[8];
        for (int i = 0; i < 8; i++)
            result[i] = (byte) (pinBlock[i] ^ panBlock[i]);

        // Zera buffers intermediários (boa prática de segurança)
        java.util.Arrays.fill(pinBlock, (byte) 0);
        java.util.Arrays.fill(panBlock, (byte) 0);
        java.util.Arrays.fill(pinNibbles, '\0');

        return result;
    }

    // ── Criptografia com 3DES (via HSM ou JCE) ─────────────────────────
    /**
     * Simula criptografia 3DES para fins didáticos.
     * Em produção SEMPRE usar HSM — nunca chave em memória.
     */
    public static byte[] encrypt3DES(byte[] pinBlock, byte[] zpk) throws Exception {
        javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(zpk, "DESede");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(pinBlock);
    }

    private static int hexVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return 0xF;  // trata 'F' de padding
    }

    // ── Utilidade: bytes → hex string ──────────────────────────────────
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
```

### Testes

```java
class PINBlockBuilderTest {

    @Test
    void testBuildFormat0_knownVector() {
        // Vetor de teste amplamente documentado
        byte[] result = PINBlockBuilder.buildFormat0("1234", "4532015112830366");
        String hex = PINBlockBuilder.toHex(result);
        assertEquals("041267DFEAEED7CF", hex);
    }

    @Test
    void testBuildFormat0_sixDigitPIN() {
        byte[] result = PINBlockBuilder.buildFormat0("123456", "4532015112830366");
        String hex = PINBlockBuilder.toHex(result);
        // Nibbles PIN: 0 6 1 2 3 4 5 6 F F F F F F F F
        // = 06 12 34 56 FF FF FF FF
        // XOR PAN Block 00 00 53 20 15 11 28 30
        // = 06 12 67 76 EA EE D7 CF
        assertEquals("061267 76EAEED7CF".replace(" ", ""), hex);
    }

    @Test
    void testPINTooShort() {
        assertThrows(IllegalArgumentException.class,
            () -> PINBlockBuilder.buildFormat0("123", "4532015112830366"));
    }

    @Test
    void testInvalidPAN() {
        assertThrows(IllegalArgumentException.class,
            () -> PINBlockBuilder.buildFormat0("1234", "123"));
    }
}
```

### Fluxo Completo Terminal → Emissor

```
Terminal                  Adquirente              HSM              Emissor
   │                          │                    │                  │
   │  PIN digitado             │                    │                  │
   │  PINBlock = Format0(PIN,PAN)                   │                  │
   │  EncPINBlock = DUKPT_enc(PINBlock)             │                  │
   │─── 0200 [DE52=EncPINBlock, KSN] ──►            │                  │
   │                          │── TranslatePIN ────►│                  │
   │                          │   (ZPK-A → ZPK-B)  │                  │
   │                          │◄── EncPINBlock' ────│                  │
   │                          │─────── 0100 [DE52=EncPINBlock'] ──────►│
   │                          │                    │  verifica PIN     │
   │                          │◄────────────────── 0110 [DE39=00] ─────│
   │◄── 0210 [DE39=00] ───────│                    │                  │
```

### Lado do Emissor — Verificação do PIN

O emissor (ou seu HSM) recebe DE52 criptografado e precisa verificar se o PIN é correto:

```java
public class IssuerPINVerifier {

    /**
     * Verifica o PIN recebido pelo emissor.
     *
     * Em produção: NUNCA sai do HSM. Aqui é apenas didático.
     *
     * @param encryptedDE52  DE52 criptografado com ZPK-B (8 bytes)
     * @param zpkB           ZPK do lado do emissor (16 ou 24 bytes)
     * @param pan            PAN do portador (para reconstruir PAN block)
     * @param correctPIN     PIN correto armazenado pelo emissor (hash ou cleartext didático)
     * @return DE39 code: "00" (correto) ou "55" (incorreto)
     */
    public String verifyPIN(byte[] encryptedDE52, byte[] zpkB,
                             String pan, String correctPIN) throws Exception {
        // Passo 1: Descriptografa com ZPK-B → cleartext PIN block
        byte[] cleartextPINBlock = decrypt3DES(encryptedDE52, zpkB);

        // Passo 2: Reconstrói PAN block (mesmo algoritmo do terminal)
        String panStripped = pan.replaceAll("\\D", "");
        String panDigits   = panStripped.substring(panStripped.length() - 13,
                                                    panStripped.length() - 1);
        String panHex = "0000" + panDigits;
        byte[] panBlock = new byte[8];
        for (int i = 0; i < 8; i++)
            panBlock[i] = (byte) ((hexVal(panHex.charAt(i * 2)) << 4)
                                 | hexVal(panHex.charAt(i * 2 + 1)));

        // Passo 3: XOR para recuperar PIN block em claro
        byte[] pinBlock = new byte[8];
        for (int i = 0; i < 8; i++)
            pinBlock[i] = (byte) (cleartextPINBlock[i] ^ panBlock[i]);

        // Passo 4: Extrai PIN dos nibbles
        // Nibble 0 = '0' (formato), Nibble 1 = comprimento, Nibbles 2..N = dígitos
        int pinLen = pinBlock[0] & 0x0F;  // segundo nibble do primeiro byte
        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < pinLen; i++) {
            int byteIdx  = (i + 2) / 2;
            boolean high = ((i + 2) % 2 == 0);
            int nibble   = high ? (pinBlock[byteIdx] >> 4) & 0x0F
                                : pinBlock[byteIdx] & 0x0F;
            pin.append((char)('0' + nibble));
        }

        // Passo 5: Zera buffers sensíveis
        java.util.Arrays.fill(cleartextPINBlock, (byte) 0);
        java.util.Arrays.fill(pinBlock, (byte) 0);
        java.util.Arrays.fill(panBlock, (byte) 0);

        // Passo 6: Compara com PIN correto
        // Em produção: compara com PIN offset armazenado (PVV/IBM 3624)
        return correctPIN.equals(pin.toString()) ? "00" : "55";
    }

    public static byte[] decrypt3DES(byte[] data, byte[] key) throws Exception {
        javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(key, "DESede");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private static int hexVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return 0xF;
    }
}
```

**Nota sobre armazenamento de PIN em produção:**

Emissores reais NUNCA armazenam o PIN em texto claro. Usam um dos métodos:

| Método | Como funciona |
|--------|--------------|
| **IBM 3624 PIN offset** | PIN derivado do PAN usando DES; offset = PIN real − PIN natural |
| **Visa PVV (PIN Verification Value)** | PVV calculado via 3DES(ZPK, PAN+PAN seq+offset) |
| **PIN Block apenas no HSM** | A comparação ocorre inteiramente dentro do HSM sem nunca expor o PIN |

A abordagem correta de produção é enviar o cleartext PIN block ao HSM do emissor que executa a verificação internamente e retorna apenas "correto/incorreto".

## 5. PAN Masking — PCI Mindset

```java
public class PANMasker {
    
    // Mostra apenas first 6 + last 4 (ou first 8 + last 4 com 8-digit BIN)
    public static String mask(String pan) {
        if (pan == null || pan.length() < 13) return "INVALID_PAN";
        int show = Math.min(6, pan.length() - 4);
        return pan.substring(0, show) 
             + "*".repeat(pan.length() - show - 4)
             + pan.substring(pan.length() - 4);
    }
    
    // Para logs: NUNCA logar PAN completo, track data, PIN, CVV
    public static String sanitizeForLog(ISOMsg msg) throws ISOException {
        StringBuilder sb = new StringBuilder();
        sb.append("MTI=").append(msg.getMTI());
        
        if (msg.hasField(2))  sb.append(" PAN=").append(mask(msg.getString(2)));
        if (msg.hasField(4))  sb.append(" AMT=").append(msg.getString(4));
        if (msg.hasField(11)) sb.append(" STAN=").append(msg.getString(11));
        if (msg.hasField(39)) sb.append(" RC=").append(msg.getString(39));
        if (msg.hasField(41)) sb.append(" TID=").append(msg.getString(41));
        // NUNCA logar: DE 35 (track), DE 52 (PIN), DE 55 raw
        
        return sb.toString();
    }
}
```

## 5. Exercícios Semana 18

1. **Implemente PIN Block Format 0** (gerar e validar)
2. **Implemente PANMasker** com testes extensivos
3. **Revise TODOS os logs do payment-switch-lab** — garanta que nenhum PAN completo, track ou PIN aparece
4. **Documente `security-policy.md`** com regras de logging, retenção, mascaramento

### Desafio
Faça um security audit no seu código: busque por qualquer lugar onde `msg.getString(2)`, `msg.getString(35)` ou `msg.getString(52)` é logado sem mascaramento. Corrija todos.

---

# Semana 19 — E-commerce, Credential on File, Tokenização

## 1. Card-Present vs Card-Not-Present

| Aspecto | CP (Presencial) | CNP (E-commerce) |
|---------|-----------------|-------------------|
| DE 22 | 051, 071 (chip) | 810, 012 (manual) |
| DE 25 | 00 (normal) | 08 (MOTO) ou 59 (e-com) |
| Autenticação | Chip + PIN | 3DS, OTP |
| Risco de fraude | Baixo | Alto |
| Interchange | Menor | Maior |
| Chargeback liability | Emissor (chip) | Merchant (sem 3DS) |

## 2. Credential on File (COF) — Framework Visa/Mastercard

Quando um merchant armazena os dados do cartão para cobranças futuras:

```
Transação inicial (CIT — Cardholder Initiated):
  Portador compra e autoriza armazenamento
  Bandeira retorna Network Transaction ID

Transações subsequentes (MIT — Merchant Initiated):
  Merchant cobra sem portador presente
  Usa Network TX ID da primeira transação como referência
  
DE 22 = 100 (credential on file)
DE 48 = indicadores COF (varia por bandeira)
```

## 3. Tokenização Network-Level

```
FPAN (Funding PAN): 4532 0151 1283 0366 ← número real do cartão
DPAN (Digital PAN):  4532 9999 8888 7777 ← token (substituto)

Apple Pay / Google Pay:
  Device armazena DPAN (não o FPAN)
  Cada transação gera um criptograma único
  DE 2 carrega o DPAN
  Emissor/processadora de-tokeniza para encontrar FPAN
```

## 4. Exercícios Semana 19

1. **Implemente distinção CP/CNP** no ValidateMessage participant
2. **Documente `cnp-and-cof.md`** com os campos que mudam em e-commerce
3. **Crie cenário de recurring** — primeira transação + 3 cobranças subsequentes com COF
4. **Implemente lógica de 3DS** como flag no DE 22 (82 = e-com com 3DS autenticado)

### Desafio
Modele o fluxo completo de uma assinatura mensal de streaming:
- Mês 1: Portador cadastra cartão (CIT)
- Mês 2-12: Cobranças automáticas (MIT)
- Mês 6: Cartão expira e é renovado (Account Updater)
- Mês 8: Portador contesta (chargeback)
Quais mensagens ISO 8583 são trocadas em cada etapa?

---

# Semana 20 — Clearing, Settlement e Reconciliação

## 1. O ciclo completo

```
Dia 0:   Autorização (real-time, ISO 8583)
Dia 0:   Capture/batch close (end of day)
Dia 1:   Clearing (batch file, adquirente → bandeira → emissor)
Dia 2:   Settlement (movimentação financeira via câmara)
Dia 30:  Pagamento ao merchant (crédito D+30, ou antecipado)
```

## 2. Clearing files

**Visa — TC (Transaction Clearing) files:**
- TC05: Original presentment
- TC07: Fee collection
- Formato: posicional, registros de ~200 campos

**Mastercard — IPM (Integrated Products Messages):**
- DE-based (similar a ISO 8583, mas para clearing)
- Cada registro tem seu próprio "MTI" de clearing

## 3. Mismatches comuns

| Mismatch | Causa | Impacto |
|----------|-------|---------|
| Auth R$100, Clearing R$115 | Gorjeta (restaurante) | Normal em DMS |
| Auth R$100, Clearing R$80 | Partial capture | Normal |
| Auth sem clearing | Merchant não capturou | Auth expira (7-30 dias) |
| Clearing sem auth | Force post | Exceção — multa possível |
| Clearing com PAN diferente | Erro de sistema | Exceção grave |

## 4. Reconciliação

```java
public class ReconciliationEngine {
    
    public List<ReconciliationException> reconcile(
            List<AuthRecord> authorizations,
            List<ClearingRecord> clearings) {
        
        List<ReconciliationException> exceptions = new ArrayList<>();
        
        // Index auths by RRN
        Map<String, AuthRecord> authByRRN = authorizations.stream()
            .collect(Collectors.toMap(AuthRecord::rrn, Function.identity()));
        
        for (ClearingRecord clearing : clearings) {
            AuthRecord auth = authByRRN.get(clearing.rrn());
            
            if (auth == null) {
                // Clearing sem auth — Force Post
                exceptions.add(new ReconciliationException(
                    "CLEARING_WITHOUT_AUTH", clearing));
                continue;
            }
            
            if (!auth.amount().equals(clearing.amount())) {
                // Amount mismatch
                exceptions.add(new ReconciliationException(
                    "AMOUNT_MISMATCH", auth, clearing,
                    "Auth=" + auth.amount() + " Clearing=" + clearing.amount()));
            }
            
            authByRRN.remove(clearing.rrn()); // Matched
        }
        
        // Auths sem clearing
        for (AuthRecord unmatched : authByRRN.values()) {
            if (unmatched.isExpired()) {
                exceptions.add(new ReconciliationException(
                    "AUTH_WITHOUT_CLEARING_EXPIRED", unmatched));
            }
        }
        
        return exceptions;
    }
}
```

## 5. Exercícios Semana 20

1. **Implemente ReconciliationEngine** com detecção de 4+ tipos de mismatch
2. **Gere dados sintéticos** de auth e clearing com mismatches intencionais
3. **Crie relatório de exceções** com classificação e severidade
4. **Documente `post-auth-lifecycle.md`** com o ciclo completo

### Desafio
Explique por escrito (para um analista financeiro não-técnico) por que uma transação autorizada em R$ 200,00 pode aparecer no clearing como R$ 185,00 e isso ser completamente legítimo. Dê 3 cenários diferentes.
