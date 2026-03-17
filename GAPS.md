# GAPS do Repositório — ISO 8583 Master Class

> Documento vivo. Atualizar ao final de cada sessão.
> Última revisão: 2026-03-17

---

## Como usar este documento

Cada sessão: escolha 1 bloco de uma prioridade, implemente e marque `[x]`.
Quando o bloco inteiro estiver completo, mova para a seção **Concluído** ao final.

**Legenda de tipo:**
- `[CÓDIGO]` — implementação Java faltando (skeleton ou zero)
- `[TEORIA]` — seção teórica rasa ou ausente
- `[EXERCÍCIO]` — exercício sem solução ou sem gabarito
- `[CONSISTÊNCIA]` — referência cruzada quebrada entre semanas

---

## PRIORIDADE 1 — Bloqueadores Críticos

> Sem estes, o pipeline da Fase 2 não funciona e as fases seguintes ficam travadas.

### P1-A: Pipeline TransactionManager (Fase 2 — Semana 7)

O `txn-manager` tem 7 participants como skeleton com `UnsupportedOperationException`. Sem eles, nenhum fluxo E2E funciona.

- [ ] `[CÓDIGO]` `ValidateMessage.prepare()` — validações por MTI (DE3, DE4, Luhn DE2, tamanho DE49)
- [ ] `[CÓDIGO]` `BuildResponse.prepare()` — montar 0210/0410 com DE38/DE39 corretos
- [ ] `[CÓDIGO]` `CheckDuplicate.prepare()` — cache Caffeine, chave STAN+DE41, retorno 94
- [ ] `[CÓDIGO]` `RouteByBIN.prepare()` — consultar BINTable, setar `CARD_ROUTE` e `ISSUER_INST` no contexto
- [ ] `[CÓDIGO]` `AuditLog.prepare()/commit()/abort()` — persistir entrada da txn, resultado e erro
- [ ] `[CÓDIGO]` `ForwardToIssuer.prepare()` — enviar via QMUX, timeout → 91, abort → reversal automático
- [ ] `[CÓDIGO]` `QueryHost.prepare()` — variante stand-in: resposta local quando issuer offline
- [ ] `[EXERCÍCIO]` Exercício integrador: subir `docker-compose`, disparar 0200 e observar fluxo completo no log

**Arquivo alvo:** `payment-switch-lab/txn-manager/src/main/java/`
**Dependência:** Nenhuma — é o ponto de partida.

---

### P1-B: Integration Tests (Fase 2 — Semana 8)

`PaymentSwitchIntegrationTest.java` tem 17 métodos `@Test` com `// TODO`. Sem eles não há feedback automatizado.

- [ ] `[CÓDIGO]` `testSuccessfulPurchase()` — 0200 → 0210 com DE39=00
- [ ] `[CÓDIGO]` `testInsufficientFunds()` — 0200 → 0210 com DE39=51
- [ ] `[CÓDIGO]` `testExpiredCard()` — DE14 no passado → DE39=54
- [ ] `[CÓDIGO]` `testDuplicateTransaction()` — mesmo STAN duas vezes → DE39=94
- [ ] `[CÓDIGO]` `testTimeout()` — issuer simulator delay > threshold → DE39=91
- [ ] `[CÓDIGO]` `testAutoReversal()` — timeout dispara 0400 automático
- [ ] `[CÓDIGO]` `testNetworkSignOn()` — 0800 DE70=001 → 0810 DE39=00
- [ ] `[CÓDIGO]` `testOnUsRouting()` — BIN mapeado → fluxo interno sem QMUX
- [ ] `[CÓDIGO]` Demais 9 cenários (reversal manual, advice, parcelamento, etc.)

**Arquivo alvo:** `payment-switch-lab/integration-tests/src/test/java/`

---

## PRIORIDADE 2 — Reversal, Advice e Network Management

> Fase 4 (Semanas 13–16). Essencial para produção mas sem nenhuma linha de código.

