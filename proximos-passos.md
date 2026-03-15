# Próximos Passos — O que Estudar Depois das 36 Semanas

Você dominou ISO 8583, jPOS, antifraude, performance e o ecossistema brasileiro. O que vem depois?

Este guia mapeia as fronteiras do conhecimento em pagamentos — o que está em transformação, o que está emergindo e o que abre as maiores oportunidades de carreira para quem já tem a base sólida.

---

## Como usar este guia

Cada tópico tem:
- **Prioridade** — o quanto impacta a carreira no curto prazo
- **Pré-requisito** — o que do curso base você precisa dominar primeiro
- **Horizonte** — quanto tempo de estudo dedicado para fluência básica

Não tente absorver tudo ao mesmo tempo. Escolha 1 trilha principal e 1 trilha complementar.

---

## Trilha 1 — Evolução de Protocolo

### ISO 20022 — O Futuro da Mensageria Financeira

**Prioridade:** 🔴 Alta — migração em andamento em produção
**Pré-requisito:** Dominar ISO 8583 completamente (semanas 1-4)
**Horizonte:** 3-6 meses para fluência prática

ISO 20022 é o protocolo que está substituindo SWIFT MT, é a base técnica do PIX, e será obrigatório nas principais infraestruturas financeiras globais até 2025-2026.

```
Diferenças fundamentais vs ISO 8583:
  ISO 8583                     ISO 20022
  ─────────────────────────────────────────────
  Campos posicionais (DE 1-128) XML/JSON estruturado
  Bitmap para indicar presença  Schema XSD/OpenAPI
  Binário compacto              Verboso mas autodescritivo
  Spec por bandeira             Spec global (ISO)
  Voltado a cartões             Agnóstico a instrumento

Mensagens equivalentes:
  0200 (auth request)   → pacs.008 (FI Credit Transfer)
  0210 (auth response)  → pacs.002 (Payment Status Report)
  0400 (reversal)       → camt.056 (Cancel Payment)
  0800 (network mgmt)   → admi.002 (System Event Notification)
```

**Onde ISO 20022 já está em produção no Brasil:**
- PIX (Bacen DICT, SPI — toda a infra é ISO 20022 internamente)
- SWIFT CBPR+ (cross-border payments entre bancos)
- SILOC/SITRAF (câmaras de clearing)

**Recursos para começar:**
- Especificação oficial: `iso20022.org/catalogue-messages`
- Mapeamento ISO 8583 ↔ ISO 20022: `swift.com/standards`
- Implementação Java: biblioteca `prowide-iso20022`

---

### nexo ASC — O Substituto do ISO 8583 no Terminal

**Prioridade:** 🟡 Média — relevante para quem trabalha com acquirer/terminal
**Pré-requisito:** ISO 8583 completo + EMV (semanas 17-18)
**Horizonte:** 2-4 meses

nexo ASC (Application Standards Committee) é um conjunto de protocolos da NFC Forum/nexo que define a comunicação entre terminal (POS) e sistema de pagamento usando XML sobre TLS — substituindo o ISO 8583 clássico na camada terminal-adquirente.

```
Família nexo:
  nexo FAST    — protocolo terminal ↔ acquirer (substitui ISO 8583 TMS)
  nexo RETAIL  — protocolo terminal ↔ caixa da loja (EFT/POS)
  nexo TOKEN   — tokenização no terminal

Mensagem de exemplo (nexo FAST — autorização):
  <SaleToPOIRequest>
    <MessageHeader ProtocolVersion="3.1"
                   MessageCategory="Payment"
                   MessageType="Request" ... />
    <PaymentRequest>
      <SaleData>
        <SaleTransactionID TransactionID="TXN001"
                           TimeStamp="2024-01-15T14:30:00"/>
      </SaleData>
      <PaymentTransaction>
        <AmountsReq Currency="BRL" RequestedAmount="150.00"/>
      </PaymentTransaction>
    </PaymentRequest>
  </SaleToPOIRequest>
```

---

## Trilha 2 — Infraestrutura Cloud-Native

### Arquitetura Cloud-Native de Switches

**Prioridade:** 🔴 Alta — é o que as fintechs modernas constroem
**Pré-requisito:** Fase 10 (performance) + arquitetura de switch (semana 24)
**Horizonte:** 4-6 meses

