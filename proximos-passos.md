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

## Trilha 2 — Go e Python no Ecossistema ISO 8583

Java/jPOS é o padrão em bancos e processadoras tradicionais. Fintechs modernas e times de risco usam Go e Python. Conhecer as três linguagens te permite transitar entre qualquer empresa do setor — e as três têm bibliotecas ISO 8583 compatíveis.

A referência de biblioteca usada aqui é a mesma família `moov-io` para Go e `pyiso8583` para Python — ambas seguem a mesma filosofia: spec configurável por campo, pack/unpack simétrico, sem dependências pesadas.

---

### Go — Fintechs de Alto Volume

**Prioridade:** 🔴 Alta — Nubank, Pismo, PicPay, Stripe usam Go em pagamentos
**Pré-requisito:** Java do curso é suficiente para a transição
**Horizonte:** 2-3 meses para produtividade real
**Biblioteca:** `github.com/moov-io/iso8583`

#### Setup

```bash
go mod init payment-switch
go get github.com/moov-io/iso8583
```

#### Spec — equivalente ao packager XML do jPOS

```go
// spec/brasil.go
package spec

import (
    "github.com/moov-io/iso8583/encoding"
    "github.com/moov-io/iso8583/field"
    "github.com/moov-io/iso8583/prefix"
    iso "github.com/moov-io/iso8583"
)

var Brasil = &iso.MessageSpec{
    Name: "ISO 8583 Brasil",
    Fields: map[int]field.Spec{
        0: {Description: "MTI",        Length: 4,  Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        2: {Description: "PAN",        Length: 19, Enc: encoding.ASCII, Pref: prefix.ASCII.LL},
        3: {Description: "Proc Code",  Length: 6,  Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        4: {Description: "Amount",     Length: 12, Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        11: {Description: "STAN",      Length: 6,  Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        39: {Description: "Resp Code", Length: 2,  Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        41: {Description: "Terminal",  Length: 8,  Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        42: {Description: "Merchant",  Length: 15, Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
        49: {Description: "Currency",  Length: 3,  Enc: encoding.ASCII, Pref: prefix.ASCII.Fixed},
    },
}
```

#### Auth Request 0200 — mesmo fluxo da semana 10

```go
// Montar 0200 (equivalente ao ISOMsg em jPOS)
func BuildAuthRequest(pan, amount, stan, terminal, merchant string) (*iso8583.Message, error) {
    msg := iso8583.NewMessage(spec.Brasil)
    if err := msg.MTI("0200"); err != nil {
        return nil, err
    }
    msg.Field(2, pan)
    msg.Field(3, "000000")   // compra à vista
    msg.Field(4, amount)     // 12 dígitos, centavos: R$150,00 = "000000015000"
    msg.Field(11, stan)
    msg.Field(41, terminal)
    msg.Field(42, merchant)
    msg.Field(49, "986")     // BRL
    return msg, nil
}

// Pack para envio TCP
func PackMessage(msg *iso8583.Message) ([]byte, error) {
    packed, err := msg.Pack()
    if err != nil {
        return nil, fmt.Errorf("pack error: %w", err)
    }
    // Header de 4 bytes com tamanho (mesmo padrão NACChannel do jPOS)
    header := make([]byte, 4)
    binary.BigEndian.PutUint32(header, uint32(len(packed)))
    return append(header, packed...), nil
}
```

#### Unpack da resposta 0210

```go
func ParseAuthResponse(raw []byte) (responseCode string, authCode string, err error) {
    msg := iso8583.NewMessage(spec.Brasil)
    if err = msg.Unpack(raw[4:]); err != nil { // pula header de 4 bytes
        return
    }
    mti, _ := msg.GetMTI()
    if mti != "0210" {
        err = fmt.Errorf("MTI inesperado: %s", mti)
        return
    }
    responseCode, _ = msg.GetString(39)
    authCode, _ = msg.GetString(38)
    return
}
```

#### Concorrência — goroutines vs threads jPOS

```go
// jPOS: TransactionManager cria threads por sessão
// Go: goroutines são muito mais leves (~2KB vs ~1MB por thread)

func ProcessTransactions(requests <-chan *iso8583.Message, results chan<- *iso8583.Message) {
    sem := make(chan struct{}, 500) // semáforo: max 500 goroutines simultâneas
    for req := range requests {
        sem <- struct{}{}
        go func(r *iso8583.Message) {
            defer func() { <-sem }()
            resp, err := forwardToIssuer(r)
            if err != nil {
                resp = buildDecline(r, "91") // emissor indisponível
            }
            results <- resp
        }(req)
    }
}

// Timeout — equivalente ao mux.request(req, 30000) do jPOS
func forwardToIssuer(req *iso8583.Message) (*iso8583.Message, error) {
    ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
    defer cancel()
    // envio via conn TCP + aguarda correlação por STAN
    return issuerConn.Send(ctx, req)
}
```

