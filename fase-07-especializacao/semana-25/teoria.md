# Semana 25 — Mercado Brasileiro Profundo

## Por que esta semana é diferente das anteriores?

Nas semanas 1-24 você aprendeu o protocolo. Agora começa a especialização real. O mercado brasileiro de pagamentos tem particularidades que não existem em nenhum outro país do mundo — e dominar essas particularidades é o que separa um implementador de um **especialista referência**.

---

## 1. A Regulação que Moldou Tudo: Lei 12.865/2013

A Lei 12.865 criou o marco regulatório dos arranjos de pagamento no Brasil. Entender essa lei é entender por que o mercado funciona como funciona.

### 1.1 O que a lei fez

Antes de 2013: Visa e Mastercard operavam sem regulação específica. Cielo e Rede tinham exclusividade de bandeiras — Cielo só aceitava Visa, Rede só aceitava Mastercard. O lojista não tinha escolha.

Depois de 2013:
- BACEN passou a regular e supervisionar arranjos de pagamento
- Exclusividade entre adquirentes e bandeiras foi proibida
- Credenciamento cruzado se tornou obrigatório
- Novos entrantes (Stone, PagSeguro, Getnet) puderam competir
- Emissores foram separados regulatoriamente de adquirentes

**Impacto técnico direto:** O mesmo terminal passou a precisar suportar múltiplas bandeiras. O switch do adquirente precisa rotear por bandeira, não apenas por BIN isolado.

### 1.2 Arranjos de Pagamento — O que são

Um **arranjo de pagamento** é o conjunto de regras, procedimentos e infraestrutura que permite transferência de recursos entre pagadores e recebedores. O BACEN autoriza e supervisiona cada arranjo.

```
Arranjos autorizados no Brasil (principais):
  Visa (Visa do Brasil Arranjos de Pagamento Ltda)
  Mastercard (Mastercard Brasil Soluções de Pagamento Ltda)
  Elo (Elo Serviços S.A.)
  American Express (Amex do Brasil)
  Hipercard (Hipercard Administradora de Cartões)
  PIX (arranjo do próprio BACEN — Banco Central)
```

### 1.3 Interoperabilidade obrigatória

O BACEN exige interoperabilidade. Na prática:
- Todo credenciador deve aceitar todos os arranjos autorizados
- Todo emissor deve participar de pelo menos um arranjo
- Portabilidade de domicílio bancário é obrigatória

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

### Catálogo de Falhas — 15 cenários essenciais

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
| 11 | CVV2 failure em massa (DE39=N7) | Campo DE48 com CVV2 em posição errada | Comparar DE48 parse com spec da bandeira | Corrigir subelemento de CVV2 no parser |
| 12 | Stand-in aprovando fora dos limites | Critérios do stand-in mal configurados | Verificar regras de stand-in vs política | Revisar limite de valor e BINs elegíveis |
| 13 | Contactless caindo para fallback chip | Floor limit do terminal desatualizado | Verificar DE60 ou parâmetros do terminal | Atualizar floor limit via parametrização remota |
| 14 | Valor arredondado incorreto em multi-moeda | Rounding mode errado na conversão | Comparar DE4 vs DE6 (cardholder billing) | Usar `HALF_UP` e atentar ao exponent da moeda |
| 15 | Memory leak em produção após 48h | `LateResponseDetector.pendingSTANs` crescendo sem expirar | Monitorar heap + size do map | Adicionar TTL no cleanup: `pendingSTANs.entrySet().removeIf(e → e.getValue().isBefore(cutoff))` |

---

### Cenário F — CVV2 Failure em Massa

```
Sintoma: Taxa de DE39=N7 (CVV2 failure) subindo de 0.1% para 8% em transações CNP
Todos os merchants afetados. Chip e débito funcionando normalmente.
```

**Diagnóstico:**
```
1. N7 é um response code proprietário para CVV2 no match
2. Afeta CNP → envolve DE48 (Additional Data) que carrega o resultado do CVV2
3. Isolado a CNP → problema na forma como o CVV2 está sendo enviado

Passos:
1. Hex dump de uma transação CNP com N7
2. Comparar DE48 com spec da bandeira
   → Visa: CVV2 fica em DE48 subelemento 92 ou campo específico
   → Mastercard: DE48 subelemento 1E
3. Verificar se houve deploy recente no sistema de montagem da mensagem
4. Comparar o DE48 de uma transação que passa (DE39=00) vs uma que falha (N7)
```

