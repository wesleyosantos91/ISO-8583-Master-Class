# Fase 2 — jPOS Real (Semanas 5-8)

---

# Semana 5 — Setup jPOS + Q2 Runtime

## 1. O que é jPOS?

jPOS é o framework Java open-source mais usado no mundo para construir switches de pagamento ISO 8583. Bancos, processadoras e adquirentes no Brasil e globalmente usam jPOS como base dos seus switches.

**Não é uma library — é um runtime.** O jPOS tem seu próprio lifecycle (Q2), sistema de deploy por XML, e padrões arquiteturais específicos.

## 2. Estrutura do projeto jPOS

```
payment-switch-lab/
├── src/main/java/
│   └── com/lab/
│       ├── participants/       ← TransactionManager participants
│       ├── channel/            ← Custom channels se necessário
│       └── util/               ← Utilitários
├── src/main/resources/
│   └── cfg/                    ← Packager XML
├── src/dist/
│   └── deploy/                 ← ★ Configurações Q2 (XML)
│       ├── 00_logger.xml       ← Logging
│       ├── 05_txnmgr.xml      ← TransactionManager
│       ├── 10_channel.xml      ← Canais TCP
│       ├── 20_mux.xml          ← QMUX (correlação)
│       └── 30_server.xml       ← QServer (recebe conexões)
├── src/test/java/              ← Testes
├── pom.xml                     ← Maven
└── README.md
```

## 3. Q2 — O Container do jPOS

Q2 é o container que gerencia o lifecycle dos componentes:
- Lê XMLs do diretório `deploy/`
- Instancia e inicia componentes na ordem do prefixo numérico
- Monitora mudanças em runtime (hot deploy)
- Gerencia shutdown graceful

```java
// Iniciar Q2
public class Application {
    public static void main(String[] args) throws Exception {
        Q2 q2 = new Q2(); // Lê deploy/ automaticamente
        q2.start();
    }
}
```

## 4. Maven Setup

```xml
<dependencies>
    <dependency>
        <groupId>org.jpos</groupId>
        <artifactId>jpos</artifactId>
        <version>2.1.9</version>
    </dependency>
    <dependency>
        <groupId>org.jpos.ee</groupId>
        <artifactId>jposee-txn</artifactId>
        <version>2.2.9</version>
    </dependency>
</dependencies>
```

## 5. Exercícios Semana 5

1. **Crie o repositório `payment-switch-lab`** com a estrutura acima
2. **Configure o Q2** para iniciar e imprimir log de startup
3. **Crie o `00_logger.xml`** com log rotativo por dia
4. **Documente no README:** como rodar, como o Q2 funciona, onde cada coisa fica

### Desafio
Suba o projeto com Docker (Dockerfile + docker-compose). O container deve iniciar o Q2 automaticamente. Bônus: health check que verifica se o Q2 subiu.

---

# Semana 6 — ISOMsg, Packager e Serialização

## 1. ISOMsg — O objeto central

`ISOMsg` é a representação em memória de uma mensagem ISO 8583. Toda operação passa por ele:

```java
ISOMsg msg = new ISOMsg();
msg.setMTI("0200");
msg.set(2, "4532015112830366");     // PAN
msg.set(3, "003000");                // Processing Code
msg.set(4, "000000015000");          // Amount (R$ 150,00)
msg.set(7, "0314143025");            // Transmission Date/Time
msg.set(11, "123456");               // STAN
msg.set(22, "051");                  // POS Entry Mode (chip)
msg.set(25, "00");                   // POS Condition Code
msg.set(41, "TERM0001");             // Terminal ID
msg.set(42, "MERCHANT00001  ");      // Merchant ID (15 chars, pad right)
msg.set(49, "986");                  // Currency (BRL)

// Acessar
String pan = msg.getString(2);
boolean hasPIN = msg.hasField(52);

// Serializar
byte[] packed = msg.pack();

// Desserializar
ISOMsg parsed = new ISOMsg();
parsed.setPackager(packager);
parsed.unpack(packed);
```