```
Stack moderno de switch de pagamentos:

  Camada de entrada:
    Kubernetes (EKS/GKE) + Istio (service mesh)
    → mTLS entre serviços, circuit breaker no mesh
    → Canary deploy para novas versões do switch

  Processamento de mensagens:
    Apache Kafka → event streaming de transações
    → Exatamente-uma-vez (exactly-once semantics)
    → Consumer groups por tipo de mensagem (auth, reversal, clearing)
    → Event sourcing: auditoria imutável de cada estado

  Estado distribuído:
    Redis Cluster (velocity, dedup, session)
    PostgreSQL com Citus (sharding por BIN para escala)
    → Write-ahead log para auditoria

  Observabilidade:
    OpenTelemetry → Jaeger (distributed tracing)
    Prometheus + Grafana (métricas)
    Loki (logs)
    → Rastrear uma transação em 50 microsserviços

Desafio principal: exatamente-uma-vez em sistemas financeiros
  → Kafka transactions + idempotency keys
  → Outbox pattern para garantia de entrega
```

**O que estudar:**
- `Designing Data-Intensive Applications` — Kleppmann (fundamental)
- `Building Event-Driven Microservices` — Adam Bellemare
- Apache Kafka documentation (confluent.io/learn)

---

### Payment Orchestration — Roteamento Inteligente

**Prioridade:** 🟠 Alta para fintechs/subadquirentes
**Pré-requisito:** Roteamento por BIN (semana 11) + fluxos avançados (semana 26)
**Horizonte:** 2-3 meses

```
O problema que orchestration resolve:
  Merchant com 3 adquirentes (Cielo, Stone, Getnet)
  → Qual usar para cada transação?

Critérios de roteamento:
  - Menor custo (MDR por bandeira por adquirente)
  - Maior taxa de aprovação histórica (por BIN, MCC, valor)
  - Menor latência (P95 por adquirente nas últimas 1h)
  - Disponibilidade (circuit breaker por adquirente)
  - Regras de negócio (débito sempre na Stone, crédito na Cielo)

Implementação:
  RouteScore = w1 * aprovação + w2 * (1/custo) + w3 * (1/latência)
  → Atualizado em tempo real com métricas de sliding window
  → Fallback automático se adquirente degradado
  → A/B testing de rotas com divisão de tráfego
```

---

## Trilha 3 — Mercado Emergente Brasil

### Open Finance — Iniciação de Pagamento (PISP)

**Prioridade:** 🔴 Alta — já regulamentado e em expansão no Brasil
**Pré-requisito:** Semana 25 (mercado brasileiro) + conhecimento de APIs REST
**Horizonte:** 3-4 meses

```
Atores do Open Finance em pagamentos:
  TPP  — Third Party Provider (a fintech que inicia o pagamento)
  PISP — Payment Initiation Service Provider
  ASPSP — Account Servicing Payment Service Provider (banco)

Fluxo técnico de uma iniciação de pagamento:
  1. TPP solicita consentimento do usuário (POST /consents)
  2. Usuário autentica no banco (redirect FAPI)
  3. TPP recebe access_token com escopo payments
  4. TPP cria pagamento (POST /payments/pix/payment-consents)
  5. Banco executa o PIX e retorna status

Segurança: FAPI 2.0 (Financial-grade API)
  → mTLS para todas as chamadas
  → DPoP (Demonstrating Proof of Possession)
  → PAR (Pushed Authorization Requests)
  → Diferente de OAuth2 comum — muito mais restritivo
```

**Recursos:**
- Especificação BR: `openbankingbrasil.org.br/apis`
- FAPI Security Profile: `openid.net/wg/fapi`
- Sandbox Bacen: `sandbox.api.raidiam.com`

---

### DREX — Infraestrutura Técnica do Real Digital

**Prioridade:** 🟡 Média — ainda em piloto, mas impacta o futuro
**Pré-requisito:** Semana 25 (mercado brasileiro) + conceitos de DLT
**Horizonte:** 2-3 meses para entender o modelo técnico

```
O que é DREX tecnicamente:
  - CBDC (Central Bank Digital Currency) do Bacen
  - DLT permissionada baseada em Hyperledger Besu (EVM-compatible)
  - Real tokenizado 1:1 com reservas no Bacen
  - Contratos inteligentes para liquidação atômica

Diferença vs PIX:
  PIX                          DREX
  ──────────────────────────────────────────
  Mensageria centralizada      DLT permissionada
  Liquidação em D+0            Liquidação atômica (DVP)
  Sem programabilidade         Smart contracts
  Instrument: crédito          Instrument: token
  Para retail                  Para atacado + retail (fase 2)

Caso de uso que muda tudo:
  → Delivery vs Payment (DVP) atômico
  → Compra de título público: entrega do título + pagamento
    acontecem no mesmo bloco — zero risco de contraparte

Smart contract de exemplo (Solidity — EVM):
  contract AtomicSwap {
      function swap(address buyer, address seller,
                    uint256 drexAmount, uint256 tokenId)
          external {
          drex.transferFrom(buyer, seller, drexAmount);
          nftBond.transferFrom(seller, buyer, tokenId);
      }
  }
```

