# Fase 3 — Autorização E2E (Semanas 9-12)

---

# Semana 9 — Campos Críticos da Autorização

## 1. Os 19 campos que você DEVE dominar

Não decore — **entenda o papel de cada um em negócio, risco e troubleshooting.**

### DE 2 — PAN (Primary Account Number)
- **O que é:** Número do cartão (13 a 19 dígitos)
- **Formato:** n ..19 LLVAR
- **Por que importa:** É a identidade da conta. Nunca logar completo (PCI). Mascarar: `4532****0366`
- **Troubleshooting:** PAN inválido → DE39=14. BIN não reconhecido → roteamento errado.
- **Validação:** Algoritmo de Luhn (dígito verificador)

```java
public static boolean luhnCheck(String pan) {
    int sum = 0;
    boolean alternate = false;
    for (int i = pan.length() - 1; i >= 0; i--) {
        int n = Character.getNumericValue(pan.charAt(i));
        if (alternate) {
            n *= 2;
            if (n > 9) n -= 9;
        }
        sum += n;
        alternate = !alternate;
    }
    return sum % 10 == 0;
}
```

### DE 3 — Processing Code
- **O que é:** Tipo de transação + conta origem + conta destino (6 dígitos)
- **Formato:** n 6 fixo
- **Estrutura:** `[TT][FF][TT]`
- **Valores que você vai ver em produção:**

```
003000 = Compra crédito à vista
003010 = Compra crédito (poupança? — depende da implementação)
012000 = Saque conta corrente
200030 = Estorno/refund crédito
300000 = Consulta de saldo
```

### DE 4 — Amount, Transaction
- **O que é:** Valor em centavos, 12 dígitos, pad zero à esquerda
- **Formato:** n 12 fixo
- **Exemplo:** R$ 150,00 → `000000015000`
- **Armadilha:** Sempre em centavos! R$ 1,50 ≠ R$ 150,00. Já vi incidente por confundir.

### DE 7 — Transmission Date and Time
- **Formato:** n 10 fixo (MMDDhhmmss)
- **Uso:** Timestamp da transmissão. Parte da chave de correlação em algumas implementações.

### DE 11 — STAN (Systems Trace Audit Number)
- **Formato:** n 6 fixo (000001 a 999999)
- **Uso:** Identificador da transação dentro de uma sessão/terminal
- **Crítico:** STAN é a principal chave de correlação. Deve ser único na janela de timeout.
- **Rollover:** Com 6 dígitos, volta ao 000001 após 999999. Em alto volume, pode reutilizar rápido.

### DE 22 — POS Entry Mode
- **Formato:** n 3 fixo `[PP][C]`
- **PP = PAN Entry Mode:**

```
01 = Manual (digitado)
02 = Tarja magnética
05 = Chip contact (inserido)
07 = Contactless chip (NFC)
10 = Credential on File
80 = Fallback (chip falhou, usou tarja)
81 = E-commerce
82 = E-commerce com 3DS
91 = Contactless tarja (MSD)
```

- **C = PIN Capability:** 1 = terminal aceita PIN, 2 = não aceita

**Por que importa pra fraude:** DE22=051 (chip+PIN) é muito mais seguro que DE22=012 (tarja sem PIN). Regras de risco e interchange mudam baseado neste campo.

### DE 25 — POS Condition Code
- **Formato:** n 2 fixo

```
00 = Normal presentation (compra padrão)
06 = Pre-authorization
08 = Mail/telephone order
59 = E-commerce
```

### DE 32 — Acquiring Institution ID
- **Formato:** n ..11 LLVAR
- **Uso:** Identifica o adquirente. Usado no roteamento de volta (response) e no clearing.

### DE 37 — Retrieval Reference Number (RRN)
- **Formato:** an 12 fixo
- **Uso:** Referência única para rastreio. Merchant usa para consultar status.
- **Diferença do STAN:** STAN é operacional (correlação request/response). RRN é para rastreio de negócio.

### DE 38 — Authorization Identification Response
- **Formato:** an 6 fixo
- **Uso:** Código de autorização gerado pelo emissor. Impresso no comprovante.
- **Só existe na response** (0110/0210).

### DE 39 — Response Code ★
- **Formato:** an 2 fixo
- **O campo mais importante da response.** Diz se aprovou, negou, e por quê.

```
00 = Approved
01 = Refer to card issuer
03 = Invalid merchant
05 = Do not honor (genérico)
12 = Invalid transaction
13 = Invalid amount
14 = Invalid card number (Luhn fail)
30 = Format error ← PROBLEMA NO SEU SISTEMA
41 = Lost card (reter)
43 = Stolen card (reter)
51 = Insufficient funds
54 = Expired card
55 = Incorrect PIN
57 = Transaction not permitted to cardholder
58 = Transaction not permitted to terminal
59 = Suspected fraud
61 = Exceeds withdrawal limit
65 = Exceeds withdrawal frequency limit
68 = Response received too late ← TIMEOUT
75 = Allowable PIN tries exceeded
76 = Unable to locate previous message ← REVERSAL de transação não encontrada
91 = Issuer unavailable ← EMISSOR FORA
96 = System malfunction
```

