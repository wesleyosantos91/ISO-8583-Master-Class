# Fase 6 — Produção (Semanas 21-24)

---

# Semana 21 — Observabilidade para Pagamentos

## 1. As métricas que um switch precisa ter

```java
// Métricas obrigatórias — Micrometer/Prometheus
public class SwitchMetrics {
    
    private final MeterRegistry registry;
    
    // LATÊNCIA por MTI e rota
    public void recordLatency(String mti, String route, boolean approved, long ms) {
        Timer.builder("iso8583.auth.latency")
            .tag("mti", mti)
            .tag("route", route)
            .tag("result", approved ? "approved" : "declined")
            .register(registry)
            .record(ms, TimeUnit.MILLISECONDS);
    }
    
    // VOLUME por MTI e response code
    public void recordTransaction(String mti, String responseCode, String route) {
        Counter.builder("iso8583.transactions.total")
            .tag("mti", mti)
            .tag("rc", responseCode)
            .tag("route", route)
            .tag("on_us", route.equals("ON_US") ? "true" : "false")
            .register(registry)
            .increment();
    }
    
    // ERROS — timeouts, reversals, duplicatas
    public void recordTimeout(String mti, String destination) {
        Counter.builder("iso8583.timeout.total")
            .tag("mti", mti)
            .tag("destination", destination)
            .register(registry).increment();
    }
    
    // SATURAÇÃO — conexões ativas por destino
    public void registerConnectionGauge(String destination, AtomicInteger count) {
        Gauge.builder("iso8583.connections.active", count::get)
            .tag("destination", destination)
            .register(registry);
    }
}
```

## 2. SLAs quantificados

| Métrica | Meta | Alerta |
|---------|------|--------|
| Auth latency P95 (on-us) | < 150ms | > 300ms |
| Auth latency P95 (off-us) | < 500ms | > 1000ms |
| Disponibilidade | 99.99% | Qualquer downtime |
| Taxa de aprovação | > 85% | < 75% |
| Taxa de timeout | < 0.1% | > 0.5% |
| Taxa de reversal | < 0.5% | > 2% |
| Duplicatas detectadas | N/A | > 1% do volume |

## 3. Logs estruturados

```java
// Formato de log para cada transação
log.info("txn.processed mti={} stan={} pan={} amount={} rc={} route={} latency_ms={} duplicate={}",
    msg.getMTI(),
    msg.getString(11),
    PANMasker.mask(msg.getString(2)),
    msg.getString(4),
    responseCode,
    route,
    latencyMs,
    isDuplicate);
```

## 4. Exercícios Semana 21

1. **Implemente SwitchMetrics** com todas as métricas listadas
2. **Adicione logging estruturado** em todos os participants
3. **Crie queries de investigação** (como se usasse Elasticsearch/Grafana)
4. **Documente `runbook-observability.md`** com: o que monitorar, thresholds, como investigar

### Desafio
Construa um cenário onde a taxa de aprovação cai de 87% para 62% em 10 minutos. Usando apenas métricas e logs, diagnostique a causa (dica: pode ser emissor fora, BIN errado, timeout, campo inválido, etc.).

---

# Semana 22 — Troubleshooting Avançado

## 1. Taxonomia de falhas

| Categoria | Exemplos | Como identificar |
|-----------|----------|-----------------|
| **Rede/TCP** | Conexão recusada, timeout TCP, RST | Logs de canal, Wireshark |
| **Framing** | Header de tamanho errado, bytes sobrando | Hex dump, contagem de bytes |
| **Encoding** | BCD vs ASCII, EBCDIC vs ASCII | Comparar raw bytes vs valor esperado |
| **Spec/Packager** | Campo no lugar errado, tamanho errado | Comparar com spec da bandeira |
| **Bitmap** | Campo presente no bitmap mas ausente nos dados | Parser de bitmap vs dados |
| **Negócio** | Response code inesperado, decline sem motivo claro | Análise do DE39 e contexto |
| **Roteamento** | Transação vai pro destino errado | Verificar BIN table e logs de routing |

## 2. Playbook de troubleshooting

```
PASSO 1: Qual é o sintoma?
  - DE39=30 (Format Error) → problema de encoding/spec
  - DE39=91 (Issuer unavailable) → problema de rede/destino
  - DE39=96 (System malfunction) → erro interno no receptor
  - Timeout → problema de rede ou emissor lento

PASSO 2: Isolar o escopo
  - Afeta todos os terminais ou só alguns?
  - Afeta todas as bandeiras ou só uma?
  - Afeta todos os BINs ou só uma faixa?
  - Começou quando? Mudou algo?

PASSO 3: Analisar a mensagem
  - Hex dump do request enviado
  - Hex dump da response (se houver)
  - Comparar com uma mensagem que funciona (baseline)
  - Verificar bitmap vs dados presentes

PASSO 4: Reproduzir
  - Enviar mesma mensagem em ambiente de teste
  - Testar com outro terminal/BIN/bandeira
  - Simular com dados de produção (mascarados)
```

## 3. Exercícios Semana 22

1. **Monte catálogo de 15+ falhas** com: sintoma, causa, diagnóstico, correção
2. **Crie massa de teste** com mensagens intencionalmente quebradas
3. **Resolva 5 cenários de incidente** (fornecidos abaixo)

### Cenário A — Solução
Dump: `0200B238000108A18000001945320151128303660030000000001500003141600001234561600000314...`
Response: DE39=30 (Format error).

**Diagnóstico passo a passo:**
```
MTI:    0200                       ← OK
Bitmap: B238000108A18000 00 (hex)  ← B2 38 00 01 08 A1 80 00

Bits presentes (bitmap B238000108A18000):
  B2 = 1011 0010 → DE1(secundário), DE2, DE4, DE7
  38 = 0011 1000 → DE11, DE12, DE13
  00 = 0000 0000
  01 = 0000 0001 → DE32
  08 = 0000 1000 → DE38 (!)
  A1 = 1010 0001 → DE41, DE43, DE48
  80 = 1000 0000 → (secondary DE65 bit)
  00 = 0000 0000

PROBLEMA: DE38 (auth code) está no bitmap do REQUEST (0200).
DE38 só deveria aparecer na RESPONSE (0210) — o emissor que gera o auth code.
O receptor (emissor) recebe um campo que não esperava na posição do bitmap
→ offset errado para os campos seguintes → Format Error (30).
```

**Causa raiz:** O packager do terminal ou do switch está incluindo DE38 no request. Verificar: quem monta a mensagem, se há cópia errada de campos de uma response anterior.

**Correção:** Remover DE38 do buildAuthRequest(). DE38 só é setado na response pelo emissor.

---

### Cenário B — Solução
BINs `6504xxxx` indo off-us pela Elo quando deveriam ser on-us.

