# Semana 2 — Anatomia da Mensagem ISO 8583

## 1. A Estrutura — Três Camadas

Toda mensagem ISO 8583 é composta de exatamente três partes, sempre nesta ordem:

```
┌─────────────────────────────────────────────────────────────────┐
│ MTI (4 bytes) │ BITMAP (8 ou 16 bytes) │ DATA ELEMENTS (variável)│
└─────────────────────────────────────────────────────────────────┘
```

Pense assim:
- **MTI** = "o que eu quero?" (tipo da mensagem)
- **Bitmap** = "o que eu estou mandando?" (quais campos estão presentes)
- **Data Elements** = "aqui estão os dados" (os campos propriamente ditos)

---

## 2. MTI — Message Type Indicator

### 2.1 Estrutura de 4 dígitos

Cada dígito tem um significado específico:

```
MTI = [V] [C] [F] [O]
       │   │   │   └── Origem (Originator)
       │   │   └────── Função (Function)
       │   └────────── Classe (Class)
       └────────────── Versão (Version)
```

### 2.2 Dígito 1 — Versão

| Valor | Versão | Uso prático |
|-------|--------|-------------|
| 0 | ISO 8583:1987 | **Usado na maioria dos switches brasileiros** |
| 1 | ISO 8583:1993 | Raro |
| 2 | ISO 8583:2003 | Moderno, suporte XML. Alguns sistemas novos. |

### 2.3 Dígito 2 — Classe da Mensagem

| Valor | Classe | Quando usar |
|-------|--------|-------------|
| 1 | Authorization | Pedido de autorização ao emissor. O dinheiro NÃO é capturado. |
| 2 | Financial | Transação financeira. Em SMS, já captura. Em DMS, é a captura. |
| 3 | File Action | Upload de arquivos (batch). Raramente usado em auth. |
| 4 | Reversal / Chargeback | Desfazer transação anterior. |
| 5 | Reconciliation | Fechamento de lote, totais. |
| 6 | Administrative | Mensagens de administração. |
| 7 | Fee Collection | Cobrança de taxas. |
| 8 | Network Management | Echo, sign-on/off, key exchange. |

### 2.4 Dígito 3 — Função

| Valor | Função | Significado |
|-------|--------|-------------|
| 0 | Request | Eu estou pedindo algo. Espero uma resposta. |
| 1 | Response | Eu estou respondendo a um request. |
| 2 | Advice | Eu estou notificando. Não preciso de "decisão" na resposta, mas preciso de confirmação de recebimento. |
| 3 | Advice Response | Confirmação de que recebi o advice. |
| 4 | Notification | Notificação sem resposta esperada. |

**A diferença entre Request e Advice é fundamental:**
- **Request (0):** "Posso cobrar R$ 100?" → Emissor decide: sim ou não.
- **Advice (2):** "Já cobrei R$ 100, estou te avisando." → Emissor não decide, apenas registra.

Quando usar advice? Store-and-forward (SAF), confirmações pós-autorização, ajustes.

### 2.5 Dígito 4 — Origem

| Valor | Origem | Significado |
|-------|--------|-------------|
| 0 | Acquirer | Mensagem originada pelo adquirente |
| 1 | Acquirer Repeat | Retransmissão do adquirente (mesma mensagem, retry) |
| 2 | Issuer | Mensagem originada pelo emissor |
| 3 | Issuer Repeat | Retransmissão do emissor |

**Aqui mora um detalhe perigoso:** O dígito 4 = 1 (repeat) tem implicações de **idempotência**. Se o receptor recebe `0201` em vez de `0200`, ele sabe que é retransmissão e deve retornar a mesma resposta anterior sem reprocessar.

### 2.6 Tabela completa dos MTIs que você vai usar

