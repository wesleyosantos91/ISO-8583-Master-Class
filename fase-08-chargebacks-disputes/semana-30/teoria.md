# Semana 30 — Representment, Prevenção e Operação de Disputes

## 1. Representment — A Arte da Defesa

**Representment** é o processo de re-apresentar uma transação ao emissor com evidências que provam que a transação foi legítima. É a principal ferramenta do adquirente/merchant para reverter um chargeback.

### 1.1 Quando representar?

Não represente todo chargeback automaticamente. Avalie:

```
┌─────────────────────────────────────────────────────────────┐
│            DECISÃO DE REPRESENTMENT                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Tenho evidências sólidas?    ──► Não ──► Aceitar CB        │
│         │                                                   │
│        Sim                                                  │
│         │                                                   │
│  Valor > custo de defesa?     ──► Não ──► Aceitar CB        │
│  (considere tempo + taxa)                                   │
│         │                                                   │
│        Sim                                                  │
│         │                                                   │
│  Reason code defensável?      ──► Não ──► Aceitar CB        │
│  (10.4 sem 3DS = difícil)                                   │
│         │                                                   │
│        Sim                                                  │
│         │                                                   │
│         ▼                                                   │
│     REPRESENTAR                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Documentação por Reason Code

| Reason Code | Documentação ideal |
|-------------|-------------------|
| 10.4 (CNP Fraud) | 3DS authentication data, device fingerprint, IP address, histórico do cliente |
| 13.1 (Não recebido) | Proof of delivery com assinatura, tracking confirmado, comunicação com cliente |
| 13.2 (Recorrência cancelada) | Termos de serviço assinados, prova de que o cancelamento NÃO foi solicitado antes da cobrança |
| 12.6 (Duplicata) | Prova de que são transações distintas (timestamps, valores, locais diferentes) |
| 13.3 (Produto diferente) | Fotos do produto enviado, descrição no site no momento da compra |
| 13.6 (Crédito não processado) | Comprovante do crédito processado com data e valor |

### 1.3 Evidências que vencem chargebacks de fraude CNP

A 3DS autenticação é a arma mais forte:

```
Sem 3DS:
  Portador contesta → Merchant perde (quase sempre)
  Liability: Adquirente/Merchant

Com 3DS Authenticated (ECI 05/02):
  Portador contesta → Merchant ganha (quase sempre)
  Liability: Emissor (fez a autenticação, responsabilidade é dele)

Com 3DS Attempted (ECI 06/01):
  Portador contesta → Merchant tem argumento, mas não é garantia
  Liability: compartilhada / depende do reason code
```

---

## 2. Pre-Arbitration e Arbitration

### 2.1 Pre-Arbitration

Se o emissor **rejeitar o representment**, o adquirente pode escalar para **Pre-Arbitration**:

- Última chance de resolução entre as partes
- Adquirente reenvia as evidências com documentação adicional
- Se o emissor aceitar: chargeback é revertido
- Se o emissor recusar: vai para Arbitration

**Custo de Pre-Arbitration:**
- Visa: USD 500 (cobrado de quem perder)
- Mastercard: USD 250 (cobrado de quem perder)

Faça o cálculo: só vale ir a pre-arb se o valor da transação + probabilidade de ganho superar o custo.

### 2.2 Arbitration

A **bandeira** decide. É o processo final e irrecorrível dentro do ecossistema de cartões:

```
Bandeira analisa:
  - Evidências do adquirente
  - Evidências do emissor
  - Compliance com as regras da bandeira

Possíveis resultados:
  - Adquirente ganha: chargeback revertido + emissor paga a fee
  - Emissor ganha: chargeback mantido + adquirente paga a fee
  - Split: raro, possível em casos específicos
```

**Custo de Arbitration:**
- Visa: USD 500 (+ USD 250 compliance fee se houver violação)
- Mastercard: USD 250

A maioria dos cases de arbitration é perdida pelo adquirente porque as evidências foram insuficientes nas etapas anteriores.

---

## 3. Ciclo Financeiro do Chargeback

```
COMPRA (D+0):
  Portador:    -R$ 0 (limite reservado)
  Merchant:    +R$ 0 (aguarda clearing)
  Adquirente:  neutro
  Emissor:     reserva limite