**Checklist de diagnóstico:**
```
1. BIN table: verificar se o BIN "6504" está cadastrado como on-us
   → Pode estar ausente ou com flag errada

2. Lookup order: checar se o switch faz longest-prefix-match
   → Se a tabela só tem BIN de 6 dígitos e o lookup usa 8 dígitos,
     pode não encontrar "65040123" (8 dig) quando a entrada é "6504" (4 dig)

3. Caching: se a BIN table tem cache, verificar se o cadastro novo foi invalidado

4. Logs de roteamento: grep por "650401" nos logs → qual destino está sendo escolhido?
   log.debug("BIN lookup: bin={} → route={}", bin, route.getMuxName());

5. Verificar prefixo Elo: BINs Elo geralmente começam com 4011, 4312, 4389, 438935,
   506699, 5067, 509, 6500, 6504...
   Se o BIN é novo (expansão 8 dígitos), a rota on-us pode estar cadastrada só com 6 dígitos
```

**Solução mais provável:** O BIN foi expandido de 6 para 8 dígitos. A tabela tem `6504xx` (6 dígitos) como off-us Elo, mas o BIN on-us correto é `65040123` (8 dígitos). Como o switch não tem o BIN de 8 dígitos cadastrado on-us, faz fallback para o 6 dígitos → roteia off-us.

**Correção:** Cadastrar o BIN de 8 dígitos com rota on-us. Usar longest-prefix-match.

---

### Cenário C — Solução
TERM0099: echo 0800 OK, mas 0200 retorna timeout.

**Diagnóstico:**
```
Echo OK → canal TCP está vivo
0200 timeout → o request chega ao emissor? Ou travou no switch?

Hipóteses:
1. QMUX: a chave de correlação do QMUX bate com o echo mas não com 0200
   → Verificar se a chave usa DE70 (presente no echo mas não no 0200)

2. GroupSelector: o 0200 foi roteado para um grupo inexistente
   → O switch consome a mensagem mas não processa → nenhuma response → timeout

3. Queue do TransactionManager saturada para 0200 mas não para 0800
   → Verificar thread pool: sessions e max-sessions

4. Emissor simulado: não tem handler para 0200, só para 0800
   → Adicionar handler no IssuerSimulator

5. Chave de correlação do 0210: o emissor está respondendo mas o QMUX
   não está casando (STAN diferente, ou falta DE41 na response)
```

**Diagnóstico confirmado:** Verificar no log do QMUX se o 0200 é enviado mas a 0210 não é correlacionada. Se `mux.request()` retorna null depois de exatamente 30s → timeout. Se retorna antes → outro problema.

**Ferramenta:** Ativar `debug=true` no QMUX para logar todas as mensagens enviadas e recebidas com suas chaves de correlação.

---

### Cenário D — Solução
DE22=071 (contactless) aprovado. DE22=051 (chip contact) negado com DE39=55 (Incorrect PIN).

**Análise:**
```
Contactless funciona → sem PIN (abaixo do floor limit) → não verifica PIN block
Chip contact falha com 55 → PIN block está chegando com erro

Hipóteses:
1. Encrypting key errada: o terminal usa uma chave de PIN diferente
   para contactless (NFC) vs contact. A chave para contact pode estar
   desatualizada ou incorreta.

2. KSN errado: em DUKPT, o KSN avança a cada transação.
   Se o contador de contactless e contact compartilham o mesmo KSN counter
   mas chaves diferentes, pode haver dessincronização.

3. PIN Block format: o terminal está usando Format 0 para contact
   mas o emissor espera Format 3 (ou vice-versa)

4. DE52 para contactless: DE52 está ausente (sem PIN) e é aprovado.
   Para contact: DE52 presente mas com valor corrompido.
```

**Verificação:** Capturar o DE52 de uma transação com chip contact. Tentar decriptar com a ZPK da sessão. Se o decrypt falhar → chave errada. Se decrypt OK mas PIN verificado está errado → problema no PIN entry do terminal.

**Solução mais provável:** A tecla de PIN entry do terminal (PIN pad) perdeu a key injection. Reinjetar ZPK no terminal ou acionar a certificadora para nova injeção de chave.

---

### Cenário E — Solução
47 transações autorizadas sem clearing. Merchant MERCHANT00042.

**Diagnóstico:**
```
1. Verificar tipo de transação: qual o MTI e DE3 dessas 47 transações?
   → Se todas são 0100 (pre-auth): faz sentido — completion (0220) não foi enviada

2. Verificar status no sistema do merchant:
   → O POS/gateway do MERCHANT00042 está enviando 0220 (capture advice)?
   → Se não → configuração de captura automática falhou

3. Verificar clearing window:
   → Qual é a janela de clearing do adquirente? D+1?
   → As transações são do dia anterior? Pode ser que o clearing ainda não fechou

4. Verificar force post:
   → Alguma dessas transações foi autorizada offline?
   → Transações offline entram como advice (0220) diretamente, sem 0100 anterior

5. Verificar se o adquirente recebeu o arquivo de clearing da bandeira:
   → O arquivo pode ter chegado mas essas transações estão com RRN não mapeado
```

**Causa mais provável para dual message:** O sistema de captura (batch sender) do merchant falhou e não enviou os 0220. Ou enviou mas com RRN diferente do original → não casou na reconciliação.

**Ação imediata:**
1. Alertar o merchant sobre as transações pendentes de captura
2. Verificar se o prazo de captura não expirou (7 dias para a maioria das redes)
3. Se expirou → as autorizações serão canceladas automaticamente pelo emissor

---

### Catálogo de Falhas — 10 cenários essenciais

| # | Sintoma | Causa comum | Diagnóstico | Correção |
|---|---------|-------------|-------------|----------|
| 1 | DE39=30 no request | Campo extra no bitmap (ex: DE38 no request) | Comparar bitmap com spec | Remover campo indevido do packager/builder |
| 2 | DE39=30 na response | Packager do switch não bate com o da contraparte | Hex dump + spec comparison | Alinhar packager XML com a spec da rede |
| 3 | DE39=91 sempre | Canal TCP caiu ou emissor fora | Ping TCP, verificar logs de canal | Reconectar, acionar NOC do emissor |
| 4 | DE39=55 em massa | ZPK expirou ou key injection falhou | Tentar decriptar DE52 com chave atual | Reinjetar chave nos terminais |
| 5 | Roteamento errado | BIN table desatualizada ou prefix mismatch | Log de routing + BIN table | Atualizar BIN table, checar 8-digit BIN |
| 6 | Timeout em massa | Emissor sobrecarregado ou network congestion | Latência P50/P95, trace route | Acionar emissor, ativar stand-in |
| 7 | Duplicatas processadas | DuplicateChecker não cobre o campo correto | Analisar STAN+Terminal da duplicata | Ajustar buildDedupKey() |
| 8 | Reversal não encontrado (DE39=76) | STAN do reversal bate com original? | Log do reversal + DE90 | Verificar copyField(11) — deve copiar STAN original, não gerar novo no DE90 |
| 9 | Auth sem clearing (47 txns) | Captura não enviada pelo merchant | Verificar 0220 no log | Alertar merchant, verificar janela |
| 10 | DE39=57 em voucher | MCC não cadastrado para o tipo de benefício | Verificar DE18 vs regra de voucher | Cadastrar MCC correto ou corrigir validação |

