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

### Cenário A
Dump: `0200B238000108A18000001945320151128303660030000000001500003141600001234561600000314...`
A response é DE39=30. Encontre o erro.

### Cenário B
Todas as transações de BIN `6504xxxx` (Elo Itaú) estão indo off-us pela Elo quando deveriam ser on-us. O que verificar?

### Cenário C
O terminal TERM0099 envia 0800 (echo) com sucesso, mas todas as 0200 retornam timeout. O que está acontecendo?

### Cenário D
Transações com DE22=071 (contactless) estão sendo aprovadas, mas as com DE22=051 (chip contact) do mesmo terminal estão sendo negadas com DE39=55 (PIN errado). O terminal alega que o PIN está correto.

### Cenário E
A reconciliação do dia mostra 47 transações autorizadas sem clearing correspondente. Todas são do merchant MERCHANT00042. O que investigar?

### Desafio
Crie um **playbook de incidente** formatado como runbook para o time de operações. Deve cobrir os 10 incidentes mais comuns com: detecção, diagnóstico, resolução, prevenção.

---

# Semana 23 — Reconciliação Real

## Foco: Implementar reconciliação que detecta mismatches entre autorização e clearing

### Exercícios
1. **Implemente ClearingFileParser** que lê um arquivo de clearing simplificado (CSV com RRN, PAN_last4, amount, date)
2. **Implemente ReconciliationEngine** completo
3. **Gere relatório** de exceções: force posts, auth sem clearing, amount mismatch, PAN mismatch
4. **Calcule totais** para conferência: total autorizado vs total no clearing vs diferença

### Desafio
Processe 10.000 autorizações e 9.800 registros de clearing. Identifique e classifique todas as exceções. Monte relatório para o time financeiro.

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