## 2. GenericPackager — Definindo a spec

O packager define como cada campo é serializado/desserializado:

```xml
<!-- cfg/iso87ascii.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE isopackager SYSTEM "genericpackager.dtd">
<isopackager>
    <isofield id="0"  length="4"   name="MTI"
              class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="1"  length="16"  name="Bitmap"
              class="org.jpos.iso.IFA_BITMAP"/>
    <isofield id="2"  length="19"  name="PAN"
              class="org.jpos.iso.IFA_LLNUM"/>
    <isofield id="3"  length="6"   name="Processing Code"
              class="org.jpos.iso.IFA_NUMERIC"/>
    <isofield id="4"  length="12"  name="Amount"
              class="org.jpos.iso.IFA_NUMERIC"/>
    <!-- ... demais campos ... -->
</isopackager>
```

**Classes de campo comuns:**

| Classe | Significado | Exemplo |
|--------|-------------|---------|
| `IFA_NUMERIC` | Numérico fixo, ASCII | DE 3, DE 4, DE 49 |
| `IFA_LLNUM` | Numérico LLVAR, ASCII | DE 2 (PAN) |
| `IFA_LLLCHAR` | Alfanumérico LLLVAR, ASCII | DE 48, DE 63 |
| `IFA_LLLBINARY` | Binário LLLVAR | DE 55 (EMV) |
| `IFA_ALPHA` | Alfabético fixo, ASCII | DE 37, DE 38 |
| `IFB_ALPHA` | Alfabético fixo, binary header | DE 41, DE 42 |
| `IFB_BINARY` | Binário fixo | DE 52 (PIN) |
| `IFA_BITMAP` | Bitmap ASCII | DE 1 |
| `IFB_BITMAP` | Bitmap binário | DE 1 (alternativo) |

## 3. Exercícios Semana 6

1. **Crie o packager XML completo** para os 19 campos mínimos do lab
2. **Implemente `MessageFactory`** que cria mensagens tipadas:
   ```java
   ISOMsg buildAuthRequest(String pan, long amount, String terminalId);
   ISOMsg buildAuthResponse(ISOMsg request, String responseCode, String authCode);
   ISOMsg buildEchoRequest();
   ISOMsg buildReversalRequest(ISOMsg originalAuth);
   ```
3. **Teste roundtrip:** pack → unpack → compare todos os campos
4. **Teste de validação:** mensagem sem campo obrigatório → exceção clara

### Desafio
Implemente dois packagers (ASCII e BCD) para o mesmo conjunto de campos. Mostre que a mesma `ISOMsg` gera bytes diferentes dependendo do packager. Implemente um teste que serializa com packager A e desserializa com packager B → deve falhar. Isso simula um problema real de integração.

---

# Semana 7 — TransactionManager + Participants ★

**Esta é a semana mais importante da fase.** O TransactionManager é o coração de um switch jPOS real.

## 1. O que é o TransactionManager?

É um orquestrador que processa transações através de uma **pipeline de participants**. Cada participant executa uma responsabilidade isolada:

```
Request chega
    │
    ▼
┌─────────────────┐
│ QueryHost        │ → Busca a request do Space
├─────────────────┤
│ ValidateMessage  │ → Valida formato, campos obrigatórios
├─────────────────┤
│ CheckDuplicate   │ → Verifica se é retry/duplicata
├─────────────────┤
│ RouteByBIN       │ → Decide: on-us ou off-us? Para onde?
├─────────────────┤
│ ForwardToIssuer  │ → Envia para o emissor (via QMUX)
├─────────────────┤
│ BuildResponse    │ → Monta a response para o adquirente
├─────────────────┤
│ AuditLog         │ → Registra tudo para auditoria
└─────────────────┘
    │
    ▼
Response enviada
```

## 2. Interface TransactionParticipant