### Desafio
Crie um **playbook de incidente** formatado como runbook para o time de operações. Deve cobrir os 10 incidentes mais comuns com: detecção, diagnóstico, resolução, prevenção.

---

# Semana 23 — Reconciliação Real

## 1. O ciclo autorização → clearing → settlement

```
D+0: Autorização (0100/0200)   → Reserva limite/saldo
D+1: Clearing file gerado      → Transações enviadas à bandeira em arquivo
D+1: Settlement               → Transferência financeira inter-bancária
D+2: Crédito ao merchant      → Adquirente repassa ao lojista (- taxa MDR)
```

**O que pode dar errado entre auth e clearing:**
- Auth aprovada mas clearing nunca chega (merchant não capturou)
- Clearing chega mas valor diferente do autorizado (gorjeta, tip adjustment)
- Clearing chega mas PAN diferente (erro de truncamento)
- Clearing chega em duplicata (double presentment)
- Auth revertida mas clearing veio mesmo assim (force post)

---

## 2. Formato do arquivo de clearing (simplificado CSV)

```
# clearing_20240314.csv
# rrn,pan_last4,amount_cents,date,mcc,merchant_id,auth_code,txn_type
# txn_type: PURCHASE, REFUND, REVERSAL
123456789012,0366,15000,20240314,5812,MERCHANT00001,AUTH01,PURCHASE
234567890123,1234,8500,20240314,5411,MERCHANT00002,AUTH02,PURCHASE
345678901234,0366,15000,20240314,5812,MERCHANT00001,AUTH01,REFUND
456789012345,5678,50000,20240314,7011,MERCHANT00003,AUTH03,PURCHASE
```

---

## 3. ClearingFileParser — Implementação completa

```java
public class ClearingFileParser {

    public record ClearingRecord(
        String rrn,
        String panLast4,
        long amountCents,
        LocalDate txnDate,
        String mcc,
        String merchantId,
        String authCode,
        String txnType       // PURCHASE, REFUND, REVERSAL
    ) {}

    /**
     * Lê um arquivo CSV de clearing e retorna lista de registros.
     * Linhas começando com '#' são comentários/cabeçalho e são ignoradas.
     */
    public List<ClearingRecord> parse(Path filePath) throws IOException {
        List<ClearingRecord> records = new ArrayList<>();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] fields = line.split(",", -1);
                if (fields.length < 8) {
                    log.warn("Clearing file line {}: expected 8 fields, got {}", lineNum, fields.length);
                    continue;
                }

                try {
                    records.add(new ClearingRecord(
                        fields[0].trim(),                              // rrn
                        fields[1].trim(),                              // panLast4
                        Long.parseLong(fields[2].trim()),              // amountCents
                        LocalDate.parse(fields[3].trim(), dateFmt),   // txnDate
                        fields[4].trim(),                              // mcc
                        fields[5].trim(),                              // merchantId
                        fields[6].trim(),                              // authCode
                        fields[7].trim()                               // txnType
                    ));
                } catch (NumberFormatException | DateTimeParseException e) {
                    log.error("Clearing file line {}: parse error — {}", lineNum, e.getMessage());
                }
            }
        }

        log.info("ClearingFileParser: {} records parsed from {}", records.size(), filePath);
        return records;
    }
}
```

---

## 4. ReconciliationEngine — Implementação completa

```java
public class ReconciliationEngine {

    public enum ExceptionType {
        AUTH_WITHOUT_CLEARING,    // Auth aprovada mas não apareceu no clearing
        CLEARING_WITHOUT_AUTH,    // Clearing chegou sem auth correspondente (force post)
        AMOUNT_MISMATCH,          // Valor no clearing ≠ valor autorizado
        PAN_MISMATCH,             // PAN (last4) diferente entre auth e clearing
        DUPLICATE_CLEARING,       // Mesmo RRN aparece 2x no clearing
        REVERSAL_IN_CLEARING,     // Reversal chegou no clearing (deve ser monitorado)
    }

    public record ReconciliationException(
        String rrn,
        ExceptionType type,
        String detail,
        Long authAmount,
        Long clearingAmount
    ) {}

    public record ReconciliationReport(
        LocalDate date,
        int totalAuths,
        int totalClearing,
        long totalAuthAmount,
        long totalClearingAmount,
        long netDifference,       // clearingAmount - authAmount
        List<ReconciliationException> exceptions
    ) {
        public boolean isBalanced() { return netDifference == 0 && exceptions.isEmpty(); }
    }

    /**
     * Reconcilia autorizações vs registros de clearing.
     *
     * @param auths    Mapa RRN → registro de autorização (vem do banco de dados do switch)
     * @param clearing Lista de registros do arquivo de clearing
     */
    public ReconciliationReport reconcile(
            Map<String, AuthRecord> auths,
            List<ClearingFileParser.ClearingRecord> clearing) {

        List<ReconciliationException> exceptions = new ArrayList<>();
        Set<String> matchedRRNs = new HashSet<>();
        Set<String> seenClearingRRNs = new HashSet<>();

        long totalAuthAmount     = 0;
        long totalClearingAmount = 0;

        // ── Processa cada registro de clearing ───────────────────────────────
        for (ClearingFileParser.ClearingRecord cr : clearing) {
            totalClearingAmount += "REFUND".equals(cr.txnType())
                ? -cr.amountCents()
                : cr.amountCents();

            // Duplicata no clearing
            if (!seenClearingRRNs.add(cr.rrn())) {
                exceptions.add(new ReconciliationException(
                    cr.rrn(), ExceptionType.DUPLICATE_CLEARING,
                    "RRN aparece 2x no arquivo de clearing", null, cr.amountCents()
                ));
                continue;
            }

            AuthRecord auth = auths.get(cr.rrn());

            if (auth == null) {
                // Force post — clearing sem auth
                if (!"REVERSAL".equals(cr.txnType())) {
                    exceptions.add(new ReconciliationException(
                        cr.rrn(), ExceptionType.CLEARING_WITHOUT_AUTH,
                        "Clearing sem autorização correspondente", null, cr.amountCents()
                    ));
                }
                continue;
            }

            matchedRRNs.add(cr.rrn());

            // Verifica PAN (last4)
            if (auth.panLast4() != null && !auth.panLast4().equals(cr.panLast4())) {
                exceptions.add(new ReconciliationException(
                    cr.rrn(), ExceptionType.PAN_MISMATCH,
                    String.format("Auth panLast4=%s, Clearing panLast4=%s",
                                  auth.panLast4(), cr.panLast4()),
                    auth.amountCents(), cr.amountCents()
                ));
            }

            // Verifica valor (com tolerância para tip/gorjeta — 20% ou R$50)
            long diff = Math.abs(auth.amountCents() - cr.amountCents());
            double pct = auth.amountCents() > 0
                ? (double) diff / auth.amountCents()
                : 1.0;
            boolean withinTolerance = diff <= 5000 || pct <= 0.20; // R$50 ou 20%
            if (diff > 0 && !withinTolerance) {
                exceptions.add(new ReconciliationException(
                    cr.rrn(), ExceptionType.AMOUNT_MISMATCH,
                    String.format("Auth=R$%.2f, Clearing=R$%.2f, diff=R$%.2f",
                                  auth.amountCents() / 100.0,
                                  cr.amountCents()  / 100.0,
                                  diff / 100.0),
                    auth.amountCents(), cr.amountCents()
                ));
            }
        }

        // ── Auth sem clearing ────────────────────────────────────────────────
        for (Map.Entry<String, AuthRecord> entry : auths.entrySet()) {
            String rrn = entry.getKey();
            AuthRecord auth = entry.getValue();

            totalAuthAmount += auth.amountCents();

            if (!matchedRRNs.contains(rrn) && "00".equals(auth.responseCode())) {
                exceptions.add(new ReconciliationException(
                    rrn, ExceptionType.AUTH_WITHOUT_CLEARING,
                    String.format("Auth aprovada em %s sem clearing — merchant=%s",
                                  auth.txnDate(), auth.merchantId()),
                    auth.amountCents(), null
                ));
            }
        }

        return new ReconciliationReport(
            LocalDate.now(),
            auths.size(),
            clearing.size(),
            totalAuthAmount,
            totalClearingAmount,
            totalClearingAmount - totalAuthAmount,
            Collections.unmodifiableList(exceptions)
        );
    }
}

// Record para representar uma autorização do banco de dados
public record AuthRecord(
    String rrn,
    String panLast4,
    long amountCents,
    String responseCode,   // DE39
    LocalDate txnDate,
    String merchantId,
    String authCode        // DE38
) {}
```

