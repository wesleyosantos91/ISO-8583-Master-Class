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

## Trilha 2 — Frameworks e Outras Linguagens

O ecossistema de pagamentos não é exclusivo do Java/jPOS. Conhecer os frameworks e linguagens usados por outras empresas do setor amplia oportunidades e permite contribuir em projetos internacionais.

### Go — A Linguagem das Fintechs Modernas

**Prioridade:** 🔴 Alta — Nubank, Stripe, Square, Mercado Pago usam Go extensivamente
**Pré-requisito:** Lógica de programação sólida (Java do curso é suficiente)
**Horizonte:** 2-3 meses para produtividade básica

```
Por que Go dominou fintechs:
  → Compilado, tipado, sem JVM overhead
  → Goroutines: concorrência nativa para alto volume de I/O
  → Tempo de startup < 50ms (crítico para lambdas/serverless)
  → Binário único, deploy trivial em container

Equivalências com o que você já sabe (Java → Go):
  TransactionParticipant   → interface com método Execute()
  CompletableFuture        → goroutine + channel
  synchronized block       → sync.Mutex / sync.RWMutex
  Optional<T>              → (T, error) — idioma Go
  ThreadPoolExecutor       → worker pool com goroutines

Implementação de parser ISO 8583 em Go:
  // Biblioteca principal: moov-io/iso8583
  import "github.com/moov-io/iso8583"

  spec := iso8583.NewSpec(...)
  msg := iso8583.NewMessage(spec)
  msg.MTI("0200")
  msg.Field(2, "4111111111111111")   // PAN
  msg.Field(4, "000000015000")       // Amount
  packed, err := msg.Pack()

Frameworks de switch em Go:
  moov-io/iso8583  — biblioteca ISO 8583 amplamente usada
  moov-io/wire     — mensageria SWIFT/Fedwire
  google/wire      — injeção de dependência (substituição do Spring DI)
```

**Onde Go é usado em pagamentos no Brasil:**
- Nubank — core bancário e microsserviços de autorização
- PicPay — gateway de pagamentos
- Pismo — core banking SaaS (adquirido pelo Visa)

---

### Python — Antifraude, Analytics e Automação

**Prioridade:** 🔴 Alta para quem quer trabalhar com risco/fraude
**Pré-requisito:** Fase 9 (antifraude) para contextualizar os casos de uso
**Horizonte:** 2-3 meses para uso em pagamentos

```
Onde Python entra no ecossistema de pagamentos:

  1. Antifraude e ML:
     → Treinamento de modelos (scikit-learn, XGBoost, LightGBM)
     → Feature engineering em datasets de transações
     → Análise de fraude histórica (pandas + jupyter)

  2. Reconciliação e analytics:
     → Processar arquivos de clearing (CSV, XML, CNAB)
     → Cruzar bases de dados entre sistemas legados
     → Relatórios financeiros automáticos

  3. Automação e testes:
     → Scripts de load test (locust.io)
     → Parsers de dump de mensagens ISO 8583
     → Automação de certificação (envio de test deck)

Biblioteca ISO 8583 em Python:
  # pyiso8583 — parser/packer puro Python
  import pyiso8583
  from pyiso8583.specs import default_ascii as spec

  raw = b"02004000000000000000..."
  decoded, encoded = pyiso8583.decode(raw, spec)
  print(decoded["t"])   # MTI: 0200
  print(decoded["2"])   # PAN

Pipeline de ML para antifraude (exemplo):
  import xgboost as xgb
  import pandas as pd

  # Features: hora, valor, MCC, BIN, país, device_type
  X = transactions[["hour", "amount", "mcc", "bin_country", ...]]
  y = transactions["is_fraud"]  # label: chargeback confirmado

  model = xgb.XGBClassifier(
      n_estimators=500,
      max_depth=6,
      learning_rate=0.05,
      scale_pos_weight=99   # dataset desbalanceado (1% fraude)
  )
  model.fit(X_train, y_train)
  # Score em produção via ONNX (interop com Java/Go)
```

---