**Causa raiz típica:** Deploy com mudança no mapeamento do DE48 que deslocou o subelemento de CVV2 uma posição. O emissor lê CVV2 em posição errada → no match → N7.

**Correção:** Rollback do deploy ou fix emergencial no parser de DE48. Validar com 5 transações de teste antes de liberar.

---

### Cenário G — Stand-in Aprovando Além do Limite

```
Sintoma: Transações de R$ 5.000+ estão sendo aprovadas mesmo com o emissor fora
Política de stand-in define limite máximo de R$ 500
```

**Diagnóstico:**
```
1. Verificar qual componente está executando o stand-in
2. Verificar configuração do stand-in: arquivo XML ou banco de dados?
3. Confirmar que a configuração atual foi recarregada após o deploy

Chave de configuração típica (stand-in):
  standin.max_amount=50000        ← em centavos, R$ 500,00
  standin.eligible_networks=VISA,MASTERCARD,ELO
  standin.exclude_mcc=6011,6012   ← ATM/saque nunca em stand-in
```

**Causa raiz típica:** Configuração de stand-in em cache não foi invalidada após update no banco. Stand-in rodando com valores do cache antigo.

**Correção:** Forçar reload da configuração + audit de todas as transações aprovadas em stand-in acima do limite. Emitir reversals das que estão além da política.

---

### Cenário H — Contactless Caindo para Fallback Chip

```
Sintoma: 30% das transações contactless estão sendo processadas como chip contact
Terminal Verifone VX520 em loja específica. Outros terminais OK.
```

**Diagnóstico:**
```
1. Contactless tem "floor limit" configurado no terminal
   → Acima do floor limit: requer contactless com CVM (PIN ou assinatura)
   → Abaixo: "tap and go" sem CVM
2. Se o terminal está caindo para chip, o valor pode estar acima do floor limit
   E o portador não está fazendo PIN contactless
3. Verificar DE22 nas transações problemáticas:
   07 = contactless (EMV)
   05 = chip contact  ← se está aparecendo aqui, caiu para fallback

Verificar parâmetros do terminal:
  - CTLS Floor Limit atual vs recomendado pela bandeira
  - Contactless CVM limit (Brasil: R$ 200,00 tipicamente)
```

**Causa raiz típica:** Floor limit desatualizado após última parametrização (ex: atualizado para R$ 50 quando deveria ser R$ 200). Acima de R$ 50, terminal exige PIN contactless; se portador remove o cartão antes do PIN, cai para chip.

**Correção:** Reparametrizar terminal com floor limit correto (via TMS — Terminal Management System).

---

### Cenário I — Valor com Arredondamento Incorreto em Multi-Moeda

```
Sintoma: Reclamações de portadores de cartão em USD sendo cobrados R$ 0,01 a mais
Transações em EUR funcionando normalmente
```

**Diagnóstico:**
```
DE4  = Transaction Amount (moeda do merchant)
DE6  = Cardholder Billing Amount (moeda do portador)
DE49 = Transaction Currency (986=BRL, 840=USD, 978=EUR)
DE51 = Cardholder Billing Currency

Regra: conversão deve usar a taxa de câmbio da bandeira + rounding definido pela ISO 4217
USD tem 2 decimal places (exponent=2)
BRL tem 2 decimal places (exponent=2)

O problema: conversão usando HALF_DOWN ao invés de HALF_UP?
Ou: taxa de câmbio com precisão insuficiente (4 casas vs 6 necessárias)?
```

**Código problemático:**
```java
// ERRADO: HALF_DOWN pode causar centavo a menos
BigDecimal converted = amount.multiply(rate)
                              .setScale(2, RoundingMode.HALF_DOWN);

// CORRETO: HALF_UP é o padrão da indústria para valores monetários
BigDecimal converted = amount.multiply(rate)
                              .setScale(2, RoundingMode.HALF_UP);
```

**Correção:** Corrigir `RoundingMode` para `HALF_UP` em todos os conversores de moeda. Verificar se a taxa de câmbio tem pelo menos 6 casas decimais antes do arredondamento.

---

### Cenário J — Memory Leak no LateResponseDetector

```
Sintoma: Switch reiniciando automaticamente a cada 48-72h com OutOfMemoryError
Heap dump mostra ConcurrentHashMap com 2M+ entradas no LateResponseDetector
```

