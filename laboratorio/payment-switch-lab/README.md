# payment-switch-lab

Mini-switch de pagamentos ISO 8583 em Java com jPOS.

## Arquitetura

```
                    ┌─────────────────────────────┐
                    │     TransactionManager       │
                    │                               │
 Acquirer    ┌──────┤  QueryHost                    │
 Simulator   │      │  ValidateMessage              │
    │        │      │  ┌─ authorization ──────────┐ │
    │   QServer     │  │  CheckDuplicate          │ │    Issuer
    │    (8583)─────┤  │  RouteByBIN              │ ├───Simulator
    │        │      │  │  ForwardToIssuer (QMUX)  │ │   (8583)
    │        │      │  └──────────────────────────┘ │
    │        │      │  ┌─ reversal ───────────────┐ │
    │        │      │  │  FindOriginal            │ │
    │        │      │  │  ProcessReversal         │ │
    │        │      │  └──────────────────────────┘ │
    │        │      │  BuildResponse                │
    │        └──────┤  AuditLog                     │
                    └─────────────────────────────┘
```

## Módulos

| Módulo | Responsabilidade |
|--------|-----------------|
| `iso-core` | Utilitários: HexUtils, BcdUtils, BitmapParser, PANMasker, Luhn |
| `iso-packager` | Packager XML e MessageFactory |
| `txn-manager` | Participants do TransactionManager |
| `routing-engine` | BINTable e políticas de roteamento |
| `issuer-simulator` | Simulador de emissor com regras configuráveis |
| `acquirer-simulator` | Cliente que gera transações de teste |
| `installments` | Lógica de parcelamento |
| `reconciliation` | Reconciliação auth vs clearing |
| `observability` | Métricas e logging |
| `integration-tests` | Testes E2E |

## Quickstart — do zero ao primeiro 0800/0810 em 5 minutos

```bash
# 1. Build de todos os módulos
mvn clean package -DskipTests

# 2. Suba o stack completo (switch + issuer-simulator + acquirer-simulator)
docker-compose up -d

# 3. Verifique que o switch subiu (porta 8583)
docker-compose logs -f payment-switch-lab

# 4. Envie um echo (0800) via acquirer-simulator
docker-compose exec acquirer-simulator java -jar acquirer-simulator.jar --echo

# Esperado: 0810 com DE39=00 nos logs do switch
```

## Como rodar (sem Docker)

```bash
# Build
mvn clean package -DskipTests

# Terminal 1 — switch principal
java -jar txn-manager/target/payment-switch-lab.jar

# Terminal 2 — emissor simulado
java -jar issuer-simulator/target/issuer-simulator.jar

# Terminal 3 — adquirente simulado (gera transações de teste)
java -jar acquirer-simulator/target/acquirer-simulator.jar
```

## Docker

```bash
cd docker
docker-compose up -d

# Stack completo inclui:
# - payment-switch (porta 8583)
# - issuer-simulator (porta 8584)
# - acquirer-simulator
# - Prometheus (porta 9090) — metricas
# - Grafana (porta 3000) — dashboards (admin/admin)
```

## Configuração

- `src/dist/deploy/05_txnmgr.xml` — TransactionManager (participants e groups)
- `src/dist/deploy/10_channel.xml` — Canais TCP (NACChannel para emissor)
- `src/dist/deploy/20_mux.xml` — QMUX (correlação por STAN + Terminal)
- `src/dist/deploy/30_server.xml` — QServer (recebe conexões na 8583)
- `src/main/resources/cfg/iso87ascii.xml` — Packager ISO 8583:1987 ASCII
- `iso-packager/src/main/resources/bin-table.csv` — Tabela de BINs (14 rotas)

## Context Keys

O `ContextKeys.java` centraliza todas as chaves usadas no Context do TransactionManager:

```java
ContextKeys.REQUEST          // ISOMsg original
ContextKeys.RESPONSE         // ISOMsg de resposta
ContextKeys.RESPONSE_CODE    // DE39 quando não há ISOMsg
ContextKeys.DESTINATION_MUX  // QMUX destino (selecionado por RouteByBIN)
ContextKeys.IS_ON_US         // boolean — rota on-us?
ContextKeys.NETWORK          // bandeira (VISA, MASTERCARD, ELO)
ContextKeys.NEEDS_REVERSAL   // boolean — timeout do emissor?
```

## Exercícios por Semana

| Semana | O que implementar |
|--------|-------------------|
| 3 | `HexUtils`, `BcdUtils` (iso-core) |
| 6 | Packager XML customizado (iso-packager) |
| 7 | `QueryHost`, `ValidateMessage`, `BuildResponse`, `AuditLog` (txn-manager) |
| 8 | QMUX config, correlação STAN + Terminal |
| 9 | Validações por campo: Luhn (DE2), Amount (DE4), POS Entry Mode (DE22) |
| 10 | `ForwardToIssuer` + `IssuerSimulator` |
| 11 | `RouteByBIN`, `BINTable.lookup()`, `InstallmentParser` |
| 13 | Auto-reversal por timeout em `ForwardToIssuer.abort()` |
| 16 | `CheckDuplicate` com Caffeine cache |
| 18 | `PANUtils.mask()` (PCI compliant) |
| 21 | `SwitchMetrics` com Micrometer + Prometheus |
| 23 | `ReconciliationEngine` — auth vs clearing |
| 33 | Load testing com `ISOLoadTester` + RateLimiter |