### Kotlin — O Futuro do Ecossistema Java/jPOS

**Prioridade:** 🟠 Média — interop total com Java, adoção crescente em fintechs
**Pré-requisito:** Java sólido (você já tem)
**Horizonte:** 1-2 meses (transição suave para quem já domina Java)

```
Por que Kotlin em pagamentos:
  → 100% interoperável com Java — roda no mesmo jPOS
  → Null safety nativa: elimina NPE em campos ISO opcionais
  → Coroutines: melhor que threads para I/O assíncrono
  → Data classes: modelos de mensagem mais limpos
  → Extension functions: adicionar métodos ao ISOMsg sem herança

Exemplo: TransactionParticipant em Kotlin vs Java

  // Java (verboso)
  public class ValidationParticipant implements TransactionParticipant {
      public int prepare(long id, Serializable ctx) {
          Context context = (Context) ctx;
          ISOMsg req = (ISOMsg) context.get("REQUEST");
          String pan = req.getString(2);
          if (pan == null || pan.isEmpty()) return ABORTED;
          return PREPARED;
      }
  }

  // Kotlin (conciso, null-safe)
  class ValidationParticipant : TransactionParticipant {
      override fun prepare(id: Long, ctx: Serializable): Int {
          val context = ctx as Context
          val req = context["REQUEST"] as ISOMsg
          val pan = req.getString(2) ?: return ABORTED
          return PREPARED
      }
  }

Coroutines para chamadas ao emissor:
  suspend fun forwardToIssuer(req: ISOMsg): ISOMsg? =
      withTimeout(30_000) {
          mux.request(req, 30_000)
      }
  // Sem bloquear thread — 10x mais eficiente que thread pool
```

---

### Rust — Alta Performance e Segurança de Memória

**Prioridade:** 🟢 Nicho — mas emergindo em infraestrutura crítica de pagamentos
**Pré-requisito:** Sólida base em C/sistemas (ou muito interesse em aprender)
**Horizonte:** 6-12 meses para produtividade real

```
Onde Rust aparece em pagamentos:
  → HSM e criptografia de baixo nível (substituindo C)
  → Parsers de protocolo de alta performance
  → Firmware de terminal (sem GC, sem runtime overhead)
  → Infraestrutura de rede (substituindo C++ em load balancers)

Vantagem única para pagamentos:
  → Sem garbage collector → sem GC pause → P99 previsível
  → Memory safety em tempo de compilação → menos CVEs
  → FFI com C → integra com SDKs de HSM legados

Parser ISO 8583 em Rust (exemplo):
  use iso8583_rs::prelude::*;

  let spec = Spec::new(/* definição de campos */);
  let raw: &[u8] = &[0x02, 0x00, /* ... */];
  let msg = Message::parse(raw, &spec)?;
  let pan = msg.field(2)?;  // Result<&str, Error>
```

---

### Comparativo: Quando Usar Cada Linguagem

```
Linguagem   Melhor para em pagamentos           Usado por
──────────────────────────────────────────────────────────
Java/jPOS   Switch completo, protocolo ISO 8583  Cielo, Rede, bancos tradicionais
Go          Microsserviços, gateway de alto TPS  Nubank, Pismo, Stripe
Python      ML/antifraude, analytics, scripts    Times de risco, data science
Kotlin      Migração de Java, Android Pay        Fintechs modernas com JVM
Rust        HSM, firmware, infraestrutura crítica Fabricantes de terminal, infra
C/C++       Kernel EMV, terminal firmware        Ingenico, Verifone, PAX
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
Trabalhar em fintech moderna       Go + Open Finance (FAPI)
Maior salário como engenheiro      Go ou Kotlin + ISO 20022
Consultoria independente           QSA (PCI-DSS)
Entender o futuro do Brasil        DREX + ISO 20022
Trabalhar com antifraude           Python (ML) + XGBoost
Sair do Brasil                     ISO 20022 + Go + SWIFT gpi
Trabalhar com hardware/terminal    EMV Kernel (C/C++) + Rust
Migrar o Java existente            Kotlin (transição suave)
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