**Diagnóstico:**
```
LateResponseDetector mantém mapa de STANs pendentes:
  Map<String, Instant> pendingSTANs = new ConcurrentHashMap<>();

Problema: entradas são ADICIONADAS quando a transação sai para o emissor
e REMOVIDAS quando a resposta chega. Mas e quando há timeout?
- A transação faz timeout → gera reversal
- O reversal é enviado → a entrada deveria ser removida
- Mas se o reversal também não responde... a entrada fica para sempre

Volume de 100k txns/h → 2M entradas em 20h → OOM
```

**Correção:**

```java
// Adicionar cleanup periódico com TTL
@Scheduled(fixedDelay = 60_000) // a cada 1 minuto
public void cleanupExpiredEntries() {
    Instant cutoff = Instant.now().minus(2, ChronoUnit.HOURS);
    int removedCount = 0;
    Iterator<Map.Entry<String, Instant>> it = pendingSTANs.entrySet().iterator();
    while (it.hasNext()) {
        if (it.next().getValue().isBefore(cutoff)) {
            it.remove();
            removedCount++;
        }
    }
    if (removedCount > 0) {
        log.info("LateResponseDetector: {} expired entries removed", removedCount);
        metrics.counter("late_response.cleanup").increment(removedCount);
    }
}
```

**Prevenção:** Adicionar métrica de tamanho do mapa ao SwitchMetrics:
```java
Gauge.builder("late_response.pending_count", pendingSTANs, Map::size)
     .register(registry);
```
Alertar quando passar de 10.000 entradas.

### Desafio
Crie um **playbook de incidente** formatado como runbook para o time de operações. Deve cobrir os 15 incidentes acima com: detecção (qual alerta dispara), diagnóstico (queries/comandos a executar), resolução (passos), verificação (como confirmar que está resolvido), e prevenção (o que mudar para não acontecer de novo).

O Elo usa especificação própria baseada em ISO 8583, com extensões relevantes:

| Campo | Visa/Master | Elo | Diferença |
|-------|-------------|-----|-----------|
| DE 48 | Subelementos proprietários | Subelementos próprios Elo | Layout diferente |
| DE 60 | Pouco usado | Central para parcelamento | Campos de parcelas Elo |
| DE 62 | Visa-specific | Elo-specific | Layout diferente |
| Response codes | Padrão ISO | Maioria igual + 8xx próprios | Alguns códigos específicos |
| Parcelamento | DE 48 principalmente | DE 60 principalmente | Especificação Elo |

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

Elo tem a maior taxa de transações on-us do mercado porque:
- Bradesco emite Elo → Cielo (controlada pelo Bradesco) adquire → rota on-us
- Caixa emite Elo → terminal próprio Caixa → rota on-us
- BB emite Elo → terminal BB → rota on-us

Menor custo de interchange para essas instituições e menor latência.

---

## 3. Teto de Interchange — A Decisão do BACEN

### 3.1 A regulação

O BACEN impôs teto de interchange em débito:
- **Débito:** máximo 0,5% (Resolução BCB nº 150/2021)
- **Pré-pago:** máximo 0,5%
- **Crédito à vista:** sem teto definido regulatório ainda (mercado pratica ~1,5%)
- **Crédito parcelado:** sem teto (mercado pratica 1,8–2,2% dependendo das parcelas)

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

| Caso de uso | PIX Tradicional | PIX com Crédito | Cartão Débito | Cartão Crédito |
|-------------|-----------------|-----------------|---------------|----------------|
| Transferência P2P | Melhor | N/A | Ruim | N/A |
| Compra presencial baixo valor | Ótimo | Bom | Bom | Bom |
| Compra online | Possível (QR) | Bom | Bom | Ótimo (proteção) |
| Parcelamento | Não existe | Sim (PIX Parcelado) | Não existe | Único |
| Chargeback/proteção | Fraco (MED) | Médio (rede cartão) | Fraco | Forte |
| Velocidade de liquidação | D+0 (merchant) | D+0 (merchant) | D+1 | D+30 |
| Custo para merchant | ~0% | ~1,5–3,5% | ~1,5–2% | ~2–4% |
| IOF para portador | Não | Sim | Não | Sim |

