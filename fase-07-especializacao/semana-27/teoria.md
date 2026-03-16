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

## 1. Estude e documente

- **Arranjos de pagamento:** Lei 12.865/2013, papel do BACEN
- **Elo:** Quem são os donos (BB, Bradesco, Caixa), specs próprias
- **Hiper/Hipercard:** Bandeira Itaú, processamento on-us
- **Sub-adquirência:** PagSeguro, Mercado Pago, iFood, Ifood
- **Teto de interchange:** Circular BACEN para débito (0.5%)
- **Antecipação de recebíveis:** O que é, regulação, registradoras (CIP, TAG, CERC)
- **PIX vs Cartão:** Onde competem, onde coexistem

## 2. Exercícios

1. **Desenhe o fluxo** de uma transação sub-adquirente: POS iFood → Stone (adquirente) → Visa → Itaú
2. **Calcule a diferença de receita** entre on-us Hiper vs off-us Visa para o Itaú
3. **Documente 10 diferenças** entre o ecossistema de cartões BR vs EUA

### Desafio
Escreva um artigo técnico (LinkedIn-ready) explicando por que parcelamento sem juros é uma peculiaridade brasileira e como impacta a infraestrutura técnica de pagamentos. Mínimo 800 palavras.

---

# Semana 26 — Fluxos Avançados

## 1. Implemente

### Pre-authorization
```
0100 DE25=06 → Pre-auth R$ 2.000 (hotel check-in)
0200 DE25=06 → Completion R$ 1.500 (checkout)
0400 → Reversal da diferença (se necessário)
```

### Incremental Authorization
```
0100 #1 → Pre-auth R$ 2.000
0100 #2 → Incremental +R$ 500 (referencia #1)
0200 → Completion R$ 2.300
```

### Partial Approval
```
0100 DE4=10000 → Request R$ 100
0110 DE4=7500 DE39=10 → Approved R$ 75 (partial)
Terminal: "Aprovado parcial. Deseja pagar R$ 25 com outro meio?"
```

### Balance Inquiry
```
0100 DE3=300000 → Consulta saldo
0110 DE39=00 DE54=1001986C000001500000 → Saldo R$ 15.000,00
```

## 2. Exercícios

1. **Implemente cada fluxo** no payment-switch-lab
2. **Teste cenário de hotel completo:** check-in → minibar → checkout
3. **Teste partial approval:** terminal lida corretamente com valor reduzido
4. **Implemente balance inquiry** com DE 54

### Desafio
Monte um cenário complexo: locadora de veículos. Pre-auth de R$ 5.000, cliente devolve carro com dano (incremental +R$ 2.000), paga R$ 4.500 no checkout, R$ 2.500 fica como chargeback potencial. Quais mensagens são trocadas?

---

# Semana 27 — Certificação e ISO 20022

## 1. Certificação com Bandeiras

- **Test deck:** Conjunto de ~200-500 cenários de teste
- Cada cenário: "envie esta mensagem, espere esta resposta"
- Inclui: happy path, declines, reversals, timeouts, EMV, contactless, recurring
- **Precisa passar 100%** para ir a produção

## 2. ISO 20022 — O Futuro

- XML/JSON based (vs binário do ISO 8583)
- Visa e Mastercard migrando clearing para ISO 20022
- PIX já é ISO 20022 nativo
- Mapeamento ISO 8583 ↔ ISO 20022 é habilidade valiosa

```
ISO 8583 DE 2 (PAN) → ISO 20022 /AcctId/IBAN ou /Acct/Id/Othr/Id
ISO 8583 DE 4 (Amount) → ISO 20022 /IntrBkSttlmAmt
ISO 8583 DE 39 (Response Code) → ISO 20022 /TxSts
```

## 3. Exercícios

1. **Crie um "mini test deck"** com 30 cenários e implemente runner automático
2. **Documente o processo de certificação** Visa e Mastercard (públicamente disponível)
3. **Mapeie 10 campos** ISO 8583 → ISO 20022

### Desafio
Rode seu mini test deck contra o payment-switch-lab. Quantos cenários passam? Corrija os que falham. Meta: 100%.

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