```java
public interface TransactionParticipant {
    // Fase 1: Prepara. Retorna PREPARED, ABORTED, ou NO_JOIN
    int prepare(long id, Serializable context);

    // Fase 2: Se todos prepararam → commit
    void commit(long id, Serializable context);

    // Fase 2 alternativa: Se alguém abortou → abort
    void abort(long id, Serializable context);
}
```

**Retornos de `prepare()`:**
- `PREPARED` → "Fiz minha parte, pode continuar"
- `ABORTED` → "Algo deu errado, aborte tudo"
- `NO_JOIN` → "Preparei, mas não preciso de commit/abort"

## 3. Context — Passando dados entre participants

```java
Context ctx = (Context) context;

// QueryHost coloca a request
ctx.put("REQUEST", isoMsg);

// RouteByBIN adiciona decisão de rota
ctx.put("ROUTE_TYPE", "ON_US");         // ou "OFF_US"
ctx.put("DESTINATION_MUX", "issuer-local");

// ForwardToIssuer coloca a response
ctx.put("RESPONSE", responseMsg);

// BuildResponse lê e monta a resposta final
ISOMsg response = ctx.get("RESPONSE");
```

## 4. Exemplo: ValidateMessage Participant

```java
public class ValidateMessage implements TransactionParticipant {

    private static final int[] REQUIRED_AUTH_FIELDS = {2, 3, 4, 7, 11, 22, 41, 42, 49};

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");

        if (msg == null) {
            ctx.put("ERROR", "No request message in context");
            return ABORTED;
        }

        try {
            String mti = msg.getMTI();

            // Validar MTI conhecido
            if (!isKnownMTI(mti)) {
                ctx.put("RESPONSE_CODE", "12"); // Invalid transaction
                return ABORTED;
            }

            // Validar campos obrigatórios para auth
            if (mti.equals("0100") || mti.equals("0200")) {
                for (int field : REQUIRED_AUTH_FIELDS) {
                    if (!msg.hasField(field)) {
                        ctx.put("RESPONSE_CODE", "30"); // Format error
                        ctx.put("ERROR", "Missing DE" + field);
                        return ABORTED;
                    }
                }
            }

            // Validar PAN (Luhn)
            if (msg.hasField(2) && !luhnCheck(msg.getString(2))) {
                ctx.put("RESPONSE_CODE", "14"); // Invalid card number
                return ABORTED;
            }

            return PREPARED;

        } catch (ISOException e) {
            ctx.put("RESPONSE_CODE", "96"); // System malfunction
            return ABORTED;
        }
    }

    @Override
    public void commit(long id, Serializable context) { }

    @Override
    public void abort(long id, Serializable context) { }
}
```

## 5. Deploy XML do TransactionManager

```xml
<!-- deploy/05_txnmgr.xml -->
<txnmgr name="txnmgr" class="org.jpos.transaction.TransactionManager"
         logger="Q2" realm="txnmgr">
    <property name="queue" value="txnmgr"/>
    <property name="sessions" value="4"/>
    <property name="max-sessions" value="128"/>
    <property name="debug" value="true"/>

    <participant class="com.lab.participants.QueryHost"/>
    <participant class="com.lab.participants.ValidateMessage"/>

    <group name="authorization">
        <participant class="com.lab.participants.CheckDuplicate"/>
        <participant class="com.lab.participants.RouteByBIN"/>
        <participant class="com.lab.participants.ForwardToIssuer"/>
    </group>

    <group name="reversal">
        <participant class="com.lab.participants.FindOriginalTransaction"/>
        <participant class="com.lab.participants.ProcessReversal"/>
    </group>

    <group name="network-mgmt">
        <participant class="com.lab.participants.ProcessEcho"/>
    </group>

    <participant class="com.lab.participants.BuildResponse"/>
    <participant class="com.lab.participants.AuditLog"/>
</txnmgr>
```

## 6. GroupSelector — Branching por MTI

