# Fase 1 — Fundação: Ecossistema + Protocolo (Semanas 1-4)

---

# Semana 1 — Ecossistema de Pagamentos com Cartões

## 1. Modelo de 4 Partes (Four-Party Model)

```
                    ┌──────────┐
                    │ Bandeira  │
                    │ (Visa/MC) │
                    └──┬───┬──┘
                       │   │
              ┌────────┘   └────────┐
              │                     │
         ┌────┴────┐          ┌────┴────┐
         │Adquirente│          │ Emissor  │
         │ (Stone)  │          │ (Nubank) │
         └────┬────┘          └────┬────┘
              │                     │
         ┌────┴────┐          ┌────┴────┐
         │Merchant │          │Portador │
         │ (Loja)  │          │ (Você)  │
         └─────────┘          └─────────┘
```

## 2. Jornada da Transação

1. **Portador** insere/aproxima cartão no terminal
2. **Terminal** captura dados e monta mensagem ISO 8583 (0200)
3. **Adquirente** recebe, valida, roteia para bandeira
4. **Bandeira** identifica emissor, roteia para ele
5. **Emissor** verifica saldo/limite/regras, decide (DE39=00 ou 05/51/54...)
6. **Resposta** volta pelo mesmo caminho: emissor → bandeira → adquirente → terminal
7. **Clearing** (D+1): adquirente e emissor trocam arquivo de liquidação via bandeira
8. **Settlement**: dinheiro efetivamente muda de mãos (banco liquidante)

## 3. On-Us vs Off-Us

- **On-Us**: emissor e adquirente são a mesma instituição → roteamento direto, sem bandeira
- **Off-Us**: instituições diferentes → mensagem passa pela bandeira

## 4. Modelo Econômico

```
MDR (Merchant Discount Rate) = Interchange + Fee Bandeira + Markup Adquirente
Exemplo: 2.5% = 1.5% + 0.2% + 0.8%
```

---

# Semana 2 — Anatomia da Mensagem ISO 8583

## 1. Estrutura: MTI + Bitmap + Data Elements

```
┌─────────┬──────────────────┬─────────────────────────────┐
│ MTI     │ BITMAP           │ DATA ELEMENTS               │
│ 4 bytes │ 8 ou 16 bytes    │ Tamanho variável            │
└─────────┴──────────────────┴─────────────────────────────┘
```

## 2. MTI — Message Type Indicator

```
MTI = [V] [C] [F] [O]
       │   │   │   └── 0=Acquirer, 1=Repeat, 2=Issuer, 5=Other
       │   │   └────── 0=Request, 1=Response, 2=Advice, 3=Advice Response
       │   └────────── 1=Auth, 2=Financial, 4=Reversal, 8=Network Mgmt
       └────────────── 0=ISO 8583:1987, 1=1993, 2=2003
```

## 3. Bitmap — Indicador de Presença

Cada bit = 1 DE. Bit 1 = "secondary bitmap present". Bits 2-128 = DEs 2-128.

## 4. Data Elements Essenciais

| DE | Nome | Tipo | Tamanho | Uso |
|----|------|------|---------|-----|
| 2  | PAN | N..19 (LLVAR) | Variável | Número do cartão |
| 3  | Processing Code | N 6 | Fixo | Tipo de transação |
| 4  | Amount | N 12 | Fixo | Valor em centavos |
| 7  | Transmission Date/Time | N 10 | Fixo | MMDDHHmmss |
| 11 | STAN | N 6 | Fixo | Trace para correlação |
| 22 | POS Entry Mode | N 3 | Fixo | Como cartão foi lido |
| 38 | Auth ID | AN 6 | Fixo | Código do emissor se aprovado |
| 39 | Response Code | AN 2 | Fixo | 00=OK, 05=Deny, 51=No funds |
| 41 | Terminal ID | ANS 8 | Fixo | Identificador do terminal |
| 42 | Merchant ID | ANS 15 | Fixo | Identificador do merchant |
| 49 | Currency Code | N 3 | Fixo | 986=BRL |

---

# Semana 3 — Encoding e Formatos de Dados

## 1. ASCII vs BCD vs Binário

- **ASCII**: 1 byte por caractere. `"4532"` = 4 bytes. Legível.
- **BCD**: 2 dígitos por byte. `"4532"` = 2 bytes. Compacto.
- **Binário**: para bitmaps, PIN blocks, dados EMV.

## 2. LLVAR / LLLVAR

- **LLVAR**: prefixo de 2 dígitos indicando comprimento (máx 99)
- **LLLVAR**: prefixo de 3 dígitos (máx 999)
- O prefixo também tem encoding (ASCII ou BCD) — fonte clássica de bugs

## 3. Utilitários a Implementar

```java
HexUtils.bytesToHex(byte[])     // {0x4A} → "4A"
HexUtils.hexToBytes(String)     // "4A3B" → {0x4A, 0x3B}
BcdUtils.stringToBcd(String)    // "4532" → {0x45, 0x32}
BcdUtils.bcdToString(byte[])    // {0x45, 0x32} → "4532"
```

---

# Semana 4 — MTIs e Lifecycle de Mensagens

## 1. Ciclo de Vida Completo

```
Compra normal:       0200 → 0210 (auth)
Compra com clearing: 0100 → 0110 (auth) + 0220 → 0230 (capture advice)
Reversal:            0400 → 0410
Network management:  0800 → 0810 (echo, sign-on, key exchange)
```

## 2. Request vs Advice

- **Request**: preciso da decisão do outro lado (autorização, reversal)
- **Advice**: estou informando o outro lado (captura, offline, SAF)

## 3. Retransmissão

- **0200** (request) vs **0201** (repeat) — mesmo conteúdo, último dígito = 1
- Host deve tratar repeat como idempotente (mesmo resultado que o original)
- Após timeout + retransmissão, terminal pode enviar 0400 (reversal) preventivo

---

## Checklist de Conclusão da Fase 1

Ao terminar esta fase, você deve:

- [ ] Explicar a jornada completa de uma transação (POS → emissor → POS)
- [ ] Diferenciar on-us e off-us com exemplos práticos
- [ ] Decompor qualquer MTI nos 4 dígitos (versão/classe/função/origem)
- [ ] Ler um bitmap hex e listar os DEs presentes
- [ ] Saber a diferença entre ASCII, BCD e binário no contexto ISO 8583
- [ ] Implementar HexUtils e BcdUtils com testes
- [ ] Montar manualmente uma mensagem 0200 e decodificar uma resposta 0210
- [ ] Saber quando usar request vs advice vs reversal