---

## 5. Relatório de exceções

```java
public class ReconciliationReporter {

    public void printReport(ReconciliationEngine.ReconciliationReport report) {
        System.out.println("=== RELATÓRIO DE RECONCILIAÇÃO ===");
        System.out.printf("Data: %s%n", report.date());
        System.out.printf("Total autorizações: %d (R$ %.2f)%n",
            report.totalAuths(), report.totalAuthAmount() / 100.0);
        System.out.printf("Total clearing:     %d (R$ %.2f)%n",
            report.totalClearing(), report.totalClearingAmount() / 100.0);
        System.out.printf("Diferença líquida:  R$ %.2f%n",
            report.netDifference() / 100.0);
        System.out.printf("Balanceado: %s%n", report.isBalanced() ? "SIM ✓" : "NÃO ✗");
        System.out.println();

        // Agrupar por tipo de exceção
        Map<ReconciliationEngine.ExceptionType, List<ReconciliationEngine.ReconciliationException>> byType =
            report.exceptions().stream()
                .collect(Collectors.groupingBy(ReconciliationEngine.ReconciliationException::type));

        byType.forEach((type, list) -> {
            System.out.printf("--- %s (%d ocorrências) ---%n", type, list.size());
            list.stream().limit(5).forEach(ex ->
                System.out.printf("  RRN=%-12s | %s%n", ex.rrn(), ex.detail()));
            if (list.size() > 5) {
                System.out.printf("  ... e mais %d%n", list.size() - 5);
            }
        });
    }
}
```

---

## 6. Exercícios Semana 23

1. **Implemente ClearingFileParser** com teste que processa CSV de exemplo
2. **Implemente ReconciliationEngine.reconcile()** e teste os 5 tipos de exceção
3. **Gere relatório** de exceções para o time financeiro
4. **Calcule totais** para conferência

### Desafio
Processe 10.000 autorizações e 9.800 registros de clearing. Identifique e classifique todas as exceções. Monte relatório. Verifique: qual a diferença líquida? Quais exceções têm maior valor financeiro em risco?

---

# Semana 24 — Arquitetura do Switch

## Exercícios
1. **Documente a arquitetura final** do payment-switch-lab em diagrama C4 (Context, Container, Component)
2. **Escreva ADRs** para as 5 decisões mais importantes:
   - Por que TransactionManager + Participants?
   - Por que cache em memória para deduplicação?
   - Por que timeout de 30s?
   - Sync vs async para reversal?
   - Como escalar horizontalmente?
3. **Diagrama de deployment** com Docker Compose
4. **Documente trade-offs** explicitamente

### Desafio
Apresente a arquitetura para 3 audiências (escreva o pitch para cada):
1. Arquiteto — foco em decisões técnicas e trade-offs
2. Gerente/Head — foco em risco, custo e prazo
3. Time de operações — foco em monitoramento e manutenção

---

# Fase 7 — Especialização (Semanas 25-28)

---

# Semana 25 — Mercado Brasileiro

## 1. Regulação — Lei 12.865/2013 e BACEN

A Lei 12.865/2013 criou o framework regulatório dos arranjos de pagamento no Brasil:

| Conceito | Definição |
|----------|-----------|
| **Arranjo de pagamento** | Conjunto de regras e procedimentos que disciplina serviços de pagamento ao público (ex: "Visa", "Mastercard", "Elo", "Pix") |
| **Instituidor de arranjo** | Empresa que cria e gerencia o arranjo (Visa, Mastercard, Elo) |
| **Participante do arranjo** | Bancos, adquirentes, credenciadores |
| **Interoperabilidade** | BACEN exige que arranjos abertos aceitem outros participantes |

**Principais Circulares do BACEN relevantes para cartões:**

| Circular | Tema |
|----------|------|
| BACEN 3.887/2018 | Teto de interchange: débito 0,5%, crédito à vista 0,7% |
| BACEN 3.886/2018 | Regras de transparência para MDR ao merchant |
| Resolução BCB 150/2021 | Regras de portabilidade de recebíveis |

## 2. Elo — A Bandeira Nacional

A **Elo** pertence ao BB (25%), Bradesco (25%) e Caixa (25%) com outros acionistas. Criada em 2011, hoje é a 3ª maior bandeira no Brasil com ~30% do parque de cartões.

### Diferenças técnicas Elo vs Visa/Master

| Aspecto | Elo | Visa/Mastercard |
|---------|-----|-----------------|
| Specs | Elo Tech (próprias) | Visa Core Specs / M-TIP |
| Processamento | Pode ser on-us entre sócios | Sempre passa pela bandeira |
| DE48 installments | Formato próprio | Padrão de mercado |
| Contactless | Paywave/PayPass licenciado | Paywave/PayPass nativos |
| Certif. Brasil | CELO (sistema próprio) | VCMS / MCW |

### BIN Ranges Elo (referência)

```
4011xx, 4312xx, 4389xx — Elo clássico (Visa-like prefix)
6363xx, 6500xx-6550xx  — Elo Nanquim/Grafite
5067xx, 5090xx         — Elo Mais (Mastercard-like prefix)
6516xx, 6550xx         — Elo Hipercard (fusão)
```