| MTI | Nome | Quem envia | Para quem | Cenário |
|------|------|-----------|-----------|---------|
| `0100` | Auth Request | Adquirente | Emissor (via bandeira) | "Autorize R$ 100 no cartão X" |
| `0110` | Auth Response | Emissor | Adquirente | "Aprovado" ou "Negado" |
| `0120` | Auth Advice | Adquirente | Emissor | "Informo que autorizei offline" |
| `0130` | Auth Advice Response | Emissor | Adquirente | "Recebi seu advice" |
| `0200` | Financial Request | Terminal/Adquirente | Emissor | "Cobre R$ 100" (single msg) |
| `0210` | Financial Response | Emissor | Adquirente | Resposta ao financial |
| `0220` | Financial Advice | Adquirente | Emissor | Confirmação de captura (DMS) |
| `0230` | Financial Advice Resp | Emissor | Adquirente | "Recebi a confirmação" |
| `0400` | Reversal Request | Adquirente | Emissor | "Desfaça a transação X" |
| `0410` | Reversal Response | Emissor | Adquirente | "Desfeita" ou "Não encontrada" |
| `0420` | Reversal Advice | Adquirente | Emissor | "Desfiz e estou avisando" |
| `0430` | Reversal Advice Resp | Emissor | Adquirente | "Recebi o aviso" |
| `0500` | Reconciliation Req | Adquirente | Emissor/Bandeira | "Fechando lote do dia" |
| `0510` | Reconciliation Resp | Emissor | Adquirente | Totais confirmados |
| `0800` | Network Mgmt Req | Qualquer | Qualquer | Echo, sign-on, key exchange |
| `0810` | Network Mgmt Resp | Qualquer | Qualquer | Resposta de rede |

### 2.7 Como transformar Request em Response

Regra simples: **somar 10 ao MTI.**

```
0100 → 0110 (Auth Request → Auth Response)
0200 → 0210 (Financial Request → Financial Response)
0400 → 0410 (Reversal Request → Reversal Response)
0800 → 0810 (Network Request → Network Response)
```

No jPOS, isso é feito com `msg.setResponseMTI()`.

---

## 3. Bitmap — O Mapa dos Campos

### 3.1 Conceito

O bitmap é uma sequência de bits onde cada posição indica se o campo correspondente está presente (1) ou ausente (0) na mensagem.

```
Posição:  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 ...
Bitmap:   1  1  1  1  0  0  1  0  0  0  1  1  1  0  0  0 ...
          │  │  │  │        │        │  │  │
          │  │  │  │        │        │  │  └── DE13 presente
          │  │  │  │        │        │  └───── DE12 presente
          │  │  │  │        │        └──────── DE11 presente
          │  │  │  │        └────────────────── DE7 presente
          │  │  │  └─────────────────────────── DE4 presente
          │  │  └────────────────────────────── DE3 presente
          │  └───────────────────────────────── DE2 presente
          └──────────────────────────────────── bit 1 = 1 → secondary bitmap EXISTE
```

### 3.2 Primary vs Secondary Bitmap

- **Primary bitmap** = 64 bits = 8 bytes → cobre DE 1 a DE 64
- **Secondary bitmap** = mais 64 bits = mais 8 bytes → cobre DE 65 a DE 128
- Se **bit 1 da primary = 1**, a secondary está presente (logo a mensagem tem 16 bytes de bitmap)
- Se **bit 1 = 0**, só a primary existe (8 bytes de bitmap)

### 3.3 Lendo um bitmap hexadecimal — passo a passo

Bitmap hex: `F230040128C28805`

**Passo 1:** Converter hex para binário, dígito a dígito:

```
F    2    3    0    0    4    0    1    2    8    C    2    8    8    0    5
1111 0010 0011 0000 0000 0100 0000 0001 0010 1000 1100 0010 1000 1000 0000 0101
```

**Passo 2:** Numerar cada posição (1 a 64):

```
Pos: 1234 5678 9012 3456 7890 1234 5678 9012 3456 7890 1234 5678 9012 3456 7890 1234
Bit: 1111 0010 0011 0000 0000 0100 0000 0001 0010 1000 1100 0010 1000 1000 0000 0101
```