**Conclusão:** O PIX com cartão de crédito surgiu para preencher o gap entre PIX (custo zero, D+0) e crédito (prazo para portador). Para o merchant, é o melhor de dois mundos: recebe D+0 mas aceita portadores sem saldo imediato. Ver seção 7 para detalhes técnicos completos.

---

## 7. PIX com Cartão de Crédito — O Produto Híbrido

### 7.1 O que é e por que surgiu

O **PIX com cartão de crédito** (também chamado de **PIX Garantido** ou **PIX Crédito**) é uma modalidade híbrida em que o pagador usa a chave PIX para iniciar a transação, mas os fundos são debitados do **limite do cartão de crédito** — não do saldo em conta corrente.

O Banco Central regulamentou essa modalidade por meio da **Resolução BCB nº 195/2022**, que autorizou instituições financeiras participantes do SPI (Sistema de Pagamentos Instantâneos) a oferecer linhas de crédito vinculadas a transações PIX. O produto combina:

- **Para o recebedor:** crédito instantâneo via PIX (D+0), sem alteração no fluxo de recebimento
- **Para o pagador:** uso do limite do cartão, cobrança na fatura (D+30) ou em parcelas
- **Para o emissor:** nova fonte de receita de crédito sem depender de terminal físico

---

### 7.2 Modalidades

| Modalidade | Descrição | Liquidação ao merchant | Cobrança do portador |
|-----------|-----------|------------------------|----------------------|
| **PIX Crédito à vista** | Débito integral na fatura | D+0 (imediato) | D+30 (fatura mensal) |
| **PIX Parcelado sem juros** | Portador paga em N parcelas; lojista absorve custo | D+0 (imediato) | N parcelas mensais fixas |
| **PIX Parcelado com juros** | Financiamento pelo emissor | D+0 (imediato) | N parcelas + juros (IOF + taxas) |

> **Diferencial competitivo:** o merchant recebe instantaneamente (como PIX tradicional), mas o portador tem prazo (como cartão de crédito). Isso eliminou o principal obstáculo do PIX para compras de alto valor.

---

### 7.3 Fluxo Técnico — Dual Leg (ISO 8583 + SPI)

O processamento envolve **duas pernas** independentes:

```
Portador              PSP/Emissor              Rede de Cartão        Recebedor
   │                      │                         │                    │
   │── Inicia PIX Crédito ►│                         │                    │
   │   (chave PIX +        │──── 0200 ISO 8583 ──────►│                    │
   │    escolhe crédito)   │  DE3=003000             │                    │
   │                      │  DE48=PIX_KEY+TXID       │                    │
   │                      │◄─── 0210 DE39=00 ────────│                    │
   │                      │                         │                    │
   │                      │──── PIX via SPI ──────────────────────────────►│
   │                      │    (endToEndId correlacionado ao STAN)  (D+0 crédito)
   │◄── Confirmação ───────│                         │                    │
```

**Leg 1 — Autorização ISO 8583:**
- PSP do pagador envia 0200 para a bandeira (Visa, Master, Elo)
- Bandeira roteia para o emissor, que autoriza o uso do limite de crédito
- DE3 primeiros 2 dígitos `00` = compra / `20` = devolução crédito
- DE48 ou campos privados da bandeira carregam chave DICT, txid PIX e tipo de transação

**Leg 2 — Liquidação PIX/SPI:**
- Após DE39=00 na Leg 1, o emissor inicia um PIX no SPI para o recebedor
- Valor creditado na conta do recebedor em até 10 segundos
- O recebedor vê apenas um crédito PIX — sem visibilidade sobre o funding vir de cartão

**Risco de idempotência:** se Leg 1 for aprovada mas Leg 2 falhar, o sistema deve garantir reenvio do PIX sem nova cobrança ao portador. Esse é o principal desafio operacional da modalidade.

---

### 7.4 Campos ISO 8583 Envolvidos

| Campo | Descrição no PIX Crédito |
|-------|--------------------------|
| **DE3 (Processing Code)** | `003000` compra crédito; `203000` devolução/crédito ao portador |
| **DE4 (Amount)** | Valor total da transação |
| **DE22 (POS Entry Mode)** | `010` credencial digitalizada/e-wallet; `812` QR Code via app PSP |
| **DE48 (Additional Data)** | Subelementos com: chave DICT do recebedor, tipo PIX (`CHAVE`, `QRCODE`, `COPIA_COLA`), txid e correlação SPI |
| **DE61 (POS Data)** | Ambiente remoto para transações iniciadas via app (sem terminal físico) |
| **DE63 (Network Data)** | Campos de bandeira para trafegar `endToEndId` e metadados SPI |
| **DE125 / DE127** | Campos Elo/privados para dados complementares do arranjo PIX |