## 3. Sub-adquirência — Fluxo Técnico

Na sub-adquirência (PagSeguro, Stone, Cielo-mobile, Mercado Pago), há um intermediário extra:

```
Terminal/App → Sub-adquirente → Adquirente → Bandeira → Emissor
              (PagSeguro)      (Cielo)       (Visa)
```

**Impacto no ISO 8583:**

```
DE 42 (Merchant ID):    ID do sub-adquirente como merchant no adquirente
DE 43 (Merchant Name):  Nome do merchant REAL (não do sub-adquirente)
DE 48 (Additional):     SubElement com ID do merchant real (Soft Descriptor)
```

### Soft Descriptor — Obrigatório para Sub-adquirentes

O BACEN exige que transações de sub-adquirentes incluam o nome real do merchant na fatura do portador. Implementação via DE 43 ou campo proprietário:

```java
public class SoftDescriptorBuilder {

    /**
     * Constrói DE43 com formato padronizado para sub-adquirência.
     * Formato: "NOME_MERCHANT*NOME_SUBACQ   CIDADE BR"
     * Máximo 40 caracteres (ISO 8583)
     */
    public static String buildDE43(String merchantName, String subAcqName,
                                    String city) {
        // Trunca merchant name para caber: nome*subacq = 22 chars, cidade BR = 12 chars
        String combined = truncate(merchantName, 15) + "*" + truncate(subAcqName, 8);
        String location = truncate(city, 9) + " BR";
        // Pad para 40 chars (campo fixo)
        return String.format("%-22s%-13s  ", combined, location).substring(0, 40);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
```

## 4. Interchange e Modelo Econômico Brasileiro

### Teto Regulatório (BACEN)

```
Débito:         máx 0,50% da transação
Crédito à vista: máx 0,70% da transação
Crédito parcelado: acima do teto (mercado livre)
Pré-pago:       máx 0,50%
```

### Impacto do On-Us

Transação on-us (emissor = adquirente, ex: Bradesco emitindo + Cielo adquirindo, ambos Bradesco) não paga interchange para si mesmo. Resultado: MDR cobrado do merchant pode ser menor.

```java
public class InterchangeCalculator {

    record InterchangeTable(String processingCode, String network, double rate) {}

    private static final List<InterchangeTable> TABLE = List.of(
        new InterchangeTable("002000", "ELO",        0.0050),  // débito
        new InterchangeTable("002000", "VISA",       0.0050),  // débito (teto BACEN)
        new InterchangeTable("003000", "VISA",       0.0070),  // crédito à vista (teto)
        new InterchangeTable("003000", "MASTERCARD", 0.0070),
        new InterchangeTable("003000", "ELO",        0.0060)
    );

    public BigDecimal calculate(BigDecimal amount, String processingCode,
                                 String network, boolean isOnUs) {
        if (isOnUs) return BigDecimal.ZERO; // On-us: sem interchange

        double rate = TABLE.stream()
            .filter(t -> t.processingCode().startsWith(processingCode.substring(0, 2))
                      && t.network().equalsIgnoreCase(network))
            .mapToDouble(InterchangeTable::rate)
            .findFirst()
            .orElse(0.007); // default crédito

        return amount.multiply(BigDecimal.valueOf(rate))
                     .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
```

## 5. Antecipação de Recebíveis

O merchant recebe o dinheiro de transações de crédito somente após o prazo (D+30 para à vista). A **antecipação** permite receber antes pagando uma taxa.

**Registradoras de Recebíveis (obrigatório desde 2021):**

| Registradora | Controladora |
|-------------|-------------|
| CIP (Câmara Interbancária) | B3 |
| TAG | Associação de Fintechs |
| CERC | Independente |

```
Fluxo regulatório de antecipação:
  1. Merchant agenda recebível na registradora
  2. Adquirente notifica registradora sobre UR (Unidade de Recebível)
  3. Banco que antecipa consulta UR disponível
  4. Fundo é transferido para merchant via PIX/TED
  5. No vencimento, adquirente paga para o banco (não merchant)
```

## 6. PIX vs Cartão — Onde Competem

| Caso de uso | PIX | Cartão Débito | Cartão Crédito |
|-------------|-----|---------------|----------------|
| Transferência P2P | Melhor | Ruim | N/A |
| Compra presencial baixo valor | Possível | Bom | Bom |
| Compra online | Possível (QR) | Bom | Ótimo (proteção) |
| Parcelamento | Não existe | Não existe | Único |
| Chargeback/proteção | Fraco | Fraco | Forte |
| Velocidade de liquidação | Instantâneo (D+0) | D+1 | D+30 |
| Custo para merchant | ~0% (PIX) | ~1,5-2% | ~2-4% |

**Conclusão:** PIX não substitui crédito. Compete diretamente com débito e transferência. O crédito (especialmente parcelado) é insubstituível no Brasil enquanto não houver produto equivalente.

## 7. Parcelamento — A Peculiaridade Brasileira

### Por que é único no Brasil

No mundo, compras parceladas são feitas via **BNPL** (Buy Now Pay Later — Klarna, Affirm) ou financiamento bancário separado. No Brasil, o parcelamento **está integrado na mensagem ISO 8583** e na fatura do cartão:

```
Compra de R$ 1.200 em 12x sem juros:
  - DE4 (Amount) = 000000120000 (R$ 1.200 total)
  - DE48 (Installment) = "0112" (lojista, 12 parcelas)
  - Fatura: mostra R$ 100/mês por 12 meses
```

**Impacto técnico:**

1. **Clearing:** cada parcela pode ser liquidada mensalmente (depende do adquirente)
2. **Interchange:** parcelas têm interchange diferente (não regulado pelo BACEN como à vista)
3. **Chargeback:** o portador pode contestar parCelas individuais ou o total
4. **Antecipação:** merchant pode antecipar parcelas futuras

## 8. Exercícios

1. **Desenhe o fluxo** de uma transação sub-adquirente: App iFood → Stone (sub-acq) → Cielo (acq) → Elo → Itaú
2. **Implemente `SoftDescriptorBuilder`** com testes: garanta que nomes de 30+ caracteres são truncados corretamente
3. **Calcule a receita** de uma transação de R$ 100 crédito à vista: quanto fica com o emissor, bandeira, adquirente e merchant? (Assuma MDR 2.5%, interchange 0.7%, assessment 0.1%)
4. **Documente 5 diferenças** entre certificação Elo (CELO) e certificação Visa (VCMS)

### Desafio
Implemente `InterchangeCalculator` com tabela completa (débito, crédito à vista, parcelado 2-6x, parcelado 7-12x) para as bandeiras Visa, Mastercard e Elo. Adicione lógica de on-us detection baseada em BIN do emissor vs BIN do adquirente.

---

# Semana 26 — Fluxos Avançados

## 1. Pre-Authorization — Estado e Lifecycle