---

## Trilha 4 — Especialização Avançada

### Caminho para QSA — Consultor PCI Certificado

**Prioridade:** 🟠 Alta para quem quer consultoria
**Pré-requisito:** Semana 27 (PCI-DSS) + experiência prática em compliance
**Horizonte:** 12-18 meses (inclui processo de certificação)

```
O que é um QSA (Qualified Security Assessor):
  → Empresa ou pessoa autorizada pelo PCI SSC
  → Conduz avaliações formais de conformidade PCI-DSS
  → Emite ROC (Report on Compliance)
  → Mercado: R$ 150-500/hora como consultor independente

Requisitos para certificação individual:
  1. PCIP (PCI Professional) — pré-requisito
  2. Background check pelo PCI SSC
  3. Treinamento QSA obrigatório (PCI SSC Training)
  4. Ser empregado/associado de empresa QSA qualificada
  5. Exame de certificação anual + re-certificação

Especializações valorosas:
  PA-QSA  — avalia aplicações de pagamento (software)
  P2PE QSA — avalia soluções de criptografia ponto-a-ponto
  3DS Assessor — avalia implementações 3DS

Oportunidade no Brasil:
  → Poucas empresas QSA atuando no país
  → Demanda crescente de fintechs que precisam de ROC
  → Combinação ISO 8583 + PCI-DSS é diferencial único
```

---

### ML em Antifraude — Além das Velocity Rules

**Prioridade:** 🟠 Alta para quem trabalha em risco
**Pré-requisito:** Fase 9 (antifraude) + Python básico
**Horizonte:** 4-6 meses

```
Evolução do antifraude por geração:

  1ª geração — Regras hardcoded:
    if (amount > 5000 && country != "BR") decline();
    Problema: fácil de contornar, muitos falsos positivos

  2ª geração — Velocity rules (o que o curso cobre):
    Redis sorted sets, janelas deslizantes, scoring
    Problema: não detecta padrões complexos novos

  3ª geração — ML supervisionado:
    Features: hora, valor, MCC, BIN, device, geolocalização
    Labels: fraude confirmada (chargeback) vs legítima
    Modelos: XGBoost, LightGBM (state-of-the-art para tabular)
    Treinamento offline, scoring online em < 5ms

  4ª geração — Graph analytics + DL:
    → Fraud rings: grafo onde PANs, IPs, devices são nós
    → Edge = "mesma transação usou ambos"
    → GraphSAGE detecta clusters de fraude coordenada

Features de alta importância em pagamentos:
  - Tempo desde última transação no mesmo PAN
  - Distância geográfica entre transação atual e anterior
  - Taxa de aprovação histórica do BIN nas últimas 24h
  - Velocidade de digitação (CNP) — biometria comportamental
  - Combinação MCC + valor + hora (vetorizado)

Stack de ML em produção:
  Treinamento: Python, XGBoost, MLflow (experiment tracking)
  Serving: ONNX runtime (interop Java) ou TorchServe
  Feature store: Redis (real-time) + Feast (batch)
  Monitoramento: drift detection (Evidently AI)
```

---

### Firmware de Terminal — Camada Mais Baixa do Stack

**Prioridade:** 🟢 Nicho mas altamente valorizado
**Pré-requisito:** Semana 17 (EMV) + C/C++ básico
**Horizonte:** 6-12 meses

```
O que é o kernel EMV:
  → Software embarcado no terminal (POS, ATM, celular)
  → Implementa EMV Book 1-4 + especificações por bandeira
  → Gera ARQC, valida ARPC, decide approved/declined offline
  → Certificado pelo EMVCo + bandeiras individualmente

Por que é difícil:
  → Spec EMV tem ~2.000 páginas + addenda por bandeira
  → Cada bandeira tem kernel próprio (Visa qVSDC, MC M/Chip)
  → Certificação leva 12-24 meses por bandeira
  → Hardware restrito (ARM Cortex-M, 256KB RAM)

Quem contrata:
  → Fabricantes de terminal (Ingenico, Verifone, PAX, ATOS)
  → Empresas de certificação (UL, Bureau Veritas)
  → Bandeiras (times de terminal specification)

Recursos:
  → EMV Specification Bulletin (emvco.com)
  → Visa payWave/VIS specification (acesso via Visa DPS)
  → Simulador: EMV Kernel Simulator (ferramentas internas das bandeiras)
```