### DE 41 — Card Acceptor Terminal Identification
- **Formato:** ans 8 fixo
- **Uso:** Identifica o terminal (POS/ATM). Parte da chave de correlação.

### DE 42 — Card Acceptor Identification Code
- **Formato:** ans 15 fixo
- **Uso:** Identifica o merchant (establishment). Aparece na fatura.

### DE 49 — Currency Code, Transaction
- **Formato:** n 3 fixo
- **Valores:** 986 = BRL, 840 = USD, 978 = EUR
- **ISO 4217:** Código numérico da moeda

### DE 52 — PIN Data
- **Formato:** b 8 fixo (64 bits)
- **Uso:** PIN Block criptografado (nunca em claro!)
- **Presente apenas se:** terminal capturou PIN e transação exige

### DE 55 — ICC System Related Data (EMV)
- **Formato:** b ..999 LLLVAR
- **Uso:** Dados do chip no formato TLV (Tag-Length-Value)
- **Presente se:** DE22 indica chip (05, 07)

---

## 2. Exercícios Semana 9

### Exercício 1 — Flash cards
Crie flash cards (digital ou papel) com os 19 campos. Frente: número e nome. Verso: formato, uso, e um exemplo de valor.

### Exercício 2 — Dado um dump, extraia significado
```
MTI=0200 DE2=4532015112830366 DE3=003000 DE4=000000150000 
DE7=0314160000 DE11=000042 DE22=071 DE25=00 DE39= 
DE41=TERM0001 DE42=MERCHANT00001   DE49=986 DE55=[TLV data]
```

Responda:
1. Que tipo de transação é?
2. Qual o valor?
3. Como o cartão foi lido?
4. Tem PIN?
5. É e-commerce?
6. Esse é request ou response? Como sabe?

### Exercício 3 — Validador de mensagem
Implemente `MessageValidator.java` que recebe `ISOMsg` e retorna lista de erros:
- PAN com Luhn inválido
- Amount zero ou negativo
- Processing Code desconhecido
- POS Entry Mode inválido
- Campos obrigatórios ausentes por MTI

### Desafio — Análise de incidente
O time de operações reporta: "Taxa de decline subiu de 3% para 25% nos últimos 30 minutos. Apenas terminais novos (TERM0100 a TERM0150)."

Investigue dado que o dump mostra:
```
DE22=012  (tarja magnética, sem PIN)
DE55=[presente com dados EMV]
```

O que está errado? Qual o impacto? Como corrigir?

---

# Semana 10 — 0200/0210 Ponta a Ponta

## 1. Fluxo completo implementado

```java
// Participant: ForwardToIssuer
public class ForwardToIssuer implements TransactionParticipant {
    
    private QMUX mux; // injetado via configuração
    private long timeout = 30000;

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get("REQUEST");
        String destination = ctx.get("DESTINATION_MUX");
        
        try {
            // Converte 0200 → 0100 se necessário (adquirente → bandeira)
            ISOMsg authRequest = convertToAuthRequest(request);
            
            // Envia via QMUX e aguarda response
            ISOMsg response = mux.request(authRequest, timeout);
            
            if (response == null) {
                // TIMEOUT — nenhuma resposta
                ctx.put("TIMEOUT", true);
                ctx.put("RESPONSE_CODE", "68");
                ctx.put("NEEDS_REVERSAL", true);
                return ABORTED; // Vai pro abort, que gera reversal
            }
            
            ctx.put("RESPONSE", response);
            ctx.put("RESPONSE_CODE", response.getString(39));
            return PREPARED;
            
        } catch (Exception e) {
            ctx.put("RESPONSE_CODE", "96");
            return ABORTED;
        }
    }
}
```

## 2. Regras de aprovação (no emissor simulado)

```java
public class IssuerSimulator implements ISORequestListener {
    
    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        
        String pan = request.getString(2);
        long amount = Long.parseLong(request.getString(4));
        
        // Regras de decisão
        String responseCode = decide(pan, amount, request);
        response.set(39, responseCode);
        
        if ("00".equals(responseCode)) {
            response.set(38, generateAuthCode()); // Authorization ID
        }
        
        source.send(response);
        return true;
    }
    
    private String decide(String pan, long amount, ISOMsg msg) {
        // Simula regras reais
        if (pan.startsWith("400000000000")) return "14"; // PAN inválido
        if (pan.startsWith("410000000000")) return "51"; // Sem saldo
        if (pan.startsWith("420000000000")) return "54"; // Expirado
        if (pan.startsWith("430000000000")) return "55"; // PIN errado
        if (amount > 1000000) return "61";                // Acima do limite
        if (isBlocked(pan)) return "05";                   // Do not honor
        return "00"; // Aprovado
    }
}
```

## 3. Exercícios Semana 10

1. **Implemente o fluxo completo** 0200 → processamento → 0210
2. **Crie o IssuerSimulator** com pelo menos 8 regras de decline
3. **Teste cada response code** individualmente
4. **Teste o happy path** end-to-end: terminal → switch → issuer → switch → terminal