A **pré-autorização** reserva fundos sem capturar. Usada em hotéis, locadoras, postos de gasolina.

```
DE 25 (Point of Service Condition Code):
  01 = Normal
  06 = Pre-authorized request   ← pré-auth
  10 = Customer not present
```

### Diagrama do fluxo hotel

```
CHECK-IN (D+0):                     CHECK-OUT (D+3):
0100 DE25=06 DE4=200000             0200 DE25=06 DE4=150000
    │                                   │
    │ 0110 DE39=00 DE38=AUTH001         │ 0210 DE39=00
    ↓ (R$ 2.000 reservado)             ↓ (R$ 1.500 capturado)

0400 (Reversal da diferença R$ 500):
    Gerado automaticamente pelo terminal/switch
    DE4=50000 (diferença entre aprovado e capturado)
```

### PreAuthorizationManager

```java
public class PreAuthorizationManager {

    private final Map<String, PreAuthRecord> store = new ConcurrentHashMap<>();

    public record PreAuthRecord(
        String authCode,          // DE 38 da pré-auth
        String rrn,               // DE 37
        BigDecimal reservedAmount, // Valor aprovado
        BigDecimal capturedAmount, // Valor capturado (0 no início)
        Instant expiresAt,        // Pre-auth expira em 7 dias (regra de rede)
        PreAuthStatus status
    ) {}

    public enum PreAuthStatus { ACTIVE, COMPLETED, REVERSED, EXPIRED }

    /** Registra uma pré-auth aprovada (0110 com DE39=00, DE25=06) */
    public void registerPreAuth(ISOMsg response) throws ISOException {
        String authCode = response.getString(38);
        String rrn      = response.getString(37);
        long   amount   = Long.parseLong(response.getString(4));

        store.put(rrn, new PreAuthRecord(
            authCode,
            rrn,
            BigDecimal.valueOf(amount, 2),
            BigDecimal.ZERO,
            Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS),
            PreAuthStatus.ACTIVE
        ));
    }

    /** Verifica se completion (0200 DE25=06) está dentro do valor reservado */
    public boolean validateCompletion(String originalRrn, BigDecimal completionAmount) {
        PreAuthRecord rec = store.get(originalRrn);
        if (rec == null) return false;
        if (rec.status() != PreAuthStatus.ACTIVE) return false;
        if (Instant.now().isAfter(rec.expiresAt())) {
            expire(originalRrn);
            return false;
        }
        // Permite até 15% acima do valor pré-autorizado (regra Visa/MC para hotel)
        BigDecimal maxAllowed = rec.reservedAmount().multiply(BigDecimal.valueOf(1.15));
        return completionAmount.compareTo(maxAllowed) <= 0;
    }

    public void complete(String originalRrn, BigDecimal capturedAmount) {
        store.computeIfPresent(originalRrn, (k, rec) ->
            new PreAuthRecord(rec.authCode(), rec.rrn(), rec.reservedAmount(),
                              capturedAmount, rec.expiresAt(), PreAuthStatus.COMPLETED));
    }

    private void expire(String rrn) {
        store.computeIfPresent(rrn, (k, rec) ->
            new PreAuthRecord(rec.authCode(), rec.rrn(), rec.reservedAmount(),
                              rec.capturedAmount(), rec.expiresAt(), PreAuthStatus.EXPIRED));
    }
}
```

## 2. Incremental Authorization

Autorização incremental permite aumentar o valor de uma pré-auth ativa sem cancelar a original.

```
0100 #1 → DE4=200000 DE25=06 → APPROVED DE38=AUTH001 (R$ 2.000)
0100 #2 → DE4=50000  DE25=06 DE56=<RRN original> → APPROVED (incremental +R$ 500)
0200    → DE4=230000 DE25=06 → Completion R$ 2.300
```

**Campo de referência:** DE56 (Original Data) ou DE48 subelemento proprietário carrega o RRN da pré-auth original.

```java
public class IncrementalAuthBuilder {

    /**
     * Constrói mensagem de autorização incremental.
     * @param originalRrn  RRN da pré-auth original (referência)
     * @param incrementalAmount  Valor ADICIONAL a ser reservado
     */
    public ISOMsg build(ISOMsg original0100, String originalRrn,
                         BigDecimal incrementalAmount) throws ISOException {
        ISOMsg inc = new ISOMsg();
        inc.setMTI("0100");
        inc.set(3,  original0100.getString(3));   // mesmo Processing Code
        inc.set(4,  formatAmount(incrementalAmount));
        inc.set(11, generateSTAN());
        inc.set(22, original0100.getString(22));
        inc.set(25, "06");                         // pre-auth condition code
        inc.set(41, original0100.getString(41));
        inc.set(42, original0100.getString(42));
        // DE56: dados originais (campo proprietário em muitas redes)
        inc.set(56, originalRrn);
        return inc;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%012d",
            amount.movePointRight(2).longValue());
    }

    private String generateSTAN() {
        return String.format("%06d",
            (int)(Math.random() * 999999));
    }
}
```

## 3. Partial Approval

O emissor aprova um valor MENOR que o solicitado. O terminal deve lidar com isso.

```
Terminal → Switch: 0100  DE4=010000 (R$ 100,00)
Emissor  → Switch: 0110  DE4=007500 DE39=10 (Partial Approved R$ 75,00)
Switch   → Terminal: 0210 DE4=007500 DE39=10
Terminal: "Aprovado parcial R$ 75,00. Deseja pagar R$ 25,00 com outro cartão?"
```

**DE39=10 = Partial Approval** (somente Visa/Master suportam; Elo também)

```java
public class PartialApprovalHandler implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg response = ctx.get("RESPONSE");
        if (response == null) return PREPARED;

        String rc = response.getString(39);
        if (!"10".equals(rc)) return PREPARED; // Não é partial approval

        // Registra o valor aprovado parcialmente
        String approvedAmount = response.getString(4);
        String requestedAmount = ((ISOMsg) ctx.get("REQUEST")).getString(4);

        ctx.put("PARTIAL_APPROVAL", true);
        ctx.put("APPROVED_AMOUNT", approvedAmount);
        ctx.put("REQUESTED_AMOUNT", requestedAmount);

        long diff = Long.parseLong(requestedAmount) - Long.parseLong(approvedAmount);
        ctx.put("REMAINING_AMOUNT", String.format("%012d", diff));

        // Log para auditoria
        log.info("PARTIAL_APPROVAL approved={} requested={} remaining={}",
                 approvedAmount, requestedAmount, diff);

        return PREPARED;
    }
}
```

## 4. Balance Inquiry

Consulta de saldo disponível no emissor. Usa DE3=300000 e retorna saldo em DE54.

### Formato DE54 (Additional Amounts)

```
AA BB CC DDDDDDDDDDDD
│  │  │  └── Valor (12 dígitos, sem vírgula)
│  │  └───── Moeda (ex: 986 = BRL)
│  └──────── Tipo de conta (10=poupança, 20=corrente, 30=crédito disponível)
└─────────── Account Type (01=corrente, 02=poupança, 04=crédito)
```