> O `endToEndId` gerado no SPI deve ser correlacionado com o STAN ISO 8583 para reconciliação entre as duas pernas. Sem esse vínculo, a auditoria das operações fica comprometida.

---

### 7.5 Modelo Econômico

O PIX com cartão de crédito **muda radicalmente o modelo econômico** comparado ao PIX tradicional:

```
PIX Tradicional:
  MDR ≈ 0% (arranjo BACEN, custo zero regulatório para merchant)
  Interchange: não existe
  Liquidação ao merchant: D+0 ou D+1 útil

PIX com Cartão de Crédito:
  MDR ≈ 1,5–3,5% (similar ao crédito tradicional)
  Interchange: tabela da bandeira (~0,7% à vista, 1,8–2,2% parcelado)
  Liquidação ao merchant: D+0 (via PIX)
  IOF: 0,38% fixo + 0,0082%/dia sobre o valor do crédito
```

**Fluxo de receita por participante:**

| Participante | O que recebe | Quem paga |
|-------------|--------------|-----------|
| Emissor | Interchange + spread de crédito/juros | Adquirente / portador |
| Bandeira | Assessment fee (~0,1%) | Adquirente |
| Adquirente/PSP | MDR − interchange − assessment | Merchant |
| Merchant | Valor líquido (D+0) | — |
| Portador | — | IOF + eventual juros |

---

### 7.6 Chargebacks e Disputes

O PIX crédito introduz complexidade nos disputes inexistente no PIX tradicional:

```
PIX Tradicional (sem crédito):
  - Sem chargeback via rede de cartão
  - Devolução via SPI: MED (Mecanismo Especial de Devolução)
  - Prazo MED: até 90 dias (fraude) ou D+1 (erro operacional)

PIX com Cartão de Crédito:
  - Chargeback via rede de cartão (reason codes Visa 10.x/13.x, Mastercard 4853/4863, Elo)
  - Merchant recebeu via PIX (irrevogável), mas pode ter chargeback retroativo
  - Dispute gerenciado pelo lado ISO 8583, não pelo SPI
  - Merchant deve preservar: endToEndId SPI + dados ISO da transação
```

**Cenário crítico — exposição do emissor:**
1. Portador contesta transação via chargeback na rede de cartão
2. Merchant perde o chargeback (sem evidência suficiente)
3. O valor PIX já foi recebido pelo merchant (irrevogável no SPI)
4. Resultado: o emissor absorve a perda (estorna o portador e não recupera o PIX)

Por isso, emissores aplicam **regras de risco mais rigorosas** (velocity, device binding, score mínimo) para autorizar PIX crédito do que para PIX tradicional.

---

### 7.7 Impacto no Switch — Novos Participants

Para suportar PIX com cartão de crédito no switch jPOS, são necessários novos participants no TransactionManager:

```java
// Participant: identifica e marca transações PIX Crédito no contexto
public class PixCreditIdentifierParticipant implements TransactionParticipant {
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = (ISOMsg) ctx.get("REQUEST");

        String de48 = msg.getString(48);
        boolean isPixCredit = de48 != null && de48.contains("PIXCRED");

        if (isPixCredit) {
            ctx.put("TX_TYPE", "PIX_CREDIT");
            ctx.put("PIX_KEY", extractPixKey(de48));   // chave DICT do recebedor
            ctx.put("PIX_TXID", extractTxId(de48));    // txid para idempotência SPI
        }
        return PREPARED;
    }
}

// Participant: após aprovação ISO 8583, dispara o PIX via SPI
public class SpiDispatchParticipant implements TransactionParticipant {
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        if (!"PIX_CREDIT".equals(ctx.get("TX_TYPE"))) return PREPARED;

        String pixKey  = (String) ctx.get("PIX_KEY");
        String txid    = (String) ctx.get("PIX_TXID");
        BigDecimal amt = (BigDecimal) ctx.get("AMOUNT");

        // Disparo assíncrono; guarda endToEndId para correlação e reconciliação
        String endToEndId = spiClient.sendPixCredit(pixKey, amt, txid);
        ctx.put("SPI_END_TO_END_ID", endToEndId);
        return PREPARED;
    }

    @Override
    public void abort(long id, Serializable context) {
        // ISO 8583 falhou APÓS SPI enviado: solicitar devolução no SPI
        String endToEndId = (String) ((Context) context).get("SPI_END_TO_END_ID");
        if (endToEndId != null) {
            spiClient.requestRefund(endToEndId, "FALHA_AUTORIZACAO_ISO");
        }
    }
}
```