CLEARING (D+1):
  Merchant:    +R$ 970 (após MDR 3%)
  Adquirente:  +R$ 30 (MDR retido)
  Emissor:     -R$ 1.000 (débito no portador futuro)
  Bandeira:    +R$ 2 (assessment fee)

CHARGEBACK (D+X):
  Emissor:     +R$ 1.000 (debita do adquirente)
  Adquirente:  -R$ 1.000 (débito forçado)
  Adquirente:  -R$ 970 do merchant reserve (recupera do merchant)
  Merchant:    -R$ 970 (perde o que recebeu)

SE REPRESENTMENT GANHO:
  Adquirente:  +R$ 1.000 (revertido)
  Merchant:    +R$ 970 (devolvido)

CUSTO FINAL SEM REPRESENTMENT:
  Merchant:    perde R$ 970 + produto/serviço fornecido
  Adquirente:  lucro zero (MDR vai junto)
  Bandeira:    taxa de chargeback pode ser cobrada adicionalmente
```

---

## 4. Ferramentas de Prevenção

### 4.1 3D Secure (3DS)

O 3DS é o protocolo de autenticação para e-commerce que transfere a liability de fraude para o emissor.

**Versões:**
- **3DS 1.0:** Redirecionamento para página do banco (UX ruim, muito abandono)
- **3DS 2.0 (EMV 3DS):** Frictionless ou Step-Up baseado em risk score

**Fluxo 3DS 2.0:**
```mermaid
sequenceDiagram
    participant MER as Merchant
    participant ACS as ACS (Emissor)
    participant NET as Bandeira 3DS Server
    participant ISS as Emissor

    MER->>NET: Authentication Request
    NET->>ISS: Device data + transaction data
    ISS->>ISS: Risk score

    alt Frictionless (baixo risco)
        ISS-->>NET: Authentication Value (CAVV)
        NET-->>MER: ECI 05 - Authenticated
        MER->>MER: Processa autorização normalmente
    else Step-Up (alto risco)
        ISS-->>MER: Challenge Required
        MER->>MER: Exibe challenge (OTP, biometria)
        MER->>ISS: Challenge response
        ISS-->>NET: CAVV
        NET-->>MER: ECI 05 - Authenticated
    end
```

**ECI Values:**
| ECI | Significado | Liability |
|-----|-------------|-----------|
| 05 (Visa) / 02 (Master) | Fully authenticated | Emissor |
| 06 (Visa) / 01 (Master) | Attempted, emissor não suporta | Emissor (maioria) |
| 07 (Visa) / 00 (Master) | Not authenticated / Not attempted | Merchant |

### 4.2 CVV2 / CVC2

- Obrigatório para CNP
- Não pode ser armazenado após autorização (PCI DSS)
- DE 48 carrega o resultado da validação (não o código em si)

```
CVV2 Match Codes (DE 48, subelemento específico por bandeira):
  M = Match      → proceder
  N = No Match   → declinar (forte indicador de fraude)
  P = Not Processed → emissor não validou
  S = Should be on card → emissor afirma que não há CVV2
  U = Issuer not certified → emissor não participa
```

### 4.3 Address Verification Service (AVS)

Verificação de endereço de cobrança (usado principalmente nos EUA, mas disponível no Brasil):

```
AVS Response Codes (DE 44 / campo proprietário):
  Y = Address + ZIP match
  A = Address match, ZIP no match
  Z = ZIP match, address no match
  N = Neither match
  U = Unavailable
```

### 4.4 Velocity Rules

Regras de velocidade que o switch/antifraude deve implementar:

```java
public class VelocityRules {

    // Armazena histórico com TTL implícito via timestamp
    private final Map<String, Deque<Instant>> cardAttempts    = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>    deviceCards     = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> ipTransactions  = new ConcurrentHashMap<>();
    private final Map<String, Instant>        recentTxns      = new ConcurrentHashMap<>();