Exemplo: `1001986C000001500000` = conta corrente, crédito disponível, BRL, R$ 15.000,00 (C = credit balance)

```java
public class BalanceInquiryBuilder {

    /** Constrói 0100 de balance inquiry */
    public ISOMsg buildRequest(String pan, String terminalId,
                                String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0100");
        msg.set(2,  pan);
        msg.set(3,  "300000");  // Balance Inquiry
        msg.set(4,  "000000000000");
        msg.set(11, generateSTAN());
        msg.set(22, "051");     // chip card, PIN
        msg.set(25, "00");
        msg.set(41, terminalId);
        msg.set(42, merchantId);
        return msg;
    }

    /** Parseia DE54 da resposta e retorna saldo em centavos */
    public long parseBalance(ISOMsg response) throws ISOException {
        String de54 = response.getString(54);
        if (de54 == null || de54.length() < 20) return -1;

        // Pode ter múltiplos grupos de 20 chars; pega o primeiro
        String group = de54.substring(0, 20);
        char sign    = group.charAt(8);   // 'C' = credit (positivo), 'D' = debit (negativo)
        long amount  = Long.parseLong(group.substring(9, 21));
        return "D".equals(String.valueOf(sign)) ? -amount : amount;
    }

    private String generateSTAN() {
        return String.format("%06d", (int)(Math.random() * 999999));
    }
}
```

## 5. Exercícios

1. **Implemente o fluxo de hotel completo:** check-in → minibar (+R$ 80) → checkout → auto-reversal da diferença usando `PreAuthorizationManager`
2. **Teste partial approval:** crie um `IssuerSimulatorAdvanced` que aprova parcialmente transações quando o saldo é insuficiente mas > 0
3. **Implemente balance inquiry** de ponta a ponta com DE54 corretamente formatado
4. **Locadora de veículos:** pre-auth R$ 5.000, dano incremental +R$ 2.000, checkout R$ 4.500 — descreva cada mensagem ISO 8583 com todos os campos relevantes

### Desafio
Implemente um `AdvancedFlowIntegrationTest` que executa os 4 fluxos acima sequencialmente contra um `IssuerSimulator` embarcado, verifica todos os estados intermediários via `PreAuthorizationManager`, e asserta que os valores finais coincidem com o esperado.

---

# Semana 27 — Certificação e ISO 20022

## 1. Certificação com Bandeiras — O Processo Real

A certificação é obrigatória antes de ir a produção. Cada bandeira tem seu próprio programa:

| Bandeira | Programa | Ferramenta |
|---------|---------|----------|
| Visa | VCMS (Visa Certification Management System) | Via portal Visa Developer |
| Mastercard | MCW (Mastercard Certification Workbench) | Via portal Mastercard Developers |
| Elo | CELO | Via parceiro certificado |
| American Express | Próprio | Contato direto |

### 1.1 Estrutura de um Test Deck

Um test deck tem ~200-500 cenários organizados em categorias:

```
CATEGORIA 1 — Authorization (Happy Path)
  TC001: Magnetic stripe, debit, approved (DE39=00)
  TC002: Chip, credit, approved
  TC003: Contactless, below floor limit, approved
  TC004: CNP, 3DS authenticated, approved

CATEGORIA 2 — Authorization (Declines)
  TC010: Incorrect PIN (DE39=55)
  TC011: Insufficient funds (DE39=51)
  TC012: Expired card (DE39=54)
  TC013: Do not honor (DE39=05)
  TC014: Card not permitted (DE39=57)

CATEGORIA 3 — Reversals
  TC020: Timeout reversal (DE39=68 → 0400)
  TC021: Customer cancellation reversal
  TC022: Partial reversal

CATEGORIA 4 — Network Management
  TC030: Sign-on (0800/0810)
  TC031: Echo (0800/0810 DE70=301)
  TC032: Key exchange (0800/0810 DE70=161)

CATEGORIA 5 — EMV/Chip specific
  TC040: ARQC validation
  TC041: AAC (offline decline) — switch deve processar corretamente
  TC042: Fallback magnetic stripe (chip falhou)
  TC043: Contactless NFC, amount below CVM limit

CATEGORIA 6 — Edge cases
  TC050: Duplicate STAN
  TC051: Timeout sem reversal (switch deve gerar)
  TC052: Response fora de tempo (late response)
  TC053: DE format errors (DE39=30 esperado)
```

### 1.2 Test Case Runner — Implementação

```java
public class CertificationTestRunner {

    public record TestCase(
        String id,
        String description,
        ISOMsg requestTemplate,       // Mensagem a enviar
        Map<String, String> expectedFields, // DE → valor esperado na resposta
        boolean expectTimeout,        // Se true, espera null response
        String category
    ) {}

    public record TestResult(
        String testId,
        boolean passed,
        String failureReason,
        ISOMsg actualResponse,
        long durationMs
    ) {}

    private final QMUX mux;
    private final long timeout;

    public List<TestResult> runDeck(List<TestCase> deck) {
        return deck.parallelStream()
                   .map(this::runSingle)
                   .collect(java.util.stream.Collectors.toList());
    }

    private TestResult runSingle(TestCase tc) {
        long start = System.currentTimeMillis();
        try {
            ISOMsg response = mux.request(tc.requestTemplate(), timeout);
            long duration   = System.currentTimeMillis() - start;

            if (tc.expectTimeout()) {
                boolean passed = response == null;
                return new TestResult(tc.id(), passed,
                    passed ? null : "Expected timeout but got response DE39=" + response.getString(39),
                    response, duration);
            }

            if (response == null) {
                return new TestResult(tc.id(), false, "TIMEOUT — no response received",
                                      null, duration);
            }

            // Verifica cada campo esperado
            for (Map.Entry<String, String> expected : tc.expectedFields().entrySet()) {
                int de = Integer.parseInt(expected.getKey());
                String actual = response.getString(de);
                if (!expected.getValue().equals(actual)) {
                    return new TestResult(tc.id(), false,
                        String.format("DE%d expected=%s actual=%s",
                                      de, expected.getValue(), actual),
                        response, duration);
                }
            }
            return new TestResult(tc.id(), true, null, response, duration);

        } catch (Exception e) {
            return new TestResult(tc.id(), false,
                "Exception: " + e.getMessage(), null,
                System.currentTimeMillis() - start);
        }
    }

    public void printReport(List<TestResult> results) {
        long passed = results.stream().filter(TestResult::passed).count();
        long failed = results.size() - passed;

        System.out.printf("═══ CERTIFICATION TEST RESULTS ═══%n");
        System.out.printf("Passed: %d / %d%n", passed, results.size());
        System.out.printf("Failed: %d%n", failed);
        System.out.printf("Pass Rate: %.1f%%%n",
                          100.0 * passed / results.size());

        if (failed > 0) {
            System.out.println("\nFAILED TESTS:");
            results.stream()
                   .filter(r -> !r.passed())
                   .forEach(r -> System.out.printf("  [%s] %s%n",
                                                    r.testId(), r.failureReason()));
        }
    }
}
```