```java
public class QueryHost implements GroupSelector {

    @Override
    public String select(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");
        String mti = msg.getMTI();

        return switch (mti) {
            case "0100", "0200" -> "authorization";
            case "0400" -> "reversal";
            case "0800" -> "network-mgmt";
            default -> null; // pula groups, vai direto pro BuildResponse
        };
    }

    @Override
    public int prepare(long id, Serializable context) {
        // Extrai msg do Space e coloca no Context
        Context ctx = (Context) context;
        ISOSource source = ctx.get("SOURCE");
        ISOMsg msg = ctx.get("REQUEST");
        return PREPARED;
    }

    // commit/abort...
}
```

## 7. Exercícios Semana 7

1. **Implemente 5 participants:** QueryHost, ValidateMessage, RouteByBIN (stub), BuildResponse, AuditLog
2. **Configure o TransactionManager** no deploy XML
3. **Teste cada participant isoladamente** (unit test com Context mockado)
4. **Teste o pipeline completo**: Request entra → passa por todos → Response sai

### Desafio
Implemente um participant `TimingParticipant` que:
- No `prepare()`: registra `System.nanoTime()` no Context
- No `commit()`: calcula latência e loga
- Gera métrica: `iso8583.txn.latency.ms` por MTI

---

# Semana 8 — QMUX, Correlação e Late Response

## 1. QMUX — O multiplexador

O QMUX gerencia a correlação entre requests e responses em conexões TCP persistentes. Ele:
1. Envia o request no canal TCP
2. Armazena uma referência indexada pela chave de correlação
3. Quando a response chega, encontra o match pela chave
4. Entrega a response ao caller

```xml
<!-- deploy/20_mux.xml -->
<mux class="org.jpos.q2.iso.QMUX" logger="Q2" name="visa-mux">
    <in>visa-receive</in>    <!-- Queue de mensagens recebidas -->
    <out>visa-send</out>     <!-- Queue de mensagens para enviar -->
    <ready>visa-ready</ready>

    <!-- Chave de correlação -->
    <key>11 41</key>  <!-- STAN + Terminal ID -->

    <property name="timeout" value="30000"/>  <!-- 30 segundos -->
</mux>
```

## 2. Chave de correlação — Escolha crítica

| Chave | Prós | Contras |
|-------|------|---------|
| `11` (STAN only) | Simples | Colisão em alto volume |
| `11 41` (STAN + Terminal) | Boa unicidade | Terminal precisa estar na response |
| `11 7` (STAN + DateTime) | Sem ambiguidade | DateTime pode variar entre req/resp |
| `37` (RRN) | Identificador único de referência | Nem sempre presente no request |

## 3. Tratamento de Late Response

```
t=0    Switch envia 0100 para emissor
t=30   TIMEOUT — QMUX não recebeu resposta
t=31   Switch gera 0400 (reversal) automaticamente
t=32   Emissor FINALMENTE responde 0110 (late response!)

O que fazer com a 0110 tardia?
```

**Opções:**
1. **Descartar silenciosamente** — mais simples, mas pode causar inconsistência
2. **Logar e alertar** — mínimo aceitável
3. **Reverter se aprovada** — enviar 0400 para desfazer a aprovação tardia
4. **Reconciliar depois** — marcar como exceção para tratamento manual

## 4. Exercícios Semana 8

1. **Configure QMUX** com chave de correlação `11 41`
2. **Teste happy path**: request → response dentro do timeout
3. **Teste timeout**: request → sem response → exception/null
4. **Teste late response**: request → timeout → response chega depois → como tratar?
5. **Implemente `ForwardToIssuer` participant** que usa QMUX para enviar e esperar resposta

### Desafio
Simule o seguinte cenário completo:
1. Terminal envia 0200 (compra R$ 100)
2. QMUX encaminha para emissor simulado
3. Emissor simulado demora 35 segundos para responder (timeout é 30s)
4. QMUX retorna timeout
5. Switch gera 0400 (reversal) automático
6. Emissor responde o 0110 original (late response)
7. Switch detecta a late response e gera um segundo 0400

Documente: quantas mensagens foram trocadas? Qual o risco financeiro? Como prevenir?