#### Onde Go é usado em pagamentos no Brasil

```
Nubank   — core bancário, microsserviços de autorização e fraude
Pismo    — core banking SaaS (adquirido pelo Visa em 2023)
PicPay   — gateway de pagamentos e carteira digital
Conductor — emissor de cartões (ex-Visa)
```

---

### Python — Risco, Analytics e Automação

**Prioridade:** 🔴 Alta para times de risco, data science e automação
**Pré-requisito:** Fase 9 (antifraude) para contextualizar os casos de uso
**Horizonte:** 2-3 meses para uso focado em pagamentos
**Biblioteca:** `pyiso8583` — mesma filosofia spec/pack/unpack da moov-io

#### Setup

```bash
pip install pyiso8583 pandas xgboost scikit-learn
```

#### Spec — mesmo conceito do packager, em dicionário Python

```python
# spec/brasil.py
from pyiso8583.specs import default_ascii

# Estende a spec padrão ASCII com campos brasileiros
BRASIL_SPEC = {**default_ascii}

# Customiza comprimentos e tipos conforme spec Elo/Visa BR
BRASIL_SPEC["t"]  = {"data_enc": "ascii", "len_type": 0, "max_len": 4}   # MTI
BRASIL_SPEC["2"]  = {"data_enc": "ascii", "len_type": 2, "max_len": 19}  # PAN (LLVAR)
BRASIL_SPEC["3"]  = {"data_enc": "ascii", "len_type": 0, "max_len": 6}   # Processing Code
BRASIL_SPEC["4"]  = {"data_enc": "ascii", "len_type": 0, "max_len": 12}  # Amount
BRASIL_SPEC["11"] = {"data_enc": "ascii", "len_type": 0, "max_len": 6}   # STAN
BRASIL_SPEC["39"] = {"data_enc": "ascii", "len_type": 0, "max_len": 2}   # Response Code
BRASIL_SPEC["41"] = {"data_enc": "ascii", "len_type": 0, "max_len": 8}   # Terminal ID
BRASIL_SPEC["42"] = {"data_enc": "ascii", "len_type": 0, "max_len": 15}  # Merchant ID
```

#### Montar e parsear 0200/0210 — mesmos campos da semana 10

```python
import pyiso8583
from spec.brasil import BRASIL_SPEC

# Montar auth request — equivalente ao ISOMsg.set() do jPOS
def build_auth_request(pan: str, amount: str, stan: str,
                        terminal: str, merchant: str) -> bytes:
    msg = {
        "t":  "0200",
        "2":  pan,
        "3":  "000000",   # compra à vista
        "4":  amount,     # centavos: R$150,00 = "000000015000"
        "11": stan,
        "41": terminal,
        "42": merchant,
        "49": "986",      # BRL
    }
    _, packed = pyiso8583.encode(msg, BRASIL_SPEC)
    return packed

# Parsear resposta 0210
def parse_auth_response(raw: bytes) -> dict:
    decoded, _ = pyiso8583.decode(raw, BRASIL_SPEC)
    assert decoded["t"] == "0210", f"MTI inesperado: {decoded['t']}"
    return {
        "response_code": decoded.get("39"),
        "auth_code":     decoded.get("38"),
        "stan":          decoded.get("11"),
    }
```

#### Caso de uso 1 — Analisar dump de mensagens ISO 8583 de produção

```python
import pandas as pd

# Processar arquivo de log com mensagens capturadas
def analyze_decline_dump(log_file: str) -> pd.DataFrame:
    records = []
    with open(log_file) as f:
        for line in f:
            raw = bytes.fromhex(line.strip())
            try:
                decoded, _ = pyiso8583.decode(raw, BRASIL_SPEC)
                if decoded.get("t") in ("0110", "0210"):
                    records.append({
                        "mti":           decoded["t"],
                        "pan_masked":    decoded["2"][:6] + "****" + decoded["2"][-4:],
                        "amount":        int(decoded.get("4", "0")) / 100,
                        "response_code": decoded.get("39"),
                        "terminal":      decoded.get("41"),
                        "merchant":      decoded.get("42"),
                    })
            except Exception:
                continue

    df = pd.DataFrame(records)
    print(df.groupby("response_code")["amount"].agg(["count", "sum"]))
    return df
```