### 1.3 Como montar os TestCases a partir de JSON

```java
public class TestDeckLoader {

    /** Carrega test deck de arquivo JSON do tipo:
     * [ { "id": "TC001", "mti": "0100", "fields": {"2": "4111...", "4": "000000010000"},
     *     "expected": {"39": "00"} } ]
     */
    public List<CertificationTestRunner.TestCase> load(Path jsonFile,
                                                        ISOPackager packager) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile.toFile());
        List<CertificationTestRunner.TestCase> deck = new ArrayList<>();

        for (JsonNode node : root) {
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.setMTI(node.get("mti").asText());

            JsonNode fields = node.get("fields");
            fields.fields().forEachRemaining(e ->
                msg.set(Integer.parseInt(e.getKey()), e.getValue().asText()));

            Map<String, String> expected = new LinkedHashMap<>();
            node.get("expected").fields().forEachRemaining(e ->
                expected.put(e.getKey(), e.getValue().asText()));

            deck.add(new CertificationTestRunner.TestCase(
                node.get("id").asText(),
                node.path("description").asText(""),
                msg, expected,
                node.path("expectTimeout").asBoolean(false),
                node.path("category").asText("GENERAL")
            ));
        }
        return deck;
    }
}
```

## 2. ISO 20022 — O Futuro do Clearing

O ISO 20022 é o padrão moderno de mensagens financeiras (XML/JSON). Visa e Mastercard já migraram o clearing para ISO 20022. O PIX é nativamente ISO 20022.

### 2.1 Mapeamento ISO 8583 → ISO 20022

| ISO 8583 Campo | ISO 20022 Path | Observação |
|---------------|----------------|------------|
| DE 2 (PAN) | `/Document/FIToFIPmtSts/TxInfAndSts/OrgnlTxRef/MndtRltdInf/MndtId` | Tokenizado |
| DE 3 (Processing Code) | `/Document/.../Purp/Cd` | Mapeamento não 1:1 |
| DE 4 (Amount) | `/Document/.../IntrBkSttlmAmt` | + moeda ISO 4217 |
| DE 11 (STAN) | `/Document/.../OrgnlEndToEndId` | Chave de correlação |
| DE 12/13 (Datetime) | `/Document/.../IntrBkSttlmDt` | Formato ISO 8601 |
| DE 37 (RRN) | `/Document/.../OrgnlTxRef/Refs/Ref` | |
| DE 38 (Auth Code) | `/Document/.../AddtlInf` (campo proprietário) | Não há campo nativo |
| DE 39 (Response Code) | `/Document/.../TxSts` com `ACSP`/`RJCT` | Mapeamento por tabela |
| DE 41 (Terminal ID) | `/Document/.../InitgPty/Id/OrgId/Othr/Id` | |
| DE 42 (Merchant ID) | `/Document/.../CdtrAgt/FinInstnId/BICFI` | Ou Id proprietário |
| DE 49 (Currency) | `Ccy` attribute em vários campos | ISO 4217 (ex: BRL=986→"BRL") |
| DE 55 (EMV) | `/Document/.../AddtlInf` (TLV base64) | Sem campo nativo padronizado |

### 2.2 Response Code Mapping

| ISO 8583 DE39 | ISO 20022 TxSts | Reason Code ISO 20022 |
|--------------|----------------|-----------------------|
| 00 | ACSP (Accepted) | — |
| 05 | RJCT | AM04 (InsufficientFunds) |
| 14 | RJCT | AC01 (InvalidAccount) |
| 51 | RJCT | AM04 (InsufficientFunds) |
| 54 | RJCT | DT01 (InvalidDate — expired) |
| 55 | RJCT | BE01 (InconsistentWithRecords — PIN) |
| 57 | RJCT | AG07 (UnsuccessfulDirectDebit) |
| 96 | RJCT | AM21 (NotAllowed) |

## 3. Exercícios

1. **Implemente 30 TestCases** cobrindo as 6 categorias do test deck acima e rode com `CertificationTestRunner` contra seu payment-switch-lab
2. **Crie `TestDeckLoader`** que lê casos de um arquivo JSON — facilita manutenção futura
3. **Mapeie 20 campos** ISO 8583 → ISO 20022 com exemplos de valores reais
4. **Implemente response code mapper:** `String toISO20022Status(String de39)` com tabela completa

### Desafio
Rode seu test deck completo. Para cada cenário que falhar, abra um "bug report" com: cenário esperado, o que foi recebido, hipótese da causa raiz, e o fix aplicado. Documente como um time real documentaria uma certificação real.

---

# Semana 28 — Projeto Final

## Entregáveis

### 1. Mini-switch funcional
- [ ] 0800/0810 (echo, sign-on)
- [ ] 0200/0210 (autorização single message)
- [ ] 0100/0110 (autorização dual message)
- [ ] 0400/0410 (reversal)
- [ ] 0220/0230 (advice)
- [ ] Roteamento por BIN (on-us/off-us)
- [ ] Parcelamento (DE 48/60/63)
- [ ] Deduplicação
- [ ] Auto-reversal por timeout
- [ ] Health check de canais
- [ ] Logs mascarados (PCI)
- [ ] Métricas (latência, volume, RC, timeouts)

### 2. Documentação
- [ ] README técnico completo
- [ ] ARCHITECTURE.md com C4 e ADRs
- [ ] Runbook operacional
- [ ] Catálogo de response codes
- [ ] Playbook de troubleshooting
- [ ] Glossário de 50+ termos
- [ ] Diagrama da jornada end-to-end

### 3. Testes
- [ ] > 80% de cobertura
- [ ] Testes unitários por participant
- [ ] Testes de integração E2E
- [ ] Mini test deck com 30+ cenários
- [ ] Testes de falha (timeout, conexão down, duplicata)

### 4. Apresentação
Prepare apresentação do sistema para:
- [ ] Arquiteto (10 min — decisões técnicas)
- [ ] Head de produto (5 min — valor de negócio)
- [ ] Time de operações (10 min — como monitorar e operar)
- [ ] Desenvolvedor júnior (15 min — como funciona)

### Exercício Final
Resolva este incidente simulado do início ao fim:

> "Às 14:32 de sexta-feira, o monitoring alertou que a taxa de timeout subiu de 0.1% para 15% em transações off-us via Visa. Transações on-us e Mastercard estão normais. O time de negócio está cobrando — é Black Friday e estamos perdendo vendas."

1. Qual sua primeira ação?
2. Que dados pede?
3. Qual seu diagnóstico inicial?
4. Qual a correção?
5. Como prevenir no futuro?
6. Como comunica ao negócio?

Documente tudo como se fosse um RCA (Root Cause Analysis) real.