### Desafio
Implemente métricas que mostrem em tempo real:
- Taxa de aprovação por minuto
- Top 5 response codes
- Latência P50, P95, P99
- Volume de transações por terminal

---

# Semana 11 — On-Us/Off-Us + Roteamento + Parcelamento

## 1. Routing Engine

```java
public class RouteByBIN implements TransactionParticipant {

    private final BINTable binTable;

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");
        String pan = msg.getString(2);
        
        // 8-digit BIN (padrão atual)
        String bin = pan.substring(0, Math.min(8, pan.length()));
        
        Route route = binTable.lookup(bin);
        if (route == null) {
            // Tenta fallback com 6-digit BIN
            route = binTable.lookup(pan.substring(0, 6));
        }
        if (route == null) {
            ctx.put("RESPONSE_CODE", "92"); // Routing error
            return ABORTED;
        }
        
        ctx.put("ROUTE", route);
        ctx.put("IS_ON_US", route.isOnUs());
        ctx.put("DESTINATION_MUX", route.getMuxName());
        ctx.put("NETWORK", route.getNetwork()); // VISA, MASTERCARD, ELO
        
        return PREPARED;
    }
}
```

## 2. Tabela de BINs

```java
public class BINTable {
    // bin_prefix → Route
    private final TreeMap<String, Route> routes = new TreeMap<>();
    
    public void addRoute(String binPrefix, Route route) {
        routes.put(binPrefix, route);
    }
    
    public Route lookup(String bin) {
        // Longest prefix match
        Map.Entry<String, Route> entry = routes.floorEntry(bin);
        if (entry != null && bin.startsWith(entry.getKey())) {
            return entry.getValue();
        }
        return null;
    }
}

public record Route(
    String binPrefix,
    String issuerName,
    String network,        // VISA, MASTERCARD, ELO
    boolean onUs,
    String muxName,        // nome do QMUX destino
    String fallbackMux     // QMUX alternativo
) {}
```

## 3. Parcelamento na mensagem

```java
// Campos privados para parcelamento (varia por adquirente/bandeira)
// Exemplo típico usando DE 48 ou DE 60

public class InstallmentData {
    private int numberOfInstallments;  // 2-99
    private InstallmentType type;      // LOJISTA, EMISSOR
    
    // Serializa para campo privado
    public String toDE48() {
        // Formato típico: "03" + tipo + quantidade
        return String.format("03%s%02d",
            type == InstallmentType.LOJISTA ? "1" : "2",
            numberOfInstallments);
    }
}

// No ISOMsg:
msg.set(48, installmentData.toDE48());  // Adiciona info de parcelas
```

## 4. Exercícios Semana 11

1. **Implemente BINTable** com pelo menos 20 BINs (5 on-us, 15 off-us, 3 bandeiras)
2. **Implemente RouteByBIN participant** com fallback 8→6 dígitos
3. **Teste roteamento**: on-us vai para mux local, off-us vai para mux da bandeira
4. **Implemente parcelamento**: 3x, 6x, 12x nos campos privados
5. **Teste: BIN não encontrado** → response code 92

### Desafio
Simule o cenário de BIN expansion: um cartão que antes tinha BIN 6 dígitos `453201` agora precisa ser roteado com 8 dígitos `45320151`. Demonstre que a tabela antiga (6 dígitos) roteia para o destino ERRADO e a tabela nova (8 dígitos) roteia corretamente.

---

# Semana 12 — Timeout, Stand-in e Resiliência

## 1. Estratégia de Timeout

```
SLAs de timeout por tipo:
  Authorization: 30 segundos (configurável)
  Reversal: 45 segundos (mais tolerante — precisa da confirmação)
  Network Management: 15 segundos (rápido)
  
Timeout no QMUX → retorna null
Ação: auto-reversal + response com DE39=68
```

## 2. Stand-In Processing (STIP)

Quando o emissor está indisponível, a **bandeira decide** (stand-in):
- Visa e Mastercard mantêm parâmetros do emissor
- Se o emissor não responde, a bandeira pode aprovar/negar baseada em regras pré-definidas
- O emissor é avisado depois (via advice)

**Seu switch deve saber diferenciar:**
- Resposta direta do emissor (DE39 vem do emissor)
- Resposta de stand-in (DE39 vem da bandeira)

## 3. Exercícios Semana 12

1. **Implemente auto-reversal por timeout** no switch
2. **Simule emissor lento** (responde em 35s com timeout de 30s)
3. **Simule emissor fora** (não responde nunca)
4. **Documente `timeout-strategy.md`** com SLAs, decisões e rationale

### Desafio
Monte um cenário de stress: envie 100 transações simultâneas para o switch. O emissor simulado responde:
- 70% em < 200ms (normal)
- 20% em 5-10s (lento)
- 10% nunca responde (timeout)

Analise: quantas aprovadas? Quantas com timeout? Quantos reversals gerados? Qual a latência P50/P95/P99?