    /**
     * Mesma cartão: máximo N tentativas em windowMinutes.
     * @return true se dentro do limite (OK para prosseguir), false se excedeu (bloquear)
     */
    public boolean checkCardVelocity(String maskedPan, int windowMinutes, int maxAttempts) {
        Instant cutoff = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        Deque<Instant> attempts = cardAttempts.computeIfAbsent(maskedPan,
                                                                k -> new ArrayDeque<>());
        synchronized (attempts) {
            // Remove entradas fora da janela
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.pollFirst();
            }
            attempts.addLast(Instant.now());
            return attempts.size() <= maxAttempts;
        }
    }

    /**
     * Mesmo dispositivo: máximo maxCards cartões distintos em windowHours.
     * Padrão de fraude: testar múltiplos cartões no mesmo terminal/app.
     */
    public boolean checkDeviceVelocity(String deviceId, int windowHours, int maxCards) {
        // Simplificação: mantém conjunto de PANs únicos por dispositivo
        // Em produção: usar TTL via Redis ZSET (scored set com timestamp)
        Set<String> cards = deviceCards.computeIfAbsent(deviceId,
                                                         k -> ConcurrentHashMap.newKeySet());
        // Para este exemplo, não expiramos as entradas (produção deve usar TTL)
        return cards.size() < maxCards;
    }

    /** Registra um cartão usado por um dispositivo */
    public void recordDeviceCard(String deviceId, String maskedPan) {
        deviceCards.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet())
                   .add(maskedPan);
    }

    /**
     * Mesmo IP: máximo maxTxns transações em windowMinutes.
     */
    public boolean checkIpVelocity(String ip, int windowMinutes, int maxTxns) {
        Instant cutoff = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        Deque<Instant> txns = ipTransactions.computeIfAbsent(ip,
                                                              k -> new ArrayDeque<>());
        synchronized (txns) {
            while (!txns.isEmpty() && txns.peekFirst().isBefore(cutoff)) {
                txns.pollFirst();
            }
            txns.addLast(Instant.now());
            return txns.size() <= maxTxns;
        }
    }

    /**
     * Mesmo PAN + merchant + valor: máximo 1 transação em windowMinutes.
     * Detecta reenvio acidental do terminal ou duplo clique no botão de pagamento.
     */
    public boolean checkDuplicateTransaction(String maskedPan, String merchantId,
                                              BigDecimal amount, int windowMinutes) {
        String key = maskedPan + "|" + merchantId + "|" + amount.toPlainString();
        Instant cutoff = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        Instant prev = recentTxns.get(key);

        if (prev != null && prev.isAfter(cutoff)) {
            return false; // Duplicata detectada — bloquear
        }
        recentTxns.put(key, Instant.now());
        return true; // OK
    }
}
```

**Exemplo de uso no TransactionParticipant:**

```java
public class FraudVelocityGuard implements TransactionParticipant {

    private final VelocityRules velocity = new VelocityRules();

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");

        String pan        = PANMasker.mask(msg.getString(2));
        String ip         = ctx.get("CLIENT_IP");      // enriquecido pelo acquirer
        String deviceId   = ctx.get("DEVICE_ID");      // para CNP / mobile
        BigDecimal amount = new BigDecimal(msg.getString(4)).movePointLeft(2);
        String merchantId = msg.getString(42);

        // 1. Velocidade de cartão: máx 5 tentativas em 10 minutos
        if (!velocity.checkCardVelocity(pan, 10, 5)) {
            ctx.put("RESPONSE_CODE", "57");
            ctx.put("FRAUD_REASON", "CARD_VELOCITY_EXCEEDED");
            return ABORTED;
        }

        // 2. IP: máx 15 transações em 60 minutos
        if (ip != null && !velocity.checkIpVelocity(ip, 60, 15)) {
            ctx.put("RESPONSE_CODE", "57");
            ctx.put("FRAUD_REASON", "IP_VELOCITY_EXCEEDED");
            return ABORTED;
        }