- [ ] `[CÓDIGO]` `ReversalBuilder` — montar DE90 (MTI original 4 dígitos + STAN 6 + DateTime 10 + acquirer/fwd IDs)
- [ ] `[CÓDIGO]` `FindOriginalTransaction` — buscar txn aprovada por STAN+DE41+DE42 no cache/DB
- [ ] `[CÓDIGO]` `ProcessReversal.prepare()` — validar DE90, estornar autorização, retornar 0410
- [ ] `[CÓDIGO]` `AutoReversalEngine` — agendar 0400 quando `ForwardToIssuer.abort()` é chamado
- [ ] `[CÓDIGO]` `AdviceProcessor` — processar 0220/0221, enviar 0230, SAF retry queue
- [ ] `[CÓDIGO]` `NetworkManagementHandler` — responder 0800/0810 DE70 (001=sign-on, 301=echo, 161=cutover)
- [ ] `[TEORIA]` Semana 14 — SAF (Store and Forward): adicionar sequência de retransmissão com exponential backoff
- [ ] `[EXERCÍCIO]` Cenário: txn aprovada + queda de rede antes do 0210 chegar → testar auto-reversal

**Arquivo alvo:** `payment-switch-lab/txn-manager/src/main/java/reversal/`  (criar pacote)

---

## PRIORIDADE 3 — EMV / TLV e PIN Block

> Fase 5 (Semanas 17–18). Bloqueia qualquer transação chip real.

- [ ] `[CÓDIGO]` `TLVParser` — parser genérico BER-TLV para DE55; extrair tag 9F26, 9F27, 82, 9F34, 9A, 9C
- [ ] `[CÓDIGO]` `EMVTagRegistry` — mapear tag hex → nome legível + tipo (binário, numérico, alfanumérico)
- [ ] `[CÓDIGO]` `EMVValidator` — checar ATC sequência, TVR, CVR, IAD presença por fluxo (offline/online/decline)
- [ ] `[CÓDIGO]` `PINBlockBuilder` — ISO 9564 Format 0: XOR(PIN block, PAN block); PAN block = 0x0 + 12 dígitos PAN direita
- [ ] `[CÓDIGO]` `PINBlockValidator` — receber PIN block cifrado, decifrar com ZPK (stub HSM), comparar
- [ ] `[TEORIA]` Semana 17 — adicionar tabela de tags EMV obrigatórias por fluxo (contactless vs. contato)
- [ ] `[EXERCÍCIO]` Exercício: dado DE55 hex real, extrair todas as tags e imprimir em formato legível

**Arquivo alvo:** `payment-switch-lab/iso-core/src/main/java/emv/` (criar pacote)

---

## PRIORIDADE 4 — Reconciliação e Clearing

> Fase 5 (Semana 20) + Fase 6 (Semana 23). Crítico para qualquer operação financeira real.

- [ ] `[CÓDIGO]` `ReconciliationEngine.match()` — cruzar autorização (0200) com clearing (1240) por RRN+DE41+DE42
- [ ] `[CÓDIGO]` `ReconciliationEngine.detectMismatches()` — comparar DE4 autorizado vs. apresentado; detectar:
  - Auth sem clearing (timeout de captura)
  - Clearing sem auth (txn não autorizada)
  - Diferença de valor > tolerância configurável
- [ ] `[CÓDIGO]` `ClearingFileParser` — ler arquivo clearing ISO 8583 batch (MTI 1240/1442) e gerar lista de txns
- [ ] `[CÓDIGO]` `SettlementSummary` — agrupar por acquirer+issuer, calcular líquido interchange
- [ ] `[EXERCÍCIO]` Exercício Semana 23: dado arquivo de autorização + clearing com 3 discrepâncias, encontrá-las

**Arquivo alvo:** `payment-switch-lab/reconciliation/src/main/java/`

---

## PRIORIDADE 5 — Chargebacks e Disputes

> Fase 8 (Semanas 29–30). Cobre fluxos reais que todo switch precisa tratar.