---

## Trilha 5 — Negócio e Produto

### Product Management em Pagamentos

**Prioridade:** 🟡 Média — abre porta para roles de liderança
**Pré-requisito:** Curso completo (você precisa do técnico para ser um PM diferenciado)
**Horizonte:** 3-6 meses de imersão em produto

```
O que o PM de pagamentos precisa entender que outros PMs não entendem:

  Métricas de negócio que você já sabe calcular:
    → Approval rate (e por que 1% = R$ milhões)
    → Chargeback ratio (e os thresholds das bandeiras)
    → MDR, interchange, markup (e como cada player ganha)
    → Cost per transaction vs revenue per transaction

  Decisões técnicas com impacto direto em produto:
    → "Podemos aprovar offline?" → SAF tem risco financeiro
    → "Quanto custa adicionar parcelamento em 24x?" → clearing 24x
    → "Por que minha taxa de aprovação caiu?" → você sabe responder

  Frameworks de produto aplicados a pagamentos:
    → Jobs-to-be-done: o portador quer pagar, não "usar cartão"
    → Conversion funnel: clique → form → auth → captura → liquidação
    → Cohort analysis: aprovação por vintage de cliente

  Leitura obrigatória:
    → "The Anatomy of a Credit Card Transaction" (Stripe blog)
    → "How Visa Makes Money" (Net Interest Newsletter)
    → Relatório anual da Cielo, Stone, PagSeguro (RI)
```

---

### M&A Técnica — Due Diligence de Processadoras

**Prioridade:** 🟢 Nicho para consultores sêniores
**Pré-requisito:** Curso completo + experiência em produção
**Horizonte:** Emerge naturalmente da experiência

```
O que avaliar na due diligence técnica de uma processadora:

  Arquitetura:
    → Single point of failure? Onde?
    → Qual o MTTR histórico? (logs de incidente)
    → Versão do jPOS / framework — quando foi atualizado?
    → Débito técnico: quantas gambiarras em produção?

  Compliance:
    → Status PCI-DSS: SAQ ou ROC? Quando expira?
    → Certificações com bandeiras: quais, quando renovam?
    → Há PAN não mascarado em algum log de produção?

  Risco financeiro:
    → Chargeback ratio por bandeira nos últimos 12 meses
    → Reserva financeira para chargebacks pendentes
    → Contratos de garantia com adquirentes

  Perguntas que só um especialista ISO 8583 faz:
    → "Mostrem o reversal de uma transação de timeout do mês passado"
    → "Como vocês tratam late response?"
    → "Qual a estratégia de SAF de vocês?"
    → Respostas vagas = risco alto
```

---

## Mapa de Prioridades

```
SE você quer...                    ESTUDE...
─────────────────────────────────────────────────────────
Maior salário como engenheiro      Cloud-native + Kafka
Trabalhar em fintech moderna       Open Finance (FAPI)
Consultoria independente           QSA (PCI-DSS)
Entender o futuro do Brasil        DREX + ISO 20022
Trabalhar com antifraude           ML (XGBoost + grafos)
Sair do Brasil                     ISO 20022 + SWIFT gpi
Trabalhar com hardware/terminal    EMV Kernel (C/C++)
Virar CTO/VP                       Produto + M&A técnica
```

---

## Recursos Fundamentais (independente da trilha)

| Recurso | Tipo | Por que ler |
|---------|------|------------|
| `Designing Data-Intensive Applications` — Kleppmann | Livro | Fundação para qualquer sistema distribuído em escala |
| `Payment Systems in the U.S.` — Benson | Livro | Visão de negócio de pagamentos que engenheiros raramente têm |
| Nilson Report | Newsletter | Dados de mercado global — referência citada por todos |
| PYMNTS.com | Site | Notícias e análises do setor |
| Celent Reports | Relatórios | Análises técnicas de tendências (via acesso corporativo) |
| Bacen Open Data | API | Dados reais de transações PIX, arranjos, inadimplência |

---

*Este guia é um mapa, não um currículo. Aprofunde um tema de cada vez, construa em cima da base que você já tem, e publique o que aprender. O conhecimento que não é compartilhado não vira reputação.*