        // 3. Duplicata exata: mesma combinação em 5 minutos
        if (!velocity.checkDuplicateTransaction(pan, merchantId, amount, 5)) {
            ctx.put("RESPONSE_CODE", "94"); // Duplicate Transmission
            ctx.put("FRAUD_REASON", "DUPLICATE_TRANSACTION");
            return ABORTED;
        }

        // 4. Registra cartão por dispositivo (para análise assíncrona)
        if (deviceId != null) {
            velocity.recordDeviceCard(deviceId, pan);
            if (!velocity.checkDeviceVelocity(deviceId, 24, 5)) {
                ctx.put("RESPONSE_CODE", "57");
                ctx.put("FRAUD_REASON", "DEVICE_CARD_VELOCITY");
                return ABORTED;
            }
        }

        return PREPARED;
    }
}
```

### 4.5 MATCH (Master Alert to Control High-risk Merchants)

Lista negra da Mastercard de merchants descredenciados. Antes de credenciar um novo merchant, o adquirente **deve** consultar o MATCH:

- Merchant pode estar listado por excesso de chargebacks, fraude, ou violação de regras
- Adquirente que credencia merchant listado no MATCH pode ser multado
- Visa tem equivalente chamado VMAS (Visa Merchant Alert Service)

---

## 5. Operação de Disputes em Escala

### 5.1 Automação do processo

Para um adquirente com alto volume, o processo manual é inviável:

```java
public class DisputeAutomationEngine {

    public DisputeDecision evaluate(Chargeback cb) {

        // 1. Buscar transação original
        Transaction original = txnRepository.findByRrn(cb.getOriginalRrn());

        // 2. Verificar se vale defender (valor mínimo)
        if (original.getAmount().compareTo(MIN_DEFENSE_THRESHOLD) < 0) {
            return DisputeDecision.ACCEPT; // Não vale o custo operacional
        }

        // 3. Verificar reason code
        return switch (cb.getReasonCode()) {
            case "10.4" -> evaluate3dsEligibility(original, cb);
            case "12.6" -> evaluateDuplicateEvidence(original, cb);
            case "13.1" -> evaluateDeliveryEvidence(original, cb);
            case "13.2" -> evaluateRecurringCancellation(original, cb);
            default -> DisputeDecision.MANUAL_REVIEW;
        };
    }

    private DisputeDecision evaluate3dsEligibility(Transaction tx, Chargeback cb) {
        if (tx.getEci() != null && (tx.getEci().equals("05") || tx.getEci().equals("02"))) {
            return DisputeDecision.REPRESENT_AUTO; // 3DS autenticado = defesa forte
        }
        return DisputeDecision.ACCEPT; // Sem 3DS = sem defesa em CNP fraud
    }
}
```

### 5.2 SLAs operacionais

| Etapa | SLA interno | SLA bandeira |
|-------|-------------|-------------|
| Merchant recebe notificação | 24h após CB | N/A |
| Merchant envia evidências | 5 dias úteis | N/A |
| Adquirente envia representment | 7 dias úteis | 30 dias |
| Decisão de aceitar ou pre-arb | 5 dias úteis | 10 dias após resultado |

### 5.3 Dashboard de disputes

Métricas que o time de operações deve monitorar diariamente:

```
KPIs de Chargeback:
  - CB ratio por merchant (alert: > 0.65%)
  - CB ratio total (alert: > 0.5%)
  - Chargebacks por reason code (tendência)
  - Win rate do representment (meta: > 50%)
  - Valor em risco (CBs recebidos nos últimos 30 dias)
  - CBs sem resposta (perto do prazo)
  - Merchants no VDMP/ECP

Alarmes automáticos:
  - Merchant com > 10 CBs em 24h → investigar
  - CB ratio semanal > 1% → notificar gerência
  - Representment vencendo prazo em 48h → escalação
