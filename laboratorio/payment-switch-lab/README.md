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

## Como rodar

```bash
# Build
mvn clean package

# Rodar switch
java -jar switch/target/switch.jar

# Rodar issuer simulator (outra janela)
java -jar issuer-simulator/target/issuer-sim.jar

# Rodar acquirer simulator (enviar transações de teste)
java -jar acquirer-simulator/target/acquirer-sim.jar
```

## Docker

```bash
docker-compose up -d
```

## Configuração

- `deploy/05_txnmgr.xml` — TransactionManager (participants e groups)
- `deploy/10_channel.xml` — Canais TCP
- `deploy/20_mux.xml` — QMUX (correlação)
- `deploy/30_server.xml` — QServer (recebe conexões)
- `cfg/iso87ascii.xml` — Packager
- `cfg/bin-table.csv` — Tabela de BINs