#### Caso de uso 2 — Velocity rules em Python (mesmo conceito da semana 31)

```python
import redis
import time

r = redis.Redis()

def check_velocity(pan: str, amount_cents: int) -> dict:
    now = int(time.time())
    key_count  = f"vel:count:{pan}"
    key_amount = f"vel:amount:{pan}"

    pipe = r.pipeline()
    # Janela deslizante de 1 hora — mesmo padrão do Redis sorted set
    pipe.zremrangebyscore(key_count, 0, now - 3600)
    pipe.zadd(key_count, {f"{now}:{id(pan)}": now})
    pipe.zcard(key_count)
    pipe.zremrangebyscore(key_amount, 0, now - 3600)
    pipe.zadd(key_amount, {str(now): amount_cents})
    pipe.zscore(key_amount, str(now))
    results = pipe.execute()

    txn_count = results[2]
    # soma acumulada (simplificado)
    total_amount = sum(
        int(r.zscore(key_amount, m) or 0)
        for m in r.zrange(key_amount, 0, -1)
    )
    return {
        "blocked":      txn_count > 10 or total_amount > 500_00,
        "txn_count_1h": txn_count,
        "amount_1h":    total_amount / 100,
    }
```

#### Caso de uso 3 — ML para score de fraude (semana 31 em produção)

```python
import xgboost as xgb
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import precision_score, recall_score

# Features derivadas dos campos ISO 8583 que você já conhece
def extract_features(df: pd.DataFrame) -> pd.DataFrame:
    return pd.DataFrame({
        "amount":          df["4"].astype(int) / 100,
        "hour":            pd.to_datetime(df["7"], format="%m%d%H%M%S").dt.hour,
        "mcc":             df["18"].astype(int),
        "entry_mode":      df["22"].str[:2].astype(int),  # DE22 — semana 9
        "is_cnp":          df["22"].str[0] == "0",         # CNP se POS entry 01/10
        "bin":             df["2"].str[:6].astype(int),
        "terminal_id":     df["41"].str.strip(),
    })

# Treinar modelo — label: chargeback confirmado (reason code 10.x)
X = extract_features(transactions)
y = transactions["is_fraud"]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

model = xgb.XGBClassifier(
    n_estimators=500,
    max_depth=6,
    learning_rate=0.05,
    scale_pos_weight=99,  # ~1% de fraude no dataset
    eval_metric="aucpr",
)
model.fit(X_train, y_train, eval_set=[(X_test, y_test)])

# Precision vs Recall — conceito da semana 32
y_pred = model.predict(X_test)
print(f"Precision: {precision_score(y_test, y_pred):.3f}")  # falsos positivos
print(f"Recall:    {recall_score(y_test, y_pred):.3f}")     # fraude passando

# Exportar para ONNX — scoring em produção dentro do Java/Go
import onnxmltools
onnx_model = onnxmltools.convert_xgboost(model)
onnxmltools.save_model(onnx_model, "fraud_model.onnx")
```

---

### Comparativo — Java/jPOS vs Go vs Python

```
Conceito do curso      Java/jPOS              Go                    Python
────────────────────────────────────────────────────────────────────────────
Spec de campos         packager XML           MessageSpec struct     dicionário BRASIL_SPEC
Montar mensagem        ISOMsg.set(2, pan)     msg.Field(2, pan)      msg["2"] = pan
Pack/Unpack            msg.pack() / unpack()  msg.Pack() / Unpack()  encode() / decode()
TransactionParticipant interface prepare()    interface Execute()    função Python pura
Correlação STAN        QMUX interno           channel + map          asyncio + dict
Velocity rules         Redis + Java           Redis + Go             Redis + Python
Score de fraude        FraudScreeningPart.    middleware Go          XGBoost → ONNX
```

**Onde cada linguagem domina:**

```
Java/jPOS → Switch de produção completo, bancos e processadoras tradicionais
Go        → Microsserviços de autorização, gateways de alto TPS, fintechs
Python    → Antifraude com ML, analytics de clearing, scripts de automação
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
Trabalhar em fintech moderna       Go (moov-io/iso8583)
Maior salário como engenheiro      Go + ISO 20022
Consultoria independente           QSA (PCI-DSS)
Entender o futuro do Brasil        DREX + ISO 20022
Trabalhar com antifraude/ML        Python (XGBoost + pyiso8583)
Analytics e reconciliação          Python (pandas + pyiso8583)
Sair do Brasil                     ISO 20022 + Go + SWIFT gpi
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