```

---

## 6. Chargebacks no Mercado Brasileiro

### 6.1 Particularidades da Elo

A Elo tem processo próprio de disputa chamado **CELO (Central Elo)** com algumas diferenças:

- Prazos ligeiramente diferentes de Visa/Master
- Reason codes próprios mas mapeáveis para Visa/Master
- Merchant pode ter processo simplificado de defesa para transações com senha digitada

### 6.2 BACEN e Regulação de Disputas

Desde 2020, o BACEN exige que emissores tenham processo de tratamento de reclamações de clientes com SLA definido. Isso impacta:

- Prazo máximo de crédito provisório ao portador: 10 dias úteis
- Notificação obrigatória ao portador sobre resultado
- Registro de reclamações no BACEN (ROCA)

### 6.3 Chargeback vs Contestação de Débito (Pix)

| Aspecto | Chargeback Cartão | MED (Pix) |
|---------|-------------------|-----------|
| Iniciador | Portador via emissor | Pagador via banco |
| Prazo para contestar | 60-120 dias | 80 dias |
| Reversibilidade | Processo estruturado | Depende do recebedor aceitar |
| Responsabilidade final | Merchant/adquirente | Mais complexo |
| Proteção ao consumidor | Mais robusta | Menor |

---

## 7. Chargeback e a Arquitetura do Switch

### 7.1 O que o switch deve garantir

```
1. RASTREABILIDADE COMPLETA
   - Cada transação deve ter RRN único e imutável
   - Todos os campos relevantes persistidos por 13+ meses
   - Logs append-only com timestamp imutável

2. SUPORTE A RETRIEVAL REQUEST (1644)
   - Switch deve responder a pedidos de documentação
   - Retornar dados originais da transação em até 15 dias

3. PARTIAL AMOUNTS
   - Suportar chargebacks de valor parcial (DE 4 < valor original)

4. LIFECYCLE TRACKING
   - Rastrear status: authorized → cleared → chargeback → representment → final

5. DUPLICATE DETECTION
   - Engine de deduplicação previne chargebacks 12.6
   - Janela de deduplicação: mínimo 24h para mesma transação
```

### 7.2 Modelo de dados para suportar disputes

```java
// Extensão do TransactionRecord para suporte a chargebacks
public class TransactionRecord {
    // Dados originais (imutáveis após criação)
    String rrn;                    // DE 37 — chave de rastreio
    String stan;                   // DE 11
    String authorizationCode;      // DE 38
    String maskedPan;              // PAN mascarado 6x4
    String mti;
    String responseCode;           // DE 39
    LocalDateTime txnDateTime;
    BigDecimal amount;
    String currency;               // DE 49
    String merchantId;             // DE 42
    String terminalId;             // DE 41
    String merchantName;           // DE 43
    String processingCode;         // DE 3
    String posEntryMode;           // DE 22
    byte[] emvData;                // DE 55 (completo)
    String eci;                    // E-commerce indicator
    String authenticationData;     // CAVV/3DS data
    boolean threedsAuthenticated;

    // Lifecycle (mutável)
    TransactionStatus status;      // AUTHORIZED, CAPTURED, REVERSED, CHARGEBACK
    LocalDateTime clearingDate;
    String chargebackRrn;          // RRN do chargeback, se ocorrer
    String chargebackReasonCode;
    DisputeStatus disputeStatus;   // RECEIVED, REPRESENTED, WON, LOST
    LocalDateTime disputeDeadline; // Prazo para representment
}
```

---

## Resumo da Fase 8

Você agora domina o ciclo completo de chargebacks e disputes:

| Conceito | Domínio esperado |
|----------|-----------------|
| Tipos de contestação | Distinguir reversal, refund e chargeback claramente |
| Reason codes | Identificar os 15+ reason codes mais comuns e suas implicações |
| Ciclo de vida | Navegar o processo do chargeback ao arbitration |
| Representment | Decidir quando e como defender com evidências corretas |
| 3DS e liability shift | Entender como 3DS muda a responsabilidade |
| Prevenção | CVV2, AVS, velocity rules, MATCH |
| Operação em escala | Automação, SLAs, dashboards |
| Brasil | Elo, BACEN, diferenças regulatórias |
| Arquitetura | O que o switch precisa garantir para suportar disputes |