**Reconciliação dual-leg:**

```
Clearing ISO 8583          Extrato SPI
  RRN:  123456789012   ←→   endToEndId: E9999...2024abc
  STAN: 001234              txid: abc123
  DE39: 00 (aprovado)       status: LIQUIDADO
  Valor: R$ 500,00          Valor: R$ 500,00
```

A reconciliação cruza RRN/STAN com `endToEndId` para garantir que cada autorização ISO 8583 tem seu PIX correspondente liquidado no SPI. Divergências geram alertas de reconciliação para tratamento manual.

---

### 7.8 On-Us e Off-Us no PIX com Cartão de Crédito

No PIX com cartão de crédito, o conceito de on-us/off-us existe em **duas dimensões independentes** — uma para cada perna do processamento dual-leg.

#### Dimensão 1 — Cartão (Leg ISO 8583)

Igual ao cartão tradicional: compara o PSP/adquirente iniciador da transação com o emissor do cartão.

```
On-us cartão:  PSP iniciador == emissor do cartão
               (ex: app Itaú inicia PIX Crédito com cartão Itaú)
               → autorização interna, sem hop de rede externa

Off-us cartão: PSP iniciador != emissor do cartão
               (ex: app PicPay inicia PIX Crédito com cartão Bradesco)
               → mensagem ISO 8583 vai pela Visa/Master/Elo até o Bradesco
```

**Impacto on-us cartão:**
- **Interchange:** zero (ou tabela interna — não paga à bandeira)
- **Latência Leg 1:** menor (sem round-trip de rede externa)
- **Risco de crédito:** avaliado internamente (sem regras de bandeira)

#### Dimensão 2 — PIX (Leg SPI)

Compara o emissor (que envia o PIX após a autorização) com o PSP do recebedor.

```
On-us PIX:  emissor == PSP do recebedor
            (ex: Itaú autorizou o crédito e o recebedor tem conta Itaú)
            → transferência interna, sem liquidação no STR/SPI do BACEN

Off-us PIX: emissor != PSP do recebedor
            (ex: Itaú autorizou o crédito e o recebedor tem conta Nubank)
            → PIX vai pelo SPI (Sistema de Pagamentos Instantâneos)
            → liquidação no STR com custo de reserva bancária
```

**Impacto on-us PIX:**
- **Custo de liquidação:** sem tarifa STR (economicamente relevante em alto volume)
- **Latência Leg 2:** menor (sem round-trip SPI)
- **Disponibilidade:** não depende do SPI estar disponível (janela de manutenção)

#### Matriz dos 4 Cenários

| Cartão | PIX | Exemplo real | Interchange | Custo STR | Complexidade |
|--------|-----|-------------|-------------|-----------|--------------|
| **On-us** | **On-us** | Itaú inicia → cartão Itaú → recebedor Itaú | Zero | Zero | Menor |
| **On-us** | **Off-us** | Itaú inicia → cartão Itaú → recebedor Nubank | Zero | Sim | Média |
| **Off-us** | **On-us** | PicPay → cartão Bradesco → recebedor Bradesco | Sim (Visa/Master) | Zero | Média |
| **Off-us** | **Off-us** | PicPay → cartão Bradesco → recebedor Nubank | Sim (Visa/Master) | Sim | Maior |

> O cenário **off-us cartão + off-us PIX** é o mais custoso e mais comum no mercado, pois o ecossistema brasileiro é altamente fragmentado (múltiplos emissores, múltiplos PSPs).

#### Impacto no Switch — Roteamento Dual

O switch precisa identificar ambas as dimensões para rotear corretamente:

```java
public class PixCreditRoutingParticipant implements TransactionParticipant {
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        if (!"PIX_CREDIT".equals(ctx.get("TX_TYPE"))) return PREPARED;

        String issuerBin    = extractIssuerBin(ctx);   // BIN do cartão (DE2)
        String acquirerInst = (String) ctx.get("ACQUIRER_INST");
        String payeeBank    = resolvePayeeBank((String) ctx.get("PIX_KEY")); // via DICT

        // Dimensão 1: on-us/off-us do cartão
        boolean cardOnUs = acquirerInst.equals(binTable.resolveIssuer(issuerBin));
        ctx.put("CARD_ON_US", cardOnUs);

        // Dimensão 2: on-us/off-us do PIX
        boolean pixOnUs = acquirerInst.equals(payeeBank);
        ctx.put("PIX_ON_US", pixOnUs);

        // Rota Leg 1
        ctx.put("CARD_ROUTE", cardOnUs ? "INTERNAL_ISSUER" : "CARD_NETWORK");

        // Rota Leg 2
        ctx.put("PIX_ROUTE", pixOnUs ? "INTERNAL_TRANSFER" : "SPI");

        return PREPARED;
    }
}
```

#### On-Us Cartão — Simplificação do Fluxo

Quando o cartão é on-us, a Leg 1 não precisa sair para a rede de cartão:

```
Off-us cartão (fluxo padrão):
  PSP Iniciador → Bandeira (Visa/Master/Elo) → Emissor → Bandeira → PSP

On-us cartão (fluxo simplificado):
  PSP Iniciador → Emissor (interno)
  (sem hop de rede; PSP e emissor são a mesma instituição)
```

Isso é economicamente relevante: grandes bancos (Itaú, Bradesco, BB) têm volumes altos de on-us e economizam o assessment fee da bandeira nessas transações.

---

## 8. Parcelamento — A Peculiaridade Brasileira

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
3. **Chargeback:** o portador pode contestar parcelas individuais ou o total
4. **Antecipação:** merchant pode antecipar parcelas futuras

## 9. Exercícios

1. **Desenhe o fluxo** de uma transação sub-adquirente: App iFood → Stone (sub-acq) → Cielo (acq) → Elo → Itaú
2. **Implemente `SoftDescriptorBuilder`** com testes: garanta que nomes de 30+ caracteres são truncados corretamente
3. **Calcule a receita** de uma transação de R$ 100 crédito à vista: quanto fica com o emissor, bandeira, adquirente e merchant? (Assuma MDR 2.5%, interchange 0.7%, assessment 0.1%)
4. **Documente 5 diferenças** entre certificação Elo (CELO) e certificação Visa (VCMS)
5. **Compare os modelos econômicos:** para uma transação de R$ 500 em PIX tradicional vs PIX Crédito à vista (MDR 2,0%, interchange 0,7%), calcule quanto o merchant recebe líquido em cada modalidade e o custo total para o portador (considerando IOF de 0,38%).

### Desafio
Implemente `InterchangeCalculator` com tabela completa (débito, crédito à vista, parcelado 2-6x, parcelado 7-12x, PIX crédito à vista, PIX parcelado) para as bandeiras Visa, Mastercard e Elo. Adicione lógica de on-us detection baseada em BIN do emissor vs BIN do adquirente.

### 4.1 O problema que resolve

Lojista vende R$ 100.000 em 12x sem juros em outubro. Receberia:
- Novembro: R$ 8.333
- Dezembro: R$ 8.333
- ... outubro do ano seguinte: R$ 8.333

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

## 6. DREX — O Real Digital

### 6.1 O que é

DREX é a CBDC (Central Bank Digital Currency) do Brasil, desenvolvida pelo BACEN em blockchain privada (Hyperledger Besu). Fase piloto em 2024-2025 com instituições selecionadas.

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

## Resumo da Semana

| Tema | O que você deve dominar |
|------|------------------------|
| Lei 12.865 | Marco regulatório, proibição de exclusividade, abertura do mercado |
| Arranjos | O BACEN autoriza cada arranjo, todos devem ser interoperáveis |
| Elo | Bandeira nacional, ISO 8583 com extensões próprias, alta taxa on-us |
| Interchange | Teto 0,5% débito, impacto em emissores e MDR, tabelas no switch |
| Recebíveis | Parcelas registradas em CIP/CERC/TAG, portabilidade obrigatória |
| Open Finance | TPP, PISP, nova camada sobre ISO 8583, convergência com PIX |
| DREX | CBDC em piloto, não usa ISO 8583, não substitui cartões no médio prazo |
| Sub-adquirência | Regulação BCB 80/2021, responsabilidade do adquirente master |