**Passo 3:** Listar as posições com bit = 1:

```
Bit 1  = 1 → Secondary bitmap presente (DE 65-128 existem)
Bit 2  = 1 → DE 2 (PAN) presente
Bit 3  = 1 → DE 3 (Processing Code) presente
Bit 4  = 1 → DE 4 (Amount) presente
Bit 7  = 1 → DE 7 (Transmission Date/Time) presente
Bit 11 = 1 → DE 11 (STAN) presente
Bit 12 = 1 → DE 12 (Local Time) presente
Bit 22 = 1 → DE 22 (POS Entry Mode) presente
Bit 32 = 1 → DE 32 (Acquiring ID) presente
Bit 35 = 1 → DE 35 (Track 2) presente
Bit 38 = 1 → DE 38 (Auth Code) presente
Bit 39 = 1 → DE 39 (Response Code) presente
Bit 41 = 1 → DE 41 (Terminal ID) presente
Bit 42 = 1 → DE 42 (Merchant ID) presente
Bit 49 = 1 → DE 49 (Currency Code) presente
Bit 55 = 1 → DE 55 (EMV Data) presente
Bit 62 = 1 → DE 62 (Private) presente
Bit 64 = 1 → DE 64 (MAC) presente
```

### 3.4 Representação do bitmap

O bitmap pode ser transmitido como:
- **Hexadecimal ASCII:** `"F230040128C28805"` → 16 caracteres ASCII = 16 bytes
- **Binary:** `\xF2\x30\x04\x01\x28\xC2\x88\x05` → 8 bytes raw

A representação afeta o tamanho da mensagem. Em hex ASCII, o bitmap ocupa o dobro.

---

## 4. Data Elements — Formatos

### 4.1 Campos Fixos

Tamanho definido na spec. Sempre ocupa exatamente N caracteres/bytes.

```
DE 3 (Processing Code): n 6 fixo
  Exemplo: "003000"  → sempre 6 dígitos, sem prefixo

DE 39 (Response Code): an 2 fixo
  Exemplo: "00"  → sempre 2 caracteres

DE 41 (Terminal ID): ans 8 fixo
  Exemplo: "TERM0001"  → sempre 8 caracteres (padded com espaços se menor)
```

### 4.2 Campos Variáveis — LLVAR e LLLVAR

O tamanho é informado por um prefixo antes do valor.

**LLVAR** — prefixo de 2 dígitos (tamanho máximo: 99):

```
DE 2 (PAN): n ..19 LLVAR
  Exemplo: "164532015112830366"
            ││└─────────────────── Valor: 4532015112830366 (16 dígitos)
            └┘ Prefixo: 16 (tamanho do valor)
```

**LLLVAR** — prefixo de 3 dígitos (tamanho máximo: 999):

```
DE 55 (EMV Data): b ..999 LLLVAR
  Exemplo: "0459F2608AABB..."
            │││└──────────── Valor: dados EMV (45 bytes)
            └┘┘ Prefixo: 045 (tamanho do valor)
```

**LLLLVAR** — prefixo de 4 dígitos (ISO 8583:2003 only, máx 9999)

### 4.3 Tipos de dados

| Símbolo | Tipo | Caracteres válidos |
|---------|------|-------------------|
| `n` | Numérico | 0-9 |
| `a` | Alfabético | A-Z, a-z |
| `an` | Alfanumérico | A-Z, a-z, 0-9 |
| `ans` | Alfanumérico especial | A-Z, a-z, 0-9, espaço, pontuação |
| `b` | Binário | Bytes raw |
| `z` | Track data | 0-9, =, D, separadores |

### 4.4 Padding

Campos fixos menores que o tamanho definido precisam de padding:

- **Numéricos:** pad com zeros à esquerda → `"00015000"` (valor 15000, 8 dígitos)
- **Alfanuméricos:** pad com espaços à direita → `"TERM0001"` (8 chars, padded)

---

## 5. Correlação Request ↔ Response