- [ ] `[CÓDIGO]` `ChargebackClassifier` — mapear DE25+DE49+contexto → reason code Visa (10.x, 11.x, 12.x, 13.x) / MC
- [ ] `[CÓDIGO]` `DisputeEvaluator` — regras: tem comprovante? CNP? CVV2 inválido? → decisão defender/aceitar
- [ ] `[CÓDIGO]` `DisputeOrchestrator` — state machine: `NEW → RETRIEVAL → FIRST_CB → REPRESENTMENT → ARBITRATION`
- [ ] `[CÓDIGO]` `RetrievalRequestHandler` — processar MTI 1644 (request) e 1646 (response)
- [ ] `[TEORIA]` Semana 29 — adicionar tabela comparativa: Visa Reason Codes 2025 vs. Mastercard Dispute Categories
- [ ] `[CONSISTÊNCIA]` Desafio Semana 29 referencia `DisputeAutomationEngine` que não existe — criar ou atualizar referência

**Arquivo alvo:** `payment-switch-lab/disputes/` (criar módulo)

---

## PRIORIDADE 6 — Antifraude

> Fase 9 (Semanas 31–32). Necessário para qualquer deploy real.

- [ ] `[CÓDIGO]` `VelocityRuleEngine` — contar txns por PAN/terminal/merchant por janela de tempo (1min, 1h, 24h)
- [ ] `[CÓDIGO]` `FraudScreeningParticipant` — participant jPOS: executar regras e setar `FRAUD_SCORE` no contexto
- [ ] `[CÓDIGO]` `NegativeListParticipant` — checar PAN, terminal ID, merchant ID, BIN, IP contra listas negras (Redis)
- [ ] `[CÓDIGO]` `RiskScoreCalculator` — combinar velocity + negative list + device + geo em score 0–1000
- [ ] `[TEORIA]` Semana 31 — adicionar seção sobre 3DS (3D Secure) e fluxo com DE48 tag PaRes
- [ ] `[EXERCÍCIO]` Semana 32 — exercício de backtesting: dado log de 1000 txns com fraudes marcadas, calcular precision/recall

**Arquivo alvo:** `payment-switch-lab/fraud/` (criar módulo)

---

## PRIORIDADE 7 — Observabilidade e Performance

> Fase 6 (Semana 21) + Fase 10 (Semana 33). Necessário para produção.

- [ ] `[CÓDIGO]` `SwitchMetrics` — implementar com Micrometer: `auth.latency` por MTI, `auth.response_code` por DE39, `routing.decision` por rota
- [ ] `[CÓDIGO]` `ISOLoadTester` — gerador de carga: configurável em TPS, tipos de MTI, perfil de BINs
- [ ] `[CÓDIGO]` `CircuitBreakerParticipant` — Resilience4j por emissor: estado OPEN/CLOSED/HALF_OPEN no contexto jPOS
- [ ] `[CÓDIGO]` `PerformanceBenchmark` — JMH baseline: P50, P95, P99 para fluxo auth completo
- [ ] `[TEORIA]` Semana 33 — adicionar configuração ZGC no `deploy.xml` do jPOS e tuning `minSessions`/`maxSessions`
- [ ] `[EXERCÍCIO]` Semana 21 — criar dashboard Grafana com 5 painéis básicos (latência, TPS, erros, on-us rate, fraud rate)

**Arquivo alvo:** `payment-switch-lab/observability/` + `payment-switch-lab/performance/`

---

## PRIORIDADE 8 — PIX com Cartão de Crédito (Código)

> Fase 7 (Semana 25). Teoria excelente (~1.700 linhas) mas zero implementação.

- [ ] `[CÓDIGO]` `PixCreditDetector` — detectar txn PIX crédito via DE3 + subelementos DE48
- [ ] `[CÓDIGO]` `PixCreditOnUsClassifier` — classificar 4 cenários on-us/off-us (já documentado na teoria seção 7.8)
- [ ] `[CÓDIGO]` `DICTClient` — stub de consulta ao DICT do BACEN via chave PIX → PSP do recebedor
- [ ] `[CÓDIGO]` `SpiLegDispatcher` — disparar Leg 2 (PIX/SPI) após autorização ISO 8583 bem-sucedida
- [ ] `[CÓDIGO]` `PixCreditReconciliation` — cruzar RRN/STAN ISO 8583 com `endToEndId` SPI
- [ ] `[EXERCÍCIO]` Exercício 6 (já criado na teoria) — implementar `PixCreditOnUsClassifier` com os 4 testes de cenário

**Arquivo alvo:** `payment-switch-lab/pix-credit/` (criar módulo)

---

## PRIORIDADE 9 — Teoria Rasa ou Incompleta

> Seções que existem mas precisam de mais profundidade.

- [ ] `[TEORIA]` Semana 14 — SAF: detalhar fila de retransmissão com TTL, dedup na retransmissão, limite de tentativas
- [ ] `[TEORIA]` Semana 17 — EMV: tabela de tags obrigatórias por fluxo (contactless, contact, fallback)
- [ ] `[TEORIA]` Semana 18 — HSM: adicionar fluxo de Key Management (ZMK→ZPK→PEK) e DUKPT derivation tree
- [ ] `[TEORIA]` Semana 27 — PCI-DSS: adicionar checklist SAQ D específico para switch; mapear DE sensíveis → controle PCI
- [ ] `[TEORIA]` Semana 28 — Certificação Visa: detalhar processo de ITF (Integrated Test Facility) e ADVT (Accept/Decline Valid Tests)
- [ ] `[TEORIA]` Semana 31 — 3DS: adicionar fluxo técnico 3DS 2.x com DE48 e mensagens AReq/ARes/CReq/CRes
- [ ] `[TEORIA]` Semana 33 — Performance: adicionar análise de hot path no TransactionManager e onde otimizar I/O

---

## PRIORIDADE 10 — Consistências e Cross-References

> Referências quebradas entre semanas; desafios que citam código inexistente.

- [ ] `[CONSISTÊNCIA]` Desafio Semana 29 cita `DisputeAutomationEngine` — criar stub ou atualizar desafio para usar `DisputeOrchestrator`
- [ ] `[CONSISTÊNCIA]` Desafio Semana 17 cita `EMVTagDecoder` — criar ou renomear para `EMVTagRegistry`
- [ ] `[CONSISTÊNCIA]` Semana 7 teoria menciona `ContextKeys.ISSUER_RESPONSE` mas constante não existe em `ContextKeys.java`
- [ ] `[CONSISTÊNCIA]` `fase-06-e-07-completa.md` — arquivo único para duas fases; separar em `fase-06-completa.md` e `fase-07-completa.md`
- [ ] `[CONSISTÊNCIA]` `fase-07-completa.md` tem apenas 150 linhas para 4 semanas de conteúdo — expandir resumo
- [ ] `[CONSISTÊNCIA]` Exercício 5 (semana 25) menciona `PixCreditDetector` mas Exercício 6 introduz `PixCreditOnUsClassifier` — garantir sequência coerente

---

## Sessões Concluídas

| Data | Sessão | O que foi feito |
|------|--------|----------------|
| 2026-03-17 | Sessão 1 | Criado `GAPS.md`; adicionada seção 7.8 (on-us/off-us PIX crédito) em teoria.md semana 25; adicionado Exercício 6 em exercicios.md semana 25 |

---

## Notas de Arquitetura

- O bloco **P1-A** (pipeline Semana 7) é desbloqueador absoluto — sem ele nenhum teste de integração passa
- **Ordem recomendada de execução:** P1-A → P1-B → P2 → P4 → P3 → P5 → P6 → P7 → P8 → P9 → P10
- Módulos novos (`disputes/`, `fraud/`, `pix-credit/`) devem seguir o padrão Maven do projeto existente e ser adicionados como módulos no `pom.xml` raiz
- Cada item `[CÓDIGO]` deve ter pelo menos 1 teste unitário e ser referenciado em um exercício correspondente