### 5.1 Como o switch sabe qual response combina com qual request?

A correlação é feita por uma **chave composta**. Não existe um campo único que identifique a transação universalmente. A combinação típica:

```
Chave de correlação = STAN (DE 11) + Terminal ID (DE 41) + Acquiring ID (DE 32)
```

Algumas implementações usam:

```
Chave alternativa = STAN (DE 11) + Transmission Date (DE 7) + Acquiring ID (DE 32)
```

### 5.2 Fluxo de correlação

```
1. Switch envia request com STAN=123456
2. Switch armazena: "STAN 123456 → esperando response, timeout em 30s"
3. Emissor responde com STAN=123456 (mesmo valor)
4. Switch encontra o match: "STAN 123456 → encontrado, entrego a response"
5. Se timeout sem match → gera reversal automático
```

### 5.3 Problemas comuns

- **STAN duplicado:** Se o STAN não é único na janela de tempo, o switch pode entregar a response para o request errado
- **Late response:** Response chega depois do timeout → switch já mandou reversal, agora precisa decidir o que fazer
- **STAN rollover:** STAN é 6 dígitos (0-999999), então dá a volta. Em alto volume, pode reutilizar rápido.

---

## 6. Exemplo Completo — Montando uma Mensagem

Vamos montar uma mensagem `0200` (Financial Request) para compra de R$ 150,00 com chip:

```
┌─────────── MTI ────────────┐
│ 0200                        │
│ 0 = versão 1987             │
│ 2 = Financial               │
│ 0 = Request                 │
│ 0 = Acquirer                │
└────────────────────────────┘

┌─────────── BITMAP ─────────┐
│ Campos presentes:           │
│ DE2, DE3, DE4, DE7, DE11,   │
│ DE12, DE13, DE22, DE25,     │
│ DE41, DE42, DE49, DE55      │
│                             │
│ Bitmap = 7234054128C08001   │
└────────────────────────────┘

┌─────────── DATA ELEMENTS ──┐
│ DE2:  164532015112830366    │ ← LLVAR: "16" + PAN
│ DE3:  003000                │ ← fixo 6: compra crédito
│ DE4:  000000015000          │ ← fixo 12: R$ 150,00
│ DE7:  0314143025            │ ← fixo 10: Mar 14, 14:30:25
│ DE11: 123456                │ ← fixo 6: STAN
│ DE12: 143025                │ ← fixo 6: hora local
│ DE13: 0314                  │ ← fixo 4: data local
│ DE22: 051                   │ ← fixo 3: chip + PIN capable
│ DE25: 00                    │ ← fixo 2: normal presentation
│ DE41: TERM0001              │ ← fixo 8: terminal ID
│ DE42: MERCHANT00001  □□     │ ← fixo 15: merchant ID (□=espaço)
│ DE49: 986                   │ ← fixo 3: BRL
│ DE55: 045[EMV bytes...]     │ ← LLLVAR: "045" + dados EMV
└────────────────────────────┘
```

A mensagem serializada (concatenação de tudo) seria:

```
02007234054128C080011645320151128303660030000000000150000314143025123456143025031405100TERM0001MERCHANT00001  986045[EMV bytes]
```

---

## 7. Versões do ISO 8583 e Diferenças Práticas

| Aspecto | 1987 (v0) | 1993 (v1) | 2003 (v2) |
|---------|-----------|-----------|-----------|
| MTI prefixo | 0xxx | 1xxx | 2xxx |
| Max DEs | 128 | 128 | 128 |
| LLLLVAR | Não | Não | Sim (4 dígitos) |
| Tertiary bitmap | Não | Não | Sim (192 DEs) |
| Suporte XML | Não | Não | Sim |
| Uso no Brasil | **Dominante** | Raro | Emergente |

**Na prática:** Você vai trabalhar com 1987 (0xxx) na grande maioria dos casos. A versão 2003 aparece em implementações mais novas, mas a base instalada é 1987.
